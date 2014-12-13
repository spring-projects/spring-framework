/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.util.Assert;

/**
 * Cache for Spring {@link ApplicationContext ApplicationContexts} in a test environment.
 *
 * <p>Maintains a cache of {@code ApplicationContexts} keyed by
 * {@link MergedContextConfiguration} instances.
 *
 * <p>This has significant performance benefits if initializing the context would take time.
 * While initializing a Spring context itself is very quick, some beans in a context, such
 * as a {@code LocalSessionFactoryBean} for working with Hibernate, may take some time to
 * initialize. Hence it often makes sense to perform that initialization only once per
 * test suite.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 */
class ContextCache {

	/**
	 * Map of context keys to Spring {@code ApplicationContext} instances.
	 */
	private final Map<MergedContextConfiguration, ApplicationContext> contextMap =
			new ConcurrentHashMap<MergedContextConfiguration, ApplicationContext>(64);

	/**
	 * Map of parent keys to sets of children keys, representing a top-down <em>tree</em>
	 * of context hierarchies. This information is used for determining which subtrees
	 * need to be recursively removed and closed when removing a context that is a parent
	 * of other contexts.
	 */
	private final Map<MergedContextConfiguration, Set<MergedContextConfiguration>> hierarchyMap =
			new ConcurrentHashMap<MergedContextConfiguration, Set<MergedContextConfiguration>>(64);

	private final AtomicInteger hitCount = new AtomicInteger();

	private final AtomicInteger missCount = new AtomicInteger();


	/**
	 * Clear all contexts from the cache and clear context hierarchy information as well.
	 */
	public void clear() {
		this.contextMap.clear();
		this.hierarchyMap.clear();
	}

	/**
	 * Clear hit and miss count statistics for the cache (i.e., reset counters to zero).
	 */
	public void clearStatistics() {
		this.hitCount.set(0);
		this.missCount.set(0);
	}

	/**
	 * Determine whether there is a cached context for the given key.
	 * @param key the context key (never {@code null})
	 * @return {@code true} if the cache contains a context with the given key
	 */
	public boolean contains(MergedContextConfiguration key) {
		Assert.notNull(key, "Key must not be null");
		return this.contextMap.containsKey(key);
	}

	/**
	 * Obtain a cached {@code ApplicationContext} for the given key.
	 * <p>The {@link #getHitCount() hit} and {@link #getMissCount() miss} counts will
	 * be updated accordingly.
	 * @param key the context key (never {@code null})
	 * @return the corresponding {@code ApplicationContext} instance, or {@code null}
	 * if not found in the cache
	 * @see #remove
	 */
	public ApplicationContext get(MergedContextConfiguration key) {
		Assert.notNull(key, "Key must not be null");
		ApplicationContext context = this.contextMap.get(key);
		if (context == null) {
			this.missCount.incrementAndGet();
		}
		else {
			this.hitCount.incrementAndGet();
		}
		return context;
	}

	/**
	 * Get the overall hit count for this cache.
	 * <p>A <em>hit</em> is an access to the cache, which returned a non-null context
	 * for a queried key.
	 */
	public int getHitCount() {
		return this.hitCount.get();
	}

	/**
	 * Get the overall miss count for this cache.
	 * <p>A <em>miss</em> is an access to the cache, which returned a {@code null} context
	 * for a queried key.
	 */
	public int getMissCount() {
		return this.missCount.get();
	}

	/**
	 * Explicitly add an {@code ApplicationContext} instance to the cache under the given key.
	 * @param key the context key (never {@code null})
	 * @param context the {@code ApplicationContext} instance (never {@code null})
	 */
	public void put(MergedContextConfiguration key, ApplicationContext context) {
		Assert.notNull(key, "Key must not be null");
		Assert.notNull(context, "ApplicationContext must not be null");

		this.contextMap.put(key, context);
		MergedContextConfiguration child = key;
		MergedContextConfiguration parent = child.getParent();
		while (parent != null) {
			Set<MergedContextConfiguration> list = this.hierarchyMap.get(parent);
			if (list == null) {
				list = new HashSet<MergedContextConfiguration>();
				this.hierarchyMap.put(parent, list);
			}
			list.add(child);
			child = parent;
			parent = child.getParent();
		}
	}

	/**
	 * Remove the context with the given key from the cache and explicitly
	 * {@linkplain ConfigurableApplicationContext#close() close} it if it is an
	 * instance of {@link ConfigurableApplicationContext}.
	 * <p>Generally speaking, you would only call this method if you change the
	 * state of a singleton bean, potentially affecting future interaction with
	 * the context.
	 * <p>In addition, the semantics of the supplied {@code HierarchyMode} will
	 * be honored. See the Javadoc for {@link HierarchyMode} for details.
	 * @param key the context key; never {@code null}
	 * @param hierarchyMode the hierarchy mode; may be {@code null} if the context
	 * is not part of a hierarchy
	 */
	public void remove(MergedContextConfiguration key, HierarchyMode hierarchyMode) {
		Assert.notNull(key, "Key must not be null");

		// startKey is the level at which to begin clearing the cache, depending
		// on the configured hierarchy mode.
		MergedContextConfiguration startKey = key;
		if (hierarchyMode == HierarchyMode.EXHAUSTIVE) {
			while (startKey.getParent() != null) {
				startKey = startKey.getParent();
			}
		}

		List<MergedContextConfiguration> removedContexts = new ArrayList<MergedContextConfiguration>();
		remove(removedContexts, startKey);

		// Remove all remaining references to any removed contexts from the
		// hierarchy map.
		for (MergedContextConfiguration currentKey : removedContexts) {
			for (Set<MergedContextConfiguration> children : this.hierarchyMap.values()) {
				children.remove(currentKey);
			}
		}

		// Remove empty entries from the hierarchy map.
		for (MergedContextConfiguration currentKey : this.hierarchyMap.keySet()) {
			if (this.hierarchyMap.get(currentKey).isEmpty()) {
				this.hierarchyMap.remove(currentKey);
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
		if (context instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) context).close();
		}
		removedContexts.add(key);
	}

	/**
	 * Determine the number of contexts currently stored in the cache.
	 * <p>If the cache contains more than {@code Integer.MAX_VALUE} elements,
	 * this method returns {@code Integer.MAX_VALUE}.
	 */
	public int size() {
		return this.contextMap.size();
	}

	/**
	 * Determine the number of parent contexts currently tracked within the cache.
	 */
	public int getParentContextCount() {
		return this.hierarchyMap.size();
	}

	/**
	 * Generate a text string, which contains the {@linkplain #size} as well
	 * as the {@linkplain #getHitCount() hit}, {@linkplain #getMissCount() miss},
	 * and {@linkplain #getParentContextCount() parent context} counts.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("size", size())
				.append("hitCount", getHitCount())
				.append("missCount", getMissCount())
				.append("parentContextCount", getParentContextCount())
				.toString();
	}

}
