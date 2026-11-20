/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
