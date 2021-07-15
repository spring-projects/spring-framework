/*
 * Copyright 2002-2021 the original author or authors.
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
