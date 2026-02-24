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
import java.nio.charset.Charset;

import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.core.SpringProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * {@link ServerHttpResponse} implementation that is based on a {@link HttpServletResponse}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.0
 */
public class ServletServerHttpResponse implements ServerHttpResponse {

	/**
	 * System property that indicates whether {@code response.getBody().flush()}
	 * should effectively flush to the network. This is by default disabled,
	 * and developers must set the {@code "spring.http.response.flush.enabled"}
	 * {@link org.springframework.core.SpringProperties Spring property} to
	 * turn it on.
	 * <p>Applications should instead {@link #flush()} on the response directly.
	 */
	public static final String BODY_FLUSH_ENABLED = "spring.http.response.flush.enabled";

	private final boolean flushEnabled = SpringProperties.getFlag(BODY_FLUSH_ENABLED);

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
		this.headers = new HttpHeaders(new ServletResponseHeadersAdapter(servletResponse));
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

	/**
	 * Return the body of the message as an output stream.
	 * <p>By default, flushing the output stream has no effect
	 * (see {@link #BODY_FLUSH_ENABLED}) and should be performed
	 * using {@link #flush()} instead.
	 * @return the output stream body (never {@code null})
	 * @throws IOException in case of I/O errors
	 */
	@Override
	public OutputStream getBody() throws IOException {
		this.bodyUsed = true;
		writeHeaders();
		return (this.flushEnabled) ? this.servletResponse.getOutputStream() :
				StreamUtils.nonFlushing(this.servletResponse.getOutputStream());
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
			// HttpServletResponse exposes some headers as properties: we should include those if not already present
			if (this.servletResponse.getContentType() == null && this.headers.containsHeader(HttpHeaders.CONTENT_TYPE)) {
				this.servletResponse.setContentType(this.headers.getFirst(HttpHeaders.CONTENT_TYPE));
			}
			if (this.servletResponse.getCharacterEncoding() == null && this.headers.containsHeader(HttpHeaders.CONTENT_TYPE)) {
				try {
					// Lazy parsing into MediaType
					MediaType contentType = this.headers.getContentType();
					if (contentType != null) {
						Charset charset = contentType.getCharset();
						if (charset != null) {
							this.servletResponse.setCharacterEncoding(charset);
						}
					}
				}
				catch (Exception ex) {
					// Leave character encoding unspecified
				}
			}
			this.headersWritten = true;
		}
	}

}
