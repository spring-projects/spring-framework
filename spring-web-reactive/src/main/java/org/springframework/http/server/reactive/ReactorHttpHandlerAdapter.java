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

import reactor.core.publisher.Mono;
import reactor.io.buffer.Buffer;
import reactor.io.ipc.ChannelFluxHandler;
import reactor.io.netty.http.HttpChannel;

import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.util.Assert;

/**
 * @author Stephane Maldini
 */
public class ReactorHttpHandlerAdapter
		implements ChannelFluxHandler<Buffer, Buffer, HttpChannel<Buffer, Buffer>> {

	private final HttpHandler httpHandler;

	private final DataBufferAllocator allocator;

	public ReactorHttpHandlerAdapter(HttpHandler httpHandler,
			DataBufferAllocator allocator) {
		Assert.notNull(httpHandler, "'httpHandler' is required.");
		this.httpHandler = httpHandler;
		this.allocator = allocator;
	}

	@Override
	public Mono<Void> apply(HttpChannel<Buffer, Buffer> channel) {
		ReactorServerHttpRequest adaptedRequest =
				new ReactorServerHttpRequest(channel, allocator);
		ReactorServerHttpResponse adaptedResponse = new ReactorServerHttpResponse(channel);
		return this.httpHandler.handle(adaptedRequest, adaptedResponse);
	}

}
