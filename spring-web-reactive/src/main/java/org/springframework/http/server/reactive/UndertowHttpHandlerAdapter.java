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

import org.springframework.util.Assert;

import io.undertow.server.HttpServerExchange;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;


/**
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 */
public class UndertowHttpHandlerAdapter implements io.undertow.server.HttpHandler {

	private static Log logger = LogFactory.getLog(UndertowHttpHandlerAdapter.class);


	private final HttpHandler delegate;


	public UndertowHttpHandlerAdapter(HttpHandler delegate) {
		Assert.notNull(delegate, "'delegate' is required.");
		this.delegate = delegate;
	}


	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {

		ServerHttpRequest request = new UndertowServerHttpRequest(exchange);
		ServerHttpResponse response = new UndertowServerHttpResponse(exchange);

		exchange.dispatch();

		this.delegate.handle(request, response).subscribe(new Subscriber<Void>() {

			@Override
			public void onSubscribe(Subscription subscription) {
				subscription.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(Void aVoid) {
				// no op
			}

			@Override
			public void onError(Throwable ex) {
				if (exchange.isResponseStarted() || exchange.getStatusCode() > 500) {
					logger.error("Error from request handling. Completing the request.", ex);
				}
				else {
					exchange.setStatusCode(500);
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
