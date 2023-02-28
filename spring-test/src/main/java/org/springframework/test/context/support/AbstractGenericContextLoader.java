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

package org.springframework.test.context.support;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextLoadException;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.aot.AotContextLoader;
import org.springframework.util.Assert;

/**
 * Abstract, generic extension of {@link AbstractContextLoader} that loads a
 * {@link GenericApplicationContext}.
 *
 * <ul>
 * <li>If instances of concrete subclasses are invoked via the
 * {@link org.springframework.test.context.ContextLoader ContextLoader} SPI, the
 * context will be loaded from the <em>locations</em> provided to
 * {@link #loadContext(String...)}.</li>
 * <li>If instances of concrete subclasses are invoked via the
 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader}
 * SPI, the context will be loaded from the {@link MergedContextConfiguration}
 * provided to {@link #loadContext(MergedContextConfiguration)}. In such
 * cases, a {@code SmartContextLoader} will decide whether to load the context
 * from <em>locations</em> or <em>annotated classes</em>.</li>
 * </ul>
 *
 * <p>Concrete subclasses must provide an appropriate implementation of
 * {@link #createBeanDefinitionReader createBeanDefinitionReader()},
 * potentially overriding {@link #loadBeanDefinitions loadBeanDefinitions()}
 * as well.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 2.5
 * @see #loadContext(MergedContextConfiguration)
 */
public abstract class AbstractGenericContextLoader extends AbstractContextLoader implements AotContextLoader {

	protected static final Log logger = LogFactory.getLog(AbstractGenericContextLoader.class);

	/**
	 * Load a {@link GenericApplicationContext} for the supplied
	 * {@link MergedContextConfiguration}.
	 * <p>Implementation details:
	 * <ul>
	 * <li>Calls {@link #validateMergedContextConfiguration(MergedContextConfiguration)}
	 * to allow subclasses to validate the supplied configuration before proceeding.</li>
	 * <li>Calls {@link #createContext()} to create a {@link GenericApplicationContext}
	 * instance.</li>
	 * <li>If the supplied {@code MergedContextConfiguration} references a
	 * {@linkplain MergedContextConfiguration#getParent() parent configuration},
	 * the corresponding {@link MergedContextConfiguration#getParentApplicationContext()
	 * ApplicationContext} will be retrieved and
	 * {@linkplain GenericApplicationContext#setParent(ApplicationContext) set as the parent}
	 * for the context created by this method.</li>
	 * <li>Calls {@link #prepareContext(GenericApplicationContext)} for backwards
	 * compatibility with the {@link org.springframework.test.context.ContextLoader
	 * ContextLoader} SPI.</li>
	 * <li>Calls {@link #prepareContext(ConfigurableApplicationContext, MergedContextConfiguration)}
	 * to allow for customizing the context before bean definitions are loaded.</li>
	 * <li>Calls {@link #customizeBeanFactory(DefaultListableBeanFactory)} to allow for customizing the
	 * context's {@code DefaultListableBeanFactory}.</li>
	 * <li>Delegates to {@link #loadBeanDefinitions(GenericApplicationContext, MergedContextConfiguration)}
	 * to populate the context from the locations or classes in the supplied
	 * {@code MergedContextConfiguration}.</li>
	 * <li>Delegates to {@link AnnotationConfigUtils} for
	 * {@link AnnotationConfigUtils#registerAnnotationConfigProcessors registering}
	 * annotation configuration processors.</li>
	 * <li>Calls {@link #customizeContext(GenericApplicationContext)} to allow for customizing the context
	 * before it is refreshed.</li>
	 * <li>Calls {@link #customizeContext(ConfigurableApplicationContext, MergedContextConfiguration)} to
	 * allow for customizing the context before it is refreshed.</li>
	 * <li>{@link ConfigurableApplicationContext#refresh Refreshes} the
	 * context and registers a JVM shutdown hook for it.</li>
	 * </ul>
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context
	 * @return a new application context
	 * @see org.springframework.test.context.SmartContextLoader#loadContext(MergedContextConfiguration)
	 */
	@Override
	public final ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
		return loadContext(mergedConfig, false);
	}

	/**
	 * Load a {@link GenericApplicationContext} for AOT build-time processing based
	 * on the supplied {@link MergedContextConfiguration}.
	 * <p>In contrast to {@link #loadContext(MergedContextConfiguration)}, this
	 * method does not
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#refresh()
	 * refresh} the {@code ApplicationContext} or
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#registerShutdownHook()
	 * register a JVM shutdown hook} for it. Otherwise, this method implements
	 * behavior identical to {@link #loadContext(MergedContextConfiguration)}.
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context
	 * @return a new application context
	 * @throws Exception if context loading failed
	 * @since 6.0
	 * @see AotContextLoader#loadContextForAotProcessing(MergedContextConfiguration)
	 */
	@Override
	public final GenericApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig)
			throws Exception {
		return loadContext(mergedConfig, true);
	}

	/**
	 * Load a {@link GenericApplicationContext} for AOT run-time execution based on
	 * the supplied {@link MergedContextConfiguration} and
	 * {@link ApplicationContextInitializer}.
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context
	 * @param initializer the {@code ApplicationContextInitializer} that should
	 * be applied to the context in order to recreate bean definitions
	 * @return a new application context
	 * @throws Exception if context loading failed
	 * @since 6.0
	 * @see AotContextLoader#loadContextForAotRuntime(MergedContextConfiguration, ApplicationContextInitializer)
	 */
	@Override
	public final GenericApplicationContext loadContextForAotRuntime(MergedContextConfiguration mergedConfig,
			ApplicationContextInitializer<ConfigurableApplicationContext> initializer) throws Exception {

		Assert.notNull(mergedConfig, "MergedContextConfiguration must not be null");
		Assert.notNull(initializer, "ApplicationContextInitializer must not be null");

		if (logger.isTraceEnabled()) {
			logger.trace("Loading ApplicationContext for AOT runtime for " + mergedConfig);
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("Loading ApplicationContext for AOT runtime for test class " +
					mergedConfig.getTestClass().getName());
		}

		validateMergedContextConfiguration(mergedConfig);

		GenericApplicationContext context = createContext();
		try {
			prepareContext(context);
			prepareContext(context, mergedConfig);
			initializer.initialize(context);
			customizeContext(context);
			customizeContext(context, mergedConfig);
			context.refresh();
			return context;
		}
		catch (Exception ex) {
			throw new ContextLoadException(context, ex);
		}
	}

	/**
	 * Load a {@link GenericApplicationContext} for the supplied
	 * {@link MergedContextConfiguration}.
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context
	 * @param forAotProcessing {@code true} if the context is being loaded for
	 * AOT processing, meaning not to refresh the {@code ApplicationContext} or
	 * register a JVM shutdown hook for it
	 * @return a new application context
	 */
	private GenericApplicationContext loadContext(
			MergedContextConfiguration mergedConfig, boolean forAotProcessing) throws Exception {

		if (logger.isTraceEnabled()) {
			logger.trace("Loading ApplicationContext %sfor %s".formatted(
					(forAotProcessing ? "for AOT processing " : ""), mergedConfig));
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("Loading ApplicationContext %sfor test class %s".formatted(
					(forAotProcessing ? "for AOT processing " : ""), mergedConfig.getTestClass().getName()));
		}

		validateMergedContextConfiguration(mergedConfig);

		GenericApplicationContext context = createContext();
		try {
			ApplicationContext parent = mergedConfig.getParentApplicationContext();
			if (parent != null) {
				context.setParent(parent);
			}

			prepareContext(context);
			prepareContext(context, mergedConfig);
			customizeBeanFactory(context.getDefaultListableBeanFactory());
			loadBeanDefinitions(context, mergedConfig);
			AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
			customizeContext(context);
			customizeContext(context, mergedConfig);

			if (!forAotProcessing) {
				context.refresh();
				context.registerShutdownHook();
			}

			return context;
		}
		catch (Exception ex) {
			throw new ContextLoadException(context, ex);
		}
	}

	/**
	 * Validate the supplied {@link MergedContextConfiguration} with respect to
	 * what this context loader supports.
	 * <p>The default implementation is a <em>no-op</em> but can be overridden by
	 * subclasses as appropriate.
	 * @param mergedConfig the merged configuration to validate
	 * @throws IllegalStateException if the supplied configuration is not valid
	 * for this context loader
	 * @since 4.0.4
	 */
	protected void validateMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		// no-op
	}

	/**
	 * Load a Spring ApplicationContext from the supplied {@code locations}.
	 * <p>Implementation details:
	 * <ul>
	 * <li>Calls {@link #createContext()} to create a {@link GenericApplicationContext}
	 * instance.</li>
	 * <li>Calls {@link #prepareContext(GenericApplicationContext)} to allow for customizing the context
	 * before bean definitions are loaded.</li>
	 * <li>Calls {@link #customizeBeanFactory(DefaultListableBeanFactory)} to allow for customizing the
	 * context's {@code DefaultListableBeanFactory}.</li>
	 * <li>Delegates to {@link #createBeanDefinitionReader(GenericApplicationContext)} to create a
	 * {@link BeanDefinitionReader} which is then used to populate the context
	 * from the specified locations.</li>
	 * <li>Delegates to {@link AnnotationConfigUtils} for
	 * {@link AnnotationConfigUtils#registerAnnotationConfigProcessors registering}
	 * annotation configuration processors.</li>
	 * <li>Calls {@link #customizeContext(GenericApplicationContext)} to allow for customizing the context
	 * before it is refreshed.</li>
	 * <li>{@link ConfigurableApplicationContext#refresh Refreshes} the
	 * context and registers a JVM shutdown hook for it.</li>
	 * </ul>
	 * <p><b>Note</b>: this method does not provide a means to set active bean definition
	 * profiles for the loaded context. See {@link #loadContext(MergedContextConfiguration)}
	 * and {@link AbstractContextLoader#prepareContext(ConfigurableApplicationContext, MergedContextConfiguration)}
	 * for an alternative.
	 * @return a new application context
	 * @since 2.5
	 * @see org.springframework.test.context.ContextLoader#loadContext
	 * @see GenericApplicationContext
	 * @see #loadContext(MergedContextConfiguration)
	 * @deprecated as of Spring Framework 6.0, in favor of {@link #loadContext(MergedContextConfiguration)}
	 */
	@Deprecated(since = "6.0")
	@Override
	public final ConfigurableApplicationContext loadContext(String... locations) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Loading ApplicationContext for locations " + Arrays.toString(locations));
		}

		GenericApplicationContext context = createContext();

		prepareContext(context);
		customizeBeanFactory(context.getDefaultListableBeanFactory());
		createBeanDefinitionReader(context).loadBeanDefinitions(locations);
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		customizeContext(context);

		context.refresh();
		context.registerShutdownHook();

		return context;
	}

	/**
	 * Factory method for creating the {@link GenericApplicationContext} used by
	 * this {@code ContextLoader}.
	 * <p>The default implementation creates a {@code GenericApplicationContext}
	 * using the default constructor. This method may be overridden &mdash; for
	 * example, to use a custom context subclass or to create a
	 * {@code GenericApplicationContext} with a custom
	 * {@link DefaultListableBeanFactory} implementation.
	 * @return a newly instantiated {@code GenericApplicationContext}
	 * @since 5.2.9
	 */
	protected GenericApplicationContext createContext() {
		return new GenericApplicationContext();
	}

	/**
	 * Prepare the {@link GenericApplicationContext} created by this {@code ContextLoader}.
	 * Called <i>before</i> bean definitions are read.
	 * <p>The default implementation is empty. Can be overridden in subclasses to
	 * customize {@code GenericApplicationContext}'s standard settings.
	 * @param context the context that should be prepared
	 * @since 2.5
	 * @see #loadContext(MergedContextConfiguration)
	 * @see GenericApplicationContext#setAllowBeanDefinitionOverriding
	 * @see GenericApplicationContext#setResourceLoader
	 * @see GenericApplicationContext#setId
	 * @see #prepareContext(ConfigurableApplicationContext, MergedContextConfiguration)
	 */
	protected void prepareContext(GenericApplicationContext context) {
	}

	/**
	 * Customize the internal bean factory of the ApplicationContext created by
	 * this {@code ContextLoader}.
	 * <p>The default implementation is empty but can be overridden in subclasses
	 * to customize {@code DefaultListableBeanFactory}'s standard settings.
	 * @param beanFactory the bean factory created by this {@code ContextLoader}
	 * @since 2.5
	 * @see #loadContext(MergedContextConfiguration)
	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see DefaultListableBeanFactory#setAllowEagerClassLoading
	 * @see DefaultListableBeanFactory#setAllowCircularReferences
	 * @see DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 */
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
	}

	/**
	 * Load bean definitions into the supplied {@link GenericApplicationContext context}
	 * from the locations or classes in the supplied {@code MergedContextConfiguration}.
	 * <p>The default implementation delegates to the {@link BeanDefinitionReader}
	 * returned by {@link #createBeanDefinitionReader(GenericApplicationContext)} to
	 * {@link BeanDefinitionReader#loadBeanDefinitions(String) load} the
	 * bean definitions.
	 * <p>Subclasses must provide an appropriate implementation of
	 * {@link #createBeanDefinitionReader(GenericApplicationContext)}. Alternatively subclasses
	 * may provide a <em>no-op</em> implementation of {@code createBeanDefinitionReader()}
	 * and override this method to provide a custom strategy for loading or
	 * registering bean definitions.
	 * @param context the context into which the bean definitions should be loaded
	 * @param mergedConfig the merged context configuration
	 * @since 3.1
	 * @see #loadContext(MergedContextConfiguration)
	 */
	protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
		createBeanDefinitionReader(context).loadBeanDefinitions(mergedConfig.getLocations());
	}

	/**
	 * Factory method for creating a new {@link BeanDefinitionReader} for loading
	 * bean definitions into the supplied {@link GenericApplicationContext context}.
	 * @param context the context for which the {@code BeanDefinitionReader}
	 * should be created
	 * @return a {@code BeanDefinitionReader} for the supplied context
	 * @since 2.5
	 * @see #loadContext(MergedContextConfiguration)
	 * @see #loadBeanDefinitions
	 * @see BeanDefinitionReader
	 */
	protected abstract BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context);

	/**
	 * Customize the {@link GenericApplicationContext} created by this
	 * {@code ContextLoader} <i>after</i> bean definitions have been
	 * loaded into the context but <i>before</i> the context is refreshed.
	 * <p>The default implementation is empty but can be overridden in subclasses
	 * to customize the application context.
	 * @param context the newly created application context
	 * @since 2.5
	 * @see #loadContext(MergedContextConfiguration)
	 * @see #customizeContext(ConfigurableApplicationContext, MergedContextConfiguration)
	 */
	protected void customizeContext(GenericApplicationContext context) {
	}

}
