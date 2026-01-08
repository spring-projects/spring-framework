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
 * An extension of {@link ApiVersionResolver}s for implementations that can
 * resolve the version in an imperative way without blocking.
 *
 * @author Rossen Stoyanchev
 * @since 7.0.3
 */
@FunctionalInterface
public interface SyncApiVersionResolver extends ApiVersionResolver {

	/**
	 * {@inheritDoc}
	 * <p>This method delegates to the synchronous
	 * {@link #resolveVersionValue} and wraps the result as {@code Mono}.
	 */
	@Override
	default Mono<String> resolveApiVersion(ServerWebExchange exchange) {
		return Mono.justOrEmpty(resolveVersionValue(exchange));
	}

	/**
	 * Resolve the version for the given exchange imperatively without blocking.
	 * @param exchange the current exchange
	 * @return the version value, or {@code null} if not found
	 */
	@Nullable String resolveVersionValue(ServerWebExchange exchange);

	@SuppressWarnings("removal")
	@Override
	default @Nullable String resolveVersion(ServerWebExchange exchange) {
		return resolveVersionValue(exchange);
	}

}
