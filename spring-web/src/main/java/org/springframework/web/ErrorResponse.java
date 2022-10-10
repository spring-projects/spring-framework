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
 * headers, and an RFC 7808 formatted {@link ProblemDetail} body. Allows any
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
	 * Build a message code for the given exception type, which consists of
	 * {@code "problemDetail."} followed by the full {@link Class#getName() class name}.
	 * @param exceptionType the exception type for which to build a code
	 * @param suffix an optional suffix, e.g. for exceptions that may have multiple
	 * error message with different arguments.
	 */
	static String getDefaultDetailMessageCode(Class<?> exceptionType, @Nullable String suffix) {
		return "problemDetail." + exceptionType.getName() + (suffix != null ? "." + suffix : "");
	}

}
