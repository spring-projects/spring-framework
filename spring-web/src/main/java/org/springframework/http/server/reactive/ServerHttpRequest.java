/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.server.reactive;

import java.net.InetSocketAddress;
import java.util.Optional;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.util.MultiValueMap;

/**
 * Represents a reactive server-side HTTP request
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ServerHttpRequest extends HttpRequest, ReactiveHttpInputMessage {

	/**
	 * Returns the portion of the URL path that represents the context path for
	 * the current {@link HttpHandler}. The context path is always at the
	 * beginning of the request path. It starts with "/" but but does not end
	 * with "/". This method may return an empty string if no context path is
	 * configured.
	 * @return the context path (not decoded) or an empty string
	 */
	default String getContextPath() {
		return "";
	}

	/**
	 * Return a read-only map with parsed and decoded query parameter values.
	 */
	MultiValueMap<String, String> getQueryParams();

	/**
	 * Return a read-only map of cookies sent by the client.
	 */
	MultiValueMap<String, HttpCookie> getCookies();

	/**
	 * Returns the remote address where this request is connected to.
	 * @return remote address if available
	 */
	Optional<InetSocketAddress> getRemoteAddress();


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
		 * Set the request URI to return.
		 */
		Builder path(String path);

		/**
		 * Set the contextPath to return.
		 */
		Builder contextPath(String contextPath);

		/**
		 * Set or override the specified header.
		 */
		Builder header(String key, String value);

		/**
		 * Build a {@link ServerHttpRequest} decorator with the mutated properties.
		 */
		ServerHttpRequest build();
	}

}
