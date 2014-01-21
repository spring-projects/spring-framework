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

package org.springframework.messaging.simp.user;

import org.springframework.messaging.Message;

import java.util.Set;

/**
 * A strategy for resolving unique, user destinations per session. User destinations
 * provide a user with the ability to subscribe to a queue unique to their session
 * as well others with the ability to send messages to those queues.
 * <p>
 * When a user attempts to subscribe to "/user/queue/position-updates", the
 * "/user" prefix is removed and a unique suffix added, resulting in something
 * like "/queue/position-updates-useri9oqdfzo" where the suffix is based on the
 * user's session and ensures it does not collide with any other users attempting
 * to subscribe to "/user/queue/position-updates".
 * <p>
 * When a message is sent to a user with a destination such as
 * "/user/{username}/queue/position-updates", the "/user/{username}" prefix is
 * removed and the suffix added, resulting in something like
 * "/queue/position-updates-useri9oqdfzo".
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 *
 * @see UserDestinationMessageHandler
 */
public interface UserDestinationResolver {

	/**
	 * Resolve the destination of the message to a set of actual target destinations
	 * to use. If the message is SUBSCRIBE/UNSUBSCRIBE, the returned set will contain
	 * only target destination. If the message represents data being sent to a user,
	 * the returned set may contain multiple target destinations, one for each active
	 * session of the target user.
	 *
	 * @param message the message to resolve
	 * @return the resolved unique user destination(s) or an empty Set
	 */
	Set<String> resolveDestination(Message<?> message);

}
