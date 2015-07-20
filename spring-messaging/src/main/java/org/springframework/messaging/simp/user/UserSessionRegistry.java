/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.messaging.simp.user;

import java.util.Set;

/**
 * A contract for adding and removing user sessions.
 *
 * <p>As of 4.2, this interface is replaced by {@link SimpUserRegistry},
 * exposing methods to return all registered users as well as to provide
 * more extensive information for each user.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @deprecated in favor of {@link SimpUserRegistry} in combination with
 * {@link org.springframework.context.ApplicationListener} listening for
 * {@link org.springframework.web.socket.messaging.AbstractSubProtocolEvent} events.
 */
@Deprecated
public interface UserSessionRegistry {

	/**
	 * Return the active session ids for the user.
	 * The returned set is a snapshot that will never be modified.
	 * @param userName the user to look up
	 * @return a set with 0 or more session ids, never {@code null}.
	 */
	Set<String> getSessionIds(String userName);

	/**
	 * Register an active session id for a user.
	 * @param userName the user name
	 * @param sessionId the session id
	 */
	void registerSessionId(String userName, String sessionId);

	/**
	 * Unregister an active session id for a user.
	 * @param userName the user name
	 * @param sessionId the session id
	 */
	void unregisterSessionId(String userName, String sessionId);

}
