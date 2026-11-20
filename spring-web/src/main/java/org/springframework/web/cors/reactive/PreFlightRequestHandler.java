/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.cors.reactive;

import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;

/**
 * Handler for CORS pre-flight requests.
 *
 * @author Rossen Stoyanchev
 * @since 5.3.4
 * @see PreFlightRequestWebFilter
 */
public interface PreFlightRequestHandler {

	/**
	 * Handle a pre-flight request by finding and applying the CORS configuration
	 * that matches the expected actual request. As a result of handling, the
	 * response should be updated with CORS headers or rejected with
	 * {@link org.springframework.http.HttpStatus#FORBIDDEN}.
	 * @param exchange the exchange for the request
	 * @return a completion handle
	 */
	Mono<Void> handlePreFlight(ServerWebExchange exchange);

}
