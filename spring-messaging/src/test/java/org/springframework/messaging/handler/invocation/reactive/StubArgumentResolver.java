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

package org.springframework.messaging.handler.invocation.reactive;

import java.util.ArrayList;
import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * Stub resolver for a fixed value type and/or value.
 *
 * @author Rossen Stoyanchev
 */
public class StubArgumentResolver implements HandlerMethodArgumentResolver {

	private final Class<?> valueType;

	@Nullable
	private final Object value;

	private List<MethodParameter> resolvedParameters = new ArrayList<>();


	public StubArgumentResolver(Object value) {
		this(value.getClass(), value);
	}

	public StubArgumentResolver(Class<?> valueType) {
		this(valueType, null);
	}

	public StubArgumentResolver(Class<?> valueType, Object value) {
		this.valueType = valueType;
		this.value = value;
	}


	public List<MethodParameter> getResolvedParameters() {
		return resolvedParameters;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getParameterType().equals(this.valueType);
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, Message<?> message) {
		this.resolvedParameters.add(parameter);
		return Mono.justOrEmpty(this.value);
	}

}
