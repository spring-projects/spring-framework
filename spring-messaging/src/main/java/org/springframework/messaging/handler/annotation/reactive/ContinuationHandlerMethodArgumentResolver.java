/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.handler.annotation.reactive;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver;

/**
 * No-op resolver for method arguments of type {@link kotlin.coroutines.Continuation}.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
public class ContinuationHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return "kotlin.coroutines.Continuation".equals(parameter.getParameterType().getName());
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, Message<?> message) {
		return Mono.empty();
	}
}
