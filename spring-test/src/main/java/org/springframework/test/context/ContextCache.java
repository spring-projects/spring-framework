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

package org.springframework.test.context;

import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;

/**
 * {@code ContextCache} defines the public API for caching Spring
 * {@link ApplicationContext ApplicationContexts} within the <em>Spring
 * TestContext Framework</em>.
 *
 * <p>A {@code ContextCache} maintains a cache of {@code ApplicationContexts}
 * keyed by {@link MergedContextConfiguration} instances.
 *
 * <h3>Rationale</h3>
 * <p>Context caching can have significant performance benefits if context
 * initialization is complex. So, although initializing a Spring context itself
 * is typically very quick, some beans in a context &mdash; for example, an
 * in-memory database or a {@code LocalSessionFactoryBean} for working with
 * Hibernate &mdash; may take several seconds to initialize. Hence it often
 * makes sense to perform that initialization only once per test suite.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.2
 */
public interface ContextCache {

	/**
	 * Determine whether there is a cached context for the given key.
	 * @param key the context key (never {@code null})
	 * @return {@code true} if the cache contains a context with the given key
	 */
	boolean contains(MergedContextConfiguration key);

	/**
	 * Obtain a cached {@code ApplicationContext} for the given key.
	 * <p>The {@link #getHitCount() hit} and {@link #getMissCount() miss} counts
	 * must be updated accordingly.
	 * @param key the context key (never {@code null})
	 * @return the corresponding {@code ApplicationContext} instance, or {@code null}
	 * if not found in the cache
	 * @see #remove
	 */
	ApplicationContext get(MergedContextConfiguration key);

	/**
	 * Explicitly add an {@code ApplicationContext} instance to the cache
	 * under the given key.
	 * @param key the context key (never {@code null})
	 * @param context the {@code ApplicationContext} instance (never {@code null})
	 */
	void put(MergedContextConfiguration key, ApplicationContext context);

	/**
	 * Remove the context with the given key from the cache and explicitly
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#close() close}
	 * it if it is an instance of {@code ConfigurableApplicationContext}.
	 * <p>Generally speaking, this method should be called if the state of
	 * a singleton bean has been modified, potentially affecting future
	 * interaction with the context.
	 * <p>In addition, the semantics of the supplied {@code HierarchyMode} must
	 * be honored. See the Javadoc for {@link HierarchyMode} for details.
	 * @param key the context key; never {@code null}
	 * @param hierarchyMode the hierarchy mode; may be {@code null} if the context
	 * is not part of a hierarchy
	 */
	void remove(MergedContextConfiguration key, HierarchyMode hierarchyMode);

	/**
	 * Determine the number of contexts currently stored in the cache.
	 * <p>If the cache contains more than {@code Integer.MAX_VALUE} elements,
	 * this method must return {@code Integer.MAX_VALUE}.
	 */
	int size();

	/**
	 * Determine the number of parent contexts currently tracked within the cache.
	 */
	int getParentContextCount();

	/**
	 * Get the overall hit count for this cache.
	 * <p>A <em>hit</em> is any access to the cache that returns a non-null
	 * context for the queried key.
	 */
	int getHitCount();

	/**
	 * Get the overall miss count for this cache.
	 * <p>A <em>miss</em> is any access to the cache that returns a {@code null}
	 * context for the queried key.
	 */
	int getMissCount();

	/**
	 * Reset all state maintained by this cache including statistics.
	 * @see #clear()
	 * @see #clearStatistics()
	 */
	void reset();

	/**
	 * Clear all contexts from the cache, clearing context hierarchy information as well.
	 */
	void clear();

	/**
	 * Clear hit and miss count statistics for the cache (i.e., reset counters to zero).
	 */
	void clearStatistics();

	/**
	 * Generate a text string containing the implementation type of this
	 * cache and its statistics.
	 * <p>The value returned by this method will be used primarily for
	 * logging purposes.
	 * <p>Specifically, the returned string should contain the name of the
	 * concrete {@code ContextCache} implementation, the {@linkplain #size},
	 * {@linkplain #getParentContextCount() parent context count},
	 * {@linkplain #getHitCount() hit count}, {@linkplain #getMissCount()
	 * miss count}, and any other information useful in monitoring the
	 * state of this cache.
	 * @return a string representation of this cache, including statistics
	 */
	@Override
	abstract String toString();

}
