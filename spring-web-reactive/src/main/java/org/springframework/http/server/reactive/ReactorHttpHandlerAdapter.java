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
import reactor.core.publisher.Mono;
import reactor.io.ipc.ChannelHandler;
import reactor.io.netty.http.HttpChannel;

import org.springframework.core.io.buffer.NettyDataBufferAllocator;
import org.springframework.util.Assert;

/**
 * @author Stephane Maldini
 */
public class ReactorHttpHandlerAdapter
		implements ChannelHandler<ByteBuf, ByteBuf, HttpChannel> {

	private final HttpHandler httpHandler;

	public ReactorHttpHandlerAdapter(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "'httpHandler' is required.");
		this.httpHandler = httpHandler;
	}

	@Override
	public Mono<Void> apply(HttpChannel channel) {
		NettyDataBufferAllocator allocator =
				new NettyDataBufferAllocator(channel.delegate().alloc());

		ReactorServerHttpRequest adaptedRequest =
				new ReactorServerHttpRequest(channel, allocator);
		ReactorServerHttpResponse adaptedResponse =
				new ReactorServerHttpResponse(channel, allocator);
		return this.httpHandler.handle(adaptedRequest, adaptedResponse);
	}

}
