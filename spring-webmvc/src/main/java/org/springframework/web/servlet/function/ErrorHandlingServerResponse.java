/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.web.servlet.ModelAndView;

/**
 * Base class for {@link ServerResponse} implementations with error handling.
 *
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

	protected final @Nullable ModelAndView handleError(Throwable t, HttpServletRequest servletRequest,
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

	protected final @Nullable ServerResponse errorResponse(Throwable t, HttpServletRequest servletRequest) {
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
