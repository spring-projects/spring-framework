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

package org.springframework.web;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;


/**
 * Representation of a complete RFC 7807 error response including status,
 * headers, and an RFC 7807 formatted {@link ProblemDetail} body. Allows any
 * exception to expose HTTP error response information.
 *
 * <p>{@link ErrorResponseException} is a default implementation of this
 * interface and a convenient base class for other exceptions to use.
 *
 * <p>{@code ErrorResponse} is supported as a return value from
 * {@code @ExceptionHandler} methods that render directly to the response, e.g.
 * by being marked {@code @ResponseBody}, or declared in an
 * {@code @RestController} or {@code RestControllerAdvice} class.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 * @see ErrorResponseException
 */
public interface ErrorResponse {

	/**
	 * Return the HTTP status code to use for the response.
	 */
	HttpStatusCode getStatusCode();

	/**
	 * Return headers to use for the response.
	 */
	default HttpHeaders getHeaders() {
		return HttpHeaders.EMPTY;
	}

	/**
	 * Return the body for the response, formatted as an RFC 7807
	 * {@link ProblemDetail} whose {@link ProblemDetail#getStatus() status}
	 * should match the response status.
	 */
	ProblemDetail getBody();

	/**
	 * Return a code to use to resolve the problem "detail" for this exception
	 * through a {@link MessageSource}.
	 * <p>By default this is initialized via
	 * {@link #getDefaultDetailMessageCode(Class, String)}.
	 */
	default String getDetailMessageCode() {
		return getDefaultDetailMessageCode(getClass(), null);
	}

	/**
	 * Return arguments to use along with a {@link #getDetailMessageCode()
	 * message code} to resolve the problem "detail" for this exception
	 * through a {@link MessageSource}. The arguments are expanded
	 * into placeholders of the message value, e.g. "Invalid content type {0}".
	 */
	@Nullable
	default Object[] getDetailMessageArguments() {
		return null;
	}

	/**
	 * Variant of {@link #getDetailMessageArguments()} that uses the given
	 * {@link MessageSource} for resolving the message argument values.
	 * This is useful for example to message codes from validation errors.
	 */
	@Nullable
	default Object[] getDetailMessageArguments(MessageSource messageSource, Locale locale) {
		return getDetailMessageArguments();
	}

	/**
	 * Return a code to use to resolve the problem "detail" for this exception
	 * through a {@link MessageSource}.
	 * <p>By default this is initialized via
	 * {@link #getDefaultDetailMessageCode(Class, String)}.
	 */
	default String getTitleMessageCode() {
		return getDefaultTitleMessageCode(getClass());
	}

	/**
	 * Resolve the {@link #getDetailMessageCode() detailMessageCode} and the
	 * {@link #getTitleMessageCode() titleCode} through the given
	 * {@link MessageSource}, and if found, update the "detail" and "title!"
	 * fields respectively.
	 * @param messageSource the {@code MessageSource} to use for the lookup
	 * @param locale the {@code Locale} to use for the lookup
	 */
	default ProblemDetail updateAndGetBody(@Nullable MessageSource messageSource, Locale locale) {
		if (messageSource != null) {
			Object[] arguments = getDetailMessageArguments(messageSource, locale);
			String detail = messageSource.getMessage(getDetailMessageCode(), arguments, null, locale);
			if (detail != null) {
				getBody().setDetail(detail);
			}
			String title = messageSource.getMessage(getTitleMessageCode(), null, null, locale);
			if (title != null) {
				getBody().setTitle(title);
			}
		}
		return getBody();
	}


	/**
	 * Build a message code for the "detail" field, for the given exception type.
	 * @param exceptionType the exception type associated with the problem
	 * @param suffix an optional suffix, e.g. for exceptions that may have multiple
	 * error message with different arguments.
	 * @return {@code "problemDetail."} followed by the full {@link Class#getName() class name}
	 */
	static String getDefaultDetailMessageCode(Class<?> exceptionType, @Nullable String suffix) {
		return "problemDetail." + exceptionType.getName() + (suffix != null ? "." + suffix : "");
	}

	/**
	 * Build a message code for the "title" field, for the given exception type.
	 * @param exceptionType the exception type associated with the problem
	 * @return {@code "problemDetail.title."} followed by the full {@link Class#getName() class name}
	 */
	static String getDefaultTitleMessageCode(Class<?> exceptionType) {
		return "problemDetail.title." + exceptionType.getName();
	}

	/**
	 * Map the given Exception to an {@link ErrorResponse}.
	 * @param ex the Exception, mostly to derive message codes, if not provided
	 * @param status the response status to use
	 * @param headers optional headers to add to the response
	 * @param defaultDetail default value for the "detail" field
	 * @param detailMessageCode the code to use to look up the "detail" field
	 * through a {@code MessageSource}, falling back on
	 * {@link #getDefaultDetailMessageCode(Class, String)}
	 * @param detailMessageArguments the arguments to go with the detailMessageCode
	 * @return the created {@code ErrorResponse} instance
	 */
	static ErrorResponse createFor(
			Exception ex, HttpStatusCode status, @Nullable HttpHeaders headers,
			String defaultDetail, @Nullable String detailMessageCode, @Nullable Object[] detailMessageArguments) {

		if (detailMessageCode == null) {
			detailMessageCode = ErrorResponse.getDefaultDetailMessageCode(ex.getClass(), null);
		}

		ErrorResponseException errorResponse = new ErrorResponseException(
				status, ProblemDetail.forStatusAndDetail(status, defaultDetail), null,
				detailMessageCode, detailMessageArguments);

		if (headers != null) {
			errorResponse.getHeaders().putAll(headers);
		}

		return errorResponse;
	}

}
