/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server;

import reactor.core.publisher.Mono;

import org.springframework.web.server.adapter.HttpWebHandlerAdapter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Contract to handle a web request.
 *
 * <p>Use {@link HttpWebHandlerAdapter} to adapt a {@code WebHandler} to an
 * {@link org.springframework.http.server.reactive.HttpHandler HttpHandler}.
 * The {@link WebHttpHandlerBuilder} provides a convenient way to do that while
 * also optionally configuring one or more filters and/or exception handlers.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebHandler {

	/**
	 * Handle the web server exchange.
	 * @param exchange the current server exchange
	 * @return {@code Mono<Void>} to indicate when request handling is complete
	 */
	Mono<Void> handle(ServerWebExchange exchange);

}
