/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

/**
 * Exception to be thrown when a method argument fails validation perhaps as a
 * result of {@code @Valid} style validation, or perhaps because it is required.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.0.1
 */
@SuppressWarnings("serial")
public class MethodArgumentNotValidException extends MethodArgumentResolutionException {

	@Nullable
	private final BindingResult bindingResult;


	/**
	 * Create a new instance with the invalid {@code MethodParameter}.
	 */
	public MethodArgumentNotValidException(Message<?> message, MethodParameter parameter) {
		super(message, parameter);
		this.bindingResult = null;
	}

	/**
	 * Create a new instance with the invalid {@code MethodParameter} and a
	 * {@link org.springframework.validation.BindingResult}.
	 */
	public MethodArgumentNotValidException(Message<?> message, MethodParameter parameter, BindingResult bindingResult) {
		super(message, parameter, getValidationErrorMessage(bindingResult));
		this.bindingResult = bindingResult;
	}


	/**
	 * Return the BindingResult if the failure is validation-related,
	 * or {@code null} if none.
	 */
	@Nullable
	public final BindingResult getBindingResult() {
		return this.bindingResult;
	}


	private static String getValidationErrorMessage(BindingResult bindingResult) {
		StringBuilder sb = new StringBuilder();
		sb.append(bindingResult.getErrorCount()).append(" error(s): ");
		for (ObjectError error : bindingResult.getAllErrors()) {
			sb.append("[").append(error).append("] ");
		}
		return sb.toString();
	}

}
