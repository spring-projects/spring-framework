/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.simp.broker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.messaging.Message;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * A default, simple in-memory implementation of {@link SubscriptionRegistry}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class DefaultSubscriptionRegistry extends AbstractSubscriptionRegistry {

	private final DestinationCache destinationCache = new DestinationCache();

	private final SessionSubscriptionRegistry subscriptionRegistry = new SessionSubscriptionRegistry();

	private AntPathMatcher pathMatcher = new AntPathMatcher();


	/**
	 * @param pathMatcher the pathMatcher to set
	 */
	public void setPathMatcher(AntPathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	public AntPathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	@Override
	protected void addSubscriptionInternal(String sessionId, String subsId, String destination, Message<?> message) {
		SessionSubscriptionInfo info = this.subscriptionRegistry.addSubscription(sessionId, subsId, destination);
		this.destinationCache.mapToDestination(destination, sessionId, subsId);
	}

	@Override
	protected void removeSubscriptionInternal(String sessionId, String subsId, Message<?> message) {
		SessionSubscriptionInfo info = this.subscriptionRegistry.getSubscriptions(sessionId);
		if (info != null) {
			String destination = info.removeSubscription(subsId);
			if (info.getSubscriptions(destination) == null) {
				this.destinationCache.unmapFromDestination(destination, sessionId, subsId);
			}
		}
	}

	@Override
	public void unregisterAllSubscriptions(String sessionId) {
		SessionSubscriptionInfo info = this.subscriptionRegistry.removeSubscriptions(sessionId);
		if (info != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Unregistering subscriptions for sessionId=" + sessionId);
			}
			this.destinationCache.removeSessionSubscriptions(info);
		}
	}

	@Override
	protected MultiValueMap<String, String> findSubscriptionsInternal(String destination, Message<?> message) {
		MultiValueMap<String,String> result;
		if (this.destinationCache.isCachedDestination(destination)) {
			result = this.destinationCache.getSubscriptions(destination);
		}
		else {
			result = new LinkedMultiValueMap<String, String>();
			for (SessionSubscriptionInfo info : this.subscriptionRegistry.getAllSubscriptions()) {
				for (String destinationPattern : info.getDestinations()) {
					if (this.pathMatcher.match(destinationPattern, destination)) {
						for (String subscriptionId : info.getSubscriptions(destinationPattern)) {
							result.add(info.sessionId, subscriptionId);
						}
					}
				}
			}
			if(!result.isEmpty()) {
				this.destinationCache.addSubscriptions(destination, result);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "[destinationCache=" + this.destinationCache + ", subscriptionRegistry="
				+ this.subscriptionRegistry + "]";
	}




	/**
	 * Provide direct lookup of session subscriptions by destination
	 */
	private static class DestinationCache {

		private AntPathMatcher pathMatcher = new AntPathMatcher();

		// destination -> ..
		private final Map<String, MultiValueMap<String, String>> subscriptionsByDestination =
				new ConcurrentHashMap<String, MultiValueMap<String, String>>();

		private final Object monitor = new Object();


		public void addSubscriptions(String destination, MultiValueMap<String, String> subscriptions) {
			this.subscriptionsByDestination.put(destination, subscriptions);
		}

		public void mapToDestination(String destination, String sessionId, String subsId) {
			synchronized(this.monitor) {
				for (String cachedDestination : this.subscriptionsByDestination.keySet()) {
					if (this.pathMatcher.match(destination, cachedDestination)) {
						MultiValueMap<String, String> registrations = this.subscriptionsByDestination.get(cachedDestination);
						if (registrations == null) {
							registrations = new LinkedMultiValueMap<String, String>();
						}
						registrations.add(sessionId, subsId);
					}
				}
			}
		}

		public void unmapFromDestination(String destination, String sessionId, String subsId) {
			synchronized(this.monitor) {
				for (String cachedDestination : this.subscriptionsByDestination.keySet()) {
					if (this.pathMatcher.match(destination, cachedDestination)) {
						MultiValueMap<String, String> registrations = this.subscriptionsByDestination.get(cachedDestination);
						List<String> subscriptions = registrations.get(sessionId);
						while(subscriptions.remove(subsId));
						if (subscriptions.isEmpty()) {
							registrations.remove(sessionId);
						}
						if (registrations.isEmpty()) {
							this.subscriptionsByDestination.remove(cachedDestination);
						}
					}
				}
			}
		}

		public void removeSessionSubscriptions(SessionSubscriptionInfo info) {
			synchronized(this.monitor) {
				for (String destination : info.getDestinations()) {
					for (String cachedDestination : this.subscriptionsByDestination.keySet()) {
						if (this.pathMatcher.match(destination, cachedDestination)) {
							MultiValueMap<String, String> map = this.subscriptionsByDestination.get(cachedDestination);
							map.remove(info.getSessionId());
							if (map.isEmpty()) {
								this.subscriptionsByDestination.remove(cachedDestination);
							}
						}
					}
				}
			}
		}

		public MultiValueMap<String, String> getSubscriptions(String destination) {
			return this.subscriptionsByDestination.get(destination);
		}

		public boolean isCachedDestination(String destination) {
			return subscriptionsByDestination.containsKey(destination);
		}

		@Override
		public String toString() {
			return "[subscriptionsByDestination=" + this.subscriptionsByDestination + "]";
		}
	}

	/**
	 * Provide access to session subscriptions by sessionId.
	 */
	private static class SessionSubscriptionRegistry {

		private final ConcurrentMap<String, SessionSubscriptionInfo> sessions =
				new ConcurrentHashMap<String, SessionSubscriptionInfo>();


		public SessionSubscriptionInfo getSubscriptions(String sessionId) {
			return this.sessions.get(sessionId);
		}

		public Collection<SessionSubscriptionInfo> getAllSubscriptions() {
			return this.sessions.values();
		}

		public SessionSubscriptionInfo addSubscription(String sessionId, String subscriptionId, String destination) {
			SessionSubscriptionInfo info = this.sessions.get(sessionId);
			if (info == null) {
				info = new SessionSubscriptionInfo(sessionId);
				SessionSubscriptionInfo value = this.sessions.putIfAbsent(sessionId, info);
				if (value != null) {
					info = value;
				}
			}
			info.addSubscription(destination, subscriptionId);
			return info;
		}

		public SessionSubscriptionInfo removeSubscriptions(String sessionId) {
			return this.sessions.remove(sessionId);
		}

		@Override
		public String toString() {
			return "[sessions=" + sessions + "]";
		}
	}

	/**
	 * Hold subscriptions for a session.
	 */
	private static class SessionSubscriptionInfo {

		private final String sessionId;

		private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<String, Set<String>>(4);

		private final Object monitor = new Object();


		public SessionSubscriptionInfo(String sessionId) {
			Assert.notNull(sessionId, "sessionId must not be null");
			this.sessionId = sessionId;
		}

		public String getSessionId() {
			return this.sessionId;
		}

		public Set<String> getDestinations() {
			return this.subscriptions.keySet();
		}

		public Set<String> getSubscriptions(String destination) {
			return this.subscriptions.get(destination);
		}

		public void addSubscription(String destination, String subscriptionId) {
			Set<String> subs = this.subscriptions.get(destination);
			if (subs == null) {
				synchronized(this.monitor) {
					subs = this.subscriptions.get(destination);
					if (subs == null) {
						subs = new HashSet<String>(4);
						this.subscriptions.put(destination, subs);
					}
				}
			}
			subs.add(subscriptionId);
		}

		public String removeSubscription(String subscriptionId) {
			for (String destination : this.subscriptions.keySet()) {
				Set<String> subscriptionIds = this.subscriptions.get(destination);
				if (subscriptionIds.remove(subscriptionId)) {
					synchronized(this.monitor) {
						if (subscriptionIds.isEmpty()) {
							this.subscriptions.remove(destination);
						}
					}
					return destination;
				}
			}
			return null;
		}

		@Override
		public String toString() {
			return "[sessionId=" + this.sessionId + ", subscriptions=" + this.subscriptions + "]";
		}
	}

}
