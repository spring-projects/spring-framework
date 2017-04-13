/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.server.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.NestedCheckedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebHandler;

/**
 * WebHandler decorator that invokes one or more {@link WebExceptionHandler}s
 * after the delegate {@link WebHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ExceptionHandlingWebHandler extends WebHandlerDecorator {

	/**
	 * Dedicated log category for disconnected client exceptions.
	 * <p>Servlet containers do not expose a notification when a client disconnects,
	 * e.g. <a href="https://java.net/jira/browse/SERVLET_SPEC-44">SERVLET_SPEC-44</a>.
	 * <p>To avoid filling logs with unnecessary stack traces, we make an
	 * effort to identify such network failures on a per-server basis, and then
	 * log under a separate log category a simple one-line message at DEBUG level
	 * or a full stack trace only at TRACE level.
	 */
	private static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
			ExceptionHandlingWebHandler.class.getName() + ".DisconnectedClient";

	private static final Set<String> DISCONNECTED_CLIENT_EXCEPTIONS =
			new HashSet<>(Arrays.asList("ClientAbortException", "EOFException", "EofException"));



	private static final Log logger = LogFactory.getLog(ExceptionHandlingWebHandler.class);

	private static final Log disconnectedClientLogger = LogFactory.getLog(DISCONNECTED_CLIENT_LOG_CATEGORY);


	private final List<WebExceptionHandler> exceptionHandlers;


	public ExceptionHandlingWebHandler(WebHandler delegate, List<WebExceptionHandler> handlers) {
		super(delegate);
		this.exceptionHandlers = initHandlers(handlers);
	}

	private List<WebExceptionHandler> initHandlers(List<WebExceptionHandler> handlers) {
		List<WebExceptionHandler> result = new ArrayList<>(handlers);
		result.add(new UnresolvedExceptionHandler());
		return Collections.unmodifiableList(result);
	}


	/**
	 * Return a read-only list of the configured exception handlers.
	 */
	public List<WebExceptionHandler> getExceptionHandlers() {
		return this.exceptionHandlers;
	}


	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {

		Mono<Void> completion;
		try {
			completion = super.handle(exchange);
		}
		catch (Throwable ex) {
			completion = Mono.error(ex);
		}

		for (WebExceptionHandler handler : this.exceptionHandlers) {
			completion = completion.switchOnError(ex -> handler.handle(exchange, ex));
		}

		return completion;
	}


	private static class UnresolvedExceptionHandler implements WebExceptionHandler {


		@Override
		public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
			logException(ex);
			exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			return exchange.getResponse().setComplete();
		}

		@SuppressWarnings("serial")
		private void logException(Throwable ex) {
			NestedCheckedException nestedEx = new NestedCheckedException("", ex) {};
			if ("Broken pipe".equalsIgnoreCase(nestedEx.getMostSpecificCause().getMessage()) ||
					DISCONNECTED_CLIENT_EXCEPTIONS.contains(ex.getClass().getSimpleName())) {

				if (disconnectedClientLogger.isTraceEnabled()) {
					disconnectedClientLogger.trace("Looks like the client has gone away", ex);
				}
				else if (disconnectedClientLogger.isDebugEnabled()) {
					disconnectedClientLogger.debug(
							"The client has gone away: " + nestedEx.getMessage() +
									" (For a full stack trace, set the log category" +
									"'" + DISCONNECTED_CLIENT_LOG_CATEGORY + "' to TRACE)");
				}
			}
			else {
				logger.error("Could not complete request", ex);
			}
		}
	}

}
