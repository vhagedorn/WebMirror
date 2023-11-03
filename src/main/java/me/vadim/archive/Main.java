package me.vadim.archive;

import com.sun.jdi.Mirror;
import me.vadim.archive.util.LinkUtil;
import me.vadim.archive.v2.LinkAggregator;
import me.vadim.archive.v2.Mirror2;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

/**
 * @author vadim
 */
public class Main {


	public static final String ARCHIVE_URL = "change me";

	@SuppressWarnings("DataFlowIssue")
	public static void v1(String[] args) throws Throwable {
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--incognito"); // avoid caching nastiness
		ChromeDriver driver = new ChromeDriver(options);

		File output = new File("web");

		Mirror mirror = new Mirror(driver, output);
		Runtime.getRuntime().addShutdownHook(new Thread(mirror::shutdown));

		mirror.load();
		mirror.setStatusCodeResolver((webdriver, status) -> {
			if (webdriver.getTitle().endsWith("404"))
				return 404;
			return null;
		});
		mirror.archive(ARCHIVE_URL);
		mirror.save();

		mirror.shutdown();
	}

	public static void v2(String[] args) throws Throwable {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%6$s%n");

		Mirror2 mirror = Mirror2.archive(ARCHIVE_URL, new File("web2"));
		mirror.execute();
		mirror.terminate();
//		Runtime.getRuntime().addShutdownHook(new Thread(mirror::terminate));
//		mirror.execute(Long.MAX_VALUE);
//		mirror.await();
//		mirror.terminate();
	}


	private static File toLocal(URL url) {
		return new File("web2", new File(url.getHost(), url.getFile()).getPath());
	}

	public static void main(String[] args) throws Throwable {
//		v1(args);
		v2(args);


//		LinkAggregator root;
//		LinkAggregator ag = root = LinkAggregator.root("root");
//		System.out.println(ag);
//		ag.push("a").push("1");
//		ag.push("b");
//		ag.push("c");
//		System.out.println(ag);
//		ag = ag.push("d");
//		System.out.println(ag);
//		ag.push("e");
//		ag.push("f");
//		ag.push("g");
//		System.out.println(ag);
//		System.out.println("a "+ag.select("a"));
//		System.out.println("1 "+ag.search("1"));
//		System.out.println("root "+ag.select("root"));
//		System.out.println("> root "+root);
//
//		System.out.println();
//		String[] trace = ag.search("1").trace();
//		System.out.println("Link " + trace[0]);
//		for (int i = 1; i < trace.length; i++) {
//			System.out.println("\tfrom "+trace[i]);
//		}

		// v3 idea:
		//  - proxy chromedriver somehow
		//  - cache every response to disk
		//  - prefer serving cache over upstream
		//  - run original linkscraper on proxy
		//  - requests problem solved
		//  - now we can implement scrolling and such
		//  - won't work on socket reliant websites
		//  - infinite scroll will probably break
	}

}
