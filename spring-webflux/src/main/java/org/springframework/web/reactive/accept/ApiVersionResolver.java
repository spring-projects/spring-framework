/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.accept;

import org.jspecify.annotations.Nullable;

import org.springframework.web.server.ServerWebExchange;

/**
 * Contract to extract the version from a request.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
@FunctionalInterface
public interface ApiVersionResolver {

	/**
	 * Resolve the version for the given exchange.
	 * <p>Implementations of this method are expected to be non-blocking.
	 * If you need to resolve the version asynchronously, please implement the
	 * {@link AsyncApiVersionResolver} subinterface instead.
	 * @param exchange the current exchange
	 * @return the version value, or {@code null} if not found
	 */
	@Nullable String resolveVersion(ServerWebExchange exchange);

}
