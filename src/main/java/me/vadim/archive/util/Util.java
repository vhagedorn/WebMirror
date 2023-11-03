package me.vadim.archive.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/**
 * @author vadim
 */
public class Util {

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void fuckUncheckedExceptions(Throwable throwable) throws T {
		throw (T) throwable;
	}

	public static void sneaky(Throwable throwable) {
		Util.<RuntimeException>fuckUncheckedExceptions(throwable);
	}

	public static void tee(Logger logger, File file) {
		try {
			// don't forward any logging to this logger to his parent
			logger.setUseParentHandlers(false);
			// log messages of all level
			logger.setLevel(Level.INFO);

			// define the logfile

			StreamHandler sout = new ConsoleHandler();
			sout.setFormatter(new CustomFormatter("%2$-7s %5$s%6$s%n"));
			logger.addHandler(sout);

			FileHandler   fh   = new FileHandler(file.getPath());
			fh.setFormatter(new CustomFormatter("[%1tH:%<tM:%<tS] %2$-7s %5$s%6$s%n"));
			logger.addHandler(fh);

		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}

}
