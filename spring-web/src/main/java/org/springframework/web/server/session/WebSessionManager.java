/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server.session;

import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

/**
 * Main class for access to the {@link WebSession} for an HTTP request.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebSessionIdResolver
 * @see WebSessionStore
 */
public interface WebSessionManager {

	/**
	 * Return the {@link WebSession} for the given exchange.
	 * <p>Always guaranteed to return either an instance matching the session id
	 * requested by the client, or a new session either because the client did not
	 * specify a session id or because the underlying session expired.
	 * @param exchange the current exchange
	 * @return promise for the WebSession
	 */
	Mono<WebSession> getSession(ServerWebExchange exchange);

}
