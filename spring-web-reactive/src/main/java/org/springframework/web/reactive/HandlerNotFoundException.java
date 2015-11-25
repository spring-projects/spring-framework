/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.web.reactive;

import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * @author Rossen Stoyanchev
 */
public class HandlerNotFoundException extends NestedRuntimeException {

	private final HttpMethod method;

	private final String requestURL;

	private final HttpHeaders headers;


	/**
	 * Constructor for NoHandlerFoundException.
	 * @param method the HTTP method
	 * @param requestURL the HTTP request URL
	 * @param headers the HTTP request headers
	 */
	public HandlerNotFoundException(HttpMethod method, String requestURL, HttpHeaders headers) {
		super("No handler found for " + method + " " + requestURL);
		this.method = method;
		this.requestURL = requestURL;
		this.headers = headers;
	}


	public HttpMethod getMethod() {
		return this.method;
	}

	public String getRequestURL() {
		return this.requestURL;
	}

	public HttpHeaders getHeaders() {
		return this.headers;
	}
}
