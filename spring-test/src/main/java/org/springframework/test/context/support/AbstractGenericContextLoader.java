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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.StringUtils;

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
 * provided to {@link #loadContext(MergedContextConfiguration)}. In such cases, a
 * {@code SmartContextLoader} will decide whether to load the context from
 * <em>locations</em> or <em>annotated classes</em>.</li>
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
 * @see #loadContext(String...)
 */
public abstract class AbstractGenericContextLoader extends AbstractContextLoader {

	protected static final Log logger = LogFactory.getLog(AbstractGenericContextLoader.class);


	/**
	 * Load a Spring ApplicationContext from the supplied {@link MergedContextConfiguration}.
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
	 * @return a new application context
	 * @since 3.1
	 * @see org.springframework.test.context.SmartContextLoader#loadContext(MergedContextConfiguration)
	 * @see GenericApplicationContext
	 */
	@Override
	public final ConfigurableApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Loading ApplicationContext for merged context configuration [%s].",
					mergedConfig));
		}

		validateMergedContextConfiguration(mergedConfig);

		GenericApplicationContext context = createContext();
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

		context.refresh();
		context.registerShutdownHook();

		return context;
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
	 */
	@Override
	public final ConfigurableApplicationContext loadContext(String... locations) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Loading ApplicationContext for locations [%s].",
					StringUtils.arrayToCommaDelimitedString(locations)));
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
	 * @see #loadContext(String...)
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
	 * @see #loadContext(String...)
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
	 * @see #loadContext(String...)
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
	 * @see #loadContext(String...)
	 * @see #customizeContext(ConfigurableApplicationContext, MergedContextConfiguration)
	 */
	protected void customizeContext(GenericApplicationContext context) {
	}

}
