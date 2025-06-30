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

package org.springframework.web.servlet.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Abstract base class for
 * {@link org.springframework.web.servlet.HandlerExceptionResolver HandlerExceptionResolver}
 * implementations that support handling exceptions from handlers of type {@link HandlerMethod}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class AbstractHandlerMethodExceptionResolver extends AbstractHandlerExceptionResolver {

	/**
	 * Checks if the handler is a {@link HandlerMethod} or the resolver has global exception
	 * handlers and then delegates to the base class implementation of {@code #shouldApplyTo}
	 * passing the bean of the {@code HandlerMethod} if necessary. Otherwise, returns {@code false}.
	 * @see HandlerMethod
	 * @see #hasGlobalExceptionHandlers()
	 */
	@Override
	protected boolean shouldApplyTo(HttpServletRequest request, @Nullable Object handler) {
		if (handler instanceof HandlerMethod handlerMethod) {
			return super.shouldApplyTo(request, handlerMethod.getBean());
		}
		else if (handler == null || (hasGlobalExceptionHandlers() && hasHandlerMappings())) {
			return super.shouldApplyTo(request, handler);
		}
		else {
			return false;
		}
	}

	/**
	 * Whether this resolver has global exception handlers, for example, not declared in
	 * the same class as the {@code HandlerMethod} that raised the exception and
	 * therefore can apply to any handler.
	 * @since 5.3
	 */
	protected boolean hasGlobalExceptionHandlers() {
		return false;
	}

	@Override
	protected final @Nullable ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

		HandlerMethod handlerMethod = (handler instanceof HandlerMethod hm ? hm : null);
		return doResolveHandlerMethodException(request, response, handlerMethod, ex);
	}

	/**
	 * Actually resolve the given exception that got thrown during on handler execution,
	 * returning a ModelAndView that represents a specific error page if appropriate.
	 * <p>May be overridden in subclasses, in order to apply specific exception checks.
	 * Note that this template method will be invoked <i>after</i> checking whether this
	 * resolved applies ("mappedHandlers" etc), so an implementation may simply proceed
	 * with its actual exception handling.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handlerMethod the executed handler method, or {@code null} if none chosen at the time
	 * of the exception (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to, or {@code null} for default processing
	 */
	protected abstract @Nullable ModelAndView doResolveHandlerMethodException(
			HttpServletRequest request, HttpServletResponse response, @Nullable HandlerMethod handlerMethod, Exception ex);

}
