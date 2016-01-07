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

import reactor.Mono;

/**
 * Represents a chain of {@link HttpFilter}s allowing each {@link HttpFilter} to
 * delegate to the next in the chain.
 *
 * @author Rossen Stoyanchev
 */
public interface HttpFilterChain {

	/**
	 *
	 * @param request current HTTP request.
	 * @param response current HTTP response.
	 * @return {@code Mono<Void>} to indicate when request handling is complete.
	 */
	Mono<Void> filter(ServerHttpRequest request, ServerHttpResponse response);

}
