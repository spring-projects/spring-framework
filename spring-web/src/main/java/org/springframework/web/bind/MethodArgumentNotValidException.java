/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.bind;

import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

/**
 * Exception to be thrown when validation on an argument annotated with {@code @Valid} fails.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@SuppressWarnings("serial")
public class MethodArgumentNotValidException extends Exception {

	private final MethodParameter parameter;

	private final BindingResult bindingResult;


	/**
	 * Constructor for {@link MethodArgumentNotValidException}.
	 * @param parameter the parameter that failed validation
	 * @param bindingResult the results of the validation
	 */
	public MethodArgumentNotValidException(MethodParameter parameter, BindingResult bindingResult) {
		this.parameter = parameter;
		this.bindingResult = bindingResult;
	}

	/**
	 * Return the method parameter that failed validation.
	 */
	public MethodParameter getParameter() {
		return this.parameter;
	}

	/**
	 * Return the results of the failed validation.
	 */
	public BindingResult getBindingResult() {
		return this.bindingResult;
	}


	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder("Validation failed for argument at index ")
			.append(this.parameter.getParameterIndex()).append(" in method: ")
			.append(this.parameter.getExecutable().toGenericString())
			.append(", with ").append(this.bindingResult.getErrorCount()).append(" error(s): ");
		for (ObjectError error : this.bindingResult.getAllErrors()) {
			sb.append("[").append(error).append("] ");
		}
		return sb.toString();
	}

}
