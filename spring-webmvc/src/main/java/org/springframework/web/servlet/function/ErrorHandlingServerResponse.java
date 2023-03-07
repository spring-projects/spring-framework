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

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.ModelAndView;

/**
 * Base class for {@link ServerResponse} implementations with error handling.
 * @author Arjen Poutsma
 * @since 5.3
 */
abstract class ErrorHandlingServerResponse implements ServerResponse {

	protected final Log logger = LogFactory.getLog(getClass());

	private final List<ErrorHandler<?>> errorHandlers = new ArrayList<>();


	protected final <T extends ServerResponse> void addErrorHandler(Predicate<Throwable> predicate,
			BiFunction<Throwable, ServerRequest, T> errorHandler) {

		Assert.notNull(predicate, "Predicate must not be null");
		Assert.notNull(errorHandler, "ErrorHandler must not be null");
		this.errorHandlers.add(new ErrorHandler<>(predicate, errorHandler));
	}

	@Nullable
	protected final ModelAndView handleError(Throwable t, HttpServletRequest servletRequest,
			HttpServletResponse servletResponse, Context context) throws ServletException, IOException {

		ServerResponse serverResponse = errorResponse(t, servletRequest);
		if (serverResponse != null) {
			return serverResponse.writeTo(servletRequest, servletResponse, context);
		}
		else if (t instanceof ServletException servletException) {
			throw servletException;
		}
		else if (t instanceof IOException ioException ) {
			throw ioException;
		}
		else {
			throw new ServletException(t);
		}
	}

	@Nullable
	protected final ServerResponse errorResponse(Throwable t, HttpServletRequest servletRequest) {
		for (ErrorHandler<?> errorHandler : this.errorHandlers) {
			if (errorHandler.test(t)) {
				ServerRequest serverRequest = (ServerRequest)
						servletRequest.getAttribute(RouterFunctions.REQUEST_ATTRIBUTE);
				return errorHandler.handle(t, serverRequest);
			}
		}
		return null;
	}

	private static class ErrorHandler<T extends ServerResponse> {

		private final Predicate<Throwable> predicate;

		private final BiFunction<Throwable, ServerRequest, T> responseProvider;

		public ErrorHandler(Predicate<Throwable> predicate, BiFunction<Throwable, ServerRequest, T> responseProvider) {
			Assert.notNull(predicate, "Predicate must not be null");
			Assert.notNull(responseProvider, "ResponseProvider must not be null");
			this.predicate = predicate;
			this.responseProvider = responseProvider;
		}

		public boolean test(Throwable t) {
			return this.predicate.test(t);
		}

		public T handle(Throwable t, ServerRequest serverRequest) {
			return this.responseProvider.apply(t, serverRequest);
		}
	}

}
