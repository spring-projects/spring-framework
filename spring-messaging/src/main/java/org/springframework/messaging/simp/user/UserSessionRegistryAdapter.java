/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
 * An adapter that allows a {@code UserSessionRegistry}, which is deprecated in
 * favor of {@code SimpUserRegistry}, to be used as a  {@code SimpUserRegistry}.
 * Due to the more limited information available, methods such as
 * {@link #getUsers()} and {@link #findSubscriptions} are not supported.
 *
 * <p>As of 4.2, this adapter is used only in applications that explicitly
 * register a custom {@code UserSessionRegistry} bean by overriding
 * {@link org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration#userSessionRegistry()}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
@SuppressWarnings("deprecation")
public class UserSessionRegistryAdapter implements SimpUserRegistry {

	private final UserSessionRegistry userSessionRegistry;


	public UserSessionRegistryAdapter(UserSessionRegistry registry) {
		this.userSessionRegistry = registry;
	}


	@Override
	public SimpUser getUser(String userName) {
		Set<String> sessionIds = this.userSessionRegistry.getSessionIds(userName);
		return (!CollectionUtils.isEmpty(sessionIds) ? new SimpUserAdapter(userName, sessionIds) : null);
	}

	@Override
	public Set<SimpUser> getUsers() {
		throw new UnsupportedOperationException("UserSessionRegistry does not expose a listing of users");
	}

	@Override
	public int getUserCount() {
		throw new UnsupportedOperationException("UserSessionRegistry does not expose a user count");
	}

	@Override
	public Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher) {
		throw new UnsupportedOperationException("UserSessionRegistry does not support operations across users");
	}


	/**
	 * Expose the only information available from a UserSessionRegistry
	 * (name and session id's) as a {@code SimpUser}.
	 */
	private static class SimpUserAdapter implements SimpUser {

		private final String name;

		private final Map<String, SimpSession> sessions;

		public SimpUserAdapter(String name, Set<String> sessionIds) {
			this.name = name;
			this.sessions = new HashMap<String, SimpSession>(sessionIds.size());
			for (String sessionId : sessionIds) {
				this.sessions.put(sessionId, new SimpSessionAdapter(sessionId));
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


	/**
	 * Expose the only information available from a UserSessionRegistry
	 * (session ids but no subscriptions) as a {@code SimpSession}.
	 */
	private static class SimpSessionAdapter implements SimpSession {

		private final String id;

		public SimpSessionAdapter(String id) {
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
