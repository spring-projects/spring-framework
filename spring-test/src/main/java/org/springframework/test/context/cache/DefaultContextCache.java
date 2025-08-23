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

package org.springframework.test.context.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link ContextCache} API.
 *
 * <p>Uses a synchronized {@link Map} configured with a maximum size
 * and a <em>least recently used</em> (LRU) eviction policy to cache
 * {@link ApplicationContext} instances.
 *
 * <p>The maximum size may be supplied as a {@linkplain #DefaultContextCache(int)
 * constructor argument} or set via a system property or Spring property named
 * {@value ContextCache#MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see ContextCacheUtils#retrieveMaxCacheSize()
 */
public class DefaultContextCache implements ContextCache {

	private static final Log statsLogger = LogFactory.getLog(CONTEXT_CACHE_LOGGING_CATEGORY);


	/**
	 * Map of context keys to Spring {@code ApplicationContext} instances.
	 */
	private final Map<MergedContextConfiguration, ApplicationContext> contextMap =
			Collections.synchronizedMap(new LruCache(32, 0.75f));

	/**
	 * Map of parent keys to sets of children keys, representing a top-down <em>tree</em>
	 * of context hierarchies. This information is used for determining which subtrees
	 * need to be recursively removed and closed when removing a context that is a parent
	 * of other contexts.
	 */
	private final Map<MergedContextConfiguration, Set<MergedContextConfiguration>> hierarchyMap =
			new ConcurrentHashMap<>(32);

	/**
	 * Map of context keys to active test classes (i.e., test classes that are actively
	 * using the corresponding {@link ApplicationContext}).
	 * @since 7.0
	 */
	private final Map<MergedContextConfiguration, Set<Class<?>>> contextUsageMap = new ConcurrentHashMap<>(32);

	/**
	 * Map of context keys to context load failure counts.
	 * @since 6.1
	 */
	private final Map<MergedContextConfiguration, Integer> failureCounts = new ConcurrentHashMap<>(32);

	private final AtomicInteger totalFailureCount = new AtomicInteger();

	private final int maxSize;

	private final AtomicInteger hitCount = new AtomicInteger();

	private final AtomicInteger missCount = new AtomicInteger();


	/**
	 * Create a new {@code DefaultContextCache} using the maximum cache size
	 * obtained via {@link ContextCacheUtils#retrieveMaxCacheSize()}.
	 * @since 4.3
	 * @see #DefaultContextCache(int)
	 * @see ContextCacheUtils#retrieveMaxCacheSize()
	 */
	public DefaultContextCache() {
		this(ContextCacheUtils.retrieveMaxCacheSize());
	}

	/**
	 * Create a new {@code DefaultContextCache} using the supplied maximum
	 * cache size.
	 * @param maxSize the maximum cache size
	 * @throws IllegalArgumentException if the supplied {@code maxSize} value
	 * is not positive
	 * @since 4.3
	 * @see #DefaultContextCache()
	 */
	public DefaultContextCache(int maxSize) {
		Assert.isTrue(maxSize > 0, "'maxSize' must be positive");
		this.maxSize = maxSize;
	}


	@Override
	public boolean contains(MergedContextConfiguration key) {
		Assert.notNull(key, "Key must not be null");
		return this.contextMap.containsKey(key);
	}

	@Override
	public @Nullable ApplicationContext get(MergedContextConfiguration key) {
		Assert.notNull(key, "Key must not be null");
		ApplicationContext context = this.contextMap.get(key);
		if (context == null) {
			this.missCount.incrementAndGet();
		}
		else {
			this.hitCount.incrementAndGet();
			restartContextIfNecessary(context);
		}
		return context;
	}

	private void restartContextIfNecessary(ApplicationContext context) {
		// Recurse up the context hierarchy first.
		ApplicationContext parent = context.getParent();
		if (parent != null) {
			restartContextIfNecessary(parent);
		}
		if (context instanceof ConfigurableApplicationContext cac && !cac.isRunning()) {
			cac.restart();
		}
	}

	@Override
	public void put(MergedContextConfiguration key, ApplicationContext context) {
		Assert.notNull(key, "Key must not be null");
		Assert.notNull(context, "ApplicationContext must not be null");

		this.contextMap.put(key, context);
		MergedContextConfiguration child = key;
		MergedContextConfiguration parent = child.getParent();
		while (parent != null) {
			Set<MergedContextConfiguration> list = this.hierarchyMap.computeIfAbsent(parent, k -> new HashSet<>());
			list.add(child);
			child = parent;
			parent = child.getParent();
		}
	}

	@Override
	public void registerContextUsage(MergedContextConfiguration mergedConfig, Class<?> testClass) {
		// Recurse up the context hierarchy first.
		MergedContextConfiguration parent = mergedConfig.getParent();
		if (parent != null) {
			registerContextUsage(parent, testClass);
		}
		getActiveTestClasses(mergedConfig).add(testClass);
	}

	@Override
	public void unregisterContextUsage(MergedContextConfiguration mergedConfig, Class<?> testClass) {
		ApplicationContext context = this.contextMap.get(mergedConfig);
		Assert.state(context != null, "ApplicationContext must not be null for: " + mergedConfig);

		Set<Class<?>> activeTestClasses = getActiveTestClasses(mergedConfig);
		activeTestClasses.remove(testClass);
		if (activeTestClasses.isEmpty()) {
			if (context instanceof ConfigurableApplicationContext cac && cac.isRunning()) {
				cac.pause();
			}
			this.contextUsageMap.remove(mergedConfig);
		}

		// Recurse up the context hierarchy last.
		MergedContextConfiguration parent = mergedConfig.getParent();
		if (parent != null) {
			unregisterContextUsage(parent, testClass);
		}
	}

	private Set<Class<?>> getActiveTestClasses(MergedContextConfiguration mergedConfig) {
		return this.contextUsageMap.computeIfAbsent(mergedConfig, mcc -> new HashSet<>());
	}

	@Override
	public void remove(MergedContextConfiguration key, @Nullable HierarchyMode hierarchyMode) {
		Assert.notNull(key, "Key must not be null");

		// startKey is the level at which to begin clearing the cache,
		// depending on the configured hierarchy mode.
		MergedContextConfiguration startKey = key;
		if (hierarchyMode == HierarchyMode.EXHAUSTIVE) {
			MergedContextConfiguration parent = startKey.getParent();
			while (parent != null) {
				startKey = parent;
				parent = startKey.getParent();
			}
		}

		List<MergedContextConfiguration> removedContexts = new ArrayList<>();
		remove(removedContexts, startKey);

		// Remove all remaining references to any removed contexts from the
		// hierarchy map.
		for (MergedContextConfiguration currentKey : removedContexts) {
			for (Set<MergedContextConfiguration> children : this.hierarchyMap.values()) {
				children.remove(currentKey);
			}
		}

		// Remove empty entries from the hierarchy map.
		for (Map.Entry<MergedContextConfiguration, Set<MergedContextConfiguration>> entry : this.hierarchyMap.entrySet()) {
			if (entry.getValue().isEmpty()) {
				this.hierarchyMap.remove(entry.getKey());
			}
		}
	}

	private void remove(List<MergedContextConfiguration> removedContexts, MergedContextConfiguration key) {
		Assert.notNull(key, "Key must not be null");

		Set<MergedContextConfiguration> children = this.hierarchyMap.get(key);
		if (children != null) {
			for (MergedContextConfiguration child : children) {
				// Recurse through lower levels
				remove(removedContexts, child);
			}
			// Remove the set of children for the current context from the hierarchy map.
			this.hierarchyMap.remove(key);
		}

		// Physically remove and close leaf nodes first (i.e., on the way back up the
		// stack as opposed to prior to the recursive call).
		ApplicationContext context = this.contextMap.remove(key);
		this.contextUsageMap.remove(key);
		if (context instanceof ConfigurableApplicationContext cac) {
			cac.close();
		}
		removedContexts.add(key);
	}

	@Override
	public int getFailureCount(MergedContextConfiguration key) {
		return this.failureCounts.getOrDefault(key, 0);
	}

	@Override
	public void incrementFailureCount(MergedContextConfiguration key) {
		this.totalFailureCount.incrementAndGet();
		this.failureCounts.merge(key, 1, Integer::sum);
	}

	@Override
	public int size() {
		return this.contextMap.size();
	}

	/**
	 * Get the maximum size of this cache.
	 */
	public int getMaxSize() {
		return this.maxSize;
	}

	@Override
	public int getContextUsageCount() {
		return this.contextUsageMap.size();
	}

	@Override
	public int getParentContextCount() {
		return this.hierarchyMap.size();
	}

	@Override
	public int getHitCount() {
		return this.hitCount.get();
	}

	@Override
	public int getMissCount() {
		return this.missCount.get();
	}

	@Override
	public void reset() {
		synchronized (this.contextMap) {
			clear();
			clearStatistics();
			this.totalFailureCount.set(0);
			this.failureCounts.clear();
		}
	}

	@Override
	public void clear() {
		synchronized (this.contextMap) {
			this.contextMap.clear();
			this.hierarchyMap.clear();
			this.contextUsageMap.clear();
		}
	}

	@Override
	public void clearStatistics() {
		synchronized (this.contextMap) {
			this.hitCount.set(0);
			this.missCount.set(0);
		}
	}

	@Override
	public void logStatistics() {
		if (statsLogger.isDebugEnabled()) {
			statsLogger.debug("Spring test ApplicationContext cache statistics: " + this);
		}
	}

	/**
	 * Generate a text string containing the implementation type of this
	 * cache and its statistics.
	 * <p>The string returned by this method contains all information
	 * required for compliance with the contract for {@link #logStatistics()}.
	 * @return a string representation of this cache, including statistics
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("size", size())
				.append("maxSize", getMaxSize())
				.append("contextUsageCount", getContextUsageCount())
				.append("parentContextCount", getParentContextCount())
				.append("hitCount", getHitCount())
				.append("missCount", getMissCount())
				.append("failureCount", this.totalFailureCount)
				.toString();
	}


	/**
	 * Simple cache implementation based on {@link LinkedHashMap} with a maximum
	 * size and a <em>least recently used</em> (LRU) eviction policy that
	 * properly closes application contexts.
	 * @since 4.3
	 */
	@SuppressWarnings("serial")
	private class LruCache extends LinkedHashMap<MergedContextConfiguration, ApplicationContext> {

		/**
		 * Create a new {@code LruCache} with the supplied initial capacity
		 * and load factor.
		 * @param initialCapacity the initial capacity
		 * @param loadFactor the load factor
		 */
		LruCache(int initialCapacity, float loadFactor) {
			super(initialCapacity, loadFactor, true);
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<MergedContextConfiguration, ApplicationContext> eldest) {
			if (this.size() > DefaultContextCache.this.getMaxSize()) {
				// Do NOT delete "DefaultContextCache.this."; otherwise, we accidentally
				// invoke java.util.Map.remove(Object, Object).
				DefaultContextCache.this.remove(eldest.getKey(), HierarchyMode.CURRENT_LEVEL);
			}

			// Return false since we invoke a custom eviction algorithm.
			return false;
		}
	}

}
