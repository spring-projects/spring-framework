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
 * Exception to be thrown when validation on an method parameter annotated with {@code @Valid} fails.
 * @author Brian Clozel
 * @since 4.0.1
 */
@SuppressWarnings("serial")
public class MethodArgumentNotValidException extends MessagingException {

	private final MethodParameter parameter;

	private final BindingResult bindingResult;


	public MethodArgumentNotValidException(Message<?> message, MethodParameter parameter, BindingResult bindingResult) {
		super(message);
		this.parameter = parameter;
		this.bindingResult = bindingResult;
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder("Validation failed for parameter at index ")
				.append(this.parameter.getParameterIndex()).append(" in method: ")
				.append(this.parameter.getMethod().toGenericString())
				.append(", with ").append(this.bindingResult.getErrorCount()).append(" error(s): ");
		for (ObjectError error : this.bindingResult.getAllErrors()) {
			sb.append("[").append(error).append("] ");
		}
		return sb.toString();
	}

}
