/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.socket.messaging;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.user.DestinationUserNameProvider;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpSubscriptionMatcher;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.util.Assert;

/**
 * A default implementation of {@link SimpUserRegistry} that relies on
 * {@link AbstractSubProtocolEvent} application context events to keep
 * track of connected users and their subscriptions.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.2
 */
public class DefaultSimpUserRegistry implements SimpUserRegistry, SmartApplicationListener {

	private int order = Ordered.LOWEST_PRECEDENCE;

	/* Primary lookup that holds all users and their sessions */
	private final Map<String, LocalSimpUser> users = new ConcurrentHashMap<>();

	/* Secondary lookup across all sessions by id */
	private final Map<String, LocalSimpSession> sessions = new ConcurrentHashMap<>();

	private final Object sessionLock = new Object();


	/**
	 * Specify the order value for this registry.
	 * <p>Default is {@link Ordered#LOWEST_PRECEDENCE}.
	 * @since 5.0.8
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
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
		MessageHeaders headers = message.getHeaders();

		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		Assert.state(sessionId != null, "No session id");

		if (event instanceof SessionSubscribeEvent) {
			LocalSimpSession session = this.sessions.get(sessionId);
			if (session != null) {
				String id = SimpMessageHeaderAccessor.getSubscriptionId(headers);
				String destination = SimpMessageHeaderAccessor.getDestination(headers);
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
			if (user instanceof DestinationUserNameProvider destinationUserNameProvider) {
				name = destinationUserNameProvider.getDestinationUserName();
			}
			synchronized (this.sessionLock) {
				LocalSimpUser simpUser = this.users.get(name);
				if (simpUser == null) {
					simpUser = new LocalSimpUser(name, user);
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
				String subscriptionId = SimpMessageHeaderAccessor.getSubscriptionId(headers);
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
	public @Nullable SimpUser getUser(String userName) {
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

	@Override
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

		private final Principal user;

		private final Map<String, SimpSession> userSessions = new ConcurrentHashMap<>(1);

		public LocalSimpUser(String userName, Principal user) {
			Assert.notNull(userName, "User name must not be null");
			this.name = userName;
			this.user = user;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public @Nullable Principal getPrincipal() {
			return this.user;
		}

		@Override
		public boolean hasSessions() {
			return !this.userSessions.isEmpty();
		}

		@Override
		public @Nullable SimpSession getSession(@Nullable String sessionId) {
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
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof SimpUser that && getName().equals(that.getName())));
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
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof SimpSubscription that && getId().equals(that.getId())));
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
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof SimpSubscription that &&
					getId().equals(that.getId()) &&
					getSession().getId().equals(that.getSession().getId())));
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
