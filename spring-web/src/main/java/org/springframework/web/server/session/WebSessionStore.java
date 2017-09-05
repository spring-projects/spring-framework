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
 * @since 5.0
 */
public interface WebSessionStore {

	/**
	 * Store the given WebSession.
	 * @param session the session to store
	 * @return a completion notification (success or error)
	 */
	Mono<Void> storeSession(WebSession session);

	/**
	 * Return the WebSession for the given id.
	 * @param sessionId the session to load
	 * @return the session, or an empty {@code Mono}.
	 */
	Mono<WebSession> retrieveSession(String sessionId);

	/**
	 * Update WebSession data storage to reflect a change in session id.
	 * <p>Note that the same can be achieved via a combination of
	 * {@link #removeSession} + {@link #storeSession}. The purpose of this method
	 * is to allow a more efficient replacement of the session id mapping
	 * without replacing and storing the session with all of its data.
	 * @param oldId the previous session id
	 * @param session the session reflecting the changed session id
	 * @return completion notification (success or error)
	 */
	Mono<Void> changeSessionId(String oldId, WebSession session);

	/**
	 * Remove the WebSession for the specified id.
	 * @param sessionId the id of the session to remove
	 * @return a completion notification (success or error)
	 */
	Mono<Void> removeSession(String sessionId);

}
