/*
 * Copyright 2002-2013 the original author or authors.
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

	private final Object monitor = new Object();

	/**
	 * Map of context keys to Spring {@code ApplicationContext} instances.
	 */
	private final Map<MergedContextConfiguration, ApplicationContext> contextMap = new ConcurrentHashMap<MergedContextConfiguration, ApplicationContext>(
		64);

	/**
	 * Map of parent keys to sets of children keys, representing a top-down <em>tree</em>
	 * of context hierarchies. This information is used for determining which subtrees
	 * need to be recursively removed and closed when removing a context that is a parent
	 * of other contexts.
	 */
	private final Map<MergedContextConfiguration, Set<MergedContextConfiguration>> hierarchyMap = new ConcurrentHashMap<MergedContextConfiguration, Set<MergedContextConfiguration>>(
		64);

	private int hitCount;

	private int missCount;


	/**
	 * Clears all contexts from the cache and clears context hierarchy information as
	 * well.
	 */
	void clear() {
		synchronized (monitor) {
			this.contextMap.clear();
			this.hierarchyMap.clear();
		}
	}

	/**
	 * Clears hit and miss count statistics for the cache (i.e., resets counters to zero).
	 */
	void clearStatistics() {
		this.hitCount = 0;
		this.missCount = 0;
	}

	/**
	 * Return whether there is a cached context for the given key.
	 *
	 * @param key the context key (never {@code null})
	 */
	boolean contains(MergedContextConfiguration key) {
		Assert.notNull(key, "Key must not be null");
		synchronized (monitor) {
			return this.contextMap.containsKey(key);
		}
	}

	/**
	 * Obtain a cached {@code ApplicationContext} for the given key.
	 *
	 * <p>The {@link #getHitCount() hit} and {@link #getMissCount() miss} counts will be
	 * updated accordingly.
	 *
	 * @param key the context key (never {@code null})
	 * @return the corresponding {@code ApplicationContext} instance, or {@code null} if
	 * not found in the cache.
	 * @see #remove
	 */
	ApplicationContext get(MergedContextConfiguration key) {
		Assert.notNull(key, "Key must not be null");
		synchronized (monitor) {
			ApplicationContext context = this.contextMap.get(key);
			if (context == null) {
				incrementMissCount();
			}
			else {
				incrementHitCount();
			}
			return context;
		}
	}

	/**
	 * Increment the hit count by one. A <em>hit</em> is an access to the cache, which
	 * returned a non-null context for a queried key.
	 */
	private void incrementHitCount() {
		this.hitCount++;
	}

	/**
	 * Increment the miss count by one. A <em>miss</em> is an access to the cache, which
	 * returned a {@code null} context for a queried key.
	 */
	private void incrementMissCount() {
		this.missCount++;
	}

	/**
	 * Get the overall hit count for this cache. A <em>hit</em> is an access to the cache,
	 * which returned a non-null context for a queried key.
	 */
	int getHitCount() {
		return this.hitCount;
	}

	/**
	 * Get the overall miss count for this cache. A <em>miss</em> is an access to the
	 * cache, which returned a {@code null} context for a queried key.
	 */
	int getMissCount() {
		return this.missCount;
	}

	/**
	 * Explicitly add an {@code ApplicationContext} instance to the cache under the given
	 * key.
	 *
	 * @param key the context key (never {@code null})
	 * @param context the {@code ApplicationContext} instance (never {@code null})
	 */
	void put(MergedContextConfiguration key, ApplicationContext context) {
		Assert.notNull(key, "Key must not be null");
		Assert.notNull(context, "ApplicationContext must not be null");

		synchronized (monitor) {
			this.contextMap.put(key, context);

			MergedContextConfiguration child = key;
			MergedContextConfiguration parent = child.getParent();
			while (parent != null) {
				Set<MergedContextConfiguration> list = hierarchyMap.get(parent);
				if (list == null) {
					list = new HashSet<MergedContextConfiguration>();
					hierarchyMap.put(parent, list);
				}
				list.add(child);
				child = parent;
				parent = child.getParent();
			}
		}
	}

	/**
	 * Remove the context with the given key from the cache and explicitly
	 * {@linkplain ConfigurableApplicationContext#close() close} it if it is an
	 * instance of {@link ConfigurableApplicationContext}.
	 *
	 * <p>Generally speaking, you would only call this method if you change the
	 * state of a singleton bean, potentially affecting future interaction with
	 * the context.
	 *
	 * <p>In addition, the semantics of the supplied {@code HierarchyMode} will
	 * be honored. See the Javadoc for {@link HierarchyMode} for details.
	 *
	 * @param key the context key; never {@code null}
	 * @param hierarchyMode the hierarchy mode; may be {@code null} if the context
	 * is not part of a hierarchy
	 */
	void remove(MergedContextConfiguration key, HierarchyMode hierarchyMode) {
		Assert.notNull(key, "Key must not be null");

		// startKey is the level at which to begin clearing the cache, depending
		// on the configured hierarchy mode.
		MergedContextConfiguration startKey = key;
		if (hierarchyMode == HierarchyMode.EXHAUSTIVE) {
			while (startKey.getParent() != null) {
				startKey = startKey.getParent();
			}
		}

		synchronized (monitor) {
			final List<MergedContextConfiguration> removedContexts = new ArrayList<MergedContextConfiguration>();

			remove(removedContexts, startKey);

			// Remove all remaining references to any removed contexts from the
			// hierarchy map.
			for (MergedContextConfiguration currentKey : removedContexts) {
				for (Set<MergedContextConfiguration> children : hierarchyMap.values()) {
					children.remove(currentKey);
				}
			}

			// Remove empty entries from the hierarchy map.
			for (MergedContextConfiguration currentKey : hierarchyMap.keySet()) {
				if (hierarchyMap.get(currentKey).isEmpty()) {
					hierarchyMap.remove(currentKey);
				}
			}
		}
	}

	private void remove(List<MergedContextConfiguration> removedContexts, MergedContextConfiguration key) {
		Assert.notNull(key, "Key must not be null");

		synchronized (monitor) {
			Set<MergedContextConfiguration> children = hierarchyMap.get(key);
			if (children != null) {
				for (MergedContextConfiguration child : children) {
					// Recurse through lower levels
					remove(removedContexts, child);
				}
				// Remove the set of children for the current context from the
				// hierarchy map.
				hierarchyMap.remove(key);
			}

			// Physically remove and close leaf nodes first (i.e., on the way back up the
			// stack as opposed to prior to the recursive call).
			ApplicationContext context = contextMap.remove(key);
			if (context instanceof ConfigurableApplicationContext) {
				((ConfigurableApplicationContext) context).close();
			}
			removedContexts.add(key);
		}
	}

	/**
	 * Determine the number of contexts currently stored in the cache. If the cache
	 * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
	 * <tt>Integer.MAX_VALUE</tt>.
	 */
	int size() {
		synchronized (monitor) {
			return this.contextMap.size();
		}
	}

	/**
	 * Determine the number of parent contexts currently tracked within the cache.
	 */
	int getParentContextCount() {
		synchronized (monitor) {
			return this.hierarchyMap.size();
		}
	}

	/**
	 * Generates a text string, which contains the {@linkplain #size() size} as well
	 * as the {@linkplain #getHitCount() hit}, {@linkplain #getMissCount() miss}, and
	 * {@linkplain #getParentContextCount() parent context} counts.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)//
		.append("size", size())//
		.append("hitCount", getHitCount())//
		.append("missCount", getMissCount())//
		.append("parentContextCount", getParentContextCount())//
		.toString();
	}

}
