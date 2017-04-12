/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.util.Assert;

/**
 * Contains the result from parsing a "user" destination from a source message
 * and translating it to target destinations (one per active user session).
 *
 * @author Rossen Stoyanchev
 * @since 4.0.2
 * @see org.springframework.messaging.simp.user.UserDestinationResolver
 */
public class UserDestinationResult {

	private final String sourceDestination;

	private final Set<String> targetDestinations;

	private final String subscribeDestination;

	private final String user;


	public UserDestinationResult(String sourceDestination,
			Set<String> targetDestinations, String subscribeDestination, String user) {

		Assert.notNull(sourceDestination, "'sourceDestination' must not be null");
		Assert.notNull(targetDestinations, "'targetDestinations' must not be null");
		Assert.notNull(subscribeDestination, "'subscribeDestination' must not be null");

		this.sourceDestination = sourceDestination;
		this.targetDestinations = targetDestinations;
		this.subscribeDestination = subscribeDestination;
		this.user = user;
	}


	/**
	 * The "user" destination from the source message. This may look like
	 * "/user/queue/position-updates" when subscribing or
	 * "/user/{username}/queue/position-updates" when sending a message.
	 * @return the "user" destination, never {@code null}.
	 */
	public String getSourceDestination() {
		return this.sourceDestination;
	}

	/**
	 * The target destinations that the source destination was translated to,
	 * one per active user session, e.g. "/queue/position-updates-useri9oqdfzo".
	 * @return the target destinations, never {@code null} but possibly an empty
	 * set if there are no active sessions for the user.
	 */
	public Set<String> getTargetDestinations() {
		return this.targetDestinations;
	}

	/**
	 * The user destination in the form expected when a client subscribes, e.g.
	 * "/user/queue/position-updates".
	 * @return the subscribe form of the "user" destination, never {@code null}.
	 */
	public String getSubscribeDestination() {
		return this.subscribeDestination;
	}

	/**
	 * The user for this user destination.
	 * @return the user name or {@code null} if we have a session id only such as
	 * when the user is not authenticated; in such cases it is possible to use
	 * sessionId in place of a user name thus removing the need for a user-to-session
	 * lookup via {@link SimpUserRegistry}.
	 */
	public String getUser() {
		return this.user;
	}

	@Override
	public String toString() {
		return "UserDestinationResult[source=" + this.sourceDestination + ", target=" + this.targetDestinations +
				", subscribeDestination=" + this.subscribeDestination + ", user=" + this.user + "]";
	}
}
