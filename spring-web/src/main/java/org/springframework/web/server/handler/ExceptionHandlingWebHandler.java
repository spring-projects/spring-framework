/*
 * Copyright 2002-2015 the original author or authors.
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
 * WebHandler that can invoke a target {@link WebHandler} and then apply
 * exception handling with one or more {@link WebExceptionHandler} instances.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ExceptionHandlingWebHandler extends WebHandlerDecorator {

	private static Log logger = LogFactory.getLog(ExceptionHandlingWebHandler.class);

	/**
	 * Log category to use on network IO exceptions after a client has gone away.
	 * <p>Servlet containers do not expose notifications when a client disconnects;
	 * see <a href="https://java.net/jira/browse/SERVLET_SPEC-44">SERVLET_SPEC-44</a>.
	 * Therefore network IO failures may occur simply because a client has gone away,
	 * and that can fill the logs with unnecessary stack traces.
	 * <p>We make a best effort to identify such network failures, on a per-server
	 * basis and log them under a separate log category. A simple one-line message
	 * is logged at DEBUG level instead while a full stack trace is shown at TRACE.
	 */
	private static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
			ExceptionHandlingWebHandler.class.getName() + ".DisconnectedClient";

	private static final Log disconnectedClientLogger = LogFactory.getLog(DISCONNECTED_CLIENT_LOG_CATEGORY);

	private static final Set<String> DISCONNECTED_CLIENT_EXCEPTIONS;

	static {
		Set<String> set = new HashSet<>(3);
		set.add("ClientAbortException"); // Tomcat
		set.add("EOFException"); // Tomcat
		set.add("EofException"); // Jetty
		// java.io.IOException("Broken pipe") on WildFly (already covered)
		DISCONNECTED_CLIENT_EXCEPTIONS = Collections.unmodifiableSet(set);
	}

	private final List<WebExceptionHandler> exceptionHandlers;


	public ExceptionHandlingWebHandler(WebHandler delegate, WebExceptionHandler... exceptionHandlers) {
		super(delegate);
		this.exceptionHandlers = initList(exceptionHandlers);
	}

	private static List<WebExceptionHandler> initList(WebExceptionHandler[] list) {
		return (list != null ? Collections.unmodifiableList(Arrays.asList(list)):
				Collections.emptyList());
	}


	/**
	 * Return a read-only list of the configured exception handlers.
	 */
	public List<WebExceptionHandler> getExceptionHandlers() {
		return this.exceptionHandlers;
	}


	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		Mono<Void> mono;
		try {
			mono = getDelegate().handle(exchange);
		}
		catch (Throwable ex) {
			mono = Mono.error(ex);
		}
		for (WebExceptionHandler exceptionHandler : this.exceptionHandlers) {
			mono = mono.otherwise(ex -> exceptionHandler.handle(exchange, ex));
		}
		return mono.otherwise(ex -> handleUnresolvedException(exchange, ex));
	}

	private Mono<? extends Void> handleUnresolvedException(ServerWebExchange exchange, Throwable ex) {
		logException(ex);
		exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
		return Mono.empty();
	}

	private void logException(Throwable ex) {
		@SuppressWarnings("serial")
		NestedCheckedException nestedException = new NestedCheckedException("", ex) {};
		if ("Broken pipe".equalsIgnoreCase(nestedException.getMostSpecificCause().getMessage()) ||
				DISCONNECTED_CLIENT_EXCEPTIONS.contains(ex.getClass().getSimpleName())) {

			if (disconnectedClientLogger.isTraceEnabled()) {
				disconnectedClientLogger.trace("Looks like the client has gone away", ex);
			}
			else if (disconnectedClientLogger.isDebugEnabled()) {
				disconnectedClientLogger.debug("Looks like the client has gone away: " +
						nestedException.getMessage() + " (For full stack trace, set the '" +
						DISCONNECTED_CLIENT_LOG_CATEGORY + "' log category to TRACE level)");
			}
		}
		else {
			logger.debug("Could not complete request", ex);
		}
	}

}
