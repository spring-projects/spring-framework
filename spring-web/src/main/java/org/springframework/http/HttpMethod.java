/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http;

import java.io.Serializable;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Represents an HTTP request method. Intended for use
 * with {@link org.springframework.http.client.ClientHttpRequest}
 * and {@link org.springframework.web.client.RestTemplate}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public final class HttpMethod implements Comparable<HttpMethod>, Serializable {

	private static final long serialVersionUID = -70133475680645360L;

	/**
	 * The HTTP method {@code GET}.
	 * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.3">HTTP 1.1, section 9.3</a>
	 */
	public static final HttpMethod GET = new HttpMethod("GET");

	/**
	 * The HTTP method {@code HEAD}.
	 * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.4">HTTP 1.1, section 9.4</a>
	 */
	public static final HttpMethod HEAD = new HttpMethod("HEAD");

	/**
	 * The HTTP method {@code POST}.
	 * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.5">HTTP 1.1, section 9.5</a>
	 */
	public static final HttpMethod POST = new HttpMethod("POST");

	/**
	 * The HTTP method {@code PUT}.
	 * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.6">HTTP 1.1, section 9.6</a>
	 */
	public static final HttpMethod PUT = new HttpMethod("PUT");

	/**
	 * The HTTP method {@code PATCH}.
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5789#section-2">RFC 5789</a>
	 */
	public static final HttpMethod PATCH = new HttpMethod("PATCH");

	/**
	 * The HTTP method {@code DELETE}.
	 * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.7">HTTP 1.1, section 9.7</a>
	 */
	public static final HttpMethod DELETE = new HttpMethod("DELETE");

	/**
	 * The HTTP method {@code OPTIONS}.
	 * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.2">HTTP 1.1, section 9.2</a>
	 */
	public static final HttpMethod OPTIONS = new HttpMethod("OPTIONS");

	/**
	 * The HTTP method {@code TRACE}.
	 * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.8">HTTP 1.1, section 9.8</a>
	 */
	public static final HttpMethod TRACE = new HttpMethod("TRACE");

	private static final HttpMethod[] values = new HttpMethod[] { GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE };


	private final String name;


	private HttpMethod(String name) {
		this.name = name;
	}

	/**
	 * Returns an array containing the standard HTTP methods. Specifically,
	 * this method returns an array containing {@link #GET}, {@link #HEAD},
	 * {@link #POST}, {@link #PUT}, {@link #PATCH}, {@link #DELETE},
	 * {@link #OPTIONS}, and {@link #TRACE}.
	 *
	 * <p>Note that the returned value does not include any HTTP methods defined
	 * in WebDav.
	 */
	public static HttpMethod[] values() {
		HttpMethod[] copy = new HttpMethod[values.length];
		System.arraycopy(values, 0, copy, 0, values.length);
		return copy;
	}

	/**
	 * Return an {@code HttpMethod} object for the given value.
	 * @param method the method value as a String
	 * @return the corresponding {@code HttpMethod}
	 */
	public static HttpMethod valueOf(String method) {
		Assert.notNull(method, "Method must not be null");
		return switch (method) {
			case "GET" -> GET;
			case "HEAD" -> HEAD;
			case "POST" -> POST;
			case "PUT" -> PUT;
			case "PATCH" -> PATCH;
			case "DELETE" -> DELETE;
			case "OPTIONS" -> OPTIONS;
			case "TRACE" -> TRACE;
			default -> new HttpMethod(method);
		};
	}


	/**
	 * Return the name of this method, e.g. "GET", "POST".
	 */
	public String name() {
		return this.name;
	}

	/**
	 * Determine whether this {@code HttpMethod} matches the given method value.
	 * @param method the HTTP method as a String
	 * @return {@code true} if it matches, {@code false} otherwise
	 * @since 4.2.4
	 */
	public boolean matches(String method) {
		return name().equals(method);
	}


	@Override
	public int compareTo(HttpMethod other) {
		return this.name.compareTo(other.name);
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof HttpMethod that && this.name.equals(that.name)));
	}

	@Override
	public String toString() {
		return this.name;
	}

}
