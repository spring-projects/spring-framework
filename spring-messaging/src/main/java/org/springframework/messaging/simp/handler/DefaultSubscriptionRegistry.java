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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.messaging.Message;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


/**
 * @author Rossen Stoyanchev
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
		if (!this.pathMatcher.isPattern(destination)) {
			this.destinationCache.mapToDestination(destination, info);
		}
	}

	@Override
	protected void removeSubscriptionInternal(String sessionId, String subscriptionId, Message<?> message) {
		SessionSubscriptionInfo info = this.subscriptionRegistry.getSubscriptions(sessionId);
		if (info != null) {
			String destination = info.removeSubscription(subscriptionId);
			if (info.getSubscriptions(destination) == null) {
				this.destinationCache.unmapFromDestination(destination, info);
			}
		}
	}

	@Override
	public void removeSessionSubscriptions(String sessionId) {
		SessionSubscriptionInfo info = this.subscriptionRegistry.removeSubscriptions(sessionId);
		this.destinationCache.removeSessionSubscriptions(info);
	}

	@Override
	protected MultiValueMap<String, String> findSubscriptionsInternal(String destination, Message<?> message) {
		MultiValueMap<String,String> result = this.destinationCache.getSubscriptions(destination);
		if (result.isEmpty()) {
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
		}
		return result;
	}


	/**
	 * Provide direct lookup of session subscriptions by destination (for non-pattern destinations).
	 */
	private static class DestinationCache {

		// destination -> ..
		private final Map<String, Set<SessionSubscriptionInfo>> subscriptionsByDestination =
				new ConcurrentHashMap<String, Set<SessionSubscriptionInfo>>();

		private final Object monitor = new Object();


		public void mapToDestination(String destination, SessionSubscriptionInfo info) {
			synchronized (monitor) {
				Set<SessionSubscriptionInfo> registrations = this.subscriptionsByDestination.get(destination);
				if (registrations == null) {
					registrations = new CopyOnWriteArraySet<SessionSubscriptionInfo>();
					this.subscriptionsByDestination.put(destination, registrations);
				}
				registrations.add(info);
			}
		}

		public void unmapFromDestination(String destination, SessionSubscriptionInfo info) {
			synchronized (monitor) {
				Set<SessionSubscriptionInfo> infos = this.subscriptionsByDestination.get(destination);
				if (infos != null) {
					infos.remove(info);
					if (infos.isEmpty()) {
						this.subscriptionsByDestination.remove(destination);
					}
				}
			}
		}

		public void removeSessionSubscriptions(SessionSubscriptionInfo info) {
			for (String destination : info.getDestinations()) {
				unmapFromDestination(destination, info);
			}
		}

		public MultiValueMap<String, String> getSubscriptions(String destination) {
			MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>();
			Set<SessionSubscriptionInfo> infos = this.subscriptionsByDestination.get(destination);
			if (infos != null) {
				for (SessionSubscriptionInfo info : infos) {
					Set<String> subscriptions = info.getSubscriptions(destination);
					if (subscriptions != null) {
						for (String subscription : subscriptions) {
							result.add(info.getSessionId(), subscription);
						}
					}
				}
			}
			return result;
		}
	}

	/**
	 * Provide access to session subscriptions by sessionId.
	 */
	private static class SessionSubscriptionRegistry {

		private final Map<String, SessionSubscriptionInfo> sessions =
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
				this.sessions.put(sessionId, info);
			}
			info.addSubscription(subscriptionId, destination);
			return info;
		}

		public SessionSubscriptionInfo removeSubscriptions(String sessionId) {
			return this.sessions.remove(sessionId);
		}
	}

	/**
	 * Hold subscriptions for a session.
	 */
	private static class SessionSubscriptionInfo {

		private final String sessionId;

		private final Map<String, Set<String>> subscriptions = new HashMap<String, Set<String>>(4);


		public SessionSubscriptionInfo(String sessionId) {
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

		public void addSubscription(String subscriptionId, String destination) {
			Set<String> subs = this.subscriptions.get(destination);
			if (subs == null) {
				subs = new HashSet<String>(4);
				this.subscriptions.put(destination, subs);
			}
			subs.add(subscriptionId);
		}

		public String removeSubscription(String subscriptionId) {
			for (String destination : this.subscriptions.keySet()) {
				Set<String> subscriptionIds = this.subscriptions.get(destination);
				if (subscriptionIds.remove(subscriptionId)) {
					if (subscriptionIds.isEmpty()) {
						this.subscriptions.remove(destination);
					}
					return destination;
				}
			}
			return null;
		}
	}

}
