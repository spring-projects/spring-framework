/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.server.session;

import java.util.Optional;

import org.springframework.web.server.ServerWebExchange;


/**
 * Contract for session id resolution strategies. Allows for session id
 * resolution through the request and for sending the session id to the
 * client through the response.
 *
 * @author Rossen Stoyanchev
 * @see CookieWebSessionIdResolver
 */
public interface WebSessionIdResolver {

	/**
	 * Resolve the session id associated with the request.
	 * @param exchange the current exchange
	 * @return the session id if present
	 */
	Optional<String> resolveSessionId(ServerWebExchange exchange);

	/**
	 * Send the given session id to the client or if the session id is "null"
	 * instruct the client to end the current session.
	 * @param exchange the current exchange
	 * @param sessionId the session id
	 */
	void setSessionId(ServerWebExchange exchange, String sessionId);

}
