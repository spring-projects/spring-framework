/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
