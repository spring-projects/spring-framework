/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server.session;

import java.util.List;

import org.springframework.web.server.ServerWebExchange;

/**
 * Contract for session ID resolution strategies. Allows for session ID
 * resolution through the request and for sending the session ID or expiring
 * the session through the response.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see CookieWebSessionIdResolver
 */
public interface WebSessionIdResolver {

	/**
	 * Resolve the session IDs associated with the request.
	 * @param exchange the current exchange
	 * @return the session IDs or an empty list
	 */
	List<String> resolveSessionIds(ServerWebExchange exchange);

	/**
	 * Send the given session ID to the client.
	 * @param exchange the current exchange
	 * @param sessionId the session ID
	 */
	void setSessionId(ServerWebExchange exchange, String sessionId);

	/**
	 * Instruct the client to end the current session.
	 * @param exchange the current exchange
	 */
	void expireSession(ServerWebExchange exchange);

}
