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

package org.springframework.web.reactive;

import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;

/**
 * Contract to abstract the details of invoking a handler of a given type.
 *
 * <p>An implementation can also choose to be an instance of
 * {@link DispatchExceptionHandler} if it wants to handle exceptions that occur
 * before the request is successfully mapped to a handler. This allows a
 * {@code HandlerAdapter} to expose the same exception handling both for handler
 * invocation errors and for errors before a handler is selected.
 * In Reactive Streams terms, {@link #handle} handles the onNext signal, while
 * {@link DispatchExceptionHandler#handleError} handles the onError signal
 * from the dispatch processing chain.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface HandlerAdapter {

	/**
	 * Whether this {@code HandlerAdapter} supports the given {@code handler}.
	 * @param handler the handler object to check
	 * @return whether the handler is supported
	 */
	boolean supports(Object handler);

	/**
	 * Handle the request with the given handler, previously checked via
	 * {@link #supports(Object)}.
	 * <p>Implementations should consider the following for exception handling:
	 * <ul>
	 * <li>Handle invocation exceptions within this method.
	 * <li>{@link HandlerResult#setExceptionHandler(DispatchExceptionHandler)
	 * Set an exception handler} on the returned {@code HandlerResult} to handle
	 * deferred exceptions from asynchronous return values, and to handle
	 * exceptions from response rendering.
	 * <li>Implement {@link DispatchExceptionHandler} to extend exception
	 * handling to exceptions that occur before a handler is selected.
	 * </ul>
	 * @param exchange current server exchange
	 * @param handler the selected handler which must have been previously
	 * checked via {@link #supports(Object)}
	 * @return {@link Mono} that emits a {@code HandlerResult}, or completes
	 * empty if the request is fully handled; any error signal would not be
	 * handled within the {@link DispatcherHandler}, and would instead be
	 * processed by the chain of registered
	 * {@link org.springframework.web.server.WebExceptionHandler}s at the end
	 * of the {@link org.springframework.web.server.WebFilter} chain
	 */
	Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler);

}
