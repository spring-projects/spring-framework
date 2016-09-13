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

package org.springframework.web.reactive.function.support;

import java.lang.reflect.Method;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.function.HandlerFunction;
import org.springframework.web.reactive.function.Request;
import org.springframework.web.reactive.function.Response;
import org.springframework.web.reactive.function.RoutingFunctions;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code HandlerAdapter} implementation that supports {@link HandlerFunction}s.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class HandlerFunctionAdapter implements HandlerAdapter {

	private static final MethodParameter HANDLER_FUNCTION_RETURN_TYPE;

	static {
		try {
			Method method = HandlerFunction.class.getMethod("handle", Request.class);
			HANDLER_FUNCTION_RETURN_TYPE = new MethodParameter(method, -1);
		}
		catch (NoSuchMethodException ex) {
			throw new Error(ex);
		}
	}

	@Override
	public boolean supports(Object handler) {
		return handler instanceof HandlerFunction;
	}

	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
		HandlerFunction<?> handlerFunction = (HandlerFunction<?>) handler;
		Request request =
				exchange.<Request>getAttribute(RoutingFunctions.REQUEST_ATTRIBUTE)
						.orElseThrow(() -> new IllegalStateException("Could not find Request in exchange attributes"));

		Response<?> response = handlerFunction.handle(request);
		HandlerResult handlerResult =
				new HandlerResult(handlerFunction, response, HANDLER_FUNCTION_RETURN_TYPE);
		return Mono.just(handlerResult);
	}
}
