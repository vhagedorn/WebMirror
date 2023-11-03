package me.vadim.archive;

import org.openqa.selenium.devtools.v118.network.model.Response;

import java.util.Collections;
import java.util.Map;

/**
 * @author vadim
 */
public class ResponseSnapshot {

	public final String requested, actual;
	public final String method;
	public final int status;
	public final String statusText;
	public final Map<String, ?> headers;
	public final String mimeType;
	public final long encodedDataLength;

	public ResponseSnapshot(String requested, String actual, String method, int status, String statusText, Map<String, ?> headers, String mimeType, long encodedDataLength) {
		this.requested         = requested;
		this.actual            = actual;
		this.method            = method;
		this.status            = status;
		this.statusText        = statusText;
		this.headers           = headers;
		this.mimeType          = mimeType;
		this.encodedDataLength = encodedDataLength;
	}

}
