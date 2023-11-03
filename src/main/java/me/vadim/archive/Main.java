package me.vadim.archive;

import me.vadim.archive.v2.Mirror2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author vadim
 */
public class Main {

	private static final void usage() {
		System.err.println();
		System.err.println("Usage:");
		System.err.println("\tjava -jar WebMirror.jar <destination> [OPTIONS]");
		System.err.println("Flags:");
		System.err.println("\t                 (default)");
		System.err.println("\t--strict          (true)      include subdomains during recursion check?");
		System.err.println("\t--recurse         (true)      enable recursion?");
		System.err.println("Params:");
		System.err.println("\t                 [one of]");
		System.err.println("\t--url           [required]    base URL to archive (e.g. index.html)");
		System.err.println("\t--file          [required]    file of URLs to batch archive (one link per line)");
		System.err.println();
	}

	public static void main(String[] args) throws Throwable {
		if (args.length < 2) {
			usage();
			System.exit(1);
		}

		boolean strict  = true;
		boolean recurse = true;
		String  file    = null;
		String  url     = null;

		for (int i = 1; i < args.length; i++)
			try {
				switch (args[i].split("=")[0].substring(2)) {
					case "strict":
						strict = Boolean.parseBoolean(args[i].split("=")[1]);
						break;
					case "recurse":
						recurse = Boolean.parseBoolean(args[i].split("=")[1]);
						break;
					case "url":
						url = args[i].split("=")[1];
						break;
					case "file":
						file = args[i].split("=")[1];
						break;
				}
			} catch (IndexOutOfBoundsException | NullPointerException e) {
				System.err.println("Invalid argument at index " + i + ": " + args[i]);
				usage();
				System.exit(2);
			}

		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%6$s%n");

		if (file == null && url == null) {
			System.err.println("A required arg is missing!");
			usage();
			System.exit(3);
		}

		Mirror2 mirror = Mirror2.archive(new File(args[0])).isStrict(strict).doRecurse(recurse).build();

		if (file != null) {
			File f = new File(file);
			if (!f.isFile()) {
				System.err.println("File not found: " + file);
				usage();
				System.exit(2);
			}
			System.out.println("Parsing file: " + file);

			try (FileInputStream fis = new FileInputStream(f)) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

				String line;
				while ((line = reader.readLine()) != null)
					mirror.execute(line);
			} catch (IOException e) {
				System.err.println("Problem parsing: " + file);
				e.printStackTrace();
				System.exit(4);
			}
		}

		if (url != null)
			mirror.execute(url);

		mirror.terminate();
	}

}