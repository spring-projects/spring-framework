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

package org.springframework.web.reactive;

import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;

/**
 * Contract to map a {@link Throwable} to a {@link HandlerResult}.
 *
 * <p>Supported by {@link DispatcherHandler} when used in the following ways:
 * <ul>
 * <li>Set on a {@link HandlerResult#setExceptionHandler HandlerResult}, allowing
 * a {@link HandlerAdapter} to apply its exception handling to deferred exceptions
 * from asynchronous return values, and to response rendering.
 * <li>Implemented by a {@link HandlerAdapter} in order to handle exceptions that
 * occur before a request is mapped to a handler, or for unhandled errors from a
 * handler.
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 * @see HandlerAdapter
 * @see HandlerResult#setExceptionHandler(DispatchExceptionHandler)
 */
public interface DispatchExceptionHandler {

	/**
	 * Handle the given exception, mapping it to a {@link HandlerResult} that can
	 * then be used to render an HTTP response.
	 * @param exchange the current exchange
	 * @param ex the exception to handle
	 * @return a {@code Mono} that emits a {@code HandlerResult} or an error
	 * signal with the original exception if it remains not handled
	 */
	Mono<HandlerResult> handleError(ServerWebExchange exchange, Throwable ex);

}
