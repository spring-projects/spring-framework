/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.web.server.WebSession;

/**
 * Strategy for {@link WebSession} persistence.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 5.0
 */
public interface WebSessionStore {

	/**
	 * Create a new WebSession.
	 * <p>Note that this does nothing more than create a new instance.
	 * The session can later be started explicitly via {@link WebSession#start()}
	 * or implicitly by adding attributes -- and then persisted via
	 * {@link WebSession#save()}.
	 * @return the created session instance
	 */
	Mono<WebSession> createWebSession();

	/**
	 * Return the WebSession for the given id.
	 * <p><strong>Note:</strong> This method should perform an expiration check,
	 * and if it has expired remove the session and return empty. This method
	 * should also update the lastAccessTime of retrieved sessions.
	 * @param sessionId the session to load
	 * @return the session, or an empty {@code Mono} .
	 */
	Mono<WebSession> retrieveSession(String sessionId);

	/**
	 * Remove the WebSession for the specified id.
	 * @param sessionId the id of the session to remove
	 * @return a completion notification (success or error)
	 */
	Mono<Void> removeSession(String sessionId);

	/**
	 * Update the last accessed timestamp to "now".
	 * @param webSession the session to update
	 * @return the session with the updated last access time
	 */
	Mono<WebSession> updateLastAccessTime(WebSession webSession);

}
