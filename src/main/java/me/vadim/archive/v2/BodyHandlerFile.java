package me.vadim.archive.v2;

import java.io.File;
import java.net.http.HttpResponse;
import java.nio.file.Path;

/**
 * An {@link java.net.http.HttpResponse.BodyHandler} which, if the {@linkplain HttpResponse.ResponseInfo#statusCode() status code}
 * is 20x, then it {@linkplain java.net.http.HttpResponse.BodyHandlers#ofFile(Path) saves} the response, else it
 * {@linkplain HttpResponse.BodyHandlers#discarding() discards} the response.
 *
 * @author vadim
 */
public class BodyHandlerFile implements HttpResponse.BodyHandler<Path> {

	private final File file;

	public BodyHandlerFile(File file) {
		this.file = file;
	}

	@Override
	public HttpResponse.BodySubscriber<Path> apply(HttpResponse.ResponseInfo responseInfo) {
		HttpResponse.BodyHandler<Path> handler;
		if (responseInfo.statusCode() / 100 == 2)
			handler = HttpResponse.BodyHandlers.ofFile(file.toPath());
		else
			handler = HttpResponse.BodyHandlers.replacing(null);
		return handler.apply(responseInfo);
	}

}
