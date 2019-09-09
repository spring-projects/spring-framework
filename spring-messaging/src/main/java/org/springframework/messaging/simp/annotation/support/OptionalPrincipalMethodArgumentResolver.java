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

package org.springframework.messaging.simp.annotation.support;

import java.lang.reflect.Type;
import java.security.Principal;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

/**
 * {@link HandlerMethodArgumentResolver} to a {@link Optional} of type {@link Principal}.
 *
 * @author Ekaterina Cherepanova
 * @since 5.x
 */
public class OptionalPrincipalMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Type genericParamType = parameter.getGenericParameterType();
		ResolvableType resolvableType = ResolvableType.forType(genericParamType).as(Optional.class);
		Class<?> genericClass = resolvableType.getGeneric().resolve(Object.class);
		return Principal.class.isAssignableFrom(genericClass);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		Principal user = SimpMessageHeaderAccessor.getUser(message.getHeaders());
		return Optional.ofNullable(user);
	}
}
