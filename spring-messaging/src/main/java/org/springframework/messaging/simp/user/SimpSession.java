/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.user;

import java.util.Set;

/**
 * Represents a session of connected user.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface SimpSession {

	/**
	 * Return the session id.
	 */
	String getId();

	/**
	 * Return the user associated with the session.
	 */
	SimpUser getUser();

	/**
	 * Return the subscriptions for this session.
	 */
	Set<SimpSubscription> getSubscriptions();

}
