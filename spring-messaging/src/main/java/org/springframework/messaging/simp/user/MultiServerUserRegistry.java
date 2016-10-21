/*
 * Copyright 2002-2016 the original author or authors.
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@code SimpUserRegistry} that looks up users in a "local" user registry as
 * well as a set of "remote" user registries. The local registry is provided as
 * a constructor argument while remote registries are updated via broadcasts
 * handled by {@link UserRegistryMessageHandler} which in turn notifies this
 * registry when updates are received.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
@SuppressWarnings("serial")
public class MultiServerUserRegistry implements SimpUserRegistry, SmartApplicationListener {

	private final String id;

	private final SimpUserRegistry localRegistry;

	private final Map<String, UserRegistrySnapshot> remoteRegistries = new ConcurrentHashMap<>();

	private final boolean delegateApplicationEvents;

	/* Cross-server session lookup (e.g. same user connected to multiple servers) */
	private final SessionLookup sessionLookup = new SessionLookup();


	/**
	 * Create an instance wrapping the local user registry.
	 */
	public MultiServerUserRegistry(SimpUserRegistry localRegistry) {
		Assert.notNull(localRegistry, "'localRegistry' is required.");
		this.id = generateId();
		this.localRegistry = localRegistry;
		this.delegateApplicationEvents = this.localRegistry instanceof SmartApplicationListener;
	}

	private static String generateId() {
		String host;
		try {
			host = InetAddress.getLocalHost().getHostAddress();
		}
		catch (UnknownHostException ex) {
			host = "unknown";
		}
		return host + '-' + UUID.randomUUID();
	}


	@Override
	public int getOrder() {
		return (this.delegateApplicationEvents ?
				((SmartApplicationListener) this.localRegistry).getOrder() : Ordered.LOWEST_PRECEDENCE);
	}


	// SmartApplicationListener methods

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return (this.delegateApplicationEvents &&
				((SmartApplicationListener) this.localRegistry).supportsEventType(eventType));
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return (this.delegateApplicationEvents &&
				((SmartApplicationListener) this.localRegistry).supportsSourceType(sourceType));
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (this.delegateApplicationEvents) {
			((SmartApplicationListener) this.localRegistry).onApplicationEvent(event);
		}
	}


	// SimpUserRegistry methods

	@Override
	public SimpUser getUser(String userName) {
		// Prefer remote registries due to cross-server SessionLookup
		for (UserRegistrySnapshot registry : this.remoteRegistries.values()) {
			SimpUser user = registry.getUserMap().get(userName);
			if (user != null) {
				return user;
			}
		}
		return this.localRegistry.getUser(userName);
	}

	@Override
	public Set<SimpUser> getUsers() {
		// Prefer remote registries due to cross-server SessionLookup
		Set<SimpUser> result = new HashSet<>();
		for (UserRegistrySnapshot registry : this.remoteRegistries.values()) {
			result.addAll(registry.getUserMap().values());
		}
		result.addAll(this.localRegistry.getUsers());
		return result;
	}

	@Override
	public Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher) {
		Set<SimpSubscription> result = new HashSet<>();
		for (UserRegistrySnapshot registry : this.remoteRegistries.values()) {
			result.addAll(registry.findSubscriptions(matcher));
		}
		result.addAll(this.localRegistry.findSubscriptions(matcher));
		return result;
	}


	// Internal methods for UserRegistryMessageHandler to manage broadcasts

	Object getLocalRegistryDto() {
		return new UserRegistrySnapshot(this.id, this.localRegistry);
	}

	void addRemoteRegistryDto(Message<?> message, MessageConverter converter, long expirationPeriod) {
		UserRegistrySnapshot registry = (UserRegistrySnapshot) converter.fromMessage(message, UserRegistrySnapshot.class);
		if (registry != null && !registry.getId().equals(this.id)) {
			registry.init(expirationPeriod, this.sessionLookup);
			this.remoteRegistries.put(registry.getId(), registry);
		}
	}

	void purgeExpiredRegistries() {
		long now = System.currentTimeMillis();
		Iterator<Map.Entry<String, UserRegistrySnapshot>> iterator = this.remoteRegistries.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, UserRegistrySnapshot> entry = iterator.next();
			if (entry.getValue().isExpired(now)) {
				iterator.remove();
			}
		}
	}


	@Override
	public String toString() {
		return "local=[" + this.localRegistry +	"], remote=" + this.remoteRegistries;
	}


	/**
	 * Holds a copy of a SimpUserRegistry for the purpose of broadcasting to and
	 * receiving broadcasts from other application servers.
	 */
	private static class UserRegistrySnapshot {

		private String id;

		private Map<String, TransferSimpUser> users;

		private long expirationTime;

		/**
		 * Default constructor for JSON deserialization.
		 */
		@SuppressWarnings("unused")
		public UserRegistrySnapshot() {
		}

		/**
		 * Constructor to create DTO from a local user registry.
		 */
		public UserRegistrySnapshot(String id, SimpUserRegistry registry) {
			this.id = id;
			Set<SimpUser> users = registry.getUsers();
			this.users = new HashMap<>(users.size());
			for (SimpUser user : users) {
				this.users.put(user.getName(), new TransferSimpUser(user));
			}
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getId() {
			return this.id;
		}

		public void setUserMap(Map<String, TransferSimpUser> users) {
			this.users = users;
		}

		public Map<String, TransferSimpUser> getUserMap() {
			return this.users;
		}

		public boolean isExpired(long now) {
			return (now > this.expirationTime);
		}

		public void init(long expirationPeriod, SessionLookup sessionLookup) {
			this.expirationTime = System.currentTimeMillis() + expirationPeriod;
			for (TransferSimpUser user : this.users.values()) {
				user.afterDeserialization(sessionLookup);
			}
		}

		public Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher) {
			Set<SimpSubscription> result = new HashSet<>();
			for (TransferSimpUser user : this.users.values()) {
				for (TransferSimpSession session : user.sessions) {
					for (SimpSubscription subscription : session.subscriptions) {
						if (matcher.match(subscription)) {
							result.add(subscription);
						}
					}
				}
			}
			return result;
		}

		@Override
		public String toString() {
			return "id=" + this.id + ", users=" + this.users;
		}
	}


	/**
	 * SimpUser that can be (de)serialized and broadcast to other servers.
	 */
	private static class TransferSimpUser implements SimpUser {

		private String name;

		/* User sessions from "this" registry only (i.e. one server) */
		private Set<TransferSimpSession> sessions;

		/* Cross-server session lookup (e.g. user connected to multiple servers) */
		private SessionLookup sessionLookup;

		/**
		 * Default constructor for JSON deserialization.
		 */
		@SuppressWarnings("unused")
		public TransferSimpUser() {
			this.sessions = new HashSet<>(1);
		}

		/**
		 * Constructor to create user from a local user.
		 */
		public TransferSimpUser(SimpUser user) {
			this.name = user.getName();
			Set<SimpSession> sessions = user.getSessions();
			this.sessions = new HashSet<>(sessions.size());
			for (SimpSession session : sessions) {
				this.sessions.add(new TransferSimpSession(session));
			}
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean hasSessions() {
			if (this.sessionLookup != null) {
				return !this.sessionLookup.findSessions(getName()).isEmpty();
			}
			return !this.sessions.isEmpty();
		}

		@Override
		public SimpSession getSession(String sessionId) {
			if (this.sessionLookup != null) {
				return this.sessionLookup.findSessions(getName()).get(sessionId);
			}
			for (TransferSimpSession session : this.sessions) {
				if (session.getId().equals(sessionId)) {
					return session;
				}
			}
			return null;
		}

		public void setSessions(Set<TransferSimpSession> sessions) {
			this.sessions.addAll(sessions);
		}

		@Override
		public Set<SimpSession> getSessions() {
			if (this.sessionLookup != null) {
				Map<String, SimpSession> sessions = this.sessionLookup.findSessions(getName());
				return new HashSet<>(sessions.values());
			}
			return new HashSet<>(this.sessions);
		}

		private void afterDeserialization(SessionLookup sessionLookup) {
			this.sessionLookup = sessionLookup;
			for (TransferSimpSession session : this.sessions) {
				session.setUser(this);
				session.afterDeserialization();
			}
		}

		private void addSessions(Map<String, SimpSession> map) {
			for (SimpSession session : this.sessions) {
				map.put(session.getId(), session);
			}
		}


		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof SimpUser && this.name.equals(((SimpUser) other).getName())));
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


	/**
	 * SimpSession that can be (de)serialized and broadcast to other servers.
	 */
	private static class TransferSimpSession implements SimpSession {

		private String id;

		private TransferSimpUser user;

		private final Set<TransferSimpSubscription> subscriptions;

		/**
		 * Default constructor for JSON deserialization.
		 */
		@SuppressWarnings("unused")
		public TransferSimpSession() {
			this.subscriptions = new HashSet<>(4);
		}

		/**
		 * Constructor to create DTO from the local user session.
		 */
		public TransferSimpSession(SimpSession session) {
			this.id = session.getId();
			Set<SimpSubscription> subscriptions = session.getSubscriptions();
			this.subscriptions = new HashSet<>(subscriptions.size());
			for (SimpSubscription subscription : subscriptions) {
				this.subscriptions.add(new TransferSimpSubscription(subscription));
			}
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public String getId() {
			return this.id;
		}

		public void setUser(TransferSimpUser user) {
			this.user = user;
		}

		@Override
		public TransferSimpUser getUser() {
			return this.user;
		}

		public void setSubscriptions(Set<TransferSimpSubscription> subscriptions) {
			this.subscriptions.addAll(subscriptions);
		}

		@Override
		public Set<SimpSubscription> getSubscriptions() {
			return new HashSet<>(this.subscriptions);
		}

		private void afterDeserialization() {
			for (TransferSimpSubscription subscription : this.subscriptions) {
				subscription.setSession(this);
			}
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof SimpSession && this.id.equals(((SimpSession) other).getId())));
		}

		@Override
		public int hashCode() {
			return this.id.hashCode();
		}

		@Override
		public String toString() {
			return "id=" + this.id + ", subscriptions=" + this.subscriptions;
		}
	}


	/**
	 * SimpSubscription that can be (de)serialized and broadcast to other servers.
	 */
	private static class TransferSimpSubscription implements SimpSubscription {

		private String id;

		private TransferSimpSession session;

		private String destination;

		/**
		 * Default constructor for JSON deserialization.
		 */
		@SuppressWarnings("unused")
		public TransferSimpSubscription() {
		}

		/**
		 * Constructor to create DTO from a local user subscription.
		 */
		public TransferSimpSubscription(SimpSubscription subscription) {
			this.id = subscription.getId();
			this.destination = subscription.getDestination();
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public String getId() {
			return this.id;
		}

		public void setSession(TransferSimpSession session) {
			this.session = session;
		}

		@Override
		public TransferSimpSession getSession() {
			return this.session;
		}

		public void setDestination(String destination) {
			this.destination = destination;
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
			return (ObjectUtils.nullSafeEquals(getSession(), otherSubscription.getSession()) &&
					this.id.equals(otherSubscription.getId()));
		}

		@Override
		public int hashCode() {
			return this.id.hashCode() * 31 + ObjectUtils.nullSafeHashCode(getSession());
		}

		@Override
		public String toString() {
			return "destination=" + this.destination;
		}
	}


	/**
	 * Helper class to find user sessions across all servers.
	 */
	private class SessionLookup {

		public Map<String, SimpSession> findSessions(String userName) {
			Map<String, SimpSession> map = new HashMap<>(1);
			SimpUser user = localRegistry.getUser(userName);
			if (user != null) {
				for (SimpSession session : user.getSessions()) {
					map.put(session.getId(), session);
				}
			}
			for (UserRegistrySnapshot registry : remoteRegistries.values()) {
				TransferSimpUser transferUser = registry.getUserMap().get(userName);
				if (transferUser != null) {
					transferUser.addSessions(map);
				}
			}
			return map;
		}

	}

}
