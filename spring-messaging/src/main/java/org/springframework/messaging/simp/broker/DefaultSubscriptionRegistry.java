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

package org.springframework.messaging.simp.broker;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.messaging.Message;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;

/**
 * A default, simple in-memory implementation of {@link SubscriptionRegistry}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DefaultSubscriptionRegistry extends AbstractSubscriptionRegistry {

	/** Default maximum number of entries for the destination cache: 1024 */
	public static final int DEFAULT_CACHE_LIMIT = 1024;


	/** The maximum number of entries in the cache */
	private volatile int cacheLimit = DEFAULT_CACHE_LIMIT;

	private PathMatcher pathMatcher = new AntPathMatcher();

	private final DestinationCache destinationCache = new DestinationCache();

	private final SessionSubscriptionRegistry subscriptionRegistry = new SessionSubscriptionRegistry();


	/**
	 * Specify the maximum number of entries for the resolved destination cache.
	 * Default is 1024.
	 */
	public void setCacheLimit(int cacheLimit) {
		this.cacheLimit = cacheLimit;
	}

	/**
	 * Return the maximum number of entries for the resolved destination cache.
	 */
	public int getCacheLimit() {
		return this.cacheLimit;
	}

	/**
	 * Specify the {@link PathMatcher} to use.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Return the configured {@link PathMatcher}.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}


	@Override
	protected void addSubscriptionInternal(String sessionId, String subsId, String destination, Message<?> message) {
		this.subscriptionRegistry.addSubscription(sessionId, subsId, destination);
		this.destinationCache.updateAfterNewSubscription(destination, sessionId, subsId);
	}

	@Override
	protected void removeSubscriptionInternal(String sessionId, String subsId, Message<?> message) {
		SessionSubscriptionInfo info = this.subscriptionRegistry.getSubscriptions(sessionId);
		if (info != null) {
			String destination = info.removeSubscription(subsId);
			if (destination != null) {
				this.destinationCache.updateAfterRemovedSubscription(sessionId, subsId);
			}
		}
	}

	@Override
	public void unregisterAllSubscriptions(String sessionId) {
		SessionSubscriptionInfo info = this.subscriptionRegistry.removeSubscriptions(sessionId);
		if (info != null) {
			this.destinationCache.updateAfterRemovedSession(info);
		}
	}

	@Override
	protected MultiValueMap<String, String> findSubscriptionsInternal(String destination, Message<?> message) {
		return this.destinationCache.getSubscriptions(destination, message);
	}

	@Override
	public String toString() {
		return "DefaultSubscriptionRegistry[" + this.destinationCache + ", " + this.subscriptionRegistry + "]";
	}


	/**
	 * A cache for destinations previously resolved via
	 * {@link DefaultSubscriptionRegistry#findSubscriptionsInternal(String, Message)}
	 */
	private class DestinationCache {

		/** Map from destination -> <sessionId, subscriptionId> for fast look-ups */
		private final Map<String, MultiValueMap<String, String>> accessCache =
				new ConcurrentHashMap<String, MultiValueMap<String, String>>(DEFAULT_CACHE_LIMIT);

		/** Map from destination -> <sessionId, subscriptionId> with locking */
		@SuppressWarnings("serial")
		private final Map<String, MultiValueMap<String, String>> updateCache =
				new LinkedHashMap<String, MultiValueMap<String, String>>(DEFAULT_CACHE_LIMIT, 0.75f, true) {
					@Override
					protected boolean removeEldestEntry(Map.Entry<String, MultiValueMap<String, String>> eldest) {
						if (size() > getCacheLimit()) {
							accessCache.remove(eldest.getKey());
							return true;
						}
						else {
							return false;
						}
					}
				};


		public MultiValueMap<String, String> getSubscriptions(String destination, Message<?> message) {
			MultiValueMap<String, String> result = this.accessCache.get(destination);
			if (result == null) {
				synchronized (this.updateCache) {
					result = new LinkedMultiValueMap<String, String>();
					for (SessionSubscriptionInfo info : subscriptionRegistry.getAllSubscriptions()) {
						for (String destinationPattern : info.getDestinations()) {
							if (getPathMatcher().match(destinationPattern, destination)) {
								for (String subscriptionId : info.getSubscriptions(destinationPattern)) {
									result.add(info.sessionId, subscriptionId);
								}
							}
						}
					}
					if (!result.isEmpty()) {
						this.updateCache.put(destination, deepCopy(result));
						this.accessCache.put(destination, result);
					}
				}
			}
			return result;
		}

		public void updateAfterNewSubscription(String destination, String sessionId, String subsId) {
			synchronized (this.updateCache) {
				for (Map.Entry<String, MultiValueMap<String, String>> entry : this.updateCache.entrySet()) {
					String cachedDestination = entry.getKey();
					if (getPathMatcher().match(destination, cachedDestination)) {
						MultiValueMap<String, String> subs = entry.getValue();
						subs.add(sessionId, subsId);
						this.accessCache.put(cachedDestination, deepCopy(subs));
					}
				}
			}
		}

		public void updateAfterRemovedSubscription(String sessionId, String subsId) {
			synchronized (this.updateCache) {
				Set<String> destinationsToRemove = new HashSet<String>();
				for (Map.Entry<String, MultiValueMap<String, String>> entry : this.updateCache.entrySet()) {
					String destination = entry.getKey();
					MultiValueMap<String, String> sessionMap = entry.getValue();
					List<String> subscriptions = sessionMap.get(sessionId);
					if (subscriptions != null) {
						subscriptions.remove(subsId);
						if (subscriptions.isEmpty()) {
							sessionMap.remove(sessionId);
						}
						if (sessionMap.isEmpty()) {
							destinationsToRemove.add(destination);
						}
						else {
							this.accessCache.put(destination, deepCopy(sessionMap));
						}
					}
				}
				for (String destination : destinationsToRemove) {
					this.updateCache.remove(destination);
					this.accessCache.remove(destination);
				}
			}
		}

		public void updateAfterRemovedSession(SessionSubscriptionInfo info) {
			synchronized (this.updateCache) {
				Set<String> destinationsToRemove = new HashSet<String>();
				for (Map.Entry<String, MultiValueMap<String, String>> entry : this.updateCache.entrySet()) {
					String destination = entry.getKey();
					MultiValueMap<String, String> sessionMap = entry.getValue();
					if (sessionMap.remove(info.getSessionId()) != null) {
						if (sessionMap.isEmpty()) {
							destinationsToRemove.add(destination);
						}
						else {
							this.accessCache.put(destination, deepCopy(sessionMap));
						}
					}
				}
				for (String destination : destinationsToRemove) {
					this.updateCache.remove(destination);
					this.accessCache.remove(destination);
				}
			}
		}

		private <K, V> LinkedMultiValueMap<K, V> deepCopy(Map<K, List<V>> map) {
			LinkedMultiValueMap<K, V> copy = new LinkedMultiValueMap<K, V>();
			for (Map.Entry<K, List<V>> entry : map.entrySet()) {
				copy.put(entry.getKey(), new LinkedList<V>(entry.getValue()));
			}
			return copy;
		}

		@Override
		public String toString() {
			return "cache[" + this.accessCache.size() + " destination(s)]";
		}
	}


	/**
	 * Provide access to session subscriptions by sessionId.
	 */
	private static class SessionSubscriptionRegistry {

		// sessionId -> SessionSubscriptionInfo
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
			return "registry[" + this.sessions.size() + " sessions]";
		}
	}


	/**
	 * Hold subscriptions for a session.
	 */
	private static class SessionSubscriptionInfo {

		private final String sessionId;

		// destination -> subscriptionIds
		private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<String, Set<String>>(4);

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
				synchronized (this.subscriptions) {
					subs = this.subscriptions.get(destination);
					if (subs == null) {
						subs = new CopyOnWriteArraySet<String>();
						this.subscriptions.put(destination, subs);
					}
				}
			}
			subs.add(subscriptionId);
		}

		public String removeSubscription(String subscriptionId) {
			for (String destination : this.subscriptions.keySet()) {
				Set<String> subscriptionIds = this.subscriptions.get(destination);
				if (subscriptionIds != null) {
					if (subscriptionIds.remove(subscriptionId)) {
						synchronized (this.subscriptions) {
							if (subscriptionIds.isEmpty()) {
								this.subscriptions.remove(destination);
							}
						}
						return destination;
					}
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
