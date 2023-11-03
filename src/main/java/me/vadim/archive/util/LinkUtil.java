package me.vadim.archive.util;

import com.google.common.net.InternetDomainName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * @author vadim
 */
public class LinkUtil {

	public static boolean isNotBlank(@Nullable String string) {
		return string != null && !string.isBlank();
	}

	public static String removeAnchor(@NotNull String url) {
		return url.replaceFirst("#.*$", ""); // allows "bad" urls and is likely faster
//		try {
//			URI uri = new URI(url);
//			URI raw = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), null);
//			return raw.toString();
//		} catch (URISyntaxException e) {
//			return url;
//		}
	}

	public static boolean isValid(@Nullable String link) {
		if (link == null)
			return false;
		try {
			new URI(link);
			new URL(link);
		} catch (URISyntaxException | MalformedURLException e) {
			return false;
		}
		return true;
	}

	public static boolean guessIsDocument(@NotNull String link) {
		switch (Optional.ofNullable(URLConnection.guessContentTypeFromName(link))
						.map(s -> s.split("/")[0])
						.orElse("text")
						.toLowerCase()
		) {
			case "image":
			case "audio":
			case "video":
				return false;
			case "text": // assume it's a document
			case "application":
			default:
				return true;
		}
	}

	public static String domain(@NotNull URL url) {
		if (!url.getHost().isBlank())
			try {
				return InternetDomainName.from(url.getHost()).topPrivateDomain().toString();
			} catch (IllegalStateException | IllegalArgumentException ignored) {
				return null;
			}
		else
			return null;
	}

	public static String domain(@Nullable String link) {
		URL url = toURL(link);
		return url == null ? null : domain(url);
	}

	public static boolean domainEquals(@Nullable String l1, @Nullable String l2) {
		String d1 = domain(l1);
		String d2 = domain(l2);

		return !(d1 == null && d2 == null) && Objects.equals(d1 == null ? null : d1.toLowerCase(), d2 == null ? null : d2.toLowerCase());
	}

	public static boolean domainEquals(@NotNull URL u1, @NotNull URL u2) {
		String d1 = domain(u1);
		String d2 = domain(u2);

		return Objects.equals(d1 == null ? null : d1.toLowerCase(), d2 == null ? null : d2.toLowerCase());
	}

	public static URL toURL(@Nullable String link) {
		if (link == null)
			return null;
		try {
			if(!link.matches("^[^:]+:.+")) // coerce no protocol to http
				link = "http://" + link;
			return new URL(link);
		} catch (MalformedURLException e) {
			try {
				return new URL(URLEncoder.encode(link, StandardCharsets.UTF_8));
			} catch (MalformedURLException x) {
				return null;
			}
		}
	}

	public static URI toURI(@Nullable String link) {
		if (link == null)
			return null;
		try {
			if(!link.matches("^[^:]+:.+")) // coerce no protocol to http
				link = "http://" + link;
			return new URI(link);
		} catch (URISyntaxException e) {
			try {
				return new URI(URLEncoder.encode(link, StandardCharsets.UTF_8));
			} catch (URISyntaxException x) {
				return null;
			}
		}
	}

	//@formatter:off
	// JDK code from java.io.File //
	public static String slashify(String path, boolean isDirectory) {
		String p = path;
		if (File.separatorChar != '/')
			p = p.replace(File.separatorChar, '/');
		if (!p.startsWith("/"))
			p = "/" + p;
		if (!p.endsWith("/") && isDirectory)
			p = p + "/";
		return p;
	}
	//@formatter:on

}
