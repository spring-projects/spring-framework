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


import java.util.function.Function;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerRequest;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.NettyDataBufferFactory;

/**
 * Adapt {@link HttpHandler} using Vertx.
 *
 * @author Yevhenii Melnyk
 * @since 5.0
 */
public class VertxHttpHandlerAdapter extends HttpHandlerAdapterSupport implements Function<HttpServerRequest, Mono<Void>> {


	public VertxHttpHandlerAdapter(HttpHandler httpHandler) {
		super(httpHandler);
	}


	@Override
	public Mono<Void> apply(HttpServerRequest serverRequest) {
		NettyDataBufferFactory bufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
		VertxServerHttpRequest request = new VertxServerHttpRequest(serverRequest, bufferFactory);
		VertxServerHttpResponse response = new VertxServerHttpResponse(serverRequest.response(), bufferFactory);
		return getHttpHandler().handle(request, response)
				.otherwise(ex -> {
					logger.error("Could not complete request", ex);
					serverRequest.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
					serverRequest.response().end();
					return Mono.empty();
				})
				.doOnSuccess(v -> {
					if (!serverRequest.response().ended()) {
						serverRequest.response().end();
					}
					logger.debug("Successfully completed request");
				});
	}

}
