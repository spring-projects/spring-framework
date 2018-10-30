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
	 * Load the {@linkplain ApplicationContext application context} for the supplied
	 * {@link MergedContextConfiguration} by delegating to the {@link ContextLoader}
	 * configured in the given {@code MergedContextConfiguration}.
	 * <p>If the context is present in the {@code ContextCache} it will simply
	 * be returned; otherwise, it will be loaded, stored in the cache, and returned.
	 * <p>The cache statistics should be logged by invoking
	 * {@link org.springframework.test.context.cache.ContextCache#logStatistics()}.
	 * @param mergedContextConfiguration the merged context configuration to use
	 * to load the application context; never {@code null}
	 * @return the application context
	 * @throws IllegalStateException if an error occurs while retrieving or loading
	 * the application context
	 */
	ApplicationContext loadContext(MergedContextConfiguration mergedContextConfiguration);

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
	 * @param mergedContextConfiguration the merged context configuration for the
	 * application context to close; never {@code null}
	 * @param hierarchyMode the hierarchy mode; may be {@code null} if the context
	 * is not part of a hierarchy
	 * @since 4.1
	 */
	void closeContext(MergedContextConfiguration mergedContextConfiguration, @Nullable HierarchyMode hierarchyMode);

}
