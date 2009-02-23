/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.http.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;

import org.springframework.util.Assert;
import org.springframework.web.http.HttpHeaders;
import org.springframework.web.http.HttpMethod;

/**
 * {@link ServerHttpRequest} implementation that is based on a {@link HttpServletRequest}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class ServletServerHttpRequest implements ServerHttpRequest {

	private final HttpServletRequest servletRequest;

	private HttpHeaders headers;

	/**
	 * Constructs a new instance of the <code>ServletHttpRequest</code> based on the given {@link HttpServletRequest}
	 *
	 * @param servletRequest the HTTP Servlet request
	 */
	public ServletServerHttpRequest(HttpServletRequest servletRequest) {
		Assert.notNull(servletRequest, "'servletRequest' must not be null");
		this.servletRequest = servletRequest;
	}

	public HttpMethod getMethod() {
		return HttpMethod.valueOf(servletRequest.getMethod());
	}

	public HttpHeaders getHeaders() {
		if (headers == null) {
			headers = new HttpHeaders();
			for (Enumeration headerNames = servletRequest.getHeaderNames(); headerNames.hasMoreElements();) {
				String headerName = (String) headerNames.nextElement();
				for (Enumeration headerValues = servletRequest.getHeaders(headerName);
						headerValues.hasMoreElements();) {
					String headerValue = (String) headerValues.nextElement();
					headers.add(headerName, headerValue);
				}
			}
		}
		return headers;
	}

	public InputStream getBody() throws IOException {
		return servletRequest.getInputStream();
	}
}
