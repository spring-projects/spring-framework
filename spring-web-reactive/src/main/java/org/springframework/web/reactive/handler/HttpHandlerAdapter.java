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

package org.springframework.web.reactive.handler;

import org.reactivestreams.Publisher;
import reactor.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.DispatcherHandler;

/**
 * Support use of {@link HttpHandler} with
 * {@link DispatcherHandler
 * DispatcherHandler} (which implements the same contract).
 * The use of {@code DispatcherHandler} this way enables routing requests to
 * one of many {@code HttpHandler} instances depending on the configured
 * handler mappings.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class HttpHandlerAdapter implements HandlerAdapter {

	private static final ResolvableType PUBLISHER_VOID = ResolvableType.forClassWithGenerics(
			Publisher.class, Void.class);


	@Override
	public boolean supports(Object handler) {
		return HttpHandler.class.isAssignableFrom(handler.getClass());
	}

	@Override
	public Mono<HandlerResult> handle(ServerHttpRequest request,
			ServerHttpResponse response, Object handler) {

		HttpHandler httpHandler = (HttpHandler)handler;
		Mono<Void> completion = httpHandler.handle(request, response);
		return Mono.just(new HandlerResult(httpHandler, completion, PUBLISHER_VOID));
	}

}
