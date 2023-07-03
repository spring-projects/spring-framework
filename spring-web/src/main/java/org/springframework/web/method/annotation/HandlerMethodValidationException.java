/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.BindErrorUtils;

/**
 * {@link ResponseStatusException} that is also {@link MethodValidationResult}.
 * Raised by {@link HandlerMethodValidator} in case of method validation errors
 * on a web controller method.
 *
 * <p>The {@link #getStatusCode()} is 400 for input validation errors, and 500
 * for validation errors on a return value.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
@SuppressWarnings("serial")
public class HandlerMethodValidationException extends ResponseStatusException implements MethodValidationResult {

	private final MethodValidationResult validationResult;


	public HandlerMethodValidationException(MethodValidationResult validationResult) {
		super(initHttpStatus(validationResult), "Validation failure", null, null, null);
		this.validationResult = validationResult;
	}

	private static HttpStatus initHttpStatus(MethodValidationResult validationResult) {
		return (!validationResult.isForReturnValue() ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR);
	}


	@Override
	public Object[] getDetailMessageArguments(MessageSource messageSource, Locale locale) {
		return new Object[] { BindErrorUtils.resolveAndJoin(getAllErrors(), messageSource, locale) };
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] { BindErrorUtils.resolveAndJoin(getAllErrors()) };
	}

	@Override
	public Object getTarget() {
		return this.validationResult.getTarget();
	}

	@Override
	public Method getMethod() {
		return this.validationResult.getMethod();
	}

	@Override
	public boolean isForReturnValue() {
		return this.validationResult.isForReturnValue();
	}

	@Override
	public List<ParameterValidationResult> getAllValidationResults() {
		return this.validationResult.getAllValidationResults();
	}

}
