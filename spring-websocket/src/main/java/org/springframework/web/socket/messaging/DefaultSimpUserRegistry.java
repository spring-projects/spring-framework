/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.socket.messaging;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.user.DestinationUserNameProvider;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpSubscriptionMatcher;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;

/**
 * A default implementation of {@link SimpUserRegistry} that relies on
 * {@link AbstractSubProtocolEvent} application context events to keep track of
 * connected users and their subscriptions.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class DefaultSimpUserRegistry implements SimpUserRegistry, SmartApplicationListener {

	/* Primary lookup that holds all users and their sessions */
	private final Map<String, LocalSimpUser> users = new ConcurrentHashMap<>();

	/* Secondary lookup across all sessions by id */
	private final Map<String, LocalSimpSession> sessions = new ConcurrentHashMap<>();

	private final Object sessionLock = new Object();


	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}


	// SmartApplicationListener methods

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return AbstractSubProtocolEvent.class.isAssignableFrom(eventType);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		AbstractSubProtocolEvent subProtocolEvent = (AbstractSubProtocolEvent) event;
		Message<?> message = subProtocolEvent.getMessage();

		SimpMessageHeaderAccessor accessor =
				MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
		Assert.state(accessor != null, "No SimpMessageHeaderAccessor");

		String sessionId = accessor.getSessionId();
		Assert.state(sessionId != null, "No session id");

		if (event instanceof SessionSubscribeEvent) {
			LocalSimpSession session = this.sessions.get(sessionId);
			if (session != null) {
				String id = accessor.getSubscriptionId();
				String destination = accessor.getDestination();
				if (id != null && destination != null) {
					session.addSubscription(id, destination);
				}
			}
		}
		else if (event instanceof SessionConnectedEvent) {
			Principal user = subProtocolEvent.getUser();
			if (user == null) {
				return;
			}
			String name = user.getName();
			if (user instanceof DestinationUserNameProvider) {
				name = ((DestinationUserNameProvider) user).getDestinationUserName();
			}
			synchronized (this.sessionLock) {
				LocalSimpUser simpUser = this.users.get(name);
				if (simpUser == null) {
					simpUser = new LocalSimpUser(name);
					this.users.put(name, simpUser);
				}
				LocalSimpSession session = new LocalSimpSession(sessionId, simpUser);
				simpUser.addSession(session);
				this.sessions.put(sessionId, session);
			}
		}
		else if (event instanceof SessionDisconnectEvent) {
			synchronized (this.sessionLock) {
				LocalSimpSession session = this.sessions.remove(sessionId);
				if (session != null) {
					LocalSimpUser user = session.getUser();
					user.removeSession(sessionId);
					if (!user.hasSessions()) {
						this.users.remove(user.getName());
					}
				}
			}
		}
		else if (event instanceof SessionUnsubscribeEvent) {
			LocalSimpSession session = this.sessions.get(sessionId);
			if (session != null) {
				String subscriptionId = accessor.getSubscriptionId();
				if (subscriptionId != null) {
					session.removeSubscription(subscriptionId);
				}
			}
		}
	}

	@Override
	public boolean supportsSourceType(@Nullable Class<?> sourceType) {
		return true;
	}


	// SimpUserRegistry methods

	@Override
	@Nullable
	public SimpUser getUser(String userName) {
		return this.users.get(userName);
	}

	@Override
	public Set<SimpUser> getUsers() {
		return new HashSet<>(this.users.values());
	}

	@Override
	public int getUserCount() {
		return this.users.size();
	}

	public Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher) {
		Set<SimpSubscription> result = new HashSet<>();
		for (LocalSimpSession session : this.sessions.values()) {
			for (SimpSubscription subscription : session.subscriptions.values()) {
				if (matcher.match(subscription)) {
					result.add(subscription);
				}
			}
		}
		return result;
	}


	@Override
	public String toString() {
		return "users=" + this.users;
	}


	private static class LocalSimpUser implements SimpUser {

		private final String name;

		private final Map<String, SimpSession> userSessions = new ConcurrentHashMap<>(1);

		public LocalSimpUser(String userName) {
			Assert.notNull(userName, "User name must not be null");
			this.name = userName;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean hasSessions() {
			return !this.userSessions.isEmpty();
		}

		@Override
		@Nullable
		public SimpSession getSession(@Nullable String sessionId) {
			return (sessionId != null ? this.userSessions.get(sessionId) : null);
		}

		@Override
		public Set<SimpSession> getSessions() {
			return new HashSet<>(this.userSessions.values());
		}

		void addSession(SimpSession session) {
			this.userSessions.put(session.getId(), session);
		}

		void removeSession(String sessionId) {
			this.userSessions.remove(sessionId);
		}

		@Override
		public boolean equals(Object other) {
			return (this == other ||
					(other instanceof SimpUser && getName().equals(((SimpUser) other).getName())));
		}

		@Override
		public int hashCode() {
			return getName().hashCode();
		}

		@Override
		public String toString() {
			return "name=" + getName() + ", sessions=" + this.userSessions;
		}
	}


	private static class LocalSimpSession implements SimpSession {

		private final String id;

		private final LocalSimpUser user;

		private final Map<String, SimpSubscription> subscriptions = new ConcurrentHashMap<>(4);

		public LocalSimpSession(String id, LocalSimpUser user) {
			Assert.notNull(id, "Id must not be null");
			Assert.notNull(user, "User must not be null");
			this.id = id;
			this.user = user;
		}

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public LocalSimpUser getUser() {
			return this.user;
		}

		@Override
		public Set<SimpSubscription> getSubscriptions() {
			return new HashSet<>(this.subscriptions.values());
		}

		void addSubscription(String id, String destination) {
			this.subscriptions.put(id, new LocalSimpSubscription(id, destination, this));
		}

		void removeSubscription(String id) {
			this.subscriptions.remove(id);
		}

		@Override
		public boolean equals(Object other) {
			return (this == other ||
					(other instanceof SimpSubscription && getId().equals(((SimpSubscription) other).getId())));
		}

		@Override
		public int hashCode() {
			return getId().hashCode();
		}

		@Override
		public String toString() {
			return "id=" + getId() + ", subscriptions=" + this.subscriptions;
		}
	}


	private static class LocalSimpSubscription implements SimpSubscription {

		private final String id;

		private final LocalSimpSession session;

		private final String destination;

		public LocalSimpSubscription(String id, String destination, LocalSimpSession session) {
			Assert.notNull(id, "Id must not be null");
			Assert.hasText(destination, "Destination must not be empty");
			Assert.notNull(session, "Session must not be null");
			this.id = id;
			this.destination = destination;
			this.session = session;
		}

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public LocalSimpSession getSession() {
			return this.session;
		}

		@Override
		public String getDestination() {
			return this.destination;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof SimpSubscription)) {
				return false;
			}
			SimpSubscription otherSubscription = (SimpSubscription) other;
			return (getId().equals(otherSubscription.getId()) &&
					getSession().getId().equals(otherSubscription.getSession().getId()));
		}

		@Override
		public int hashCode() {
			return getId().hashCode() * 31 + getSession().getId().hashCode();
		}

		@Override
		public String toString() {
			return "destination=" + this.destination;
		}
	}

}
