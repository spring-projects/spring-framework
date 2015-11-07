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

package org.springframework.reactive.web.http.undertow;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.ReactiveServerHttpRequest;
import org.springframework.util.StringUtils;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import org.reactivestreams.Publisher;

/**
 * @author Marek Hawrylczak
 */
class UndertowServerHttpRequest implements ReactiveServerHttpRequest {

	private final HttpServerExchange exchange;

	private final Publisher<ByteBuffer> requestBodyPublisher;

	private HttpHeaders headers;

	public UndertowServerHttpRequest(HttpServerExchange exchange,
			Publisher<ByteBuffer> requestBodyPublisher) {

		this.exchange = exchange;
		this.requestBodyPublisher = requestBodyPublisher;
	}

	@Override
	public Publisher<ByteBuffer> getBody() {
		return this.requestBodyPublisher;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.exchange.getRequestMethod().toString());
	}

	@Override
	public URI getURI() {
		try {
			StringBuilder uri = new StringBuilder(this.exchange.getRequestPath());
			if (StringUtils.hasLength(this.exchange.getQueryString())) {
				uri.append('?').append(this.exchange.getQueryString());
			}
			return new URI(uri.toString());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not get URI: " + ex.getMessage(), ex);
		}
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			for (HeaderValues headerValues : this.exchange.getRequestHeaders()) {
				for (String value : headerValues) {
					this.headers.add(headerValues.getHeaderName().toString(), value);
				}
			}
		}
		return this.headers;
	}
}
