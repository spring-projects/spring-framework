/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.function.Consumer;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * Represents a reactive server-side HTTP request.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public interface ServerHttpRequest extends HttpRequest, ReactiveHttpInputMessage {

	/**
	 * Return an id that represents the underlying connection, if available,
	 * or the request for the purpose of correlating log messages.
	 * @since 5.1
	 * @see org.springframework.web.server.ServerWebExchange#getLogPrefix()
	 */
	String getId();

	/**
	 * Returns a structured representation of the request path including the
	 * context path + path within application portions, path segments with
	 * encoded and decoded values, and path parameters.
	 */
	RequestPath getPath();

	/**
	 * Return a read-only map with parsed and decoded query parameter values.
	 */
	MultiValueMap<String, String> getQueryParams();

	/**
	 * Return a read-only map of cookies sent by the client.
	 */
	MultiValueMap<String, HttpCookie> getCookies();

	/**
	 * Return the remote address where this request is connected to, if available.
	 */
	@Nullable
	default InetSocketAddress getRemoteAddress() {
		return null;
	}

	/**
	 * Return the SSL session information if the request has been transmitted
	 * over a secure protocol including SSL certificates, if available.
	 * @return the session information, or {@code null} if none available
	 * @since 5.0.2
	 */
	@Nullable
	default SslInfo getSslInfo() {
		return null;
	}

	/**
	 * Return a builder to mutate properties of this request by wrapping it
	 * with {@link ServerHttpRequestDecorator} and returning either mutated
	 * values or delegating back to this instance.
	 */
	default ServerHttpRequest.Builder mutate() {
		return new DefaultServerHttpRequestBuilder(this);
	}


	/**
	 * Builder for mutating an existing {@link ServerHttpRequest}.
	 */
	interface Builder {

		/**
		 * Set the HTTP method to return.
		 */
		Builder method(HttpMethod httpMethod);

		/**
		 * Set the URI to use with the following conditions:
		 * <ul>
		 * <li>If {@link #path(String) path} is also set, it overrides the path
		 * of the URI provided here.
		 * <li>If {@link #contextPath(String) contextPath} is also set, or
		 * already present, it must match the start of the path of the URI
		 * provided here.
		 * </ul>
		 */
		Builder uri(URI uri);

		/**
		 * Set the path to use instead of the {@code "rawPath"} of the URI of
		 * the request with the following conditions:
		 * <ul>
		 * <li>If {@link #uri(URI) uri} is also set, the path given here
		 * overrides the path of the given URI.
		 * <li>If {@link #contextPath(String) contextPath} is also set, or
		 * already present, it must match the start of the path given here.
		 * <li>The given value must begin with a slash.
		 * </ul>
		 */
		Builder path(String path);

		/**
		 * Set the contextPath to use.
		 * <p>The given value must be a valid {@link RequestPath#contextPath()
		 * contextPath} and it must match the start of the path of the URI of
		 * the request. That means changing the contextPath, implies also
		 * changing the path via {@link #path(String)}.
		 */
		Builder contextPath(String contextPath);

		/**
		 * Add the given, single header value under the given name.
		 * @param headerName the header name
		 * @param headerValue the header value
		 * @deprecated This method will be removed in Spring Framework 5.2 in
		 * favor of {@link #header(String, String...)}.
		 */
		@Deprecated
		Builder header(String headerName, String headerValue);

		/**
		 * Set or override the specified header values under the given name.
		 * <p>If you need to set a single header value, you may invoke this
		 * method with an explicit one-element array &mdash; for example,
		 * <code>header("key", new String[] { "value" })</code> &mdash; or you
		 * may choose to use {@link #headers(Consumer)} for greater control.
		 * @param headerName the header name
		 * @param headerValues the header values
		 * @since 5.1.9
		 * @see #headers(Consumer)
		 */
		default Builder header(String headerName, String... headerValues) {
			return headers(httpHeaders -> httpHeaders.put(headerName, Arrays.asList(headerValues)));
		}

		/**
		 * Manipulate request headers. The provided {@code HttpHeaders} contains
		 * current request headers, so that the {@code Consumer} can
		 * {@linkplain HttpHeaders#set(String, String) overwrite} or
		 * {@linkplain HttpHeaders#remove(Object) remove} existing values, or
		 * use any other {@link HttpHeaders} methods.
		 * @see #header(String, String...)
		 */
		Builder headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * Set the SSL session information. This may be useful in environments
		 * where TLS termination is done at the router, but SSL information is
		 * made available in some other way such as through a header.
		 * @since 5.0.7
		 */
		Builder sslInfo(SslInfo sslInfo);

		/**
		 * Build a {@link ServerHttpRequest} decorator with the mutated properties.
		 */
		ServerHttpRequest build();
	}

}
