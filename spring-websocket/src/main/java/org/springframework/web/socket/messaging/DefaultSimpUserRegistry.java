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

package org.springframework.web.socket.messaging;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
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
 * Default, mutable, thread-safe implementation of {@link SimpUserRegistry} that
 * listens ApplicationContext events of type {@link AbstractSubProtocolEvent} to
 * keep track of user presence and subscription information.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class DefaultSimpUserRegistry implements SimpUserRegistry, SmartApplicationListener {

	private final Map<String, DefaultSimpUser> users = new ConcurrentHashMap<String, DefaultSimpUser>();

	private final Map<String, DefaultSimpSession> sessions = new ConcurrentHashMap<String, DefaultSimpSession>();


	@Override
	public SimpUser getUser(String userName) {
		return this.users.get(userName);
	}

	@Override
	public Set<SimpUser> getUsers() {
		return new HashSet<SimpUser>(this.users.values());
	}

	public Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher) {
		Set<SimpSubscription> result = new HashSet<SimpSubscription>();
		for (DefaultSimpSession session : this.sessions.values()) {
			for (SimpSubscription subscription : session.subscriptions.values()) {
				if (matcher.match(subscription)) {
					result.add(subscription);
				}
			}
		}
		return result;
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return AbstractSubProtocolEvent.class.isAssignableFrom(eventType);
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return true;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {

		AbstractSubProtocolEvent subProtocolEvent = (AbstractSubProtocolEvent) event;
		Message<?> message = subProtocolEvent.getMessage();
		SimpMessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
		String sessionId = accessor.getSessionId();

		if (event instanceof SessionSubscribeEvent) {
			DefaultSimpSession session = this.sessions.get(sessionId);
			if (session != null) {
				String id = accessor.getSubscriptionId();
				String destination = accessor.getDestination();
				session.addSubscription(id, destination);
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
			synchronized (this) {
				DefaultSimpUser simpUser = this.users.get(name);
				if (simpUser == null) {
					simpUser = new DefaultSimpUser(name, sessionId);
					this.users.put(name, simpUser);
				}
				else {
					simpUser.addSession(sessionId);
				}
				this.sessions.put(sessionId, (DefaultSimpSession) simpUser.getSession(sessionId));
			}
		}
		else if (event instanceof SessionDisconnectEvent) {
			synchronized (this) {
				DefaultSimpSession session = this.sessions.remove(sessionId);
				if (session != null) {
					DefaultSimpUser user = session.getUser();
					user.removeSession(sessionId);
					if (!user.hasSessions()) {
						this.users.remove(user.getName());
					}
				}
			}
		}
		else if (event instanceof SessionUnsubscribeEvent) {
			DefaultSimpSession session = this.sessions.get(sessionId);
			if (session != null) {
				String subscriptionId = accessor.getSubscriptionId();
				session.removeSubscription(subscriptionId);
			}
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public String toString() {
		return "users=" + this.users;
	}

	private static class DefaultSimpUser implements SimpUser {

		private final String name;

		private final Map<String, SimpSession> sessions =
				new ConcurrentHashMap<String, SimpSession>(1);


		public DefaultSimpUser(String userName, String sessionId) {
			Assert.notNull(userName);
			Assert.notNull(sessionId);
			this.name = userName;
			this.sessions.put(sessionId, new DefaultSimpSession(sessionId, this));
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
			return (sessionId != null ? this.sessions.get(sessionId) : null);
		}

		@Override
		public Set<SimpSession> getSessions() {
			return new HashSet<SimpSession>(this.sessions.values());
		}

		void addSession(String sessionId) {
			DefaultSimpSession session = new DefaultSimpSession(sessionId, this);
			this.sessions.put(sessionId, session);
		}

		void removeSession(String sessionId) {
			this.sessions.remove(sessionId);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || !(other instanceof SimpUser)) {
				return false;
			}
			return this.name.equals(((SimpUser) other).getName());
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}

		@Override
		public String toString() {
			return "name=" + this.name + ", sessions=" + this.sessions;
		}
	}

	private static class DefaultSimpSession implements SimpSession {

		private final String id;

		private final DefaultSimpUser user;

		private final Map<String, SimpSubscription> subscriptions = new ConcurrentHashMap<String, SimpSubscription>(4);


		public DefaultSimpSession(String id, DefaultSimpUser user) {
			Assert.notNull(id);
			Assert.notNull(user);
			this.id = id;
			this.user = user;
		}

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public DefaultSimpUser getUser() {
			return this.user;
		}

		@Override
		public Set<SimpSubscription> getSubscriptions() {
			return new HashSet<SimpSubscription>(this.subscriptions.values());
		}

		void addSubscription(String id, String destination) {
			this.subscriptions.put(id, new DefaultSimpSubscription(id, destination, this));
		}

		void removeSubscription(String id) {
			this.subscriptions.remove(id);
		}

		@Override
		public int hashCode() {
			return this.id.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || !(other instanceof SimpSubscription)) {
				return false;
			}
			return this.id.equals(((SimpSubscription) other).getId());
		}

		@Override
		public String toString() {
			return "id=" + this.id + ", subscriptions=" + this.subscriptions;
		}
	}

	private static class DefaultSimpSubscription implements SimpSubscription {

		private final String id;

		private final DefaultSimpSession session;

		private final String destination;


		public DefaultSimpSubscription(String id, String destination, DefaultSimpSession session) {
			Assert.notNull(id);
			Assert.hasText(destination);
			Assert.notNull(session);
			this.id = id;
			this.destination = destination;
			this.session = session;
		}

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public DefaultSimpSession getSession() {
			return this.session;
		}

		@Override
		public String getDestination() {
			return this.destination;
		}

		@Override
		public int hashCode() {
			return 31 * this.id.hashCode() + getSession().hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || !(other instanceof SimpSubscription)) {
				return false;
			}
			SimpSubscription otherSubscription = (SimpSubscription) other;
			return (getSession().getId().equals(otherSubscription.getSession().getId()) &&
					this.id.equals(otherSubscription.getId()));
		}

		@Override
		public String toString() {
			return "destination=" + this.destination;
		}
	}

}
