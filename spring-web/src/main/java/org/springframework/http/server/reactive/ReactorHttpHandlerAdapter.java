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

import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.HttpChannel;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.util.Assert;

/**
 * Adapt {@link HttpHandler} to the Reactor Netty channel handling function.
 *
 * @author Stephane Maldini
 * @since 5.0
 */
public class ReactorHttpHandlerAdapter implements Function<HttpChannel, Mono<Void>> {

	private final HttpHandler httpHandler;


	public ReactorHttpHandlerAdapter(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "'httpHandler' is required.");
		this.httpHandler = httpHandler;
	}


	@Override
	public Mono<Void> apply(HttpChannel channel) {
		NettyDataBufferFactory bufferFactory = new NettyDataBufferFactory(channel.delegate().alloc());
		ReactorServerHttpRequest adaptedRequest = new ReactorServerHttpRequest(channel, bufferFactory);
		ReactorServerHttpResponse adaptedResponse = new ReactorServerHttpResponse(channel, bufferFactory);
		return this.httpHandler.handle(adaptedRequest, adaptedResponse);
	}

}
