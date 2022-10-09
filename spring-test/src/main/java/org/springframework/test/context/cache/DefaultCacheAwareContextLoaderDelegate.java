/*
 * Copyright 2002-2022 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.log.LogMessage;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.aot.AotContextLoader;
import org.springframework.test.context.aot.AotTestContextInitializers;
import org.springframework.test.context.aot.TestContextAotException;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link CacheAwareContextLoaderDelegate} interface.
 *
 * <p>To use a static {@link DefaultContextCache}, invoke the
 * {@link #DefaultCacheAwareContextLoaderDelegate()} constructor; otherwise,
 * invoke the {@link #DefaultCacheAwareContextLoaderDelegate(ContextCache)}
 * and provide a custom {@link ContextCache} implementation.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class DefaultCacheAwareContextLoaderDelegate implements CacheAwareContextLoaderDelegate {

	private static final Log logger = LogFactory.getLog(DefaultCacheAwareContextLoaderDelegate.class);

	/**
	 * Default static cache of Spring application contexts.
	 */
	static final ContextCache defaultContextCache = new DefaultContextCache();

	private final AotTestContextInitializers aotTestContextInitializers = new AotTestContextInitializers();

	private final ContextCache contextCache;


	/**
	 * Construct a new {@code DefaultCacheAwareContextLoaderDelegate} using
	 * a static {@link DefaultContextCache}.
	 * <p>This default cache is static so that each context can be cached
	 * and reused for all subsequent tests that declare the same unique
	 * context configuration within the same JVM process.
	 * @see #DefaultCacheAwareContextLoaderDelegate(ContextCache)
	 */
	public DefaultCacheAwareContextLoaderDelegate() {
		this(defaultContextCache);
	}

	/**
	 * Construct a new {@code DefaultCacheAwareContextLoaderDelegate} using
	 * the supplied {@link ContextCache}.
	 * @see #DefaultCacheAwareContextLoaderDelegate()
	 */
	public DefaultCacheAwareContextLoaderDelegate(ContextCache contextCache) {
		Assert.notNull(contextCache, "ContextCache must not be null");
		this.contextCache = contextCache;
	}


	@Override
	public boolean isContextLoaded(MergedContextConfiguration mergedContextConfiguration) {
		synchronized (this.contextCache) {
			return this.contextCache.contains(replaceIfNecessary(mergedContextConfiguration));
		}
	}

	@Override
	public ApplicationContext loadContext(MergedContextConfiguration mergedContextConfiguration) {
		mergedContextConfiguration = replaceIfNecessary(mergedContextConfiguration);
		synchronized (this.contextCache) {
			ApplicationContext context = this.contextCache.get(mergedContextConfiguration);
			if (context == null) {
				try {
					if (mergedContextConfiguration instanceof AotMergedContextConfiguration aotMergedConfig) {
						context = loadContextInAotMode(aotMergedConfig);
					}
					else {
						context = loadContextInternal(mergedContextConfiguration);
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Storing ApplicationContext [%s] in cache under key %s".formatted(
								System.identityHashCode(context), mergedContextConfiguration));
					}
					this.contextCache.put(mergedContextConfiguration, context);
				}
				catch (Exception ex) {
					throw new IllegalStateException(
						"Failed to load ApplicationContext for " + mergedContextConfiguration, ex);
				}
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Retrieved ApplicationContext [%s] from cache with key %s".formatted(
							System.identityHashCode(context), mergedContextConfiguration));
				}
			}

			this.contextCache.logStatistics();

			return context;
		}
	}

	@Override
	public void closeContext(MergedContextConfiguration mergedContextConfiguration, @Nullable HierarchyMode hierarchyMode) {
		synchronized (this.contextCache) {
			this.contextCache.remove(replaceIfNecessary(mergedContextConfiguration), hierarchyMode);
		}
	}

	/**
	 * Get the {@link ContextCache} used by this context loader delegate.
	 */
	protected ContextCache getContextCache() {
		return this.contextCache;
	}

	/**
	 * Load the {@code ApplicationContext} for the supplied merged context configuration.
	 * <p>Supports both the {@link SmartContextLoader} and {@link ContextLoader} SPIs.
	 * @throws Exception if an error occurs while loading the application context
	 */
	@SuppressWarnings("deprecation")
	protected ApplicationContext loadContextInternal(MergedContextConfiguration mergedContextConfiguration)
			throws Exception {

		ContextLoader contextLoader = getContextLoader(mergedContextConfiguration);
		if (contextLoader instanceof SmartContextLoader smartContextLoader) {
			return smartContextLoader.loadContext(mergedContextConfiguration);
		}
		else {
			String[] locations = mergedContextConfiguration.getLocations();
			Assert.notNull(locations, """
					Cannot load an ApplicationContext with a NULL 'locations' array. \
					Consider annotating test class [%s] with @ContextConfiguration or \
					@ContextHierarchy.""".formatted(mergedContextConfiguration.getTestClass().getName()));
			return contextLoader.loadContext(locations);
		}
	}

	protected ApplicationContext loadContextInAotMode(AotMergedContextConfiguration aotMergedConfig) throws Exception {
		Class<?> testClass = aotMergedConfig.getTestClass();
		ApplicationContextInitializer<ConfigurableApplicationContext> contextInitializer =
				this.aotTestContextInitializers.getContextInitializer(testClass);
		Assert.state(contextInitializer != null,
				() -> "Failed to load AOT ApplicationContextInitializer for test class [%s]"
						.formatted(testClass.getName()));
		ContextLoader contextLoader = getContextLoader(aotMergedConfig);
		logger.info(LogMessage.format("Loading ApplicationContext in AOT mode for %s", aotMergedConfig.getOriginal()));
		if (!((contextLoader instanceof AotContextLoader aotContextLoader) &&
				(aotContextLoader.loadContextForAotRuntime(aotMergedConfig.getOriginal(), contextInitializer)
						instanceof GenericApplicationContext gac))) {
			throw new TestContextAotException("""
					Cannot load ApplicationContext for AOT runtime for %s. The configured \
					ContextLoader [%s] must be an AotContextLoader and must create a \
					GenericApplicationContext."""
						.formatted(aotMergedConfig.getOriginal(), contextLoader.getClass().getName()));
		}
		gac.registerShutdownHook();
		return gac;
	}

	private ContextLoader getContextLoader(MergedContextConfiguration mergedConfig) {
		ContextLoader contextLoader = mergedConfig.getContextLoader();
		Assert.notNull(contextLoader, """
				Cannot load an ApplicationContext with a NULL 'contextLoader'. \
				Consider annotating test class [%s] with @ContextConfiguration or \
				@ContextHierarchy.""".formatted(mergedConfig.getTestClass().getName()));
		return contextLoader;
	}

	/**
	 * If the test class associated with the supplied {@link MergedContextConfiguration}
	 * has an AOT-optimized {@link ApplicationContext}, this method will create an
	 * {@link AotMergedContextConfiguration} to replace the provided {@code MergedContextConfiguration}.
	 * <p>Otherwise, this method simply returns the supplied {@code MergedContextConfiguration}
	 * unmodified.
	 * <p>This allows for transparent {@link org.springframework.test.context.cache.ContextCache ContextCache}
	 * support for AOT-optimized application contexts.
	 */
	@SuppressWarnings("unchecked")
	private MergedContextConfiguration replaceIfNecessary(MergedContextConfiguration mergedConfig) {
		Class<?> testClass = mergedConfig.getTestClass();
		if (this.aotTestContextInitializers.isSupportedTestClass(testClass)) {
			Class<? extends ApplicationContextInitializer<?>> contextInitializerClass =
					(Class<? extends ApplicationContextInitializer<?>>)
							this.aotTestContextInitializers.getContextInitializer(testClass).getClass();
			return new AotMergedContextConfiguration(testClass, contextInitializerClass, mergedConfig, this);
		}
		return mergedConfig;
	}

}
