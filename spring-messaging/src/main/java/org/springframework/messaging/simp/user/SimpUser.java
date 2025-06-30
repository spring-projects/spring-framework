/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.messaging.simp.user;

import java.security.Principal;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Represents a connected user.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface SimpUser {

	/**
	 * The unique user name.
	 */
	String getName();

	/**
	 * Return the user associated with the session, if available. Typically, the
	 * user information is available unless the user is connected to a different
	 * server in a multi-server user registry scenario.
	 * @since 5.3
	 */
	@Nullable Principal getPrincipal();

	/**
	 * Whether the user has any sessions.
	 */
	boolean hasSessions();

	/**
	 * Look up the session for the given id.
	 * @param sessionId the session id
	 * @return the matching session, or {@code null} if none found
	 */
	@Nullable SimpSession getSession(String sessionId);

	/**
	 * Return the sessions for the user.
	 * The returned set is a copy and will never be modified.
	 * @return a set of session ids, or an empty set if none
	 */
	Set<SimpSession> getSessions();

}
