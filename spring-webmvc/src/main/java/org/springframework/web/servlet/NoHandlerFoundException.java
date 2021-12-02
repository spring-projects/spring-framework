/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;

/**
 * By default when the DispatcherServlet can't find a handler for a request it
 * sends a 404 response. However if its property "throwExceptionIfNoHandlerFound"
 * is set to {@code true} this exception is raised and may be handled with
 * a configured HandlerExceptionResolver.
 *
 * @author Brian Clozel
 * @since 4.0
 * @see DispatcherServlet#setThrowExceptionIfNoHandlerFound(boolean)
 * @see DispatcherServlet#noHandlerFound(HttpServletRequest, HttpServletResponse)
 */
@SuppressWarnings("serial")
public class NoHandlerFoundException extends ServletException {

	private final String httpMethod;

	private final String requestURL;

	private final HttpHeaders headers;


	/**
	 * Constructor for NoHandlerFoundException.
	 * @param httpMethod the HTTP method
	 * @param requestURL the HTTP request URL
	 * @param headers the HTTP request headers
	 */
	public NoHandlerFoundException(String httpMethod, String requestURL, HttpHeaders headers) {
		super("No handler found for " + httpMethod + " " + requestURL);
		this.httpMethod = httpMethod;
		this.requestURL = requestURL;
		this.headers = headers;
	}


	public String getHttpMethod() {
		return this.httpMethod;
	}

	public String getRequestURL() {
		return this.requestURL;
	}

	public HttpHeaders getHeaders() {
		return this.headers;
	}

}
