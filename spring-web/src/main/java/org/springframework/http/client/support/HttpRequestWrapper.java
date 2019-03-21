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

package org.springframework.http.client.support;

import java.net.URI;

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
	 * Return the headers of the wrapped request.
	 */
	@Override
	public HttpHeaders getHeaders() {
		return this.request.getHeaders();
	}

}
