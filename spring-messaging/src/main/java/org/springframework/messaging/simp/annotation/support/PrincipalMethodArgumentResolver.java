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

import java.security.Principal;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

/**
 * Resolver for arguments of type {@link Principal}, including {@code Optional<Principal>}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class PrincipalMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		MethodParameter nestedParameter = parameter.nestedIfOptional();
		Class<?> paramType = nestedParameter.getNestedParameterType();
		return Principal.class.isAssignableFrom(paramType);
	}

	@Override
	@Nullable
	public Object resolveArgument(MethodParameter parameter, Message<?> message){
		Principal user = SimpMessageHeaderAccessor.getUser(message.getHeaders());
		return parameter.isOptional() ? Optional.ofNullable(user) : user;
	}

}
