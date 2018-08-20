package org.springframework.web.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

/**
 * Spring's default implementation of the {@link HttpErrorDetailsExtractor} interface.
 *
 * <p>This extractor will compose a short summary of the http error response, including:
 * <ul>
 *     <li>request URI
 *     <li>request method
 *     <li>a 200-character preview of the response body, unformatted
 * </ul>
 *
 * An example:
 * <pre>
 * 404 Not Found after GET http://example.com:8080/my-endpoint : [{'id': 123, 'message': 'my very long... (500 bytes)]</code>
 * </pre>
 *
 * @author Jerzy Krolak
 * @since 5.1
 * @see DefaultResponseErrorHandler#setHttpErrorDetailsExtractor(HttpErrorDetailsExtractor)
 */
public class DefaultHttpErrorDetailsExtractor implements HttpErrorDetailsExtractor {

	private static final int MAX_BODY_BYTES_LENGTH = 400;

	private static final int MAX_BODY_CHARS_LENGTH = 200;

	/**
	 * Assemble a short summary of the HTTP error response.
	 * @param rawStatusCode HTTP status code
	 * @param statusText HTTP status text
	 * @param responseBody response body
	 * @param responseCharset response charset
	 * @param url request URI
	 * @param method request method
	 * @return error details string. Example: <pre>404 Not Found after GET http://example.com:8080/my-endpoint : [{'id': 123, 'message': 'my very long... (500 bytes)]</code></pre>
	 */
	@Override
	@NotNull
	public String getErrorDetails(int rawStatusCode, String statusText, @Nullable byte[] responseBody,
			@Nullable Charset responseCharset, @Nullable URI url, @Nullable HttpMethod method) {

		if (url == null || method == null) {
			return getSimpleErrorDetails(rawStatusCode, statusText);
		}

		return getCompleteErrorDetails(rawStatusCode, statusText, responseBody, responseCharset, url, method);
	}

	@NotNull
	private String getCompleteErrorDetails(int rawStatusCode, String statusText, @Nullable byte[] responseBody,
			@Nullable Charset responseCharset, @Nullable URI url, @Nullable HttpMethod method) {

		StringBuilder result = new StringBuilder();

		result.append(getSimpleErrorDetails(rawStatusCode, statusText))
				.append(" after ")
				.append(method)
				.append(" ")
				.append(url)
				.append(" : ");

		if (responseBody == null || responseBody.length == 0) {
			result.append("[no body]");
		}
		else {
			result
					.append("[")
					.append(getResponseBody(responseBody, responseCharset))
					.append("]");
		}

		return result.toString();
	}

	@NotNull
	private String getSimpleErrorDetails(int rawStatusCode, String statusText) {
		return rawStatusCode + " " + statusText;
	}

	private String getResponseBody(byte[] responseBody, @Nullable Charset responseCharset) {
		Charset charset = getCharsetOrDefault(responseCharset);
		if (responseBody.length < MAX_BODY_BYTES_LENGTH) {
			return getCompleteResponseBody(responseBody, charset);
		}
		return getResponseBodyPreview(responseBody, charset);
	}

	@NotNull
	private String getCompleteResponseBody(byte[] responseBody, Charset responseCharset) {
		return new String(responseBody, responseCharset);
	}

	private String getResponseBodyPreview(byte[] responseBody, Charset responseCharset) {
		try {
			String bodyPreview = readBodyAsString(responseBody, responseCharset);
			return bodyPreview + "... (" + responseBody.length + " bytes)";
		}
		catch (IOException e) {
			// should never happen
			throw new IllegalStateException(e);
		}
	}

	@NotNull
	private String readBodyAsString(byte[] responseBody, Charset responseCharset) throws IOException {

		Reader reader = new InputStreamReader(new ByteArrayInputStream(responseBody), responseCharset);
		CharBuffer result = CharBuffer.allocate(MAX_BODY_CHARS_LENGTH);

		reader.read(result);
		reader.close();
		result.flip();

		return result.toString();
	}

	private Charset getCharsetOrDefault(@Nullable Charset responseCharset) {
		if (responseCharset == null) {
			return StandardCharsets.ISO_8859_1;
		}
		return responseCharset;
	}

}
