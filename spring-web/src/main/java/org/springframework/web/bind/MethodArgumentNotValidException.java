/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.ErrorResponse;

/**
 * Exception to be thrown when validation on an argument annotated with {@code @Valid} fails.
 * Extends {@link BindException} as of 5.3.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
@SuppressWarnings("serial")
public class MethodArgumentNotValidException extends BindException implements ErrorResponse {

	private final MethodParameter parameter;

	private final ProblemDetail body;


	/**
	 * Constructor for {@link MethodArgumentNotValidException}.
	 * @param parameter the parameter that failed validation
	 * @param bindingResult the results of the validation
	 */
	public MethodArgumentNotValidException(MethodParameter parameter, BindingResult bindingResult) {
		super(bindingResult);
		this.parameter = parameter;
		this.body = ProblemDetail.forStatusAndDetail(getStatusCode(), "Invalid request content.");
	}


	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatus.BAD_REQUEST;
	}

	@Override
	public ProblemDetail getBody() {
		return this.body;
	}

	/**
	 * Return the method parameter that failed validation.
	 */
	public final MethodParameter getParameter() {
		return this.parameter;
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder("Validation failed for argument [")
				.append(this.parameter.getParameterIndex()).append("] in ")
				.append(this.parameter.getExecutable().toGenericString());
		BindingResult bindingResult = getBindingResult();
		if (bindingResult.getErrorCount() > 1) {
			sb.append(" with ").append(bindingResult.getErrorCount()).append(" errors");
		}
		sb.append(": ");
		for (ObjectError error : bindingResult.getAllErrors()) {
			sb.append('[').append(error).append("] ");
		}
		return sb.toString();
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] {
				errorsToStringList(getBindingResult().getGlobalErrors()),
				errorsToStringList(getBindingResult().getFieldErrors())
		};
	}

	@Override
	public Object[] getDetailMessageArguments(MessageSource messageSource, Locale locale) {
		return new Object[] {
				errorsToStringList(getBindingResult().getGlobalErrors(), messageSource, locale),
				errorsToStringList(getBindingResult().getFieldErrors(), messageSource, locale)
		};
	}

	/**
	 * Convert each given {@link ObjectError} to a String in single quotes, taking
	 * either the error's default message, or its error code.
	 * @since 6.0
	 */
	public static List<String> errorsToStringList(List<? extends ObjectError> errors) {
		return errorsToStringList(errors, error ->
				error.getDefaultMessage() != null ? error.getDefaultMessage() : error.getCode());
	}

	/**
	 * Variant of {@link #errorsToStringList(List)} that uses a
	 * {@link MessageSource} to resolve the message code of the error, or fall
	 * back on the error's default message.
	 * @since 6.0
	 */
	@SuppressWarnings("ConstantConditions")
	public static List<String> errorsToStringList(
			List<? extends ObjectError> errors, MessageSource source, Locale locale) {

		return errorsToStringList(errors, error -> source.getMessage(
				error.getCode(), error.getArguments(), error.getDefaultMessage(), locale));
	}

	private static List<String> errorsToStringList(
			List<? extends ObjectError> errors, Function<ObjectError, String> formatter) {

		List<String> result = new ArrayList<>(errors.size());
		for (ObjectError error : errors) {
			String value = formatter.apply(error);
			if (StringUtils.hasText(value)) {
				result.add(error instanceof FieldError fieldError ?
						fieldError.getField() + ": '" + value + "'" : "'" + value + "'");
			}
		}
		return result;
	}

}
