package me.vadim.archive;

import me.vadim.archive.util.AbortableCountDownLatch;
import org.openqa.selenium.devtools.v118.network.Network;
import org.openqa.selenium.devtools.v118.network.model.LoadingFinished;
import org.openqa.selenium.devtools.v118.network.model.RequestId;
import org.openqa.selenium.devtools.v118.network.model.RequestWillBeSent;
import org.openqa.selenium.devtools.v118.network.model.ResponseReceived;

import java.util.concurrent.CountDownLatch;

/**
 * @author vadim
 */
public class PartialResponse {

	private RequestWillBeSent request;
	private ResponseReceived response;
	private LoadingFinished entry;
	private Network.GetResponseBodyResponse body;
	public final AbortableCountDownLatch latch = new AbortableCountDownLatch(3); // req, res, body

	public final RequestId id;

	public PartialResponse(RequestId id) {
		this.id = id;
	}

	public ResponseSnapshot snapshot() {
		return new ResponseSnapshot(request.getRequest().getUrl(),
									response.getResponse().getUrl(),
									request.getRequest().getMethod(),
									response.getResponse().getStatus(),
									response.getResponse().getStatusText(),
									response.getResponse().getHeaders(),
									response.getResponse().getMimeType(),
									entry != null
									? entry.getEncodedDataLength().longValue()
									: response.getResponse().getEncodedDataLength().longValue());
	}

	public synchronized RequestWillBeSent getRequest() {
		return request;
	}

	public synchronized void setRequest(RequestWillBeSent request) {
		this.request = request;
	}

	public synchronized ResponseReceived getResponse() {
		return response;
	}

	public synchronized void setResponse(ResponseReceived response) {
		this.response = response;
	}

	public synchronized LoadingFinished getEntry() {
		return entry;
	}

	public synchronized void setEntry(LoadingFinished entry) {
		this.entry = entry;
	}

	public synchronized Network.GetResponseBodyResponse getBody() {
		return body;
	}

	public synchronized void setBody(Network.GetResponseBodyResponse body) {
		this.body = body;
	}

}
