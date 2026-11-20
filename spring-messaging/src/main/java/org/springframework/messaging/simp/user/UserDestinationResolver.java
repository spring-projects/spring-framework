/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.user;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;

/**
 * A strategy for resolving a "user" destination by translating it to one or more
 * actual destinations one per active user session. When sending a message to a
 * user destination, the destination must contain the user name so it may be
 * extracted and used to look up the user sessions. When subscribing to a user
 * destination, the destination does not have to contain the user's own name.
 * We simply use the current session.
 *
 * <p>See implementation classes and the documentation for example destinations.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 *
 * @see org.springframework.messaging.simp.user.DefaultUserDestinationResolver
 * @see UserDestinationMessageHandler
 */
@FunctionalInterface
public interface UserDestinationResolver {

	/**
	 * Resolve the given message with a user destination to one or more messages
	 * with actual destinations, one for each active user session.
	 * @param message the message to try to resolve
	 * @return 0 or more target messages (one for each active session), or
	 * {@code null} if the source message does not contain a user destination.
	 */
	@Nullable UserDestinationResult resolveDestination(Message<?> message);

}
