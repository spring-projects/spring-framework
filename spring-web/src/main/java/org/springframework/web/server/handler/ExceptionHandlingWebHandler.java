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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

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

	private final List<WebExceptionHandler> exceptionHandlers;


	public ExceptionHandlingWebHandler(WebHandler delegate, WebExceptionHandler... exceptionHandlers) {
		super(delegate);
		this.exceptionHandlers = initList(exceptionHandlers);
	}

	private static List<WebExceptionHandler> initList(WebExceptionHandler[] list) {
		return (list != null ? Collections.unmodifiableList(Arrays.asList(list)): Collections.emptyList());
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
		if (logger.isDebugEnabled()) {
			logger.debug("Could not complete request", ex);
		}
		exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
		return Mono.empty();
	}

}
