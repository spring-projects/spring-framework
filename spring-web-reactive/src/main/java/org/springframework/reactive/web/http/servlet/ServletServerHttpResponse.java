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
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import org.reactivestreams.Publisher;
import reactor.Publishers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ReactiveServerHttpResponse;
import org.springframework.util.Assert;

/**
 * @author Rossen Stoyanchev
 */
public class ServletServerHttpResponse implements ReactiveServerHttpResponse {

	private final HttpServletResponse response;

	private final ResponseBodySubscriber subscriber;

	private final HttpHeaders headers;

	private boolean headersWritten = false;


	public ServletServerHttpResponse(HttpServletResponse response,
			ResponseBodySubscriber subscriber) {

		Assert.notNull(response, "'response' must not be null");
		Assert.notNull(subscriber, "'subscriber' must not be null");
		this.response = response;
		this.subscriber = subscriber;
		this.headers = new HttpHeaders();
	}


	@Override
	public void setStatusCode(HttpStatus status) {
		this.response.setStatus(status.value());
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
	public Publisher<Void> setBody(final Publisher<ByteBuffer> contentPublisher) {
		applyHeaders();
		return (s -> contentPublisher.subscribe(subscriber));
	}

	private void applyHeaders() {
		if (!this.headersWritten) {
			for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
				String headerName = entry.getKey();
				for (String headerValue : entry.getValue()) {
					this.response.addHeader(headerName, headerValue);
				}
			}
			MediaType contentType = this.headers.getContentType();
			if (this.response.getContentType() == null && contentType != null) {
				this.response.setContentType(contentType.toString());
			}
			Charset charset = (contentType != null ? contentType.getCharSet() : null);
			if (this.response.getCharacterEncoding() == null && charset != null) {
				this.response.setCharacterEncoding(charset.name());
			}
			this.headersWritten = true;
		}
	}

}
