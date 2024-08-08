/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.server;

import reactor.core.publisher.Mono;

/**
 * Contract for interception-style, chained processing of Web requests that may
 * be used to implement cross-cutting, application-agnostic requirements such
 * as security, timeouts, and others.
 *
 * <p>Consider using {@code org.springframework.web.server.CoWebFilter} with
 * Kotlin Coroutines.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebFilter {

	/**
	 * Process the Web request and (optionally) delegate to the next
	 * {@code WebFilter} through the given {@link WebFilterChain}.
	 * @param exchange the current server exchange
	 * @param chain provides a way to delegate to the next filter
	 * @return {@code Mono<Void>} to indicate when request processing is complete
	 */
	Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain);

}
