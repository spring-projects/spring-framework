/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.web.servlet.observability;

import java.util.Collection;

import io.micrometer.api.instrument.transport.http.HttpServerRequest;
import io.micrometer.api.instrument.transport.http.HttpServerResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A {@link HttpServerResponse} that wraps and delegates to {@link HttpServletResponse}.
 *
 * @author Marcin Grzejszczak
 * @author Jonatan Ivanov
 *
 * @since 6.0.0
 */
public final class HttpServletResponseWrapper implements HttpServerResponse {
	private final HttpServerRequest request;
	private final HttpServletResponse response;
	private final Throwable error;

	private HttpServletResponseWrapper(HttpServerRequest request, HttpServletResponse response, Throwable error) {
		this.request = request;
		this.response = response;
		this.error = error;
	}

	/**
	 * Static factory method to create an instance.
	 * @param request the request to wrap
	 * @param response the response to wrap
	 * @param error the error to wrap
	 * @return an {@link HttpServletResponseWrapper} instance that uses the provided response.
	 */
	static HttpServletResponseWrapper wrap(HttpServerRequest request, HttpServletResponse response, Throwable error) {
		return new HttpServletResponseWrapper(request, response, error);
	}

	@Override
	public int statusCode() {
		return this.response.getStatus();
	}

	@Override
	public String header(String name) {
		return this.response.getHeader(name);
	}

	@Override
	public Collection<String> headerNames() {
		return this.response.getHeaderNames();
	}

	@Override
	public Object unwrap() {
		return this.response;
	}

	@Override
	public HttpServerRequest request() {
		return this.request;
	}

	@Override
	public Throwable error() {
		return this.error;
	}
}
