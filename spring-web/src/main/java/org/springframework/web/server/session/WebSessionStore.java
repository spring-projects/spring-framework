/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	 * should also update the {@code lastAccessTime} of retrieved sessions.
	 * @param sessionId the session to load
	 * @return the session, or an empty {@code Mono}
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
