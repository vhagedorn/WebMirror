package me.vadim.archive;

import me.vadim.archive.v2.Mirror2;

import java.io.File;

/**
 * @author vadim
 */
public class Main {

	public static void main(String[] args) throws Throwable {
		if(args.length != 2) {
			System.err.println("Usage:");
			System.err.println("\tjava -jar WebMirror.jar <destination> <url>");
			System.exit(1);
		}

		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%6$s%n");

		Mirror2 mirror = Mirror2.archive(args[1], new File(args[0]));
		mirror.execute();
		mirror.terminate();
	}

}