/*
 * Copyright 2002-2018 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

/**
 * Handle {@link ResponseStatusException} by setting the response status.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class ResponseStatusExceptionHandler implements WebExceptionHandler {

	private static final Log logger = LogFactory.getLog(ResponseStatusExceptionHandler.class);


	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		HttpStatus status = resolveStatus(ex);
		if (status != null && exchange.getResponse().setStatusCode(status)) {
			if (status.is5xxServerError()) {
				logger.error(buildMessage(exchange.getRequest(), ex));
			}
			else if (status == HttpStatus.BAD_REQUEST) {
				logger.warn(buildMessage(exchange.getRequest(), ex));
			}
			else {
				logger.trace(buildMessage(exchange.getRequest(), ex));
			}
			return exchange.getResponse().setComplete();
		}
		return Mono.error(ex);
	}

	private String buildMessage(ServerHttpRequest request, Throwable ex) {
		return "Failed to handle request [" + request.getMethod() + " " + request.getURI() + "]: " + ex.getMessage();
	}

	@Nullable
	private HttpStatus resolveStatus(Throwable ex) {
		HttpStatus status = determineStatus(ex);
		if (status == null) {
			Throwable cause = ex.getCause();
			if (cause != null) {
				status = resolveStatus(cause);
			}
		}
		return status;
	}

	/**
	 * Determine the HTTP status implied by the given exception.
	 * @param ex the exception to introspect
	 * @return the associated HTTP status, if any
	 * @since 5.0.5
	 */
	@Nullable
	protected HttpStatus determineStatus(Throwable ex) {
		if (ex instanceof ResponseStatusException) {
			return ((ResponseStatusException) ex).getStatus();
		}
		return null;
	}

}
