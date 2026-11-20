/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http;

import java.net.URI;
import java.util.Map;

/**
 * Represents an HTTP request message, consisting of a
 * {@linkplain #getMethod() method} and a {@linkplain #getURI() URI}.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public interface HttpRequest extends HttpMessage {

	/**
	 * Return the HTTP method of the request.
	 * @return the HTTP method as an HttpMethod value
	 * @see HttpMethod#valueOf(String)
	 */
	HttpMethod getMethod();

	/**
	 * Return the URI of the request (including a query string if any,
	 * but only if it is well-formed for a URI representation).
	 * @return the URI of the request (never {@code null})
	 */
	URI getURI();

	/**
	 * Return a mutable map of request attributes for this request.
	 * @since 6.2
	 */
	Map<String, Object> getAttributes();

}
