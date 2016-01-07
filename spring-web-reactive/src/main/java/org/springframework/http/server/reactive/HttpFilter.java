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

import org.reactivestreams.Publisher;
import reactor.Mono;

/**
 * Contract for interception-style, chained processing of HTTP requests.
 *
 * <p>Filters may be used to implement cross-cutting, application-agnostic
 * requirements such as security, timeouts, and others.
 *
 * <p>{@link FilterChainHttpHandler} provides a way of constructing a chain of
 * {@link HttpFilter}s followed by a target {@link HttpHandler}.
 *
 * @author Rossen Stoyanchev
 * @see FilterChainHttpHandler
 */
public interface HttpFilter {

	/**
	 * Process the given request and optionally delegate to the next HttpFilter.
	 *
	 * @param request current HTTP request.
	 * @param response current HTTP response.
	 * @param chain provides a way to delegate to the next HttpFilter.
	 * @return {@code Mono<Void>} to indicate when request processing is complete.
	 */
	Mono<Void> filter(ServerHttpRequest request, ServerHttpResponse response,
			HttpFilterChain chain);

}
