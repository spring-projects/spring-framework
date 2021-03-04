/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;

/**
 * Contract that decouples the {@link DispatcherHandler} from the details of
 * invoking a handler and makes it possible to support any handler type.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface HandlerAdapter {

	/**
	 * Whether this {@code HandlerAdapter} supports the given {@code handler}.
	 * @param handler the handler object to check
	 * @return whether or not the handler is supported
	 */
	boolean supports(Object handler);

	/**
	 * Handle the request with the given handler.
	 * <p>Implementations are encouraged to handle exceptions resulting from the
	 * invocation of a handler in order and if necessary to return an alternate
	 * result that represents an error response.
	 * <p>Furthermore since an async {@code HandlerResult} may produce an error
	 * later during result handling implementations are also encouraged to
	 * {@link HandlerResult#setExceptionHandler(Function) set an exception
	 * handler} on the {@code HandlerResult} so that may also be applied later
	 * after result handling.
	 * @param exchange current server exchange
	 * @param handler the selected handler which must have been previously
	 * checked via {@link #supports(Object)}
	 * @return {@link Mono} that emits a single {@code HandlerResult} or none if
	 * the request has been fully handled and doesn't require further handling.
	 */
	Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler);

}
