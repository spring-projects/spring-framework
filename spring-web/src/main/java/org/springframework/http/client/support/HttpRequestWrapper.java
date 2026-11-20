/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client.support;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.util.Assert;

/**
 * Provides a convenient implementation of the {@link HttpRequest} interface
 * that can be overridden to adapt the request.
 *
 * <p>These methods default to calling through to the wrapped request object.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public class HttpRequestWrapper implements HttpRequest {

	private final HttpRequest request;


	/**
	 * Create a new {@code HttpRequest} wrapping the given request object.
	 * @param request the request object to be wrapped
	 */
	public HttpRequestWrapper(HttpRequest request) {
		Assert.notNull(request, "HttpRequest must not be null");
		this.request = request;
	}


	/**
	 * Return the wrapped request.
	 */
	public HttpRequest getRequest() {
		return this.request;
	}

	/**
	 * Return the method of the wrapped request.
	 */
	@Override
	public HttpMethod getMethod() {
		return this.request.getMethod();
	}

	/**
	 * Return the URI of the wrapped request.
	 */
	@Override
	public URI getURI() {
		return this.request.getURI();
	}

	/**
	 * Return the attributes of the wrapped request.
	 */
	@Override
	public Map<String, Object> getAttributes() {
		return this.request.getAttributes();
	}

	/**
	 * Return the headers of the wrapped request.
	 */
	@Override
	public HttpHeaders getHeaders() {
		return this.request.getHeaders();
	}

}
