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

package org.springframework.web.messaging.support;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.util.Assert;
import org.springframework.web.messaging.SessionSubscriptionRegistration;
import org.springframework.web.messaging.SessionSubscriptionRegistry;


/**
 * A decorator for a {@link SessionSubscriptionRegistry} that intercepts subscriptions
 * being added and removed and maintains a lookup cache of registrations by destination.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class CachingSessionSubscriptionRegistry implements SessionSubscriptionRegistry {

	private final SessionSubscriptionRegistry delegate;

	private final DestinationCache destinationCache = new DestinationCache();


	public CachingSessionSubscriptionRegistry(SessionSubscriptionRegistry delegate) {
		Assert.notNull(delegate, "delegate SessionSubscriptionRegistry is required");
		this.delegate = delegate;
	}


	@Override
	public SessionSubscriptionRegistration getRegistration(String sessionId) {
		SessionSubscriptionRegistration reg = this.delegate.getRegistration(sessionId);
		return (reg != null) ? new CachingSessionSubscriptionRegistration(reg) : null;
	}

	@Override
	public SessionSubscriptionRegistration getOrCreateRegistration(String sessionId) {
		return new CachingSessionSubscriptionRegistration(this.delegate.getOrCreateRegistration(sessionId));
	}

	@Override
	public SessionSubscriptionRegistration removeRegistration(String sessionId) {
		SessionSubscriptionRegistration registration = this.delegate.removeRegistration(sessionId);
		if (registration != null) {
			this.destinationCache.removeRegistration(registration);
		}
		return registration;
	}

	@Override
	public Set<String> getSessionSubscriptions(String sessionId, String destination) {
		return this.delegate.getSessionSubscriptions(sessionId, destination);
	}

	@Override
	public Set<SessionSubscriptionRegistration> getRegistrationsByDestination(String destination) {
		return this.destinationCache.getRegistrations(destination);
	}


	private static class DestinationCache {

		private final Map<String, Set<SessionSubscriptionRegistration>> cache =
				new ConcurrentHashMap<String, Set<SessionSubscriptionRegistration>>();

		private final Object monitor = new Object();


		public void mapRegistration(String destination, SessionSubscriptionRegistration registration) {
			synchronized (monitor) {
				Set<SessionSubscriptionRegistration> registrations = this.cache.get(destination);
				if (registrations == null) {
					registrations = new CopyOnWriteArraySet<SessionSubscriptionRegistration>();
					this.cache.put(destination, registrations);
				}
				registrations.add(registration);
			}
		}

		public void unmapRegistration(String destination, SessionSubscriptionRegistration registration) {
			synchronized (monitor) {
				Set<SessionSubscriptionRegistration> registrations = this.cache.get(destination);
				if (registrations != null) {
					registrations.remove(registration);
					if (registrations.isEmpty()) {
						this.cache.remove(destination);
					}
				}
			}
		}

		private void removeRegistration(SessionSubscriptionRegistration registration) {
			for (String destination : registration.getDestinations()) {
				unmapRegistration(destination, registration);
			}
		}

		public Set<SessionSubscriptionRegistration> getRegistrations(String destination) {
			return this.cache.get(destination);
		}

		@Override
		public String toString() {
			return "DestinationCache [cache=" + this.cache + "]";
		}
	}

	private class CachingSessionSubscriptionRegistration implements SessionSubscriptionRegistration {

		private final SessionSubscriptionRegistration delegate;


		public CachingSessionSubscriptionRegistration(SessionSubscriptionRegistration delegate) {
			Assert.notNull(delegate, "delegate SessionSubscriptionRegistration is required");
			this.delegate = delegate;
		}

		@Override
		public String getSessionId() {
			return this.delegate.getSessionId();
		}

		@Override
		public void addSubscription(String destination, String subscriptionId) {
			destinationCache.mapRegistration(destination, this);
			this.delegate.addSubscription(destination, subscriptionId);
		}

		@Override
		public String removeSubscription(String subscriptionId) {
			String destination = this.delegate.removeSubscription(subscriptionId);
			if (destination != null && this.delegate.getSubscriptionsByDestination(destination) == null) {
				destinationCache.unmapRegistration(destination, this);
			}
			return destination;
		}

		@Override
		public Set<String> getSubscriptionsByDestination(String destination) {
			return this.delegate.getSubscriptionsByDestination(destination);
		}

		@Override
		public Set<String> getDestinations() {
			return this.delegate.getDestinations();
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof CachingSessionSubscriptionRegistration)) {
				return false;
			}
			CachingSessionSubscriptionRegistration otherType = (CachingSessionSubscriptionRegistration) other;
			return this.delegate.equals(otherType.delegate);
		}

		@Override
		public int hashCode() {
			return this.delegate.hashCode();
		}

		@Override
		public String toString() {
			return "CachingSessionSubscriptionRegistration [delegate=" + delegate + "]";
		}
	}

}
