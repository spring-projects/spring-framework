/*
 * Copyright 2002-2014 the original author or authors.
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
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

/**
 * Exception to be thrown when a method argument is not valid. For instance, this
 * can be issued if a validation on a method parameter annotated with
 * {@code @Valid} fails.
 *
 * @author Brian Clozel
 * @since 4.0.1
 */
@SuppressWarnings("serial")
public class MethodArgumentNotValidException extends MessagingException {

	/**
	 * Create a new message with the given description.
	 * @see #getMessage()
	 */
	public MethodArgumentNotValidException(Message<?> message, String description) {
		super(message, description);
	}

	/**
	 * Create a new instance with a failed validation described by
	 * the given {@link BindingResult}.
	 */
	public MethodArgumentNotValidException(Message<?> message,
			MethodParameter parameter, BindingResult bindingResult) {
		this(message, generateMessage(parameter, bindingResult));
	}

	private static String generateMessage(MethodParameter parameter, BindingResult bindingResult) {
		StringBuilder sb = new StringBuilder("Validation failed for parameter at index ")
				.append(parameter.getParameterIndex()).append(" in method: ")
				.append(parameter.getMethod().toGenericString())
				.append(", with ").append(bindingResult.getErrorCount()).append(" error(s): ");
		for (ObjectError error : bindingResult.getAllErrors()) {
			sb.append("[").append(error).append("] ");
		}
		return sb.toString();
	}

}
