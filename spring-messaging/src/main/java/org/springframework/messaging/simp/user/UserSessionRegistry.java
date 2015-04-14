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
 * A registry for looking up active user sessions. For use when resolving user
 * destinations.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see DefaultUserDestinationResolver
 */
public interface UserSessionRegistry {

	/**
	 * Return the active session id's for the user.
	 * @param user the user
	 * @return a set with 0 or more session id's, never {@code null}.
	 */
	Set<String> getSessionIds(String user);

	/**
	 * Register an active session id for a user.
	 * @param user the user
	 * @param sessionId the session id
	 */
	void registerSessionId(String user, String sessionId);

	/**
	 * Unregister an active session id for a user.
	 * @param user the user
	 * @param sessionId the session id
	 */
	void unregisterSessionId(String user, String sessionId);

}
