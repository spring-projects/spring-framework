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

import java.lang.reflect.Method;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;

/**
 * HandlerAdapter that allows using the plain {@link WebHandler} contract with
 * the generic {@link DispatcherHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class SimpleHandlerAdapter implements HandlerAdapter {

	private static final MethodParameter RETURN_TYPE;

	static {
		try {
			Method method = WebHandler.class.getMethod("handle", ServerWebExchange.class);
			RETURN_TYPE = new MethodParameter(method, -1);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException(
					"Failed to initialize the return type for WebHandler: " + ex.getMessage());
		}
	}


	@Override
	public boolean supports(Object handler) {
		return WebHandler.class.isAssignableFrom(handler.getClass());
	}

	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
		WebHandler webHandler = (WebHandler) handler;
		Mono<Void> mono = webHandler.handle(exchange);
		return mono.then(aVoid -> Mono.empty());
	}

}
