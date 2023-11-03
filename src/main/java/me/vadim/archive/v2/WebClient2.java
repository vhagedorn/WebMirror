package me.vadim.archive.v2;

import me.vadim.archive.URIElement;
import me.vadim.archive.util.LinkUtil;
import me.vadim.archive.util.LocalExecutors;
import me.vadim.archive.util.Util;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author vadim
 */
public class WebClient2 {

	public static final int HTTP_CACHED = 1;
	public static final String HTTP_GET = "GET";

	public static final int DOWNLOAD_MODE_ALL = 0;
	public static final int DOWNLOAD_MODE_DOCUMENTS = 1;
	public static final int DOWNLOAD_MODE_MEDIA = 2;

	private final ExecutorService io;
	private final HttpClient client;

	private final Logger log = Logger.getLogger(getClass().getSimpleName());

	public WebClient2() {
		Util.tee(log, new File("logs/webclient.log"));
		this.io = LocalExecutors.newExtendedThreadPool(Runtime.getRuntime().availableProcessors());
		// this isn't throttled max_concurrent_streams anymore since we're basically synchronous now

		this.client = HttpClient.newBuilder()
								.executor(io)
								.followRedirects(HttpClient.Redirect.NORMAL)
								.connectTimeout(Duration.ofSeconds(30))
								.build();
	}

	public void terminate() {
		io.shutdown();
	}

	private static HttpRequest get(String link) {
		URI uri;
		try {
			uri = LinkUtil.toURL(link).toURI();
		} catch (URISyntaxException e) {
			Logger.getLogger(WebClient2.class.getSimpleName()).severe("Invalid link provided: " + link);
			return null;
		}

		return HttpRequest.newBuilder().GET().uri(uri).header("User-Agent", "WebMirror2").build();
	}

	public WebResponse download(String link, File file, int downloadMode) {
		switch (downloadMode) {
			case DOWNLOAD_MODE_MEDIA:
				if (LinkUtil.guessIsDocument(link))
					return null;
				break;
			case DOWNLOAD_MODE_DOCUMENTS:
				if (!LinkUtil.guessIsDocument(link))
					return null;
				break;
		}

		if (file.getParentFile().mkdirs())
			log.fine("Created directory " + file.getParentFile());

		WebResponse res;
		if (file.exists())
			res = new WebResponse(link, link, HTTP_GET, HTTP_CACHED, Map.of("Cached-Response", true), file.toPath());
		else
			try {
				HttpRequest req = get(link);
				if (req == null)
					return null;
				HttpResponse<Path> http = client.send(get(link), new BodyHandlerFile(file));
				res = new WebResponse(http.request().uri().toString(), http.uri().toString(), http.request().method(), http.statusCode(), http.headers().map(), http.body());
				log.info(http.request().method() + " " + http.uri() + " " + http.statusCode());
			} catch (InterruptedException e) {
				return null;
			} catch (IOException e) {
				throw new LinkResolveException(String.format("Exception downloading `%s`, likely a malformed link.", link), e);
			}

		return res;
	}

	public Set<String> scrape(String link, File file) {
		WebResponse res = download(link, file, DOWNLOAD_MODE_DOCUMENTS);
		if (res == null || res.body == null)
			return Collections.emptySet();

		log.fine("PARSE " + res.body + " (" + res.actual + ")");

		Document doc;
		try {
			doc = Jsoup.parse(res.body.toFile(), StandardCharsets.UTF_8.name(), res.requested);
		} catch (IOException e) {
			throw new LinkResolveException("Exception parsing file: " + res.body);
		}

		Set<String> links = new HashSet<>();
		for (URIElement tag : URIElement.values())
			for (Element elem : doc.getElementsByTag(tag.getTag()))
				for (String attr : tag.getAttrs())
					if (elem.hasAttr(attr))
						links.add(LinkUtil.removeAnchor(elem.absUrl(attr)));

		return links;
	}

}
