/*
 * Copyright 2002-2015 the original author or authors.
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
	 * @return the user or {@code null} if not connected
	 */
	SimpUser getUser(String userName);

	/**
	 * Return a snapshot of all connected users. The returned set is a copy and
	 * will never be modified.
	 * @return the connected users or an empty set.
	 */
	Set<SimpUser> getUsers();

	/**
	 * Find subscriptions with the given matcher.
	 * @param matcher the matcher to use
	 * @return a set of matching subscriptions or an empty set.
	 */
	Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher);

}
