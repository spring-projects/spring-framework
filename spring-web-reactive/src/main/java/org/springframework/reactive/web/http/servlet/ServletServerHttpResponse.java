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
package org.springframework.reactive.web.http.servlet;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.reactivestreams.Publisher;
import reactor.Publishers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.reactive.web.http.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * @author Rossen Stoyanchev
 */
public class ServletServerHttpResponse implements ServerHttpResponse {

	private final HttpServletResponse servletResponse;

	private final ResponseBodySubscriber responseSubscriber;

	private final HttpHeaders headers;

	private boolean headersWritten = false;


	public ServletServerHttpResponse(HttpServletResponse servletResponse, ResponseBodySubscriber responseSubscriber) {
		Assert.notNull(servletResponse, "'servletResponse' must not be null");
		Assert.notNull(responseSubscriber, "'responseSubscriber' must not be null");
		this.servletResponse = servletResponse;
		this.responseSubscriber = responseSubscriber;
		this.headers = new HttpHeaders();
	}


	@Override
	public void setStatusCode(HttpStatus status) {
		this.servletResponse.setStatus(status.value());
	}

	@Override
	public HttpHeaders getHeaders() {
		return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	@Override
	public Publisher<Void> writeHeaders() {
		applyHeaders();
		return Publishers.empty();
	}

	@Override
	public Publisher<Void> writeWith(final Publisher<ByteBuffer> contentPublisher) {
		applyHeaders();
		return (s -> contentPublisher.subscribe(responseSubscriber));
	}

	private void applyHeaders() {
		if (!this.headersWritten) {
			for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
				String headerName = entry.getKey();
				for (String headerValue : entry.getValue()) {
					this.servletResponse.addHeader(headerName, headerValue);
				}
			}
			// HttpServletResponse exposes some headers as properties: we should include those if not already present
			if (this.servletResponse.getContentType() == null && this.headers.getContentType() != null) {
				this.servletResponse.setContentType(this.headers.getContentType().toString());
			}
			if (this.servletResponse.getCharacterEncoding() == null && this.headers.getContentType() != null &&
					this.headers.getContentType().getCharSet() != null) {
				this.servletResponse.setCharacterEncoding(this.headers.getContentType().getCharSet().name());
			}
			this.headersWritten = true;
		}
	}

}
