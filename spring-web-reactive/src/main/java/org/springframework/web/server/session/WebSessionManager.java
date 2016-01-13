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

import reactor.core.publisher.Mono;

import org.springframework.web.server.WebServerExchange;
import org.springframework.web.server.WebSession;

/**
 * Main contract abstracting support for access to {@link WebSession} instances
 * associated with HTTP requests as well as the subsequent management such as
 * persistence and others.
 *
 * <p>The {@link DefaultWebSessionManager} implementation in turn delegates to
 * {@link WebSessionIdResolver} and {@link WebSessionStore} which abstract
 * underlying concerns related to the management of web sessions.
 *
 * @author Rossen Stoyanchev
 * @see WebSessionIdResolver
 * @see WebSessionStore
 */
public interface WebSessionManager {

	/**
	 * Return the {@link WebSession} for the given exchange. Always guaranteed
	 * to return an instance either matching to the session id requested by the
	 * client, or with a new session id either because the client did not
	 * specify one or because the underlying session had expired.
	 * @param exchange the current exchange
	 * @return {@code Mono} for async access to the session
	 */
	Mono<WebSession> getSession(WebServerExchange exchange);

}
