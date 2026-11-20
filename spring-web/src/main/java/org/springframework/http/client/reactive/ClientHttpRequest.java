/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client.reactive;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.util.MultiValueMap;

/**
 * Represents a client-side reactive HTTP request.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
public interface ClientHttpRequest extends ReactiveHttpOutputMessage {

	/**
	 * Return the HTTP method of the request.
	 */
	HttpMethod getMethod();

	/**
	 * Return the URI of the request.
	 */
	URI getURI();

	/**
	 * Return a mutable map of request cookies to send to the server.
	 */
	MultiValueMap<String, HttpCookie> getCookies();

	/**
	 * Return a mutable map of the request attributes.
	 * @since 6.2
	 */
	Map<String, Object> getAttributes();

	/**
	 * Return the request from the underlying HTTP library.
	 * @param <T> the expected type of the request to cast to
	 * @since 5.3
	 */
	<T> T getNativeRequest();

}
