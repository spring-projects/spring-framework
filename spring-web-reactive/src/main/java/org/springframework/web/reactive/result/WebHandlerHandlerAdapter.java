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

package org.springframework.web.reactive.result;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.ServerWebExchange;

/**
 * Adapter to use a {@link WebHandler} through the {@link DispatcherHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class WebHandlerHandlerAdapter implements HandlerAdapter {

	private static final ResolvableType PUBLISHER_VOID = ResolvableType.forClassWithGenerics(
			Publisher.class, Void.class);


	@Override
	public boolean supports(Object handler) {
		return WebHandler.class.isAssignableFrom(handler.getClass());
	}

	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
		WebHandler webHandler = (WebHandler) handler;
		Mono<Void> mono = webHandler.handle(exchange);
		return Mono.just(new HandlerResult(webHandler, mono, PUBLISHER_VOID));
	}

}
