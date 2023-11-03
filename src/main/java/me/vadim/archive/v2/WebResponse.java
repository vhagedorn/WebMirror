package me.vadim.archive.v2;

import java.nio.file.Path;
import java.util.Map;

/**
 * @author vadim
 */
public class WebResponse {

	public final String requested, actual;
	public final String method;
	public final int status;
	public final Map<String, ?> headers;
	public final Path body;

	public WebResponse(String requested, String actual, String method, int status, Map<String, ?> headers, Path body) {
		this.requested = requested;
		this.actual    = actual;
		this.method    = method;
		this.status    = status;
		this.headers   = headers;
		this.body      = body;
	}

}
