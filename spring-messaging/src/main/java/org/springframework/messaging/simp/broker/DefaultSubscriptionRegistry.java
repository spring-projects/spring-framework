/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.messaging.simp.broker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link SubscriptionRegistry} that stores subscriptions
 * in memory and uses a {@link org.springframework.util.PathMatcher PathMatcher}
 * for matching destinations.
 *
 * <p>As of 4.2, this class supports a {@link #setSelectorHeaderName selector}
 * header on subscription messages with Spring EL expressions evaluated against
 * the headers to filter out messages in addition to destination matching.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DefaultSubscriptionRegistry extends AbstractSubscriptionRegistry {

	/** Default maximum number of entries for the destination cache: 1024. */
	public static final int DEFAULT_CACHE_LIMIT = 1024;

	/** Static evaluation context to reuse. */
	private static final EvaluationContext messageEvalContext =
			SimpleEvaluationContext.forPropertyAccessors(new SimpMessageHeaderPropertyAccessor()).build();


	private PathMatcher pathMatcher = new AntPathMatcher();

	private int cacheLimit = DEFAULT_CACHE_LIMIT;

	@Nullable
	private String selectorHeaderName = "selector";

	private volatile boolean selectorHeaderInUse;

	private final ExpressionParser expressionParser = new SpelExpressionParser();

	private final DestinationCache destinationCache = new DestinationCache();

	private final SessionSubscriptionRegistry subscriptionRegistry = new SessionSubscriptionRegistry();


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

	/**
	 * Specify the maximum number of entries for the resolved destination cache.
	 * Default is 1024.
	 */
	public void setCacheLimit(int cacheLimit) {
		this.cacheLimit = cacheLimit;
		this.destinationCache.ensureCacheLimit();
	}

	/**
	 * Return the maximum number of entries for the resolved destination cache.
	 */
	public int getCacheLimit() {
		return this.cacheLimit;
	}

	/**
	 * Configure the name of a header that a subscription message can have for
	 * the purpose of filtering messages matched to the subscription. The header
	 * value is expected to be a Spring EL boolean expression to be applied to
	 * the headers of messages matched to the subscription.
	 * <p>For example:
	 * <pre>
	 * headers.foo == 'bar'
	 * </pre>
	 * <p>By default this is set to "selector". You can set it to a different
	 * name, or to {@code null} to turn off support for a selector header.
	 * @param selectorHeaderName the name to use for a selector header
	 * @since 4.2
	 */
	public void setSelectorHeaderName(@Nullable String selectorHeaderName) {
		this.selectorHeaderName = (StringUtils.hasText(selectorHeaderName) ? selectorHeaderName : null);
	}

	/**
	 * Return the name for the selector header name.
	 * @since 4.2
	 */
	@Nullable
	public String getSelectorHeaderName() {
		return this.selectorHeaderName;
	}

	@Override
	protected void addSubscriptionInternal(@NonNull String sessionId, @NonNull String subscriptionId,
											@NonNull String destination, @NonNull Message<?> message) {
		Expression expression = getSelectorExpression(message.getHeaders());
		boolean isAntPattern = this.pathMatcher.isPattern(destination);
		Subscription subscription = new Subscription(subscriptionId, expression, destination, isAntPattern);

		Subscription previousValue = this.subscriptionRegistry.addSubscription(sessionId, subscriptionId, subscription);
		if (previousValue == null) {
			this.destinationCache.updateAfterNewSubscription(destination, isAntPattern, sessionId, subscriptionId);
		}
	}

	@Nullable
	private Expression getSelectorExpression(MessageHeaders headers) {
		Expression expression = null;
		if (getSelectorHeaderName() != null) {
			String selector = SimpMessageHeaderAccessor.getFirstNativeHeader(getSelectorHeaderName(), headers);
			if (selector != null) {
				try {
					expression = this.expressionParser.parseExpression(selector);
					this.selectorHeaderInUse = true;
					if (logger.isTraceEnabled()) {
						logger.trace("Subscription selector: [" + selector + "]");
					}
				}
				catch (Throwable ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to parse selector: " + selector, ex);
					}
				}
			}
		}
		return expression;
	}

	@Override
	protected void removeSubscriptionInternal(String sessionId, String subsId, Message<?> message) {
		SessionSubscriptionInfo info = this.subscriptionRegistry.getSubscriptions(sessionId);
		if (info != null) {
			Subscription subscription = info.removeSubscription(subsId);
			if (subscription != null) {
				this.destinationCache.updateAfterRemovedSubscription(sessionId, subscription);
			}
		}
	}

	@Override
	public void unregisterAllSubscriptions(String sessionId) {
		SessionSubscriptionInfo info = this.subscriptionRegistry.removeSubscriptions(sessionId);
		if (info != null) {
			this.destinationCache.updateAfterRemovedSession(sessionId, info.getSubscriptions());
		}
	}

	@Override
	protected MultiValueMap<String, String> findSubscriptionsInternal(String destination, Message<?> message) {
		MultiValueMap<String, String> result = this.destinationCache.getSubscriptions(destination);
		return filterSubscriptions(result, message);
	}

	private MultiValueMap<String, String> filterSubscriptions(
			MultiValueMap<String, String> allMatches, Message<?> message) {

		if (!this.selectorHeaderInUse) {
			return allMatches;
		}
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>(allMatches.size());
		allMatches.forEach((sessionId, subscriptionsIds) -> {
			SessionSubscriptionInfo subscriptions = this.subscriptionRegistry.getSubscriptions(sessionId);
			if (subscriptions != null) {
				for (String subscriptionId : subscriptionsIds) {
					Subscription subscription = subscriptions.getSubscription(subscriptionId);
					if (subscription != null && evaluateExpression(subscription.getSelectorExpression(), message)) {
						result.add(sessionId, subscription.getId());
					}
				}
			}
		});

		return result;
	}

	private boolean evaluateExpression(@Nullable Expression expression, Message<?> message) {
		boolean result = false;
		try {
			if (expression == null || Boolean.TRUE.equals(expression.getValue(messageEvalContext, message, Boolean.class))) {
				result = true;
			}
		}
		catch (SpelEvaluationException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to evaluate selector: " + ex.getMessage());
			}
		}
		catch (Throwable ex) {
			logger.debug("Failed to evaluate selector", ex);
		}
		return result;
	}

	/**
	 * A cache for destinations previously resolved via
	 * {@link DefaultSubscriptionRegistry#findSubscriptionsInternal(String, Message)}.
	 */
	private final class DestinationCache {

		/** Map from destination to {@code <sessionId, subscriptionId>} for fast look-ups. */
		private final Map<String, LinkedMultiValueMap<String, String>> destinationCache =
				new ConcurrentHashMap<>(DEFAULT_CACHE_LIMIT);

		private final Queue<String> cacheEvictionPolicy = new ConcurrentLinkedQueue<>();

		private final AtomicInteger cacheSize = new AtomicInteger();

		public LinkedMultiValueMap<String, String> getSubscriptions(String destination) {
			LinkedMultiValueMap<String, String> subscriptions = this.destinationCache.get(destination);
			if (subscriptions == null) {
				subscriptions = this.destinationCache.computeIfAbsent(destination, dest -> {
					LinkedMultiValueMap<String, String> sessionSubscriptions = calculateSubscriptions(destination);
					this.cacheEvictionPolicy.add(destination);
					this.cacheSize.incrementAndGet();
					return sessionSubscriptions;
				});
				ensureCacheLimit();
			}
			return subscriptions;
		}

		@NonNull
		private LinkedMultiValueMap<String, String> calculateSubscriptions(String destination) {
			LinkedMultiValueMap<String, String> sessionsToSubscriptions = new LinkedMultiValueMap<>();

			DefaultSubscriptionRegistry.this.subscriptionRegistry.forEachSubscription((sessionId, subscriptionDetail) -> {
				if (subscriptionDetail.isAntPattern()) {
					if (pathMatcher.match(subscriptionDetail.getDestination(), destination)) {
						sessionsToSubscriptions.compute(sessionId, (s, subscriptions) ->
								addToList(subscriptionDetail.getId(), subscriptions));
					}
				}
				else if (destination.equals(subscriptionDetail.getDestination())) {
					sessionsToSubscriptions.compute(sessionId, (s, subscriptions) ->
							addToList(subscriptionDetail.getId(), subscriptions));
				}
			});
			return sessionsToSubscriptions;
		}

		@NonNull
		private List<String> addToList(String subscriptionId, @Nullable  List<String> subscriptions) {
			if (subscriptions == null) {
				return Collections.singletonList(subscriptionId);
			}
			else {
				List<String> newSubscriptions = new ArrayList<>(subscriptions.size() + 1);
				newSubscriptions.addAll(subscriptions);
				newSubscriptions.add(subscriptionId);
				return newSubscriptions;
			}
		}

		private void ensureCacheLimit() {
			int size = this.cacheSize.get();
			if (size > cacheLimit) {
				do {
					if (this.cacheSize.compareAndSet(size, size - 1)) {
						this.destinationCache.remove(this.cacheEvictionPolicy.poll());
					}
				} while ((size = this.cacheSize.get()) > cacheLimit);
			}
		}

		public void updateAfterNewSubscription(String destination, boolean isPattern, String sessionId, String subscriptionId) {
			if (isPattern) {
				for (String cachedDestination : this.destinationCache.keySet()) {
					if (pathMatcher.match(destination, cachedDestination)) {
						addToDestination(cachedDestination, sessionId, subscriptionId);
					}
				}
			}
			else {
				addToDestination(destination, sessionId, subscriptionId);
			}
		}

		private void addToDestination(String destination, String sessionId, String subscriptionId) {
			this.destinationCache.computeIfPresent(destination, (dest, sessionsToSubscriptions) -> {
				sessionsToSubscriptions = sessionsToSubscriptions.clone();
				sessionsToSubscriptions.compute(sessionId, (s, subscriptions) -> addToList(subscriptionId, subscriptions));
				return sessionsToSubscriptions;
			});
		}

		public void updateAfterRemovedSubscription(String sessionId, Subscription subscriptionDetail) {
			if (subscriptionDetail.isAntPattern()) {
				String patternDestination = subscriptionDetail.getDestination();
				for (String destination : this.destinationCache.keySet()) {
					if (pathMatcher.match(patternDestination, destination)) {
						removeInternal(destination, sessionId, subscriptionDetail.getId());
					}
				}
			}
			else {
				removeInternal(subscriptionDetail.getDestination(), sessionId, subscriptionDetail.getId());
			}
		}

		private void removeInternal(String destination, String sessionId, String subscription) {
			this.destinationCache.computeIfPresent(destination, (dest, subscriptions) -> {
				subscriptions = subscriptions.clone();
				subscriptions.computeIfPresent(sessionId, (session, subs) -> {
					/* it is very likely that one session has only one subscription per one destination */
					if (subs.size() == 1 && subscription.equals(subs.get(0))) {
						return null;
					}
					else {
						subs = new ArrayList<>(subs);
						subs.remove(subscription);
						return emptyListToNUll(subs);
					}
				});
				return subscriptions;
			});
		}

		@Nullable
		private <T> List<T> emptyListToNUll(@NonNull List<T> list) {
			return list.isEmpty() ? null : list;
		}

		public void updateAfterRemovedSession(String sessionId, Collection<Subscription> subscriptionDetails) {
			for (Subscription subscriptionDetail : subscriptionDetails) {
				updateAfterRemovedSubscription(sessionId, subscriptionDetail);
			}
		}
	}

	/**
	 * Provide access to session subscriptions by sessionId.
	 */
	private static final class SessionSubscriptionRegistry {

		// 'sessionId' -> 'subscriptionId' -> 'destination, selector expression'
		private final ConcurrentMap<String, SessionSubscriptionInfo> sessions = new ConcurrentHashMap<>();

		@Nullable
		public SessionSubscriptionInfo getSubscriptions(String sessionId) {
			return this.sessions.get(sessionId);
		}

		public void forEachSubscription(BiConsumer<String, Subscription> consumer) {
			this.sessions.forEach((sessionId, subscriptions) ->
				subscriptions.getSubscriptions().forEach(subscriptionDetail ->
					consumer.accept(sessionId, subscriptionDetail)));
		}

		@Nullable
		public Subscription addSubscription(String sessionId, String subscriptionId, Subscription subscriptionDetail) {
			SessionSubscriptionInfo subscriptions = this.sessions.computeIfAbsent(sessionId, s -> new SessionSubscriptionInfo());
			return subscriptions.addSubscription(subscriptionId, subscriptionDetail);
		}

		@Nullable
		public SessionSubscriptionInfo removeSubscriptions(String sessionId) {
			return this.sessions.remove(sessionId);
		}
	}

	/**
	 * Hold subscriptions for a session.
	 */
	private static final class SessionSubscriptionInfo {

		private final Map<String, Subscription> subscriptionLookup = new ConcurrentHashMap<>();

		public Collection<Subscription> getSubscriptions() {
			return this.subscriptionLookup.values();
		}

		@Nullable
		public Subscription getSubscription(String subscriptionId) {
			return this.subscriptionLookup.get(subscriptionId);
		}

		@Nullable
		public Subscription addSubscription(String subscriptionId, Subscription subscriptionDetail) {
			return this.subscriptionLookup.putIfAbsent(subscriptionId, subscriptionDetail);
		}

		@Nullable
		public Subscription removeSubscription(String subscriptionId) {
			return this.subscriptionLookup.remove(subscriptionId);
		}
	}

	private static final class Subscription {

		private final String id;

		@Nullable
		private final Expression selectorExpression;

		private final String destination;

		private final boolean isAntPattern;

		public Subscription(String id, @Nullable Expression selector, String destination, boolean isAntPattern) {
			Assert.notNull(id, "Subscription id must not be null");
			Assert.notNull(destination, "Subscription destination must not be null");
			this.id = id;
			this.selectorExpression = selector;
			this.destination = destination;
			this.isAntPattern = isAntPattern;
		}

		public String getId() {
			return this.id;
		}

		public String getDestination() {
			return this.destination;
		}

		public boolean isAntPattern() {
			return this.isAntPattern;
		}

		@Nullable
		public Expression getSelectorExpression() {
			return this.selectorExpression;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof Subscription && this.id.equals(((Subscription) other).id)));
		}

		@Override
		public int hashCode() {
			return this.id.hashCode();
		}

		@Override
		public String toString() {
			return "subscription(id=" + this.id + ")";
		}
	}


	private static class SimpMessageHeaderPropertyAccessor implements PropertyAccessor {

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return new Class<?>[] {Message.class, MessageHeaders.class};
		}

		@Override
		public boolean canRead(EvaluationContext context, @Nullable Object target, String name) {
			return true;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public TypedValue read(EvaluationContext context, @Nullable Object target, String name) {
			Object value;
			if (target instanceof Message) {
				value = name.equals("headers") ? ((Message) target).getHeaders() : null;
			}
			else if (target instanceof MessageHeaders) {
				MessageHeaders headers = (MessageHeaders) target;
				SimpMessageHeaderAccessor accessor =
						MessageHeaderAccessor.getAccessor(headers, SimpMessageHeaderAccessor.class);
				Assert.state(accessor != null, "No SimpMessageHeaderAccessor");
				if ("destination".equalsIgnoreCase(name)) {
					value = accessor.getDestination();
				}
				else {
					value = accessor.getFirstNativeHeader(name);
					if (value == null) {
						value = headers.get(name);
					}
				}
			}
			else {
				// Should never happen...
				throw new IllegalStateException("Expected Message or MessageHeaders.");
			}
			return new TypedValue(value);
		}

		@Override
		public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) {
			return false;
		}

		@Override
		public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object value) {
		}
	}

}
