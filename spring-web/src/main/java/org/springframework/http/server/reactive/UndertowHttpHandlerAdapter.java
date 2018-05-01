/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.IOException;
import java.net.URISyntaxException;

import io.undertow.server.HttpServerExchange;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Adapt {@link HttpHandler} to the Undertow {@link io.undertow.server.HttpHandler}.
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 5.0
 */
public class UndertowHttpHandlerAdapter implements io.undertow.server.HttpHandler {

	private static final Log logger = LogFactory.getLog(UndertowHttpHandlerAdapter.class);


	private final HttpHandler httpHandler;

	private DataBufferFactory bufferFactory = new DefaultDataBufferFactory(false);


	public UndertowHttpHandlerAdapter(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "HttpHandler must not be null");
		this.httpHandler = httpHandler;
	}


	public void setDataBufferFactory(DataBufferFactory bufferFactory) {
		Assert.notNull(bufferFactory, "DataBufferFactory must not be null");
		this.bufferFactory = bufferFactory;
	}

	public DataBufferFactory getDataBufferFactory() {
		return this.bufferFactory;
	}


	@Override
	public void handleRequest(HttpServerExchange exchange) {
		ServerHttpRequest request = null;
		try {
			request = new UndertowServerHttpRequest(exchange, getDataBufferFactory());
		}
		catch (URISyntaxException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Invalid URL for incoming request: " + ex.getMessage());
			}
			exchange.setStatusCode(400);
			return;
		}
		ServerHttpResponse response = new UndertowServerHttpResponse(exchange, getDataBufferFactory());

		if (request.getMethod() == HttpMethod.HEAD) {
			response = new HttpHeadResponseDecorator(response);
		}

		HandlerResultSubscriber resultSubscriber = new HandlerResultSubscriber(exchange);
		this.httpHandler.handle(request, response).subscribe(resultSubscriber);
	}


	private class HandlerResultSubscriber implements Subscriber<Void> {

		private final HttpServerExchange exchange;

		public HandlerResultSubscriber(HttpServerExchange exchange) {
			this.exchange = exchange;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Void aVoid) {
			// no-op
		}

		@Override
		public void onError(Throwable ex) {
			logger.warn("Handling completed with error: " + ex.getMessage());
			if (this.exchange.isResponseStarted()) {
				try {
					logger.debug("Closing connection");
					this.exchange.getConnection().close();
				}
				catch (IOException ex2) {
					// ignore
				}
			}
			else {
				logger.debug("Setting response status code to 500");
				this.exchange.setStatusCode(500);
				this.exchange.endExchange();
			}
		}

		@Override
		public void onComplete() {
			logger.debug("Handling completed with success");
			this.exchange.endExchange();
		}
	}

}
