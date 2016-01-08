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
package org.springframework.web.server;

import reactor.Mono;

import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * A contract for resolving exceptions from HTTP request handling.
 *
 * <p>{@link ErrorHandlingHttpHandler} provides a way of applying a list
 * {@link HttpExceptionHandler}s to a target {@link HttpHandler}.
 *
 * @author Rossen Stoyanchev
 * @see ErrorHandlingHttpHandler
 */
public interface HttpExceptionHandler {

	/**
	 * Handle the given exception and return a completion Publisher to indicate
	 * when error handling is complete, or send an error signal if the exception
	 * was not handled.
	 *
	 * @param request the current request
	 * @param response the current response
	 * @param ex the exception to handle
	 * @return {@code Mono<Void>} to indicate when exception handling is complete.
	 */
	Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response, Throwable ex);

}
