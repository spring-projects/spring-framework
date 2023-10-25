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

package org.springframework.web.bind;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.util.BindErrorUtils;

/**
 * Exception to be thrown when validation on an argument annotated with {@code @Valid} fails.
 * Extends {@link BindException} as of 5.3.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
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


	/**
	 * Return the method parameter that failed validation.
	 */
	public final MethodParameter getParameter() {
		return this.parameter;
	}

	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatus.BAD_REQUEST;
	}

	@Override
	public ProblemDetail getBody() {
		return this.body;
	}

	@Override
	public Object[] getDetailMessageArguments(MessageSource source, Locale locale) {
		return new Object[] {
				BindErrorUtils.resolveAndJoin(getGlobalErrors(), source, locale),
				BindErrorUtils.resolveAndJoin(getFieldErrors(), source, locale)};
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] {
				BindErrorUtils.resolveAndJoin(getGlobalErrors()),
				BindErrorUtils.resolveAndJoin(getFieldErrors())};
	}

	/**
	 * Convert each given {@link ObjectError} to a String.
	 * @since 6.0
	 * @deprecated in favor of using {@link BindErrorUtils} and
	 * {@link #getAllErrors()}, to be removed in 6.2
	 */
	@Deprecated(since = "6.1", forRemoval = true)
	public static List<String> errorsToStringList(List<? extends ObjectError> errors) {
		return BindErrorUtils.resolve(errors).values().stream().toList();
	}

	/**
	 * Convert each given {@link ObjectError} to a String, and use a
	 * {@link MessageSource} to resolve each error.
	 * @since 6.0
	 * @deprecated in favor of {@link BindErrorUtils}, to be removed in 6.2
	 */
	@Deprecated(since = "6.1", forRemoval = true)
	public static List<String> errorsToStringList(
			List<? extends ObjectError> errors, @Nullable MessageSource messageSource, Locale locale) {

		return (messageSource != null ?
				BindErrorUtils.resolve(errors, messageSource, locale).values().stream().toList() :
				BindErrorUtils.resolve(errors).values().stream().toList());
	}

	/**
	 * Resolve global and field errors to messages with the given
	 * {@link MessageSource} and {@link Locale}.
	 * @return a Map with errors as keys and resolved messages as values
	 * @since 6.0.3
	 * @deprecated in favor of using {@link BindErrorUtils} and
	 * {@link #getAllErrors()}, to be removed in 6.2
	 */
	@Deprecated(since = "6.1", forRemoval = true)
	public Map<ObjectError, String> resolveErrorMessages(MessageSource messageSource, Locale locale) {
		return BindErrorUtils.resolve(getAllErrors(), messageSource, locale);
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

}
