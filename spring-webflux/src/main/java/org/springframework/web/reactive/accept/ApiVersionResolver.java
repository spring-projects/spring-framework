/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.accept;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;

/**
 * Contract to extract the version from a request.
 *
 * @author Rossen Stoyanchev
 * @author Jonathan Kaplan
 * @since 7.0
 */
@FunctionalInterface
public interface ApiVersionResolver {

	/**
	 * Resolve the version for the given exchange.
	 * @param exchange the current exchange
	 * @return {@code Mono} emitting the version value, or an empty {@code Mono}
	 * @since 7.0.3
	 */
	Mono<String> resolveApiVersion(ServerWebExchange exchange);

	/**
	 * Resolve the version for the given exchange.
	 * @param exchange the current exchange
	 * @return the version value, or {@code null} if not found
	 * @deprecated in favor of {@link #resolveApiVersion(ServerWebExchange)}
	 */
	@Deprecated(since = "7.0.3", forRemoval = true)
	default @Nullable String resolveVersion(ServerWebExchange exchange) {
		return null;
	}

}
