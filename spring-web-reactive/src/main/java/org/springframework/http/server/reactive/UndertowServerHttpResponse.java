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
import java.util.function.Function;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.reactivestreams.Publisher;
import reactor.Publishers;

import org.springframework.http.ExtendedHttpHeaders;
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

	private final Function<Publisher<ByteBuffer>, Publisher<Void>> responseBodyWriter;

	private final HttpHeaders headers;


	public UndertowServerHttpResponse(HttpServerExchange exchange,
			Function<Publisher<ByteBuffer>, Publisher<Void>> responseBodyWriter) {

		Assert.notNull(exchange, "'exchange' is required.");
		Assert.notNull(responseBodyWriter, "'responseBodyWriter' must not be null");
		this.exchange = exchange;
		this.responseBodyWriter = responseBodyWriter;
		this.headers = new ExtendedHttpHeaders(new UndertowHeaderChangeListener());
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
		return this.headers;
	}

	@Override
	public Publisher<Void> setBody(Publisher<ByteBuffer> publisher) {
		return Publishers.lift(publisher, new WriteWithOperator<>(this::setBodyInternal));
	}

	protected Publisher<Void> setBodyInternal(Publisher<ByteBuffer> publisher) {
		return this.responseBodyWriter.apply(publisher);
	}


	private class UndertowHeaderChangeListener implements ExtendedHttpHeaders.HeaderChangeListener {

		@Override
		public void headerAdded(String name, String value) {
			HttpString headerName = HttpString.tryFromString(name);
			getUndertowExchange().getResponseHeaders().add(headerName, value);
		}

		@Override
		public void headerPut(String key, List<String> values) {
			HttpString headerName = HttpString.tryFromString(key);
			getUndertowExchange().getResponseHeaders().putAll(headerName, values);
		}

		@Override
		public void headerRemoved(String key) {
			getUndertowExchange().getResponseHeaders().remove(key);
		}
	}

}
