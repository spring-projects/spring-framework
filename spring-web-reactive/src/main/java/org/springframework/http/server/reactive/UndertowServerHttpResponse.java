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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.reactivestreams.Publisher;
import reactor.Flux;
import reactor.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the Undertow {@link HttpServerExchange}.
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 */
public class UndertowServerHttpResponse implements ServerHttpResponse {

	private final HttpServerExchange exchange;

	private final Function<Publisher<ByteBuffer>, Mono<Void>> responseBodyWriter;

	private final HttpHeaders headers;

	private boolean headersWritten = false;


	public UndertowServerHttpResponse(HttpServerExchange exchange,
			Function<Publisher<ByteBuffer>, Mono<Void>> responseBodyWriter) {

		Assert.notNull(exchange, "'exchange' is required.");
		Assert.notNull(responseBodyWriter, "'responseBodyWriter' must not be null");
		this.exchange = exchange;
		this.responseBodyWriter = responseBodyWriter;
		this.headers = new HttpHeaders();
	}


	public HttpServerExchange getUndertowExchange() {
		return this.exchange;
	}

	@Override
	public void setStatusCode(HttpStatus status) {
		Assert.notNull(status);
		getUndertowExchange().setStatusCode(status.value());
	}

	@Override
	public HttpHeaders getHeaders() {
		return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	@Override
	public Mono<Void> setBody(Publisher<ByteBuffer> publisher) {
		return Flux.from(publisher).lift(new WriteWithOperator<>(this::setBodyInternal)).after();
	}

	protected Mono<Void> setBodyInternal(Publisher<ByteBuffer> publisher) {
		writeHeaders();
		return this.responseBodyWriter.apply(publisher);
	}

	@Override
	public void writeHeaders() {
		if (!this.headersWritten) {
			for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
				HttpString headerName = HttpString.tryFromString(entry.getKey());
				this.exchange.getResponseHeaders().addAll(headerName, entry.getValue());
			}
			this.headersWritten = true;
		}
	}

}
