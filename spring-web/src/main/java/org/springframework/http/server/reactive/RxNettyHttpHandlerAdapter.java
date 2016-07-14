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
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import org.reactivestreams.Publisher;
import reactor.core.converter.RxJava1ObservableConverter;
import rx.Observable;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.util.Assert;

/**
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RxNettyHttpHandlerAdapter implements RequestHandler<ByteBuf, ByteBuf> {

	private final HttpHandler httpHandler;

	public RxNettyHttpHandlerAdapter(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "'httpHandler' is required");
		this.httpHandler = httpHandler;
	}

	@Override
	public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
		NettyDataBufferFactory dataBufferFactory =
				new NettyDataBufferFactory(response.unsafeNettyChannel().alloc());

		RxNettyServerHttpRequest adaptedRequest =
				new RxNettyServerHttpRequest(request, dataBufferFactory);
		RxNettyServerHttpResponse adaptedResponse =
				new RxNettyServerHttpResponse(response, dataBufferFactory);
		Publisher<Void> result = this.httpHandler.handle(adaptedRequest, adaptedResponse);
		return RxJava1ObservableConverter.fromPublisher(result);
	}

}
