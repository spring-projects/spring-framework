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

package org.springframework.http.server.reactive;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import org.reactivestreams.Publisher;
import reactor.Flux;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpRequest} to the Underow {@link HttpServerExchange}.
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 */
public class UndertowServerHttpRequest implements ServerHttpRequest {

	private final HttpServerExchange exchange;

	private URI uri;

	private HttpHeaders headers;

	private final Flux<ByteBuffer> body;


	public UndertowServerHttpRequest(HttpServerExchange exchange, Publisher<ByteBuffer> body) {
		Assert.notNull(exchange, "'exchange' is required.");
		Assert.notNull(exchange, "'body' is required.");
		this.exchange = exchange;
		this.body = Flux.from(body);
	}


	public HttpServerExchange getUndertowExchange() {
		return this.exchange;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.getUndertowExchange().getRequestMethod().toString());
	}

	@Override
	public URI getURI() {
		if (this.uri == null) {
			try {
				return new URI(this.getUndertowExchange().getRequestScheme(), null,
						this.getUndertowExchange().getHostName(),
						this.getUndertowExchange().getHostPort(),
						this.getUndertowExchange().getRequestURI(),
						this.getUndertowExchange().getQueryString(), null);
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException("Could not get URI: " + ex.getMessage(), ex);
			}
		}
		return this.uri;
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			for (HeaderValues values : this.getUndertowExchange().getRequestHeaders()) {
				this.headers.put(values.getHeaderName().toString(), values);
			}
		}
		return this.headers;
	}

	@Override
	public Flux<ByteBuffer> getBody() {
		return this.body;
	}

}
