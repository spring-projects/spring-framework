/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.invocation;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;

/**
 * Strategy interface for resolving method parameters into argument values
 * in the context of a given {@link Message}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface HandlerMethodArgumentResolver {

	/**
	 * Whether the given {@linkplain MethodParameter method parameter} is
	 * supported by this resolver.
	 * @param parameter the method parameter to check
	 * @return {@code true} if this resolver supports the supplied parameter;
	 * {@code false} otherwise
	 */
	boolean supportsParameter(MethodParameter parameter);

	/**
	 * Resolves a method parameter into an argument value from a given message.
	 * @param parameter the method parameter to resolve.
	 * This parameter must have previously been passed to
	 * {@link #supportsParameter(org.springframework.core.MethodParameter)}
	 * which must have returned {@code true}.
	 * @param message the currently processed message
	 * @return the resolved argument value, or {@code null}
	 * @throws Exception in case of errors with the preparation of argument values
	 */
	@Nullable Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception;

}
