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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link CacheAwareContextLoaderDelegate} interface.
 *
 * <p>Although {@code DefaultCacheAwareContextLoaderDelegate} was first introduced
 * in Spring Framework 4.1, the initial implementation of this class was extracted
 * from the existing code base for {@code CacheAwareContextLoaderDelegate} when
 * {@code CacheAwareContextLoaderDelegate} was converted into an interface.
 *
 * @author Sam Brannen
 * @since 4.1
 */
class DefaultCacheAwareContextLoaderDelegate implements CacheAwareContextLoaderDelegate {

	private static final Log logger = LogFactory.getLog(DefaultCacheAwareContextLoaderDelegate.class);

	private static final Log statsLogger = LogFactory.getLog("org.springframework.test.context.cache");

	private final ContextCache contextCache;


	DefaultCacheAwareContextLoaderDelegate(ContextCache contextCache) {
		Assert.notNull(contextCache, "ContextCache must not be null");
		this.contextCache = contextCache;
	}


	/**
	 * Load the {@code ApplicationContext} for the supplied merged context configuration.
	 * <p>Supports both the {@link SmartContextLoader} and {@link ContextLoader} SPIs.
	 * @throws Exception if an error occurs while loading the application context
	 */
	private ApplicationContext loadContextInternal(MergedContextConfiguration mergedContextConfiguration)
			throws Exception {

		ContextLoader contextLoader = mergedContextConfiguration.getContextLoader();
		Assert.notNull(contextLoader, "Cannot load an ApplicationContext with a NULL 'contextLoader'. " +
				"Consider annotating your test class with @ContextConfiguration or @ContextHierarchy.");

		ApplicationContext applicationContext;

		if (contextLoader instanceof SmartContextLoader) {
			SmartContextLoader smartContextLoader = (SmartContextLoader) contextLoader;
			applicationContext = smartContextLoader.loadContext(mergedContextConfiguration);
		}
		else {
			String[] locations = mergedContextConfiguration.getLocations();
			Assert.notNull(locations, "Cannot load an ApplicationContext with a NULL 'locations' array. " +
					"Consider annotating your test class with @ContextConfiguration or @ContextHierarchy.");
			applicationContext = contextLoader.loadContext(locations);
		}

		return applicationContext;
	}

	@Override
	public ApplicationContext loadContext(MergedContextConfiguration mergedContextConfiguration) {
		synchronized (this.contextCache) {
			ApplicationContext context = this.contextCache.get(mergedContextConfiguration);
			if (context == null) {
				try {
					context = loadContextInternal(mergedContextConfiguration);
					if (logger.isDebugEnabled()) {
						logger.debug(String.format("Storing ApplicationContext in cache under key [%s]",
								mergedContextConfiguration));
					}
					this.contextCache.put(mergedContextConfiguration, context);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Failed to load ApplicationContext", ex);
				}
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Retrieved ApplicationContext from cache with key [%s]",
							mergedContextConfiguration));
				}
			}

			if (statsLogger.isDebugEnabled()) {
				statsLogger.debug("Spring test ApplicationContext cache statistics: " + this.contextCache);
			}

			return context;
		}
	}

	@Override
	public void closeContext(MergedContextConfiguration mergedContextConfiguration, HierarchyMode hierarchyMode) {
		synchronized (this.contextCache) {
			this.contextCache.remove(mergedContextConfiguration, hierarchyMode);
		}
	}

}
