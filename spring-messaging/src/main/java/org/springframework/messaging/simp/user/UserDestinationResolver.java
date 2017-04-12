/*
 * Copyright 2002-2016 the original author or authors.
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
	UserDestinationResult resolveDestination(Message<?> message);

}
