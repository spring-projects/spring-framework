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

package org.springframework.reactive.web.dispatch.handler;

import org.reactivestreams.Publisher;
import reactor.Publishers;

import org.springframework.http.server.ReactiveServerHttpRequest;
import org.springframework.http.server.ReactiveServerHttpResponse;
import org.springframework.reactive.web.dispatch.HandlerAdapter;
import org.springframework.reactive.web.dispatch.HandlerResult;
import org.springframework.reactive.web.http.HttpHandler;

/**
 * Support use of {@link HttpHandler} with
 * {@link org.springframework.reactive.web.dispatch.DispatcherHandler
 * DispatcherHandler} (which implements the same contract).
 * The use of {@code DispatcherHandler} this way enables routing requests to
 * one of many {@code HttpHandler} instances depending on the configured
 * handler mappings.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class HttpHandlerAdapter implements HandlerAdapter {


	@Override
	public boolean supports(Object handler) {
		return HttpHandler.class.isAssignableFrom(handler.getClass());
	}

	@Override
	public Publisher<HandlerResult> handle(ReactiveServerHttpRequest request,
			ReactiveServerHttpResponse response, Object handler) {

		HttpHandler httpHandler = (HttpHandler)handler;
		Publisher<Void> completion = httpHandler.handle(request, response);
		return Publishers.just(new HandlerResult(httpHandler, completion));
	}

}
