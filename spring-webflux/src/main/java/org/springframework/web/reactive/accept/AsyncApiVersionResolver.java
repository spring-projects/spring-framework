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
 * Extension of {@link ApiVersionResolver} for implementations that resolve the
 * version in an asynchronous way.
 *
 * @author Rossen Stoyanchev
 * @author Jonathan Kaplan
 * @since 7.0.3
 */
@FunctionalInterface
public interface AsyncApiVersionResolver extends ApiVersionResolver {

	/**
	 * Resolve the version for the given exchange.
	 * @param exchange the current exchange
	 * @return {@code Mono} emitting the version value, or an empty {@code Mono}
	 */
	Mono<String> resolveVersionAsync(ServerWebExchange exchange);

	@Override
	default @Nullable String resolveVersion(ServerWebExchange exchange) {
		throw new UnsupportedOperationException(
				"Async resolver does not support blocking resolution");
	}

}
