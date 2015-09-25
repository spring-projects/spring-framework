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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.util.CollectionUtils;

/**
 * A temporary adapter to allow use of deprecated {@link UserSessionRegistry}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
@SuppressWarnings("deprecation")
public class UserSessionRegistryAdapter implements SimpUserRegistry {

	private final UserSessionRegistry delegate;


	public UserSessionRegistryAdapter(UserSessionRegistry delegate) {
		this.delegate = delegate;
	}


	@Override
	public SimpUser getUser(String userName) {
		Set<String> sessionIds = this.delegate.getSessionIds(userName);
		return (!CollectionUtils.isEmpty(sessionIds) ? new SimpleSimpUser(userName, sessionIds) : null);
	}

	@Override
	public Set<SimpUser> getUsers() {
		throw new UnsupportedOperationException("UserSessionRegistry does not expose a listing of users");
	}

	@Override
	public Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher) {
		throw new UnsupportedOperationException("UserSessionRegistry does not support operations across users");
	}


	private static class SimpleSimpUser implements SimpUser {

		private final String name;

		private final Map<String, SimpSession> sessions;

		public SimpleSimpUser(String name, Set<String> sessionIds) {
			this.name = name;
			this.sessions = new HashMap<String, SimpSession>(sessionIds.size());
			for (String sessionId : sessionIds) {
				this.sessions.put(sessionId, new SimpleSimpSession(sessionId));
			}
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean hasSessions() {
			return !this.sessions.isEmpty();
		}

		@Override
		public SimpSession getSession(String sessionId) {
			return this.sessions.get(sessionId);
		}

		@Override
		public Set<SimpSession> getSessions() {
			return new HashSet<SimpSession>(this.sessions.values());
		}
	}


	private static class SimpleSimpSession implements SimpSession {

		private final String id;

		public SimpleSimpSession(String id) {
			this.id = id;
		}

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public SimpUser getUser() {
			return null;
		}

		@Override
		public Set<SimpSubscription> getSubscriptions() {
			return Collections.<SimpSubscription>emptySet();
		}
	}

}
