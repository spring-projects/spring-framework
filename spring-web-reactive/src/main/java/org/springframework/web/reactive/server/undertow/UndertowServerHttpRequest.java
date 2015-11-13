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

package org.springframework.web.reactive.server.undertow;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import org.reactivestreams.Publisher;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.ReactiveServerHttpRequest;

/**
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 */
class UndertowServerHttpRequest implements ReactiveServerHttpRequest {

	private final HttpServerExchange exchange;

	private final Publisher<ByteBuffer> body;

	private HttpHeaders headers;


	public UndertowServerHttpRequest(HttpServerExchange exchange, Publisher<ByteBuffer> body) {
		this.exchange = exchange;
		this.body = body;
	}


	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.exchange.getRequestMethod().toString());
	}

	@Override
	public URI getURI() {
		try {
			return new URI(this.exchange.getRequestScheme(), null, this.exchange.getHostName(),
					this.exchange.getHostPort(), this.exchange.getRequestURI(),
					this.exchange.getQueryString(), null);
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

	@Override
	public Publisher<ByteBuffer> getBody() {
		return this.body;
	}

}
