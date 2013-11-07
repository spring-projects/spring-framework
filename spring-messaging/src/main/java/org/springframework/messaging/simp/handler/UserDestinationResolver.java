/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp.handler;

import org.springframework.messaging.Message;

import java.util.Set;


/**
 * A strategy for resolving unique, user destinations per session. User destinations
 * provide a user with the ability to subscribe to a queue unique to their session
 * as well others with the ability to send messages to those queues.
 * <p>
 * For example when a user attempts to subscribe to "/user/queue/position-updates",
 * the destination may be resolved to "/queue/position-updates-useri9oqdfzo" yielding a
 * unique queue name that does not collide with any other user attempting to do the same.
 * Subsequently when messages are sent to "/user/{username}/queue/position-updates",
 * the destination is translated to "/queue/position-updates-useri9oqdfzo".
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 *
 * @see UserDestinationMessageHandler
 */
public interface UserDestinationResolver {

	/**
	 * Resolve the destination of the message to one or more user/session-specific target
	 * destinations. If the user has multiple sessions, the method may return more than
	 * one target destinations.
	 *
	 * @param message the message to resolve
	 *
	 * @return the resolved unique user destinations or an empty Set if the message
	 * 		destination is not recognized as a user destination
	 */
	Set<String> resolveDestination(Message<?> message);

}
