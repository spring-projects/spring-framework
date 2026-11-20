/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server;

import reactor.core.publisher.Mono;

/**
 * Contract to allow a {@link WebFilter} to delegate to the next in the chain.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebFilterChain {

	/**
	 * Delegate to the next {@code WebFilter} in the chain.
	 * @param exchange the current server exchange
	 * @return {@code Mono<Void>} to indicate when request handling is complete
	 */
	Mono<Void> filter(ServerWebExchange exchange);

}
