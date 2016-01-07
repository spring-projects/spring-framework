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
package org.springframework.http.server.reactive;

import java.util.Arrays;
import java.util.List;

import reactor.Mono;

import org.springframework.util.Assert;

/**
 * {@link HttpHandler} that delegates to a target {@link HttpHandler} and handles
 * any errors from it by invoking one or more {@link HttpExceptionHandler}s
 * sequentially until one of them completes successfully.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 */
public class ErrorHandlingHttpHandler extends HttpHandlerDecorator {

	private final List<HttpExceptionHandler> exceptionHandlers;


	public ErrorHandlingHttpHandler(HttpHandler targetHandler, HttpExceptionHandler... exceptionHandlers) {
		super(targetHandler);
		Assert.notEmpty(exceptionHandlers, "At least one exception handler is required");
		this.exceptionHandlers = Arrays.asList(exceptionHandlers);
	}


	@Override
	public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
		Mono<Void> mono;
		try {
			mono = getDelegate().handle(request, response);
		}
		catch (Throwable ex) {
			mono = Mono.error(ex);
		}
		for (HttpExceptionHandler handler : this.exceptionHandlers) {
			mono = applyExceptionHandler(mono, handler, request, response);
		}
		return mono;
	}

	private static Mono<Void> applyExceptionHandler(Mono<Void> mono, HttpExceptionHandler handler,
			ServerHttpRequest request, ServerHttpResponse response) {

		return mono.otherwise(ex -> handler.handle(request, response, ex)).after();
	}

}
