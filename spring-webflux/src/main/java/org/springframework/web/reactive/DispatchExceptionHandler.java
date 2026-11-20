/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
