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
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.web.http.HttpHeaders;
import org.springframework.web.http.HttpStatus;

/**
 * {@link ServerHttpResponse} implementation that is based on a {@link HttpServletResponse}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class ServletServerHttpResponse implements ServerHttpResponse {

	private final HttpServletResponse servletResponse;

	private final HttpHeaders headers = new HttpHeaders();

	private boolean headersWritten = false;

	/**
	 * Constructs a new instance of the <code>ServletHttpResponse</code> based on the given {@link HttpServletResponse}
	 *
	 * @param servletResponse the HTTP Servlet response
	 */
	public ServletServerHttpResponse(HttpServletResponse servletResponse) {
		Assert.notNull(servletResponse, "'servletResponse' must not be null");
		this.servletResponse = servletResponse;
	}

	public void setStatusCode(HttpStatus status) {
		servletResponse.setStatus(status.value());
	}

	public HttpHeaders getHeaders() {
		return headers;
	}

	public OutputStream getBody() throws IOException {
		writeHeaders();
		return servletResponse.getOutputStream();
	}

	private void writeHeaders() {
		if (!headersWritten) {
			for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
				String headerName = entry.getKey();
				for (String headerValue : entry.getValue()) {
					servletResponse.addHeader(headerName, headerValue);
				}
			}
			headersWritten = true;
		}
	}

	public void close() {
		writeHeaders();
	}
}
