/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.client;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * Exceptions that contain actual HTTP request data.
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
public class WebClientRequestException extends WebClientException {

	private static final long serialVersionUID = -5139991985321385005L;


	private final HttpMethod method;

	private final URI uri;

	private final HttpHeaders headers;


	/**
	 * Constructor for throwable.
	 */
	public WebClientRequestException(Throwable ex, HttpMethod method, URI uri, HttpHeaders headers) {
		super(ex.getMessage(), ex);

		this.method = method;
		this.uri = uri;
		this.headers = copy(headers);
	}

	/**
	 * Not all {@code HttpHeaders} implementations are serializable, so we
	 * make a copy to ensure that {@code WebClientRequestException} is.
	 */
	private static HttpHeaders copy(HttpHeaders headers) {
		HttpHeaders result = new HttpHeaders();
		for (Map.Entry<String, List<String>> entry : headers.headerSet()) {
			for (String value : entry.getValue()) {
				result.add(entry.getKey(), value);
			}
		}
		return result;
	}


	/**
	 * Return the HTTP request method.
	 */
	public HttpMethod getMethod() {
		return this.method;
	}

	/**
	 * Return the request URI.
	 */
	public URI getUri() {
		return this.uri;
	}

	/**
	 * Return the HTTP request headers.
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

}
