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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ReactiveServerHttpResponse;
import org.springframework.util.Assert;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

/**
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 */
class UndertowServerHttpResponse implements ReactiveServerHttpResponse {

	private final HttpServerExchange exchange;

	private final ResponseBodySubscriber bodySubscriber;

	private final HttpHeaders headers = new HttpHeaders();

	private boolean headersWritten = false;


	public UndertowServerHttpResponse(HttpServerExchange exchange, ResponseBodySubscriber body) {
		this.exchange = exchange;
		this.bodySubscriber = body;
	}


	@Override
	public void setStatusCode(HttpStatus status) {
		Assert.notNull(status);
		this.exchange.setStatusCode(status.value());
	}


	@Override
	public Publisher<Void> setBody(Publisher<ByteBuffer> bodyPublisher) {
		applyHeaders();
		return (subscriber -> bodyPublisher.subscribe(bodySubscriber));
	}

	@Override
	public HttpHeaders getHeaders() {
		return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	@Override
	public Publisher<Void> writeHeaders() {
		applyHeaders();
		return s -> s.onSubscribe(new Subscription() {
			@Override
			public void request(long n) {
				s.onComplete();
			}

			@Override
			public void cancel() {
			}
		});
	}

	private void applyHeaders() {
		if (!this.headersWritten) {
			for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
				HttpString headerName = HttpString.tryFromString(entry.getKey());
				this.exchange.getResponseHeaders().addAll(headerName, entry.getValue());

			}
			this.headersWritten = true;
		}
	}

}
