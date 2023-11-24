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

package org.springframework.test.context.cache;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.AotDetector;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ApplicationContextFailureProcessor;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextLoadException;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.aot.AotContextLoader;
import org.springframework.test.context.aot.AotTestContextInitializers;
import org.springframework.test.context.aot.TestContextAotException;
import org.springframework.test.context.util.TestContextSpringFactoriesUtils;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link CacheAwareContextLoaderDelegate} strategy.
 *
 * <p>To use a static {@link DefaultContextCache}, invoke the
 * {@link #DefaultCacheAwareContextLoaderDelegate()} constructor; otherwise,
 * invoke the {@link #DefaultCacheAwareContextLoaderDelegate(ContextCache)}
 * and provide a custom {@link ContextCache} implementation.
 *
 * <p>As of Spring Framework 6.0, this class loads {@link ApplicationContextFailureProcessor}
 * implementations via the {@link org.springframework.core.io.support.SpringFactoriesLoader
 * SpringFactoriesLoader} mechanism and delegates to them in
 * {@link #loadContext(MergedContextConfiguration)} to process context load failures.
 *
 * <p>As of Spring Framework 6.1, this class supports the <em>failure threshold</em>
 * feature described in {@link CacheAwareContextLoaderDelegate#loadContext},
 * delegating to {@link ContextCacheUtils#retrieveContextFailureThreshold()} to
 * obtain the threshold value to use.
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


	private final List<ApplicationContextFailureProcessor> contextFailureProcessors = TestContextSpringFactoriesUtils
			.loadFactoryImplementations(ApplicationContextFailureProcessor.class);

	private final AotTestContextInitializers aotTestContextInitializers = new AotTestContextInitializers();

	private final ContextCache contextCache;

	/**
	 * The configured failure threshold for errors encountered while attempting to
	 * load an {@link ApplicationContext}.
	 * @since 6.1
	 */
	private final int failureThreshold;


	/**
	 * Construct a new {@code DefaultCacheAwareContextLoaderDelegate} using a
	 * static {@link DefaultContextCache}.
	 * <p>The default cache is static so that each context can be cached and
	 * reused for all subsequent tests that declare the same unique context
	 * configuration within the same JVM process.
	 * @see #DefaultCacheAwareContextLoaderDelegate(ContextCache)
	 */
	public DefaultCacheAwareContextLoaderDelegate() {
		this(defaultContextCache);
	}

	/**
	 * Construct a new {@code DefaultCacheAwareContextLoaderDelegate} using the
	 * supplied {@link ContextCache} and the default or user-configured context
	 * failure threshold.
	 * @see #DefaultCacheAwareContextLoaderDelegate()
	 * @see ContextCacheUtils#retrieveContextFailureThreshold()
	 */
	public DefaultCacheAwareContextLoaderDelegate(ContextCache contextCache) {
		this(contextCache, ContextCacheUtils.retrieveContextFailureThreshold());
	}

	/**
	 * Construct a new {@code DefaultCacheAwareContextLoaderDelegate} using the
	 * supplied {@link ContextCache} and context failure threshold.
	 * @since 6.1
	 */
	private DefaultCacheAwareContextLoaderDelegate(ContextCache contextCache, int failureThreshold) {
		Assert.notNull(contextCache, "ContextCache must not be null");
		Assert.isTrue(failureThreshold > 0, "'failureThreshold' must be positive");
		this.contextCache = contextCache;
		this.failureThreshold = failureThreshold;
	}


	@Override
	public boolean isContextLoaded(MergedContextConfiguration mergedConfig) {
		mergedConfig = replaceIfNecessary(mergedConfig);
		synchronized (this.contextCache) {
			return this.contextCache.contains(mergedConfig);
		}
	}

	@Override
	public ApplicationContext loadContext(MergedContextConfiguration mergedConfig) {
		mergedConfig = replaceIfNecessary(mergedConfig);
		synchronized (this.contextCache) {
			ApplicationContext context = this.contextCache.get(mergedConfig);
			try {
				if (context == null) {
					int failureCount = this.contextCache.getFailureCount(mergedConfig);
					if (failureCount >= this.failureThreshold) {
						throw new IllegalStateException("""
								ApplicationContext failure threshold (%d) exceeded: \
								skipping repeated attempt to load context for %s"""
									.formatted(this.failureThreshold, mergedConfig));
					}
					try {
						if (mergedConfig instanceof AotMergedContextConfiguration aotMergedConfig) {
							context = loadContextInAotMode(aotMergedConfig);
						}
						else {
							context = loadContextInternal(mergedConfig);
						}
						if (logger.isTraceEnabled()) {
							logger.trace("Storing ApplicationContext [%s] in cache under key %s".formatted(
									System.identityHashCode(context), mergedConfig));
						}
						this.contextCache.put(mergedConfig, context);
					}
					catch (Exception ex) {
						if (logger.isTraceEnabled()) {
							logger.trace("Incrementing ApplicationContext failure count for " + mergedConfig);
						}
						this.contextCache.incrementFailureCount(mergedConfig);
						Throwable cause = ex;
						if (ex instanceof ContextLoadException cle) {
							cause = cle.getCause();
							for (ApplicationContextFailureProcessor contextFailureProcessor : this.contextFailureProcessors) {
								try {
									contextFailureProcessor.processLoadFailure(cle.getApplicationContext(), cause);
								}
								catch (Throwable throwable) {
									if (logger.isDebugEnabled()) {
										logger.debug("Ignoring exception thrown from ApplicationContextFailureProcessor [%s]: %s"
												.formatted(contextFailureProcessor, throwable));
									}
								}
							}
						}
						throw new IllegalStateException(
								"Failed to load ApplicationContext for " + mergedConfig, cause);
					}
				}
				else {
					if (logger.isTraceEnabled()) {
						logger.trace("Retrieved ApplicationContext [%s] from cache with key %s".formatted(
								System.identityHashCode(context), mergedConfig));
					}
				}
			}
			finally {
				this.contextCache.logStatistics();
			}

			return context;
		}
	}

	@Override
	public void closeContext(MergedContextConfiguration mergedConfig, @Nullable HierarchyMode hierarchyMode) {
		mergedConfig = replaceIfNecessary(mergedConfig);
		synchronized (this.contextCache) {
			this.contextCache.remove(mergedConfig, hierarchyMode);
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
	protected ApplicationContext loadContextInternal(MergedContextConfiguration mergedConfig)
			throws Exception {

		ContextLoader contextLoader = getContextLoader(mergedConfig);
		if (contextLoader instanceof SmartContextLoader smartContextLoader) {
			return smartContextLoader.loadContext(mergedConfig);
		}
		else {
			String[] locations = mergedConfig.getLocations();
			Assert.notNull(locations, () -> """
					Cannot load an ApplicationContext with a NULL 'locations' array. \
					Consider annotating test class [%s] with @ContextConfiguration or \
					@ContextHierarchy.""".formatted(mergedConfig.getTestClass().getName()));
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

		if (logger.isTraceEnabled()) {
			logger.trace("Loading ApplicationContext for AOT runtime for " + aotMergedConfig.getOriginal());
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("Loading ApplicationContext for AOT runtime for test class " +
					aotMergedConfig.getTestClass().getName());
		}

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
		Assert.notNull(contextLoader, () -> """
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
	 * @param mergedConfig the original {@code MergedContextConfiguration}
	 * @return {@code AotMergedContextConfiguration} or the original {@code MergedContextConfiguration}
	 * @throws IllegalStateException if running in AOT mode and the test class does not
	 * have an AOT-optimized {@code ApplicationContext}
	 * @since 6.0
	 */
	private MergedContextConfiguration replaceIfNecessary(MergedContextConfiguration mergedConfig) {
		if (AotDetector.useGeneratedArtifacts()) {
			Class<?> testClass = mergedConfig.getTestClass();
			Class<? extends ApplicationContextInitializer<?>> contextInitializerClass =
					this.aotTestContextInitializers.getContextInitializerClass(testClass);
			Assert.state(contextInitializerClass != null, () -> """
					Failed to load AOT ApplicationContextInitializer class for test class [%s]. \
					This can occur if AOT processing has not taken place for the test suite. It \
					can also occur if AOT processing failed for the test class, in which case you \
					can consult the logs generated during AOT processing.""".formatted(testClass.getName()));
			return new AotMergedContextConfiguration(testClass, contextInitializerClass, mergedConfig, this);
		}
		return mergedConfig;
	}

}
