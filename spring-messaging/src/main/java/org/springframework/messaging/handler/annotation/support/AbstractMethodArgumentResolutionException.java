/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.messaging.handler.annotation.support;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Base class for exceptions resulting from the invocation of
 * {@link org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
@SuppressWarnings("serial")
public abstract class AbstractMethodArgumentResolutionException extends MessagingException {

	private final MethodParameter parameter;


	/**
	 * Create a new instance providing the invalid {@code MethodParameter}.
	 */
	protected AbstractMethodArgumentResolutionException(Message<?> message, MethodParameter parameter) {
		this(message, parameter, getMethodParamMessage(parameter));
	}

	/**
	 * Create a new instance providing the invalid {@code MethodParameter} and
	 * a prepared description. Subclasses should prepend the description with
	 * the help of {@link #getMethodParamMessage(org.springframework.core.MethodParameter)}.
	 */
	protected AbstractMethodArgumentResolutionException(Message<?> message, MethodParameter param, String description) {
		super(message, description);
		this.parameter = param;
	}


	/**
	 * Return the MethodParameter that was rejected.
	 */
	public final MethodParameter getMethodParameter() {
		return this.parameter;
	}


	protected static String getMethodParamMessage(MethodParameter param) {
		return new StringBuilder("Could not resolve method parameter at index ")
				.append(param.getParameterIndex()).append(" in method: ")
				.append(param.getMethod().toGenericString()).append(" ").toString();
	}

}
