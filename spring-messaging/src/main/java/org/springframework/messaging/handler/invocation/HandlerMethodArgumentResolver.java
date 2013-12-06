/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.handler.invocation;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;

/**
 * Strategy interface for resolving method parameters into argument values in
 * the context of a given {@link Message}.
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
	 * @param parameter the method parameter to resolve. This parameter must
	 * have previously been passed to
	 * {@link #supportsParameter(org.springframework.core.MethodParameter)}
	 * and it must have returned {@code true}
	 * @param message the currently processed message
	 * @return the resolved argument value, or {@code null}.
	 * @throws Exception in case of errors with the preparation of argument values
	 */
	Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception;

}
