/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;

/**
 * Strategy to render an RFC 9457 {@link ProblemDetail} as a view, typically
 * an HTML error page, instead of writing it with message converters.
 *
 * <p>This is called when handling {@code ProblemDetail}, {@link ErrorResponse}
 * and {@code ResponseEntity<ProblemDetail>} values, if the client prefers HTML
 * through content negotiation. This applies to problem details returned
 * by {@code @ExceptionHandler} methods.
 *
 * <p>Before view resolution, the problem detail is passed to
 * {@link ErrorResponse.Interceptor}s, so that it has the same content as
 * when it is written with message converters.
 *
 * @author Brian Clozel
 * @since 7.1
 * @see org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver#setErrorResponseViewResolver(ErrorResponseViewResolver)
 */
@FunctionalInterface
public interface ErrorResponseViewResolver {

	/**
	 * Resolve the view to render for the given problem detail.
	 * <p>The returned {@code ModelAndView} can declare a status; by default,
	 * the status of the response entity or problem detail is used.
	 * @param request the current request
	 * @param problemDetail the problem detail to render
	 * @param errorResponse the error response the problem detail is from, if any
	 * @return the {@code ModelAndView} to render, or {@code null} to write the
	 * problem detail with message converters
	 */
	@Nullable ModelAndView resolveErrorView(
			HttpServletRequest request, ProblemDetail problemDetail, @Nullable ErrorResponse errorResponse);

}
