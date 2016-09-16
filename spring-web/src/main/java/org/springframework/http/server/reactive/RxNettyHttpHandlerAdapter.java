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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.adapter.RxJava1Adapter;
import reactor.core.publisher.Mono;
import rx.Observable;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.util.Assert;

/**
 * Adapt {@link HttpHandler} to the RxNetty {@link RequestHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RxNettyHttpHandlerAdapter implements RequestHandler<ByteBuf, ByteBuf> {

	private static Log logger = LogFactory.getLog(RxNettyHttpHandlerAdapter.class);


	private final HttpHandler httpHandler;


	public RxNettyHttpHandlerAdapter(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "'httpHandler' is required");
		this.httpHandler = httpHandler;
	}


	@Override
	public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
		NettyDataBufferFactory bufferFactory = new NettyDataBufferFactory(response.unsafeNettyChannel().alloc());
		RxNettyServerHttpRequest adaptedRequest = new RxNettyServerHttpRequest(request, bufferFactory);
		RxNettyServerHttpResponse adaptedResponse = new RxNettyServerHttpResponse(response, bufferFactory);
		Publisher<Void> result = this.httpHandler.handle(adaptedRequest, adaptedResponse)
				.otherwise(ex -> {
					logger.debug("Could not complete request", ex);
					response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
					return Mono.empty();
				})
				.doOnSuccess(aVoid -> logger.debug("Successfully completed request"));
		return RxJava1Adapter.publisherToObservable(result);
	}

}
