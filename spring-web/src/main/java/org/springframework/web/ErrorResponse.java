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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;


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

}
