/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.cors.reactive;

import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * WebFilter that handles pre-flight requests through a
 * {@link PreFlightRequestHandler} and bypasses the rest of the chain.
 *
 * <p>A WebFlux application can simply inject PreFlightRequestHandler and use
 * it to create an instance of this WebFilter since {@code @EnableWebFlux}
 * declares {@code DispatcherHandler} as a bean and that is a
 * PreFlightRequestHandler.
 *
 * @author Rossen Stoyanchev
 * @since 5.3.7
 */
public class PreFlightRequestWebFilter implements WebFilter {

	private final PreFlightRequestHandler handler;


	/**
	 * Create an instance that will delegate to the given handler.
	 */
	public PreFlightRequestWebFilter(PreFlightRequestHandler handler) {
		Assert.notNull(handler, "PreFlightRequestHandler is required");
		this.handler = handler;
	}


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return (CorsUtils.isPreFlightRequest(exchange.getRequest()) ?
				this.handler.handlePreFlight(exchange) : chain.filter(exchange));
	}

}
