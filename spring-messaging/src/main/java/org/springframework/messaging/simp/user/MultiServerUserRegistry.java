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
 * A user registry that is a composite of the "local" user registry as well as
 * snapshots of remote user registries. For use with
 * {@link UserRegistryMessageHandler} which broadcasts periodically the content
 * of the local registry and receives updates from other servers.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
@SuppressWarnings("serial")
public class MultiServerUserRegistry implements SimpUserRegistry, SmartApplicationListener {

	private final String id;

	private final SimpUserRegistry localRegistry;

	private final SmartApplicationListener listener;

	private final Map<String, UserRegistryDto> remoteRegistries =
			new ConcurrentHashMap<String, UserRegistryDto>();


	/**
	 * Create an instance wrapping the local user registry.
	 */
	public MultiServerUserRegistry(SimpUserRegistry localRegistry) {
		Assert.notNull(localRegistry, "'localRegistry' is required.");
		this.id = generateId();
		this.localRegistry = localRegistry;
		this.listener = (this.localRegistry instanceof SmartApplicationListener ?
				(SmartApplicationListener) this.localRegistry : new NoOpSmartApplicationListener());
	}


	private static String generateId() {
		String host;
		try {
			host = InetAddress.getLocalHost().getHostAddress();
		}
		catch (UnknownHostException ex) {
			host = "unknown";
		}
		return host + "-" + UUID.randomUUID();
	}


	@Override
	public SimpUser getUser(String userName) {
		SimpUser user = this.localRegistry.getUser(userName);
		if (user != null) {
			return user;
		}
		for (UserRegistryDto registry : this.remoteRegistries.values()) {
			user = registry.getUsers().get(userName);
			if (user != null) {
				return user;
			}
		}
		return null;
	}

	@Override
	public Set<SimpUser> getUsers() {
		Set<SimpUser> result = new HashSet<SimpUser>(this.localRegistry.getUsers());
		for (UserRegistryDto registry : this.remoteRegistries.values()) {
			result.addAll(registry.getUsers().values());
		}
		return result;
	}

	@Override
	public Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher) {
		Set<SimpSubscription> result = new HashSet<SimpSubscription>(this.localRegistry.findSubscriptions(matcher));
		for (UserRegistryDto registry : this.remoteRegistries.values()) {
			result.addAll(registry.findSubscriptions(matcher));
		}
		return result;
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return this.listener.supportsEventType(eventType);
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return this.listener.supportsSourceType(sourceType);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		this.listener.onApplicationEvent(event);
	}

	@Override
	public int getOrder() {
		return this.listener.getOrder();
	}

	Object getLocalRegistryDto() {
		return new UserRegistryDto(this.id, this.localRegistry);
	}

	void addRemoteRegistryDto(Message<?> message, MessageConverter converter, long expirationPeriod) {
		UserRegistryDto registryDto = (UserRegistryDto) converter.fromMessage(message, UserRegistryDto.class);
		if (registryDto != null && !registryDto.getId().equals(this.id)) {
			long expirationTime = System.currentTimeMillis() + expirationPeriod;
			registryDto.setExpirationTime(expirationTime);
			registryDto.restoreParentReferences();
			this.remoteRegistries.put(registryDto.getId(), registryDto);
		}
	}

	void purgeExpiredRegistries() {
		long now = System.currentTimeMillis();
		Iterator<Map.Entry<String, UserRegistryDto>> iterator = this.remoteRegistries.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, UserRegistryDto> entry = iterator.next();
			if (now > entry.getValue().getExpirationTime()) {
				iterator.remove();
			}
		}
	}

	@Override
	public String toString() {
		return "local=[" + this.localRegistry +	"], remote=" + this.remoteRegistries;
	}


	private static class UserRegistryDto {

		private String id;

		private Map<String, SimpUserDto> users;

		private long expirationTime;

		/**
		 * Default constructor for JSON deserialization.
		 */
		@SuppressWarnings("unused")
		public UserRegistryDto() {
		}

		public UserRegistryDto(String id, SimpUserRegistry registry) {
			this.id = id;
			Set<SimpUser> users = registry.getUsers();
			this.users = new HashMap<String, SimpUserDto>(users.size());
			for (SimpUser user : users) {
				this.users.put(user.getName(), new SimpUserDto(user));
			}
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getId() {
			return this.id;
		}

		public void setUsers(Map<String, SimpUserDto> users) {
			this.users = users;
		}

		public Map<String, SimpUserDto> getUsers() {
			return this.users;
		}

		public Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher) {
			Set<SimpSubscription> result = new HashSet<SimpSubscription>();
			for (SimpUserDto user : this.users.values()) {
				for (SimpSessionDto session : user.sessions) {
					for (SimpSubscription subscription : session.subscriptions) {
						if (matcher.match(subscription)) {
							result.add(subscription);
						}
					}
				}
			}
			return result;
		}

		public void setExpirationTime(long expirationTime) {
			this.expirationTime = expirationTime;
		}

		public long getExpirationTime() {
			return this.expirationTime;
		}

		private void restoreParentReferences() {
			for (SimpUserDto user : this.users.values()) {
				user.restoreParentReferences();
			}
		}
		@Override
		public String toString() {
			return "id=" + this.id + ", users=" + this.users;
		}
	}


	private static class SimpUserDto implements SimpUser {

		private String name;

		private Set<SimpSessionDto> sessions;

		/**
		 * Default constructor for JSON deserialization.
		 */
		@SuppressWarnings("unused")
		public SimpUserDto() {
			this.sessions = new HashSet<SimpSessionDto>(1);
		}

		public SimpUserDto(SimpUser user) {
			this.name = user.getName();
			Set<SimpSession> sessions = user.getSessions();
			this.sessions = new HashSet<SimpSessionDto>(sessions.size());
			for (SimpSession session : sessions) {
				this.sessions.add(new SimpSessionDto(session));
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
			return !this.sessions.isEmpty();
		}

		@Override
		public SimpSessionDto getSession(String sessionId) {
			for (SimpSessionDto session : this.sessions) {
				if (session.getId().equals(sessionId)) {
					return session;
				}
			}
			return null;
		}

		public void setSessions(Set<SimpSessionDto> sessions) {
			this.sessions.addAll(sessions);
		}

		@Override
		public Set<SimpSession> getSessions() {
			return new HashSet<SimpSession>(this.sessions);
		}

		private void restoreParentReferences() {
			for (SimpSessionDto session : this.sessions) {
				session.setUser(this);
				session.restoreParentReferences();
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


	private static class SimpSessionDto implements SimpSession {

		private String id;

		private SimpUserDto user;

		private final Set<SimpSubscriptionDto> subscriptions;

		/**
		 * Default constructor for JSON deserialization.
		 */
		@SuppressWarnings("unused")
		public SimpSessionDto() {
			this.subscriptions = new HashSet<SimpSubscriptionDto>(4);
		}

		public SimpSessionDto(SimpSession session) {
			this.id = session.getId();
			Set<SimpSubscription> subscriptions = session.getSubscriptions();
			this.subscriptions = new HashSet<SimpSubscriptionDto>(subscriptions.size());
			for (SimpSubscription subscription : subscriptions) {
				this.subscriptions.add(new SimpSubscriptionDto(subscription));
			}
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public String getId() {
			return this.id;
		}

		public void setUser(SimpUserDto user) {
			this.user = user;
		}

		@Override
		public SimpUserDto getUser() {
			return this.user;
		}

		public void setSubscriptions(Set<SimpSubscriptionDto> subscriptions) {
			this.subscriptions.addAll(subscriptions);
		}

		@Override
		public Set<SimpSubscription> getSubscriptions() {
			return new HashSet<SimpSubscription>(this.subscriptions);
		}

		private void restoreParentReferences() {
			for (SimpSubscriptionDto subscription : this.subscriptions) {
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


	private static class SimpSubscriptionDto implements SimpSubscription {

		private String id;

		private SimpSessionDto session;

		private String destination;

		/**
		 * Default constructor for JSON deserialization.
		 */
		@SuppressWarnings("unused")
		public SimpSubscriptionDto() {
		}

		public SimpSubscriptionDto(SimpSubscription subscription) {
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

		public void setSession(SimpSessionDto session) {
			this.session = session;
		}

		@Override
		public SimpSessionDto getSession() {
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


	private static class NoOpSmartApplicationListener implements SmartApplicationListener {

		@Override
		public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
			return false;
		}

		@Override
		public boolean supportsSourceType(Class<?> sourceType) {
			return false;
		}

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
		}

		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE;
		}
	}

}
