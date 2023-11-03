package me.vadim.archive.util.log;

import me.vadim.archive.util.CustomFormatter;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * gay-ass java impl uses stderr
 * @author vadim
 */
public class StdoutHandler extends StreamHandler {

	public StdoutHandler() {
		super(System.out, new CustomFormatter());
	}

	@Override
	public void publish(LogRecord record) {
		super.publish(record);
		flush();
	}

	@Override
	public void close() {
		flush();
	}

}
