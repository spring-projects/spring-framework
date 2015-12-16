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

package org.springframework.web.reactive;

import reactor.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * Process the {@link HandlerResult}, usually returned by an {@link HandlerAdapter}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public interface HandlerResultHandler {

	/**
	 * Given a handler instance, return whether or not this {@code HandlerResultHandler}
	 * can support it.
	 *
	 * @param result result object to check
	 * @return whether or not this object can use the given result
	 */
	boolean supports(HandlerResult result);

	/**
	 * Process the given result in an asynchronous non blocking way, by eventually modifying
	 * response headers, or writing some data stream into the response.
	 * Implementations should not throw exceptions but signal them via the returned
	 * {@code Mono<Void>}.
	 *
	 * @return A {@code Mono<Void>} used to signal the demand, and receive a notification
	 * when the handling is complete (success or error) including the flush of the data on the
	 * network.
	 */
	Mono<Void> handleResult(ServerHttpRequest request, ServerHttpResponse response,
			HandlerResult result);

}