/*
 * Copyright 2002-2015 the original author or authors.
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

import reactor.Mono;
import reactor.io.buffer.Buffer;
import reactor.io.net.ReactiveChannelHandler;
import reactor.io.net.http.HttpChannel;

import org.springframework.util.Assert;

/**
 * @author Stephane Maldini
 */
public class ReactorHttpHandlerAdapter
		implements ReactiveChannelHandler<Buffer, Buffer, HttpChannel<Buffer, Buffer>> {

	private final HttpHandler httpHandler;


	public ReactorHttpHandlerAdapter(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "'httpHandler' is required.");
		this.httpHandler = httpHandler;
	}

	@Override
	public Mono<Void> apply(HttpChannel<Buffer, Buffer> channel) {
		ReactorServerHttpRequest adaptedRequest = new ReactorServerHttpRequest(channel);
		ReactorServerHttpResponse adaptedResponse = new ReactorServerHttpResponse(channel);
		return this.httpHandler.handle(adaptedRequest, adaptedResponse);
	}

}
