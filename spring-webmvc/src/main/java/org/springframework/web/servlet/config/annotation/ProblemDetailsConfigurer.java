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

package org.springframework.web.servlet.config.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.web.servlet.ErrorResponseViewResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

/**
 * Configure how RFC 9457 problem details are produced and rendered.
 *
 * <p>For now, {@link org.springframework.web.ErrorResponse.Interceptor ErrorResponse.Interceptor}s
 * are configured separately, see
 * {@link WebMvcConfigurer#addErrorResponseInterceptors(java.util.List)}.
 *
 * @author Brian Clozel
 * @since 7.1
 * @see WebMvcConfigurer#configureProblemDetails(ProblemDetailsConfigurer)
 */
public class ProblemDetailsConfigurer {

	private boolean renderUnhandledExceptions;

	private @Nullable ErrorResponseViewResolver viewResolver;


	/**
	 * Whether to render exceptions that are not handled by any
	 * {@code @ExceptionHandler} method as problem details, instead of only
	 * setting the response status and leaving the response body to the
	 * Servlet container.
	 * <p>By default, this is set to {@code false}.
	 * @param renderUnhandledExceptions whether to render unhandled exceptions
	 * as problem details
	 * @see ExceptionHandlerExceptionResolver#setRenderUnhandledExceptionsAsProblemDetails(boolean)
	 */
	public ProblemDetailsConfigurer renderUnhandledExceptions(boolean renderUnhandledExceptions) {
		this.renderUnhandledExceptions = renderUnhandledExceptions;
		return this;
	}

	/**
	 * Configure an {@link ErrorResponseViewResolver} to render problem details
	 * returned by {@code @ExceptionHandler} methods as views, typically HTML
	 * error pages, when the client prefers HTML.
	 * <p>By default, problem details are always written with message converters.
	 * @param viewResolver the resolver to use
	 * @see ExceptionHandlerExceptionResolver#setErrorResponseViewResolver(ErrorResponseViewResolver)
	 */
	public ProblemDetailsConfigurer viewResolver(ErrorResponseViewResolver viewResolver) {
		this.viewResolver = viewResolver;
		return this;
	}


	protected boolean isRenderUnhandledExceptions() {
		return this.renderUnhandledExceptions;
	}

	protected @Nullable ErrorResponseViewResolver getViewResolver() {
		return this.viewResolver;
	}

}
