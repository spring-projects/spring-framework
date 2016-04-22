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

package org.springframework.web.reactive.result;

import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.ServerWebExchange;

/**
 * HandlerAdapter that allows using the plain {@link WebHandler} contract with
 * the generic {@link DispatcherHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class SimpleHandlerAdapter implements HandlerAdapter {

	private static final ResolvableType MONO_VOID = ResolvableType.forClassWithGenerics(
			Mono.class, Void.class);


	@Override
	public boolean supports(Object handler) {
		return WebHandler.class.isAssignableFrom(handler.getClass());
	}

	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
		WebHandler webHandler = (WebHandler) handler;
		Mono<Void> mono = webHandler.handle(exchange);
		return Mono.just(new HandlerResult(webHandler, mono, MONO_VOID));
	}

}
