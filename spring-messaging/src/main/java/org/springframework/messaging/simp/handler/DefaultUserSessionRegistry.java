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

import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A default thread-safe implementation of {@link UserSessionRegistry}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultUserSessionRegistry implements UserSessionRegistry {

	// userId -> sessionId
	private final ConcurrentMap<String, Set<String>> userSessionIds = new ConcurrentHashMap<String, Set<String>>();

	private final Object lock = new Object();


	@Override
	public Set<String> getSessionIds(String user) {
		Set<String> set = this.userSessionIds.get(user);
		return (set != null) ? set : Collections.<String>emptySet();
	}

	@Override
	public void registerSessionId(String user, String sessionId) {
		Assert.notNull(user, "User must not be null");
		Assert.notNull(sessionId, "Session ID must not be null");
		synchronized (this.lock) {
			Set<String> set = this.userSessionIds.get(user);
			if (set == null) {
				set = new CopyOnWriteArraySet<String>();
				this.userSessionIds.put(user, set);
			}
			set.add(sessionId);
		}
	}

	@Override
	public void unregisterSessionId(String userName, String sessionId) {
		Assert.notNull(userName, "User Name must not be null");
		Assert.notNull(sessionId, "Session ID must not be null");
		synchronized (lock) {
			Set<String> set = this.userSessionIds.get(userName);
			if (set != null) {
				if (set.remove(sessionId) && set.isEmpty()) {
					this.userSessionIds.remove(userName);
				}
			}
		}
	}

}
