package me.vadim.archive;

import com.google.common.net.InternetDomainName;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.vadim.archive.util.ConsumerJob;
import me.vadim.archive.util.LocalExecutors;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v118.network.Network;
import org.openqa.selenium.devtools.v118.network.model.RequestId;
import org.openqa.selenium.devtools.v118.network.model.Response;
import org.openqa.selenium.devtools.v118.network.model.ResponseReceived;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * todo list:
 * 	- auto scroll
 * 	- smart scanning of links already downloaded
 * 		- think: mime type by extension (or lack thereof)
 * 			- don't visit known mime type links by extension
 * 			- if unknown, attempt to scrape
 * 			- otherwise initiate background download (still add to ss)
 * 		- think: images	downloaded by the page don't need to be opened individually
 * 	- maybe check if toLocal already exists or not before fetching
 * 	- rate limiting?
 *
 * @author vadim
 */
public class Mirror {

	private static final int PAD_DIGITS = 5;

	private static String removeAnchor(String url) {
		try {
			URI uri = new URI(url);
			URI raw = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), null);
			return raw.toString();
		} catch (URISyntaxException e) {
			return url;
		}
	}

	//@formatter:off
	// JDK code from java.io.File //
	private static String slashify(String path, boolean isDirectory) {
		String p = path;
		if (File.separatorChar != '/')
			p = p.replace(File.separatorChar, '/');
		if (!p.startsWith("/"))
			p = "/" + p;
		if (!p.endsWith("/") && isDirectory)
			p = p + "/";
		return p;
	}
	//@formatter:on

	private static String domain(URL url) {
		if (!url.getHost().isBlank())
			return InternetDomainName.from(url.getHost()).topPrivateDomain().toString();
		else
			return null;
	}

	private static boolean shouldParse(String url) {
		if (url == null)
			return false;

		switch (Optional.ofNullable(URLConnection.guessContentTypeFromName(url))
						.map(s -> s.split("/")[0])
						.orElse("text")
						.toLowerCase()
		) {
			case "image":
			case "audio":
			case "video":
				return false;
			case "text":
			case "application":
			default:
				return true;
		}
	}

	private static final int initalCapacity = 5000;
	private static final String reslog = "responses.json";
	private static final String arclog = "archived.json";

	private final ChromeDriver driver;
	private final File cachedir;
	private final Set<String> ext = new HashSet<>(initalCapacity); // to scrape
	private final Set<String> arc = new HashSet<>(initalCapacity); // already archived
	private final Map<String, ResponseSnapshot> ss = new HashMap<>(initalCapacity); // response snapshots


	private final ExecutorService io = LocalExecutors.newExtendedThreadPool(Runtime.getRuntime().availableProcessors());
	private final Map<String, PartialResponse> rpool = new HashMap<>(initalCapacity); // req,res,body lifecycle pool
	private final Object lock_rpool = new Object();

	private PartialResponse partial(RequestId id) {
		synchronized (lock_rpool) {
			PartialResponse partial = rpool.get(id.toJson());
			if (partial == null) {
				partial = new PartialResponse(id);
				rpool.put(id.toJson(), partial);
			}
			return partial;
		}
	}

	public Mirror(ChromeDriver driver, File cachedir) {
		this.driver   = driver;
		this.cachedir = cachedir;

		DevTools devTools = driver.getDevTools();

		devTools.createSession();
		devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.of(100000000)));
		devTools.addListener(Network.requestWillBeSent(), req -> {
			PartialResponse partial = partial(req.getRequestId());
			partial.setRequest(req);
			partial.latch.countDown();

			//System.out.println("RequestWillBeSent " + req.getRequestId() + " (" + partial + ")");
		});
		devTools.addListener(Network.responseReceived(), res -> {
			PartialResponse partial = partial(res.getRequestId());
			partial.setResponse(res);
			partial.latch.countDown();

			//System.out.println("ResponseReceived " + res.getRequestId() + " (" + partial + ")");
		});
		devTools.addListener(Network.loadingFinished(), entry -> {
			PartialResponse partial = partial(entry.getRequestId());

			partial.setEntry(entry);
			partial.latch.countDown();

			//System.out.println("LoadingFinished " + entry.getRequestId() + " (" + partial + ")");

			io.submit(new ConsumerJob<>(partial, this::dump)); // this isn't always called last
		});
	}

	private File toLocal(URL url) {
		return new File(cachedir, new File(url.getHost(), url.getFile()).getPath());
	}

	private URL toRemote(URL url) {
		if (url == null)
			return null;

		if (!url.getProtocol().equalsIgnoreCase("file"))
			return url;

		File f, j, n;
		try {
			f = Paths.get(url.toURI()).toFile();
		} catch (Exception e) {
			return url;
		}

		String host = null;
		j = new File("");
		do {
			n = f.getParentFile();

			if (n.getName().equalsIgnoreCase(cachedir.getName()))
				break;
			else
				j = new File(f.getName(), j.getPath());

			host = n.getName();
		}
		while ((f = n).getParentFile() != null);

		if (host == null)
			return url;

		try {
			return new URI("http", host, slashify(j.getPath(), j.isDirectory()), null).toURL();
		} catch (URISyntaxException | MalformedURLException e) {
			return url;
		}
	}

	private void await(long sec) {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(sec)).until(__ -> rpool.isEmpty());
		} catch (TimeoutException e) {
			System.err.println("Warning: timeout when waiting for page load (" + sec + "s)");
			rpool.forEach((a, b) -> b.latch.abort());
			rpool.clear();
		}
	}

	public void archive(String orig) {
		final String host;
		try {
			URL original = new URL(orig);
			host = domain(original);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Invalid origin URL: " + orig, e);
		}

		if (host == null)
			throw new IllegalArgumentException("Invalid URL: " + orig);

		Stack<String> stack = new Stack<>();
		stack.push(orig);
		long i = 0, szC = 1, szM = 1;
		while (!stack.isEmpty()) { // avoid StackOverFlowError
			String url = stack.pop();
			String get = url;

			URL u;
			try {
				u = new URL(url);
			} catch (MalformedURLException e) {
				System.err.println("Warning: Unable to archive malformed URL: " + url);
				continue;
			}

			if (u.getProtocol().equalsIgnoreCase("file")) { // file -> url
				boolean resolve = false; // resolve to remote URL ?

				File f;
				try {
					f = new File(u.toURI());
					if (!f.exists())
						resolve = true;
				} catch (URISyntaxException e) {
					resolve = true;
				}

				url = toRemote(u).toString();
				if (resolve)
					get = url;
				else
					get = u.toString(); // file
			} else { // url -> file
				File f = toLocal(u);
				if (f.exists()) // no need to re-download
					try {
						get = f.toURI().toURL().toString();
					} catch (MalformedURLException e) {
						System.err.println("Warning: Malformed URL: " + f.toURI());
					}
			}

			System.out.println(String.format("+%d ", Math.max(0, ((szC = stack.size()) + i) - szM)) +
							   String.format(" (%d/%d) ", ++i, (szM = Math.max(szM, szC + i))) +
							   " " + url);

			if (shouldParse(url)) {
				driver.get(get);
				await(40);
				scrape();
			} else {
				io.submit(new ConsumerJob<>(get, this::download));
			}

			ext.remove(url);
			arc.add(url); // it's been fetched and awaited, so this url is definitely archived

			Iterator<String> iter = ext.iterator();
			while (iter.hasNext()) {
				String ex = iter.next();

				URL x;
				try {
					x = new URL(ex);
				} catch (MalformedURLException e) {
					System.err.println("Warning: Skipping malformed URL: " + ex);
					iter.remove();
					continue;
				}

				if (host.equalsIgnoreCase(domain(toRemote(x)))) // recurse only over this host
					stack.push(ex);

				iter.remove(); // removing them from `ext` is not the same thing as adding them to `arc`
			}
		}

		Iterator<String> iter = ext.iterator();
		while (iter.hasNext()) { // do not scrape remaining links
			String s = iter.next();

			URL url;
			try {
				url = new URL(s);
			} catch (MalformedURLException e) {
				System.err.println("Warning: Skipping malformed URL: " + s);
				iter.remove();
				continue;
			}
			if (toLocal(url).exists()) { // no need to re-download
				iter.remove();
				continue;
			}

			System.out.println("CACHE " + s);
			driver.get(s);
			await(60);
			arc.add(s); // archive but no scrape or recursion
			iter.remove();
		}
	}

	private void scrape() {
		for (URIElement uri : URIElement.values()) {
			for (WebElement elem : driver.findElements(By.tagName(uri.getTag()))) {
				for (String attr : uri.getAttrs()) {
					String value = elem.getAttribute(attr);
					if (value != null && !value.isBlank())
						ext.add(removeAnchor(value));
				}
//				if (uri.isMulti()) {
//					for (String attr : uri.getMulti()) {
//						String value = elem.getAttribute(attr);
//						if (value != null && !value.isBlank())
//							ext.addAll(Arrays.stream(value.split(",")).map(Mirror::removeAnchor).collect(Collectors.toList()));
//					}
//				}
			}
		}
		ext.removeAll(arc);
	}

	public void save() {
		System.out.println("Info: Save");
		Gson gson = new GsonBuilder().serializeNulls().create();

		reslog:
		{
			byte[] resp = gson.toJson(ss).getBytes(StandardCharsets.UTF_8);
			try {
				File file = new File(cachedir, reslog);
				file.getParentFile().mkdirs();
				Files.write(file.toPath(), resp, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
			} catch (IOException e) {
				System.err.println("Error: Unable to save response log. Dumping to console...");
				System.err.println();
				System.err.println(new String(resp, StandardCharsets.UTF_8));
				System.err.println();
				e.printStackTrace();
			}
		}

		arclog:
		{
			if (true) break arclog;
			byte[] done = gson.toJson(arc).getBytes(StandardCharsets.UTF_8);
			try {
				File file = new File(cachedir, arclog);
				file.getParentFile().mkdirs();
				Files.write(file.toPath(), done, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
			} catch (IOException e) {
				System.err.println("Error: Unable to save archive list. Dumping to console...");
				System.err.println();
				System.err.println(new String(done, StandardCharsets.UTF_8));
				System.err.println();
				e.printStackTrace();
			}
		}
	}

	public void load() {
		System.out.println("Info: Load");
		Gson gson = new GsonBuilder().serializeNulls().create();

		reslog:
		{
			try {
				File file = new File(cachedir, reslog);
				if (!file.exists())
					break reslog;
				byte[] resp = Files.readAllBytes(file.toPath());

				Map<String, ResponseSnapshot> map = gson.fromJson(new String(resp, StandardCharsets.UTF_8), TypeToken.getParameterized(Map.class, String.class, ResponseSnapshot.class).getType());
				if (map != null)
					ss.putAll(map);
			} catch (IOException e) {
				System.err.println("Error: Unable to read response log. Exiting...");
				e.printStackTrace();
				System.exit(1);
			}
		}

		arclog:
		{
			if (true) break arclog;
			try {
				File file = new File(cachedir, arclog);
				if (!file.exists())
					break arclog;
				byte[] done = Files.readAllBytes(file.toPath());

				List<String> list = gson.fromJson(new String(done, StandardCharsets.UTF_8), TypeToken.getParameterized(List.class, String.class).getType());
				if (list != null)
					arc.addAll(list);
			} catch (IOException e) {
				System.err.println("Error: Unable to read archive log. Exiting...");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	public void shutdown() {
		io.shutdown();
		save();
		driver.quit();
		System.out.println("Info: Quit");
	}

	BiFunction<WebDriver, Integer, Integer> statusCodeResolver;

	public void setStatusCodeResolver(BiFunction<WebDriver, Integer, Integer> resolver) {
		this.statusCodeResolver = resolver;
	}

	private void download(String url) {
		URL u;
		try {
			u = new URL(url);
		} catch (MalformedURLException e) {
			System.err.println("Warning: Cannot download malformed URL: " + url);
			return;
		}

		if (u.getProtocol().equalsIgnoreCase("file"))
			return;

		File f = toLocal(u);
		if (f.exists()) // no need to re-download
			return;

		// goofy ahh code
		//http://www.tbray.org/ongoing/When/201x/2012/01/17/HttpURLConnection

		HttpURLConnection con = null;
		try {
			con = (HttpURLConnection) u.openConnection();
			con.connect();

			try (
					ReadableByteChannel rbc = Channels.newChannel(con.getInputStream());
					FileOutputStream fos = new FileOutputStream(f);
			) {
				if (con.getResponseCode() / 100 == 2)
					fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			}

			ss.put(url,
				   new ResponseSnapshot(url,
										con.getURL().toString(),
										con.getRequestMethod(),
										con.getResponseCode(),
										con.getResponseMessage(),
										con.getHeaderFields(),
										con.getContentType(),
										con.getContentLengthLong())
				  );
		} catch (IOException e) {
			System.err.println("Error: Exception when downloading: " + url);
			e.printStackTrace();
		} finally {
			if (con != null)
				con.disconnect();
		}
	}

	private void dump(PartialResponse partial) {
		try {
			//System.out.println("AWAIT " + partial.id);
			partial.latch.await(); // await all parts of this request's lifecycle
		} catch (InterruptedException e) {
			return;
		}
		synchronized (lock_rpool) {
			//System.out.println("REMOVE " + partial.id);
			rpool.remove(partial.id.toJson());
		}

		Response response = partial.getResponse().getResponse();
		int status = response.getStatus();
		if (this.statusCodeResolver != null) {
			Integer ret = statusCodeResolver.apply(driver, response.getStatus());
			if (ret != null)
				status = ret;
		}

		URL url;
		try {
			url = new URL(response.getUrl());
		} catch (MalformedURLException e) {
			System.err.println("Warning: Invalid response received: " + response.getUrl() + " (" + partial.id + ")");
			return;
		}

		if (url.getProtocol().equalsIgnoreCase("file"))
			return;

		partial.setBody(driver.getDevTools().send(Network.getResponseBody(partial.id)));

		// I must parse html body here an din download(0

		ss.put(response.getUrl(), partial.snapshot());

		System.out.println("\t" + status + " " + response.getUrl());
		if (status == HttpURLConnection.HTTP_OK) {
			try {
				File file = toLocal(url);
				if (!file.exists()) {
					file.getParentFile().mkdirs();
					Network.GetResponseBodyResponse body = partial.getBody();

					byte[] data = body.getBody().getBytes(StandardCharsets.UTF_8);
					if (body.getBase64Encoded())
						data = Base64.getDecoder().decode(data);
					Files.write(file.toPath(), data, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
				}
			} catch (IOException e) {
				System.err.println("Error: Exception when archiving " + response.getUrl());
				e.printStackTrace();
			}
		}
	}

}
