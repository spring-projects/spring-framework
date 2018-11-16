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

import java.net.URISyntaxException;
import java.util.function.BiFunction;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.server.HttpServerRequest;
import reactor.ipc.netty.http.server.HttpServerResponse;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Adapt {@link HttpHandler} to the Reactor Netty channel handling function.
 *
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorHttpHandlerAdapter implements BiFunction<HttpServerRequest, HttpServerResponse, Mono<Void>> {

	// 5.0.x only: no buffer pooling
	private static final NettyDataBufferFactory BUFFER_FACTORY =
			new NettyDataBufferFactory(new UnpooledByteBufAllocator(false));

	private static final Log logger = LogFactory.getLog(ReactorHttpHandlerAdapter.class);


	private final HttpHandler httpHandler;


	public ReactorHttpHandlerAdapter(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "HttpHandler must not be null");
		this.httpHandler = httpHandler;
	}


	@Override
	public Mono<Void> apply(HttpServerRequest request, HttpServerResponse response) {
		ServerHttpRequest adaptedRequest;
		ServerHttpResponse adaptedResponse;
		try {
			adaptedRequest = new ReactorServerHttpRequest(request, BUFFER_FACTORY);
			adaptedResponse = new ReactorServerHttpResponse(response, BUFFER_FACTORY);
		}
		catch (URISyntaxException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Invalid URL for incoming request: " + ex.getMessage());
			}
			response.status(HttpResponseStatus.BAD_REQUEST);
			return Mono.empty();
		}

		if (adaptedRequest.getMethod() == HttpMethod.HEAD) {
			adaptedResponse = new HttpHeadResponseDecorator(adaptedResponse);
		}

		return this.httpHandler.handle(adaptedRequest, adaptedResponse)
				.doOnError(ex -> logger.warn("Handling completed with error: " + ex.getMessage()))
				.doOnSuccess(aVoid -> logger.debug("Handling completed with success"));
	}

}
