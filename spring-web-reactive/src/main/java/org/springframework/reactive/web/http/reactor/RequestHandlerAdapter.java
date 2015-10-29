/*
 * Copyright (c) 2011-2015 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.reactive.web.http.reactor;

import org.reactivestreams.Publisher;
import reactor.core.publisher.convert.DependencyUtils;
import reactor.io.buffer.Buffer;
import reactor.io.net.ReactiveChannelHandler;
import reactor.io.net.http.HttpChannel;

import org.springframework.reactive.web.http.HttpHandler;
import org.springframework.util.Assert;

/**
 * @author Stephane Maldini
 */
public class RequestHandlerAdapter
		implements ReactiveChannelHandler<Buffer, Buffer, HttpChannel<Buffer, Buffer>> {

	private final HttpHandler httpHandler;


	public RequestHandlerAdapter(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "'httpHandler' is required.");
		this.httpHandler = httpHandler;
	}

	@Override
	public Publisher<Void> apply(HttpChannel<Buffer, Buffer> channel) {
		final PublisherReactorServerHttpRequest adaptedRequest;
		final PublisherReactorServerHttpResponse adaptedResponse;

		if(DependencyUtils.hasReactorStream()){
			adaptedRequest = new ReactorServerHttpRequest(channel);
			adaptedResponse = new ReactorServerHttpResponse(channel);
		}
		else{
			adaptedRequest = new PublisherReactorServerHttpRequest(channel);
			adaptedResponse = new PublisherReactorServerHttpResponse(channel);
		}
		return this.httpHandler.handle(adaptedRequest, adaptedResponse);
	}
}
