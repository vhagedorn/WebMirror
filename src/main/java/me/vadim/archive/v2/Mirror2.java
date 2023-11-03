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

	private final File dir;
	private final WebClient2 web;

	/**
	 * domain name handling <p>
	 * if {@code true}, then the host must match exactly, else only the top private domain must match
	 */
	public final boolean strict;

	/**
	 * recursion level <p>
	 * if {@code true}, then scraping will recurse based on the {@link #strict strictness}, else no recursion will happen
	 */
	public final boolean recurse;

	private final Logger log = Logger.getLogger(getClass().getSimpleName());

	private Mirror2(File dir, boolean strict, boolean recurse) {
		Util.tee(log, new File("logs/mirror2.log"));

		this.dir = dir;
		this.web = new WebClient2();

		this.strict  = strict;
		this.recurse = recurse;

		// scan file structure and generate populate links
		// then scrape all files and add remote urls accordingly
		// since we're using bs4, relative urls are not a problem
		// remove links from download list if already archived
		// repeat this, downloading new remote urls as needed
	}

	public static Mirror2Builder archive(File dir) {
		if (dir == null)
			throw new IllegalArgumentException("dir");

		try {
			if (!dir.exists())
				if (!dir.mkdirs())
					throw new IOException("Failure to create dir " + dir);
			if (!dir.isDirectory())
				throw new FileAlreadyExistsException("File " + dir + " is not a directory");

			return new Mirror2Builder(dir);
		} catch (IOException e) {
			throw new IllegalArgumentException("Bad destination: " + dir, e);
		}
	}

	private File toLocal(URL url) {
		return new File(dir, new File(url.getHost(), url.getFile()).getPath());
	}

	@SuppressWarnings("BusyWait")
	public void execute(String target) {
		URL base = LinkUtil.toURL(target);
		if (target == null || base == null)
			throw new IllegalArgumentException("target: " + target);

		log.info(">> Archiving " + target);

		Set<String>   fetched = new HashSet<>(200_000);
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
				if (strict ? base.getHost().equalsIgnoreCase(url.getHost()) : LinkUtil.domainEquals(base.toString(), link)) {
					web.download(link, toLocal(url), WebClient2.DOWNLOAD_MODE_ALL);

					Set<String> links = web.scrape(link, toLocal(url));
					if (recurse) // add all to stack
						stack.addAll(links); // double download is OK (caching logic will take care)
					else // just download media files
						for (String l : links)
							if(LinkUtil.isValid(l))
								web.download(l, toLocal(LinkUtil.toURL(l)), WebClient2.DOWNLOAD_MODE_MEDIA);

					ag.pushAll(links);
				} else {
					web.download(link, toLocal(url), WebClient2.DOWNLOAD_MODE_MEDIA);
					// do not push anything to the stack
				}
			} catch (LinkResolveException e) {
				log.log(Level.WARNING, e.getMessage(), e.getCause());
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


	public static final class Mirror2Builder {

		private final File dir;

		private boolean strict = true;
		private boolean recurse = true;

		private Mirror2Builder(File dir) {
			this.dir = dir;
		}

		public Mirror2Builder isStrict(boolean strict) {
			this.strict = strict;
			return this;
		}

		public Mirror2Builder doRecurse(boolean recurse) {
			this.recurse = recurse;
			return this;
		}

		public Mirror2 build() {
			return new Mirror2(dir, strict, recurse);
		}

	}

}