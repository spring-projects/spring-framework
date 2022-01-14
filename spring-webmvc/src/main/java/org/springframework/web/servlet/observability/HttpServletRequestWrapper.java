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
import java.util.Collections;

import io.micrometer.core.instrument.transport.http.HttpServerRequest;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.HandlerMapping;

/**
 * An {@link HttpServerRequest} that wraps/delegates to {@link HttpServletRequest}.
 *
 * @author Marcin Grzejszczak
 * @author Jonatan Ivanov
 *
 * @since 6.0.0
 */
public final class HttpServletRequestWrapper implements HttpServerRequest {
	private final HttpServletRequest request;

	private HttpServletRequestWrapper(HttpServletRequest request) {
		this.request = request;
	}

	/**
	 * Static factory method to create an instance.
	 * @param request the request to wrap
	 * @return an {@link HttpServletRequestWrapper} instance that uses the provided request.
	 */
	static HttpServletRequestWrapper wrap(HttpServletRequest request) {
		return new HttpServletRequestWrapper(request);
	}

	@Override
	public String method() {
		return this.request.getMethod();
	}

	@Override
	public String path() {
		return this.request.getRequestURI();
	}

	@Override
	public String route() {
		Object routeCandidate = this.request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		return routeCandidate instanceof String ? (String) routeCandidate : null;
	}

	@Override
	public String url() {
		StringBuffer url = this.request.getRequestURL();
		if (this.request.getQueryString() != null && !this.request.getQueryString().isEmpty()) {
			url.append('?').append(this.request.getQueryString());
		}

		return url.toString();
	}

	@Override
	public String header(String name) {
		return this.request.getHeader(name);
	}

	@Override
	public String remoteIp() {
		return this.request.getRemoteAddr();
	}

	@Override
	public int remotePort() {
		return this.request.getRemotePort();
	}

	@Override
	public Collection<String> headerNames() {
		return Collections.list(this.request.getHeaderNames());
	}

	@Override
	public Object unwrap() {
		return this.request;
	}

	@Override
	public Object getAttribute(String key) {
		return this.request.getAttribute(key);
	}

	@Override
	public void setAttribute(String key, Object value) {
		this.request.setAttribute(key, value);
	}
}
