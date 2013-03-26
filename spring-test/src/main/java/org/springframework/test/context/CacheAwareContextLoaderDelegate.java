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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * {@code CacheAwareContextLoaderDelegate} loads application contexts from
 * {@link MergedContextConfiguration} by delegating to the
 * {@link ContextLoader} configured in the {@code MergedContextConfiguration}
 * and interacting transparently with the {@link ContextCache} behind the scenes.
 *
 * <p>Note: {@code CacheAwareContextLoaderDelegate} does not implement the
 * {@link ContextLoader} or {@link SmartContextLoader} interface.
 *
 * @author Sam Brannen
 * @since 3.2.2
 */
public class CacheAwareContextLoaderDelegate {

	private static final Log logger = LogFactory.getLog(CacheAwareContextLoaderDelegate.class);

	private final ContextCache contextCache;


	CacheAwareContextLoaderDelegate(ContextCache contextCache) {
		Assert.notNull(contextCache, "ContextCache must not be null");
		this.contextCache = contextCache;
	}

	/**
	 * Load the {@code ApplicationContext} for the supplied merged context
	 * configuration. Supports both the {@link SmartContextLoader} and
	 * {@link ContextLoader} SPIs.
	 * @throws Exception if an error occurs while loading the application context
	 */
	private ApplicationContext loadContextInternal(MergedContextConfiguration mergedContextConfiguration)
			throws Exception {
		ContextLoader contextLoader = mergedContextConfiguration.getContextLoader();
		Assert.notNull(contextLoader, "Cannot load an ApplicationContext with a NULL 'contextLoader'. "
				+ "Consider annotating your test class with @ContextConfiguration or @ContextHierarchy.");

		ApplicationContext applicationContext;

		if (contextLoader instanceof SmartContextLoader) {
			SmartContextLoader smartContextLoader = (SmartContextLoader) contextLoader;
			applicationContext = smartContextLoader.loadContext(mergedContextConfiguration);
		}
		else {
			String[] locations = mergedContextConfiguration.getLocations();
			Assert.notNull(locations, "Cannot load an ApplicationContext with a NULL 'locations' array. "
					+ "Consider annotating your test class with @ContextConfiguration or @ContextHierarchy.");
			applicationContext = contextLoader.loadContext(locations);
		}

		return applicationContext;
	}

	/**
	 * Load the {@link ApplicationContext application context} for the supplied
	 * merged context configuration.
	 *
	 * <p>If the context is present in the cache it will simply be returned;
	 * otherwise, it will be loaded, stored in the cache, and returned.
	 * @return the application context
	 * @throws IllegalStateException if an error occurs while retrieving or
	 * loading the application context
	 */
	public ApplicationContext loadContext(MergedContextConfiguration mergedContextConfiguration) {
		synchronized (contextCache) {
			ApplicationContext context = contextCache.get(mergedContextConfiguration);
			if (context == null) {
				try {
					context = loadContextInternal(mergedContextConfiguration);
					if (logger.isDebugEnabled()) {
						logger.debug(String.format("Storing ApplicationContext in cache under key [%s].",
							mergedContextConfiguration));
					}
					contextCache.put(mergedContextConfiguration, context);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Failed to load ApplicationContext", ex);
				}
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Retrieved ApplicationContext from cache with key [%s].",
						mergedContextConfiguration));
				}
			}
			return context;
		}
	}

}
