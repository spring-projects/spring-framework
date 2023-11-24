/*
 * Copyright 2002-2022 the original author or authors.
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

import java.net.URISyntaxException;
import java.util.function.BiFunction;

import io.netty5.handler.codec.http.HttpResponseStatus;
import org.apache.commons.logging.Log;
import reactor.core.publisher.Mono;
import reactor.netty5.http.server.HttpServerRequest;
import reactor.netty5.http.server.HttpServerResponse;

import org.springframework.core.io.buffer.Netty5DataBufferFactory;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Adapt {@link HttpHandler} to the Reactor Netty 5 channel handling function.
 *
 * <p>This class is based on {@link ReactorHttpHandlerAdapter}.
 *
 * @author Violeta Georgieva
 * @since 6.0
 */
public class ReactorNetty2HttpHandlerAdapter implements BiFunction<HttpServerRequest, HttpServerResponse, Mono<Void>> {

	private static final Log logger = HttpLogging.forLogName(ReactorNetty2HttpHandlerAdapter.class);


	private final HttpHandler httpHandler;


	public ReactorNetty2HttpHandlerAdapter(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "HttpHandler must not be null");
		this.httpHandler = httpHandler;
	}


	@Override
	public Mono<Void> apply(HttpServerRequest reactorRequest, HttpServerResponse reactorResponse) {
		Netty5DataBufferFactory bufferFactory = new Netty5DataBufferFactory(reactorResponse.alloc());
		try {
			ReactorNetty2ServerHttpRequest request = new ReactorNetty2ServerHttpRequest(reactorRequest, bufferFactory);
			ServerHttpResponse response = new ReactorNetty2ServerHttpResponse(reactorResponse, bufferFactory);

			if (request.getMethod() == HttpMethod.HEAD) {
				response = new HttpHeadResponseDecorator(response);
			}

			return this.httpHandler.handle(request, response)
					.doOnError(ex -> logger.trace(request.getLogPrefix() + "Failed to complete: " + ex.getMessage()))
					.doOnSuccess(aVoid -> logger.trace(request.getLogPrefix() + "Handling completed"));
		}
		catch (URISyntaxException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to get request URI: " + ex.getMessage());
			}
			reactorResponse.status(HttpResponseStatus.BAD_REQUEST);
			return Mono.empty();
		}
	}

}
