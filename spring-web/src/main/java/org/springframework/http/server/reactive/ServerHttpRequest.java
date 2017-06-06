/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Represents a reactive server-side HTTP request
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ServerHttpRequest extends HttpRequest, ReactiveHttpInputMessage {

	/**
	 * Returns the portion of the URL path that represents the application.
	 * The context path is always at the beginning of the path and starts but
	 * does not end with "/". It is shared for URLs of the same application.
	 * <p>The context path may come from the underlying runtime API such as
	 * when deploying as a WAR to a Servlet container or it may also be assigned
	 * through the use of {@link ContextPathCompositeHandler} or both.
	 * <p>The context path is not decoded.
	 * @return the context path (not decoded) or an empty string
	 */
	default String getContextPath() {
		return "";
	}

	/**
	 * Returns the portion of the URL path after the {@link #getContextPath()
	 * contextPath}. The returned path is not decoded.
	 * @return the path under the contextPath
	 */
	default String getPathWithinApplication() {
		String path = getURI().getRawPath();
		String contextPath = getContextPath();
		if (StringUtils.hasText(contextPath)) {
			int length = contextPath.length();
			return (path.length() > length ? path.substring(length) : "");
		}
		else {
			return path;
		}
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
	 * Return the remote address where this request is connected to, if available.
	 */
	@Nullable
	InetSocketAddress getRemoteAddress();


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
