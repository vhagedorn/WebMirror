package me.vadim.archive.v2;

/**
 * Exception for control flow to print link traces.
 * Catchers should perform a sleep to avoid ratelimit
 * errors, as these exceptions are thrown in bursts.
 * @author vadim
 */
public class LinkResolveException extends RuntimeException {

	public LinkResolveException(String message, Throwable cause) {
		super(message, cause);
	}

	public LinkResolveException(String message) {
		super(message);
	}

}