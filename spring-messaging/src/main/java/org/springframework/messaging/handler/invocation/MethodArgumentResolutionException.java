/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.handler.invocation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Common exception resulting from the invocation of
 * {@link org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver}.
 *
 * @author Juergen Hoeller
 * @since 4.3.6
 * @see HandlerMethodArgumentResolver
 */
@SuppressWarnings("serial")
public class MethodArgumentResolutionException extends MessagingException {

	private final MethodParameter parameter;


	/**
	 * Create a new instance providing the invalid {@code MethodParameter}.
	 */
	public MethodArgumentResolutionException(Message<?> message, MethodParameter parameter) {
		super(message, getMethodParameterMessage(parameter));
		this.parameter = parameter;
	}

	/**
	 * Create a new instance providing the invalid {@code MethodParameter} and
	 * a prepared description.
	 */
	public MethodArgumentResolutionException(Message<?> message, MethodParameter parameter, String description) {
		super(message, getMethodParameterMessage(parameter) + ": " + description);
		this.parameter = parameter;
	}

	/**
	 * Create a new instance providing the invalid {@code MethodParameter},
	 * prepared description, and a cause.
	 */
	public MethodArgumentResolutionException(
			Message<?> message, MethodParameter parameter, String description, @Nullable Throwable cause) {

		super(message, getMethodParameterMessage(parameter) + ": " + description, cause);
		this.parameter = parameter;
	}


	/**
	 * Return the MethodParameter that was rejected.
	 */
	public final MethodParameter getMethodParameter() {
		return this.parameter;
	}


	private static String getMethodParameterMessage(MethodParameter parameter) {
		return "Could not resolve method parameter at index " + parameter.getParameterIndex() +
				" in " + parameter.getExecutable().toGenericString();
	}

}
