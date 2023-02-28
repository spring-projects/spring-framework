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

package org.springframework.web.bind.annotation;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Enumeration of HTTP request methods. Intended for use with the
 * {@link RequestMapping#method()} attribute of the {@link RequestMapping} annotation.
 *
 * <p>Note that, by default, {@link org.springframework.web.servlet.DispatcherServlet}
 * supports GET, HEAD, POST, PUT, PATCH, and DELETE only. DispatcherServlet will
 * process TRACE and OPTIONS with the default HttpServlet behavior unless explicitly
 * told to dispatch those request types as well: Check out the "dispatchOptionsRequest"
 * and "dispatchTraceRequest" properties, switching them to "true" if necessary.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see RequestMapping
 * @see org.springframework.web.servlet.DispatcherServlet#setDispatchOptionsRequest
 * @see org.springframework.web.servlet.DispatcherServlet#setDispatchTraceRequest
 */
public enum RequestMethod {

	GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE;


	/**
	 * Resolve the given method value to an {@code RequestMethod} enum value.
	 * Returns {@code null} if {@code method} has no corresponding value.
	 * @param method the method value as a String
	 * @return the corresponding {@code RequestMethod}, or {@code null} if not found
	 * @since 6.0.6
	 */
	@Nullable
	public static RequestMethod resolve(String method) {
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
			default -> null;
		};
	}

	/**
	 * Resolve the given {@link HttpMethod} to a {@code RequestMethod} enum value.
	 * Returns {@code null} if {@code httpMethod} has no corresponding value.
	 * @param httpMethod the http method object
	 * @return the corresponding {@code RequestMethod}, or {@code null} if not found
	 * @since 6.0.6
	 */
	@Nullable
	public static RequestMethod resolve(HttpMethod httpMethod) {
		Assert.notNull(httpMethod, "HttpMethod must not be null");
		return resolve(httpMethod.name());
	}


	/**
	 * Return the {@link HttpMethod} corresponding to this {@code RequestMethod}.
	 * @return the http method for this request method
	 * @since 6.0.6
	 */
	public HttpMethod asHttpMethod() {
		return switch (this) {
			case GET -> HttpMethod.GET;
			case HEAD -> HttpMethod.HEAD;
			case POST -> HttpMethod.POST;
			case PUT -> HttpMethod.PUT;
			case PATCH -> HttpMethod.PATCH;
			case DELETE -> HttpMethod.DELETE;
			case OPTIONS -> HttpMethod.OPTIONS;
			case TRACE -> HttpMethod.TRACE;
		};
	}

}
