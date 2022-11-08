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
 * Contract to map a {@link Throwable} to a {@link HandlerResult}.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public interface DispatchExceptionHandler {

	/**
	 * Handler the given exception and resolve it to {@link HandlerResult} that
	 * can be used for rendering an HTTP response.
	 * @param exchange the current exchange
	 * @param ex the exception to handle
	 * @return a {@code Mono} that emits a {@code HandlerResult} or the original exception
	 */
	Mono<HandlerResult> handleError(ServerWebExchange exchange, Throwable ex);

}
