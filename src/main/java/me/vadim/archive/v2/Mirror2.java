package me.vadim.archive.v2;


import me.vadim.archive.util.LinkUtil;
import me.vadim.archive.util.StrPad;
import me.vadim.archive.util.Util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author vadim
 */
public class Mirror2 {

	private final URL base;
	private final File dir;
	private final WebClient2 web;
	private final Logger log = Logger.getLogger(getClass().getSimpleName());

	private Mirror2(URL url, File dir) {
		Util.tee(log, new File("logs/mirror2.log"));

		this.base = url;
		this.dir  = dir;
		this.web  = new WebClient2();

		// scan file structure and generate populate links
		// then scrape all files and add remote urls accordingly
		// since we're using bs4, relative urls are not a problem
		// remove links from download list if already archived
		// repeat this, downloading new remote urls as needed
	}

	public static Mirror2 archive(String url, File dir) {
		if (url == null)
			throw new IllegalArgumentException("url");
		if (dir == null)
			throw new IllegalArgumentException("dir");

		try {
			if (!dir.exists())
				if (!dir.mkdirs())
					throw new IOException("Failure to create dir " + dir);
			if (!dir.isDirectory())
				throw new FileAlreadyExistsException("File " + dir + " is not a directory");

			return new Mirror2(new URL(url), dir);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Bad URL: " + url, e);
		} catch (IOException e) {
			throw new IllegalArgumentException("Bad destination: " + dir, e);
		}
	}

	private File toLocal(URL url) {
		return new File(dir, new File(url.getHost(), url.getFile()).getPath());
	}

	@SuppressWarnings("BusyWait")
	public void execute() {
		Set<String>   fetched = new HashSet<>(5000);
		Stack<String> stack   = new Stack<>();

		LinkAggregator ag = LinkAggregator.root(base.toString());

		stack.push(base.toString());

		long i = 0, szC = 1, szM = 1;
		while (!stack.isEmpty()) {
			String link = stack.pop();
			ag = ag.select(link);
			URL url;
			try {
				url = new URL(link);
			} catch (MalformedURLException e) {
				log.warning("Malformed URL: " + link);
				ag.printTrace(log);
				continue;
			}

			log.info(StrPad.rightPad(String.format("+%d ", Math.max(0, ((szC = stack.size()) + i) - szM)), 6 + 1 + 1) +
					 String.format(" (%d/%d) ", ++i, (szM = Math.max(szM, szC + i))) +
					 " " + link);

			try {
				if (LinkUtil.domainEquals(base.toString(), link)) {
					web.download(link, toLocal(url), WebClient2.DOWNLOAD_MODE_ALL);
					Set<String> links = web.scrape(link, toLocal(url));
					stack.addAll(links); // double download is OK (caching logic will take care)
					ag.pushAll(links);
				} else {
					web.download(link, toLocal(url), WebClient2.DOWNLOAD_MODE_MEDIA);
					// do not push anything to the stack
				}
			} catch (LinkResolveException e) {
				log.log(Level.WARNING,  e.getMessage(), e.getCause());
				ag.printTrace(log);
				try {
					Thread.sleep(TimeUnit.SECONDS.toMillis(3)); // sleep to avoid concurrent ratelimit
				} catch (InterruptedException x) {
					break;
				}
				continue;
			}

			fetched.add(link);
			stack.removeAll(fetched);
		}
	}

	public void terminate() {
		web.terminate();
	}

}