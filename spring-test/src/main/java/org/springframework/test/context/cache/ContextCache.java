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

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;

/**
 * {@code ContextCache} defines the SPI for caching Spring
 * {@link ApplicationContext ApplicationContexts} within the
 * <em>Spring TestContext Framework</em>.
 *
 * <p>A {@code ContextCache} maintains a cache of {@code ApplicationContexts}
 * keyed by {@link MergedContextConfiguration} instances, potentially configured
 * with a {@linkplain ContextCacheUtils#retrieveMaxCacheSize maximum size} and
 * a custom eviction policy.
 *
 * <p>As of Spring Framework 6.1, this SPI includes optional support for
 * {@linkplain #getFailureCount(MergedContextConfiguration) tracking} and
 * {@linkplain #incrementFailureCount(MergedContextConfiguration) incrementing}
 * failure counts. As of Spring Framework 7.0, this SPI includes optional support for
 * {@linkplain #registerContextUsage(MergedContextConfiguration, Class) registering} and
 * {@linkplain #unregisterContextUsage(MergedContextConfiguration, Class) unregistering}
 * context usage.
 *
 * <h3>Rationale</h3>
 * <p>Context caching can have significant performance benefits if context
 * initialization is complex. Although the initialization of a Spring context
 * itself is typically very quick, some beans in a context &mdash; for example,
 * an embedded database or a {@code LocalContainerEntityManagerFactoryBean} for
 * working with JPA &mdash; may take several seconds to initialize. Hence, it
 * often makes sense to perform that initialization only once per test suite or
 * JVM process.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.2
 * @see ContextCacheUtils#retrieveMaxCacheSize()
 */
public interface ContextCache {

	/**
	 * The name of the logging category used for reporting {@code ContextCache}
	 * statistics.
	 */
	String CONTEXT_CACHE_LOGGING_CATEGORY = "org.springframework.test.context.cache";

	/**
	 * The default maximum size of the context cache: {@value}.
	 * @since 4.3
	 * @see #MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME
	 */
	int DEFAULT_MAX_CONTEXT_CACHE_SIZE = 32;

	/**
	 * System property used to configure the maximum size of the {@link ContextCache}
	 * as a positive integer: {@value}.
	 * <p>May alternatively be configured via the
	 * {@link org.springframework.core.SpringProperties} mechanism.
	 * <p>Note that implementations of {@code ContextCache} are not required to
	 * actually support a maximum cache size. Consult the documentation of the
	 * corresponding implementation for details.
	 * @since 4.3
	 * @see #DEFAULT_MAX_CONTEXT_CACHE_SIZE
	 */
	String MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME = "spring.test.context.cache.maxSize";


	/**
	 * Determine whether there is a cached context for the given key.
	 * @param key the context key; never {@code null}
	 * @return {@code true} if the cache contains a context with the given key
	 */
	boolean contains(MergedContextConfiguration key);

	/**
	 * Obtain a cached {@link ApplicationContext} for the given key.
	 * <p>If the cached application context was previously
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#pause() paused},
	 * it must be
	 * {@linkplain org.springframework.context.support.AbstractApplicationContext#restart()
	 * restarted}. This applies to parent contexts as well.
	 * <p>In addition, the {@linkplain #getHitCount() hit} and
	 * {@linkplain #getMissCount() miss} counts must be updated accordingly.
	 * @param key the context key; never {@code null}
	 * @return the corresponding {@code ApplicationContext} instance, or {@code null}
	 * if not found in the cache
	 * @see #put(MergedContextConfiguration, LoadFunction)
	 * @see #unregisterContextUsage(MergedContextConfiguration, Class)
	 * @see #remove(MergedContextConfiguration, HierarchyMode)
	 */
	@Nullable ApplicationContext get(MergedContextConfiguration key);

	/**
	 * Explicitly add an {@link ApplicationContext} to the cache under the given
	 * key, potentially honoring a custom eviction policy.
	 * @param key the context key; never {@code null}
	 * @param context the {@code ApplicationContext}; never {@code null}
	 * @see #get(MergedContextConfiguration)
	 * @see #put(MergedContextConfiguration, LoadFunction)
	 */
	void put(MergedContextConfiguration key, ApplicationContext context);

	/**
	 * Explicitly add an {@link ApplicationContext} to the cache under the given
	 * key, potentially honoring a custom eviction policy.
	 * <p>The supplied {@link LoadFunction} will be invoked to load the
	 * {@code ApplicationContext}.
	 * <p>Concrete implementations which honor a custom eviction policy must
	 * override this method to ensure that an evicted context is removed from the
	 * cache and closed before a new context is loaded via the supplied
	 * {@code LoadFunction}.
	 * @param key the context key; never {@code null}
	 * @param loadFunction a function which loads the context for the supplied key;
	 * never {@code null}
	 * @return the {@code ApplicationContext}; never {@code null}
	 * @since 7.0
	 * @see #get(MergedContextConfiguration)
	 * @see #put(MergedContextConfiguration, ApplicationContext)
	 */
	default ApplicationContext put(MergedContextConfiguration key, LoadFunction loadFunction) {
		Assert.notNull(key, "Key must not be null");
		Assert.notNull(loadFunction, "LoadFunction must not be null");

		ApplicationContext applicationContext = loadFunction.loadContext(key);
		Assert.state(applicationContext != null, "LoadFunction must return a non-null ApplicationContext");
		put(key, applicationContext);
		return applicationContext;
	}

	/**
	 * Remove the context with the given key from the cache and explicitly
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#close() close}
	 * it if it is an instance of {@code ConfigurableApplicationContext}.
	 * <p>Generally speaking, this method should be called to properly evict
	 * a context from the cache (for example, due to a custom eviction policy) or if
	 * the state of a singleton bean has been modified, potentially affecting
	 * future interaction with the context.
	 * <p>In addition, the semantics of the supplied {@code HierarchyMode} must
	 * be honored. See the Javadoc for {@link HierarchyMode} for details.
	 * @param key the context key; never {@code null}
	 * @param hierarchyMode the hierarchy mode; may be {@code null} if the context
	 * is not part of a hierarchy
	 */
	void remove(MergedContextConfiguration key, @Nullable HierarchyMode hierarchyMode);

	/**
	 * Get the failure count for the given key.
	 * <p>A <em>failure</em> is any attempt to load the {@link ApplicationContext}
	 * for the given key that results in an exception.
	 * <p>The default implementation of this method always returns {@code 0}.
	 * Concrete implementations are therefore highly encouraged to override this
	 * method and {@link #incrementFailureCount(MergedContextConfiguration)} with
	 * appropriate behavior. Note that the standard {@code ContextContext}
	 * implementation in Spring overrides these methods appropriately.
	 * @param key the context key; never {@code null}
	 * @since 6.1
	 * @see #incrementFailureCount(MergedContextConfiguration)
	 */
	default int getFailureCount(MergedContextConfiguration key) {
		return 0;
	}

	/**
	 * Increment the failure count for the given key.
	 * <p>The default implementation of this method does nothing. Concrete
	 * implementations are therefore highly encouraged to override this
	 * method and {@link #getFailureCount(MergedContextConfiguration)} with
	 * appropriate behavior. Note that the standard {@code ContextContext}
	 * implementation in Spring overrides these methods appropriately.
	 * @param key the context key; never {@code null}
	 * @since 6.1
	 * @see #getFailureCount(MergedContextConfiguration)
	 */
	default void incrementFailureCount(MergedContextConfiguration key) {
		/* no-op */
	}

	/**
	 * Register usage of the {@link ApplicationContext} for the supplied
	 * {@link MergedContextConfiguration} and any of its parents.
	 * <p>The default implementation of this method does nothing. Concrete
	 * implementations are therefore highly encouraged to override this
	 * method, {@link #unregisterContextUsage(MergedContextConfiguration, Class)},
	 * and {@link #getContextUsageCount()} with appropriate behavior. Note that
	 * the standard {@code ContextContext} implementation in Spring overrides
	 * these methods appropriately.
	 * @param key the context key; never {@code null}
	 * @param testClass the test class that is using the application context(s)
	 * @since 7.0
	 * @see #unregisterContextUsage(MergedContextConfiguration, Class)
	 * @see #getContextUsageCount()
	 */
	default void registerContextUsage(MergedContextConfiguration key, Class<?> testClass) {
		/* no-op */
	}

	/**
	 * Unregister usage of the {@link ApplicationContext} for the supplied
	 * {@link MergedContextConfiguration} and any of its parents.
	 * <p>If no other test classes are actively using the same application
	 * context(s), the application context(s) should be
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#pause() paused}.
	 * <p>The default implementation of this method does nothing. Concrete
	 * implementations are therefore highly encouraged to override this
	 * method, {@link #registerContextUsage(MergedContextConfiguration, Class)},
	 * and {@link #getContextUsageCount()} with appropriate behavior. Note that
	 * the standard {@code ContextContext} implementation in Spring overrides
	 * these methods appropriately.
	 * @param key the context key; never {@code null}
	 * @param testClass the test class that is no longer using the application context(s)
	 * @since 7.0
	 * @see #registerContextUsage(MergedContextConfiguration, Class)
	 * @see #getContextUsageCount()
	 */
	default void unregisterContextUsage(MergedContextConfiguration key, Class<?> testClass) {
		/* no-op */
	}

	/**
	 * Determine the number of contexts within the cache that are currently in use.
	 * <p>The default implementation of this method always returns {@code 0}.
	 * Concrete implementations are therefore highly encouraged to override this
	 * method, {@link #registerContextUsage(MergedContextConfiguration, Class)},
	 * and {@link #unregisterContextUsage(MergedContextConfiguration, Class)} with
	 * appropriate behavior. Note that the standard {@code ContextContext}
	 * implementation in Spring overrides these methods appropriately.
	 * @since 7.0
	 * @see #registerContextUsage(MergedContextConfiguration, Class)
	 * @see #unregisterContextUsage(MergedContextConfiguration, Class)
	 */
	default int getContextUsageCount() {
		return 0;
	}

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
	 * Clear {@linkplain #getHitCount() hit count} and {@linkplain #getMissCount()
	 * miss count} statistics for the cache (i.e., reset counters to zero).
	 */
	void clearStatistics();

	/**
	 * Log the statistics for this {@code ContextCache} at {@code DEBUG} level
	 * using the {@value #CONTEXT_CACHE_LOGGING_CATEGORY} logging category.
	 * <p>The following information should be logged.
	 * <ul>
	 * <li>name of the concrete {@code ContextCache} implementation</li>
	 * <li>{@linkplain #size}</li>
	 * <li>{@linkplain #getContextUsageCount() context usage count}</li>
	 * <li>{@linkplain #getParentContextCount() parent context count}</li>
	 * <li>{@linkplain #getHitCount() hit count}</li>
	 * <li>{@linkplain #getMissCount() miss count}</li>
	 * <li>any other information useful for monitoring the state of this cache</li>
	 * </ul>
	 */
	void logStatistics();


	/**
	 * Represents a function that loads an {@link ApplicationContext}.
	 *
	 * @since 7.0
	 */
	@FunctionalInterface
	interface LoadFunction {

		/**
		 * Load a new {@link ApplicationContext} based on the supplied
		 * {@link MergedContextConfiguration} and return the context in a fully
		 * <em>refreshed</em> state.
		 * @param mergedConfig the merged context configuration to use to load the
		 * application context
		 * @return a new application context; never {@code null}
		 * @see org.springframework.test.context.SmartContextLoader#loadContext(MergedContextConfiguration)
		 */
		ApplicationContext loadContext(MergedContextConfiguration mergedConfig);

	}

}
