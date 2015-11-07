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

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import org.springframework.http.server.ReactiveServerHttpRequest;
import org.springframework.http.server.ReactiveServerHttpResponse;
import org.springframework.reactive.web.http.HttpHandler;
import org.springframework.util.Assert;

import io.undertow.server.HttpServerExchange;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * @author Marek Hawrylczak
 */
class RequestHandlerAdapter implements io.undertow.server.HttpHandler {

	private final HttpHandler httpHandler;

	public RequestHandlerAdapter(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "'httpHandler' is required.");
		this.httpHandler = httpHandler;
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		RequestBodyPublisher requestBodyPublisher = new RequestBodyPublisher(exchange);
		ReactiveServerHttpRequest request =
				new UndertowServerHttpRequest(exchange, requestBodyPublisher);

		ResponseBodySubscriber responseBodySubscriber = new ResponseBodySubscriber(exchange);
		ReactiveServerHttpResponse response =
				new UndertowServerHttpResponse(exchange, responseBodySubscriber);

		exchange.dispatch();
		this.httpHandler.handle(request, response).subscribe(new Subscriber<Void>() {
			@Override
			public void onSubscribe(Subscription subscription) {
				subscription.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(Void aVoid) {
			}

			@Override
			public void onError(Throwable t) {
				if (!exchange.isResponseStarted() &&
						exchange.getStatusCode() < INTERNAL_SERVER_ERROR.value()) {

					exchange.setStatusCode(INTERNAL_SERVER_ERROR.value());
				}
				exchange.endExchange();
			}

			@Override
			public void onComplete() {
				exchange.endExchange();
			}
		});
	}
}
