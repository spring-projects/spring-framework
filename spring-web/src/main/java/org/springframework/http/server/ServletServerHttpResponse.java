/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@link ServerHttpResponse} implementation that is based on a {@link HttpServletResponse}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public class ServletServerHttpResponse implements ServerHttpResponse {

	private final HttpServletResponse servletResponse;

	private final HttpHeaders headers;

	private boolean headersWritten = false;

	private boolean bodyUsed = false;

	private @Nullable HttpHeaders readOnlyHeaders;


	/**
	 * Construct a new instance of the ServletServerHttpResponse based on the given {@link HttpServletResponse}.
	 * @param servletResponse the servlet response
	 */
	public ServletServerHttpResponse(HttpServletResponse servletResponse) {
		Assert.notNull(servletResponse, "HttpServletResponse must not be null");
		this.servletResponse = servletResponse;
		this.headers = new ServletResponseHttpHeaders();
	}


	/**
	 * Return the {@code HttpServletResponse} this object is based on.
	 */
	public HttpServletResponse getServletResponse() {
		return this.servletResponse;
	}

	@Override
	public void setStatusCode(HttpStatusCode status) {
		Assert.notNull(status, "HttpStatusCode must not be null");
		this.servletResponse.setStatus(status.value());
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.readOnlyHeaders != null) {
			return this.readOnlyHeaders;
		}
		else if (this.headersWritten) {
			this.readOnlyHeaders = HttpHeaders.readOnlyHttpHeaders(this.headers);
			return this.readOnlyHeaders;
		}
		else {
			return this.headers;
		}
	}

	@Override
	public OutputStream getBody() throws IOException {
		this.bodyUsed = true;
		writeHeaders();
		return this.servletResponse.getOutputStream();
	}

	@Override
	public void flush() throws IOException {
		writeHeaders();
		if (this.bodyUsed) {
			this.servletResponse.flushBuffer();
		}
	}

	@Override
	public void close() {
		writeHeaders();
	}

	private void writeHeaders() {
		if (!this.headersWritten) {
			getHeaders().forEach((headerName, headerValues) -> {
				for (String headerValue : headerValues) {
					this.servletResponse.addHeader(headerName, headerValue);
				}
			});
			// HttpServletResponse exposes some headers as properties: we should include those if not already present
			MediaType contentTypeHeader = this.headers.getContentType();
			if (this.servletResponse.getContentType() == null && contentTypeHeader != null) {
				this.servletResponse.setContentType(contentTypeHeader.toString());
			}
			if (this.servletResponse.getCharacterEncoding() == null && contentTypeHeader != null &&
					contentTypeHeader.getCharset() != null) {
				this.servletResponse.setCharacterEncoding(contentTypeHeader.getCharset().name());
			}
			long contentLength = getHeaders().getContentLength();
			if (contentLength != -1) {
				this.servletResponse.setContentLengthLong(contentLength);
			}
			this.headersWritten = true;
		}
	}


	/**
	 * Extends HttpHeaders with the ability to look up headers already present in
	 * the underlying HttpServletResponse.
	 *
	 * <p>The intent is merely to expose what is available through the HttpServletResponse
	 * i.e. the ability to look up specific header values by name. All other
	 * map-related operations (for example, iteration, removal, etc) apply only to values
	 * added directly through HttpHeaders methods.
	 *
	 * @since 4.0.3
	 */
	private class ServletResponseHttpHeaders extends HttpHeaders {

		private static final long serialVersionUID = 3410708522401046302L;

		@Override
		public boolean containsHeader(String key) {
			return (super.containsHeader(key) || (get(key) != null));
		}

		@Override
		public @Nullable String getFirst(String headerName) {
			if (headerName.equalsIgnoreCase(CONTENT_TYPE)) {
				// Content-Type is written as an override so check super first
				String value = super.getFirst(headerName);
				return (value != null ? value : servletResponse.getContentType());
			}
			else {
				String value = servletResponse.getHeader(headerName);
				return (value != null ? value : super.getFirst(headerName));
			}
		}

		@Override
		public @Nullable List<String> get(String headerName) {
			if (headerName.equalsIgnoreCase(CONTENT_TYPE)) {
				// Content-Type is written as an override so don't merge
				String value = getFirst(headerName);
				return (value != null ? Collections.singletonList(value) : null);
			}

			Collection<String> values1 = servletResponse.getHeaders(headerName);
			if (headersWritten) {
				return new ArrayList<>(values1);
			}
			boolean isEmpty1 = CollectionUtils.isEmpty(values1);

			List<String> values2 = super.get(headerName);
			boolean isEmpty2 = CollectionUtils.isEmpty(values2);

			if (isEmpty1 && isEmpty2) {
				return null;
			}

			List<String> values = new ArrayList<>();
			if (!isEmpty1) {
				values.addAll(values1);
			}
			if (!isEmpty2) {
				values.addAll(values2);
			}
			return values;
		}
	}

}
