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

import reactor.core.publisher.Mono;

import org.springframework.web.server.WebServerExchange;

/**
 * Process the {@link HandlerResult}, usually returned by an {@link HandlerAdapter}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public interface HandlerResultHandler {

	/**
	 * Whether this handler supports the given {@link HandlerResult}.
	 *
	 * @param result result object to check
	 * @return whether or not this object can use the given result
	 */
	boolean supports(HandlerResult result);

	/**
	 * Process the given result modifying response headers and/or writing data
	 * to the response.
	 *
	 * @param exchange current server exchange
	 * @param result the result from the handling
	 * @return {@code Mono<Void>} to indicate when request handling is complete.
	 */
	Mono<Void> handleResult(WebServerExchange exchange, HandlerResult result);

}