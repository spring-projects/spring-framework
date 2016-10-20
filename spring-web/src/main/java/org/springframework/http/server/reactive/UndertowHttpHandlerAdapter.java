/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.Map;

import io.undertow.server.HttpServerExchange;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.Assert;

/**
 * Adapt {@link HttpHandler} to the Undertow {@link io.undertow.server.HttpHandler}.
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 5.0
 */
public class UndertowHttpHandlerAdapter extends HttpHandlerAdapterSupport
		implements io.undertow.server.HttpHandler {

	private DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory(false);


	public UndertowHttpHandlerAdapter(HttpHandler httpHandler) {
		super(httpHandler);
	}

	public UndertowHttpHandlerAdapter(Map<String, HttpHandler> handlerMap) {
		super(handlerMap);
	}


	public void setDataBufferFactory(DataBufferFactory dataBufferFactory) {
		Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
		this.dataBufferFactory = dataBufferFactory;
	}


	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {

		ServerHttpRequest request = new UndertowServerHttpRequest(exchange, this.dataBufferFactory);
		ServerHttpResponse response = new UndertowServerHttpResponse(exchange, this.dataBufferFactory);

		getHttpHandler().handle(request, response).subscribe(new Subscriber<Void>() {
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
				logger.error("Could not complete request", ex);
				if (!exchange.isResponseStarted() && exchange.getStatusCode() <= 500) {
					exchange.setStatusCode(500);
				}
				exchange.endExchange();
			}
			@Override
			public void onComplete() {
				logger.debug("Successfully completed request");
				exchange.endExchange();
			}
		});
	}

}
