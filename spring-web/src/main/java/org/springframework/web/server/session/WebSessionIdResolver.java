/*
 * Copyright 2002-2022 the original author or authors.
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
