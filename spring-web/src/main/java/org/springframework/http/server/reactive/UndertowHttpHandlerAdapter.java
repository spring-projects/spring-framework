/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpLogging;
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

	private static final Log logger = HttpLogging.forLogName(UndertowHttpHandlerAdapter.class);


	private final HttpHandler httpHandler;

	private DataBufferFactory bufferFactory = DefaultDataBufferFactory.sharedInstance;


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
		UndertowServerHttpRequest request = null;
		try {
			request = new UndertowServerHttpRequest(exchange, getDataBufferFactory());
		}
		catch (URISyntaxException ex) {
			if (logger.isWarnEnabled()) {
				logger.debug("Failed to get request URI: " + ex.getMessage());
			}
			exchange.setStatusCode(400);
			return;
		}
		ServerHttpResponse response = new UndertowServerHttpResponse(exchange, getDataBufferFactory(), request);

		if (request.getMethod() == HttpMethod.HEAD) {
			response = new HttpHeadResponseDecorator(response);
		}

		HandlerResultSubscriber resultSubscriber = new HandlerResultSubscriber(exchange, request);
		this.httpHandler.handle(request, response).subscribe(resultSubscriber);
	}


	private class HandlerResultSubscriber implements Subscriber<Void> {

		private final HttpServerExchange exchange;

		private final String logPrefix;


		public HandlerResultSubscriber(HttpServerExchange exchange, UndertowServerHttpRequest request) {
			this.exchange = exchange;
			this.logPrefix = request.getLogPrefix();
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
			logger.trace(this.logPrefix + "Failed to complete: " + ex.getMessage());
			if (this.exchange.isResponseStarted()) {
				try {
					logger.debug(this.logPrefix + "Closing connection");
					this.exchange.getConnection().close();
				}
				catch (IOException ex2) {
					// ignore
				}
			}
			else {
				logger.debug(this.logPrefix + "Setting HttpServerExchange status to 500 Server Error");
				this.exchange.setStatusCode(500);
				this.exchange.endExchange();
			}
		}

		@Override
		public void onComplete() {
			logger.trace(this.logPrefix + "Handling completed");
			this.exchange.endExchange();
		}
	}

}
