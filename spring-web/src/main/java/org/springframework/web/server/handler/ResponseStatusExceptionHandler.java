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

package org.springframework.web.server.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

/**
 * Handle {@link ResponseStatusException} by setting the response status.
 *
 * <p>By default exception stack traces are not shown for successfully resolved
 * exceptions. Use {@link #setWarnLogCategory(String)} to enable logging with
 * stack traces.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class ResponseStatusExceptionHandler implements WebExceptionHandler {

	private static final Log logger = LogFactory.getLog(ResponseStatusExceptionHandler.class);


	@Nullable
	private Log warnLogger;


	/**
	 * Set the log category for warn logging.
	 * <p>Default is no warn logging. Specify this setting to activate warn
	 * logging into a specific category.
	 * @since 5.1
	 * @see org.apache.commons.logging.LogFactory#getLog(String)
	 * @see java.util.logging.Logger#getLogger(String)
	 */
	public void setWarnLogCategory(String loggerName) {
		this.warnLogger = LogFactory.getLog(loggerName);
	}


	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		if (!updateResponse(exchange.getResponse(), ex)) {
			return Mono.error(ex);
		}

		// Mirrors AbstractHandlerExceptionResolver in spring-webmvc...
		String logPrefix = exchange.getLogPrefix();
		if (this.warnLogger != null && this.warnLogger.isWarnEnabled()) {
			this.warnLogger.warn(logPrefix + formatError(ex, exchange.getRequest()));
		}
		else if (logger.isDebugEnabled()) {
			logger.debug(logPrefix + formatError(ex, exchange.getRequest()));
		}

		return exchange.getResponse().setComplete();
	}


	private String formatError(Throwable ex, ServerHttpRequest request) {
		String className = ex.getClass().getSimpleName();
		String message = LogFormatUtils.formatValue(ex.getMessage(), -1, true);
		String path = request.getURI().getRawPath();
		return "Resolved [" + className + ": " + message + "] for HTTP " + request.getMethod() + " " + path;
	}

	@SuppressWarnings("deprecation")
	private boolean updateResponse(ServerHttpResponse response, Throwable ex) {
		boolean result = false;
		HttpStatusCode statusCode = determineStatus(ex);
		int code = (statusCode != null ? statusCode.value() : determineRawStatusCode(ex));
		if (code != -1) {
			if (response.setStatusCode(statusCode)) {
				if (ex instanceof ResponseStatusException responseStatusException) {
					responseStatusException.getHeaders().forEach((name, values) ->
							values.forEach(value -> response.getHeaders().add(name, value)));
				}
				result = true;
			}
		}
		else {
			Throwable cause = ex.getCause();
			if (cause != null) {
				result = updateResponse(response, cause);
			}
		}
		return result;
	}

	/**
	 * Determine the HTTP status for the given exception.
	 * @param ex the exception to check
	 * @return the associated HTTP status code, or {@code null} if it can't be
	 * derived
	 */
	@Nullable
	protected HttpStatusCode determineStatus(Throwable ex) {
		if (ex instanceof ResponseStatusException responseStatusException) {
			return responseStatusException.getStatusCode();
		}
		else {
			return null;
		}
	}

	/**
	 * Determine the raw status code for the given exception.
	 * @param ex the exception to check
	 * @return the associated HTTP status code, or -1 if it can't be derived.
	 * @since 5.3
	 * @deprecated as of 6.0, in favor of {@link #determineStatus(Throwable)}
	 */
	@Deprecated(since = "6.0")
	protected int determineRawStatusCode(Throwable ex) {
		if (ex instanceof ResponseStatusException responseStatusException) {
			return responseStatusException.getStatusCode().value();
		}
		return -1;
	}

}
