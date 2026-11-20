/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.annotation.support;

import java.security.Principal;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
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
	public @Nullable Object resolveArgument(MethodParameter parameter, Message<?> message){
		Principal user = SimpMessageHeaderAccessor.getUser(message.getHeaders());
		return parameter.isOptional() ? Optional.ofNullable(user) : user;
	}

}
