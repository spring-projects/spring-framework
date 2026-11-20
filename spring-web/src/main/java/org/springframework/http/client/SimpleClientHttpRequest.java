/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

/**
 * {@link ClientHttpRequest} implementation that uses standard JDK facilities to
 * execute streaming requests. Created via the {@link SimpleClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @since 6.1
 * @see SimpleClientHttpRequestFactory#createRequest(URI, HttpMethod)
 */
final class SimpleClientHttpRequest extends AbstractStreamingClientHttpRequest {

	private final HttpURLConnection connection;

	private final int chunkSize;


	SimpleClientHttpRequest(HttpURLConnection connection, int chunkSize) {
		this.connection = connection;
		this.chunkSize = chunkSize;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.connection.getRequestMethod());
	}

	@Override
	public URI getURI() {
		try {
			return this.connection.getURL().toURI();
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not get HttpURLConnection URI: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, @Nullable Body body) throws IOException {
		if (this.connection.getDoOutput()) {
			long contentLength = headers.getContentLength();
			if (contentLength >= 0) {
				this.connection.setFixedLengthStreamingMode(contentLength);
			}
			else {
				this.connection.setChunkedStreamingMode(this.chunkSize);
			}
		}

		addHeaders(this.connection, headers);

		this.connection.connect();

		if (this.connection.getDoOutput() && body != null) {
			try (OutputStream os = this.connection.getOutputStream()) {
				body.writeTo(os);
			}
		}
		else {
			// Immediately trigger the request in a no-output scenario as well
			this.connection.getResponseCode();
		}
		return new SimpleClientHttpResponse(this.connection);
	}


	/**
	 * Add the given headers to the given HTTP connection.
	 * @param connection the connection to add the headers to
	 * @param headers the headers to add
	 */
	static void addHeaders(HttpURLConnection connection, HttpHeaders headers) {
		String method = connection.getRequestMethod();
		if (method.equals("PUT") || method.equals("DELETE")) {
			if (!StringUtils.hasText(headers.getFirst(HttpHeaders.ACCEPT))) {
				// Avoid "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2"
				// from HttpUrlConnection which prevents JSON error response details.
				headers.set(HttpHeaders.ACCEPT, "*/*");
			}
		}
		headers.forEach((headerName, headerValues) -> {
			if (HttpHeaders.COOKIE.equalsIgnoreCase(headerName)) {  // RFC 6265
				String headerValue = StringUtils.collectionToDelimitedString(headerValues, "; ");
				connection.setRequestProperty(headerName, headerValue);
			}
			else {
				for (String headerValue : headerValues) {
					String actualHeaderValue = headerValue != null ? headerValue : "";
					connection.addRequestProperty(headerName, actualHeaderValue);
				}
			}
		});
	}

}
