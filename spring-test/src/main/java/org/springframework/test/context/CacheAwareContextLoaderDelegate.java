/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;

/**
 * A {@code CacheAwareContextLoaderDelegate} is responsible for {@linkplain
 * #loadContext loading} and {@linkplain #closeContext closing} application
 * contexts, interacting transparently with a
 * {@link org.springframework.test.context.cache.ContextCache ContextCache}
 * behind the scenes.
 *
 * <p>Note: {@code CacheAwareContextLoaderDelegate} does not extend the
 * {@link ContextLoader} or {@link SmartContextLoader} interface.
 *
 * @author Sam Brannen
 * @since 3.2.2
 */
public interface CacheAwareContextLoaderDelegate {

	/**
	 * The default failure threshold for errors encountered while attempting to
	 * load an application context: {@value}.
	 * @since 6.1
	 * @see #CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME
	 */
	int DEFAULT_CONTEXT_FAILURE_THRESHOLD = 1;

	/**
	 * System property used to configure the failure threshold for errors
	 * encountered while attempting to load an application context: {@value}.
	 * <p>May alternatively be configured via the
	 * {@link org.springframework.core.SpringProperties} mechanism.
	 * <p>Implementations of {@code CacheAwareContextLoaderDelegate} are not
	 * required to support this feature. Consult the documentation of the
	 * corresponding implementation for details. Note, however, that the standard
	 * {@code CacheAwareContextLoaderDelegate} implementation in Spring supports
	 * this feature.
	 * @since 6.1
	 * @see #DEFAULT_CONTEXT_FAILURE_THRESHOLD
	 * @see #loadContext(MergedContextConfiguration)
	 */
	String CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME = "spring.test.context.failure.threshold";


	/**
	 * Determine if the {@linkplain ApplicationContext application context} for
	 * the supplied {@link MergedContextConfiguration} has been loaded (i.e.,
	 * is present in the {@code ContextCache}).
	 * <p>Implementations of this method <strong>must not</strong> load the
	 * application context as a side effect. In addition, implementations of
	 * this method should not log the cache statistics via
	 * {@link org.springframework.test.context.cache.ContextCache#logStatistics()}.
	 * <p>The default implementation of this method always returns {@code false}.
	 * Custom {@code CacheAwareContextLoaderDelegate} implementations are
	 * therefore highly encouraged to override this method with a more meaningful
	 * implementation. Note that the standard {@code CacheAwareContextLoaderDelegate}
	 * implementation in Spring overrides this method appropriately.
	 * @param mergedConfig the merged context configuration used to load the
	 * application context; never {@code null}
	 * @return {@code true} if the application context has been loaded
	 * @since 5.2
	 * @see #loadContext
	 * @see #closeContext
	 */
	default boolean isContextLoaded(MergedContextConfiguration mergedConfig) {
		return false;
	}

	/**
	 * Load the {@linkplain ApplicationContext application context} for the supplied
	 * {@link MergedContextConfiguration} by delegating to the {@link ContextLoader}
	 * configured in the given {@code MergedContextConfiguration}.
	 * <p>If the context is present in the {@code ContextCache} it will simply
	 * be returned; otherwise, it will be loaded, stored in the cache, and returned.
	 * <p>As of Spring Framework 6.0, implementations of this method should load
	 * {@link ApplicationContextFailureProcessor} implementations via the
	 * {@link org.springframework.core.io.support.SpringFactoriesLoader SpringFactoriesLoader}
	 * mechanism, catch any exception thrown by the {@link ContextLoader}, and
	 * delegate to each of the configured failure processors to process the context
	 * load failure if the exception is an instance of {@link ContextLoadException}.
	 * <p>As of Spring Framework 6.1, implementations of this method are encouraged
	 * to support the <em>failure threshold</em> feature. Specifically, if repeated
	 * attempts are made to load an application context and that application
	 * context consistently fails to load &mdash; for example, due to a configuration
	 * error that prevents the context from successfully loading &mdash; this
	 * method should preemptively throw an {@link IllegalStateException} if the
	 * configured failure threshold has been exceeded. Note that the {@code ContextCache}
	 * provides support for tracking and incrementing the failure count for a given
	 * context cache key.
	 * <p>The cache statistics should be logged by invoking
	 * {@link org.springframework.test.context.cache.ContextCache#logStatistics()}.
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context; never {@code null}
	 * @return the application context (never {@code null})
	 * @throws IllegalStateException if an error occurs while retrieving or loading
	 * the application context
	 * @see #isContextLoaded
	 * @see #closeContext
	 * @see #CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME
	 */
	ApplicationContext loadContext(MergedContextConfiguration mergedConfig);

	/**
	 * Remove the {@linkplain ApplicationContext application context} for the
	 * supplied {@link MergedContextConfiguration} from the {@code ContextCache}
	 * and {@linkplain ConfigurableApplicationContext#close() close} it if it is
	 * an instance of {@link ConfigurableApplicationContext}.
	 * <p>The semantics of the supplied {@code HierarchyMode} must be honored when
	 * removing the context from the cache. See the Javadoc for {@link HierarchyMode}
	 * for details.
	 * <p>Generally speaking, this method should only be called if the state of
	 * a singleton bean has been changed (potentially affecting future interaction
	 * with the context) or if the context needs to be prematurely removed from
	 * the cache.
	 * @param mergedConfig the merged context configuration for the application
	 * context to close; never {@code null}
	 * @param hierarchyMode the hierarchy mode; may be {@code null} if the context
	 * is not part of a hierarchy
	 * @since 4.1
	 * @see #isContextLoaded
	 * @see #loadContext
	 */
	void closeContext(MergedContextConfiguration mergedConfig, @Nullable HierarchyMode hierarchyMode);

}
