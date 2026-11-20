/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.user;

import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * A registry of currently connected users.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface SimpUserRegistry {

	/**
	 * Get the user for the given name.
	 * @param userName the name of the user to look up
	 * @return the user, or {@code null} if not connected
	 */
	@Nullable SimpUser getUser(String userName);

	/**
	 * Return a snapshot of all connected users.
	 * <p>The returned set is a copy and will not reflect further changes.
	 * @return the connected users, or an empty set if none
	 */
	Set<SimpUser> getUsers();

	/**
	 * Return the count of all connected users.
	 * @return the number of connected users
	 * @since 4.3.5
	 */
	int getUserCount();

	/**
	 * Find subscriptions with the given matcher.
	 * @param matcher the matcher to use
	 * @return a set of matching subscriptions, or an empty set if none
	 */
	Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher);

}
