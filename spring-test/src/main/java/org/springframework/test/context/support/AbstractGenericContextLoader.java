/*
 * Copyright 2002-2011 the original author or authors.
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
 * {@link #loadContext(ApplicationContext, String...)}.</li>
 * <li>If instances of concrete subclasses are invoked via the
 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader}
 * SPI, the context will be loaded from the {@link MergedContextConfiguration}
 * provided to {@link #loadContext(ApplicationContext ,MergedContextConfiguration)}. In such cases, a
 * <code>SmartContextLoader</code> will decide whether to load the context from
 * <em>locations</em> or
 * {@link org.springframework.context.annotation.Configuration configuration classes}.</li>
 * </ul>
 *
 * <p>Concrete subclasses must provide an appropriate implementation of
 * {@link #createBeanDefinitionReader createBeanDefinitionReader()},
 * potentially overriding {@link #loadBeanDefinitions loadBeanDefinitions()}
 * as well.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see #loadContext(ApplicationContext, MergedContextConfiguration)
 * @see #loadContext(ApplicationContext, String...)
 */
public abstract class AbstractGenericContextLoader extends AbstractContextLoader {

	protected static final Log logger = LogFactory.getLog(AbstractGenericContextLoader.class);


	/**
	 * Load a Spring ApplicationContext from the supplied {@link MergedContextConfiguration}.
	 * <p>Implementation details:
	 * <ul>
	 * <li>Creates a {@link GenericApplicationContext} instance.</li>
	 * <li>Sets the <em>active bean definition profiles</em> from the supplied
	 * <code>MergedContextConfiguration</code> in the
	 * {@link org.springframework.core.env.Environment Environment} of the context.</li>
	 * <li>Calls {@link #prepareContext()} to allow for customizing the context
	 * before bean definitions are loaded.</li>
	 * <li>Calls {@link #customizeBeanFactory()} to allow for customizing the
	 * context's <code>DefaultListableBeanFactory</code>.</li>
	 * <li>Delegates to {@link #loadBeanDefinitions()} to populate the context
	 * from the configuration locations or classes in the supplied
	 * <code>MergedContextConfiguration</code>.</li>
	 * <li>Delegates to {@link AnnotationConfigUtils} for
	 * {@link AnnotationConfigUtils#registerAnnotationConfigProcessors registering}
	 * annotation configuration processors.</li>
	 * <li>Calls {@link #customizeContext()} to allow for customizing the context
	 * before it is refreshed.</li>
	 * <li>{@link ConfigurableApplicationContext#refresh Refreshes} the
	 * context and registers a JVM shutdown hook for it.</li>
	 * </ul>
	 * @return a new application context
	 * @see org.springframework.test.context.SmartContextLoader#loadContext(ApplicationContext, MergedContextConfiguration)
	 * @see GenericApplicationContext
	 * @since 3.1
	 */
	public final ConfigurableApplicationContext loadContext(ApplicationContext parentContext, MergedContextConfiguration mergedConfig) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Loading ApplicationContext for merged context configuration [%s].",
				mergedConfig));
		}
		GenericApplicationContext context = new GenericApplicationContext();
		if (parentContext != null) {
			context.setParent(parentContext);
		}

		context.getEnvironment().setActiveProfiles(mergedConfig.getActiveProfiles());
		prepareContext(context);
		customizeBeanFactory(context.getDefaultListableBeanFactory());
		loadBeanDefinitions(context, mergedConfig);
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		customizeContext(context);
		context.refresh();
		context.registerShutdownHook();
		return context;
	}

	/**
	 * Load a Spring ApplicationContext from the supplied <code>locations</code>.
	 * <p>Implementation details:
	 * <ul>
	 * <li>Creates a {@link GenericApplicationContext} instance.</li>
	 * <li>Calls {@link #prepareContext()} to allow for customizing the context
	 * before bean definitions are loaded.</li>
	 * <li>Calls {@link #customizeBeanFactory()} to allow for customizing the
	 * context's <code>DefaultListableBeanFactory</code>.</li>
	 * <li>Delegates to {@link #createBeanDefinitionReader()} to create a 
	 * {@link BeanDefinitionReader} which is then used to populate the context
	 * from the specified config locations.</li>
	 * <li>Delegates to {@link AnnotationConfigUtils} for
	 * {@link AnnotationConfigUtils#registerAnnotationConfigProcessors registering}
	 * annotation configuration processors.</li>
	 * <li>Calls {@link #customizeContext()} to allow for customizing the context
	 * before it is refreshed.</li>
	 * <li>{@link ConfigurableApplicationContext#refresh Refreshes} the
	 * context and registers a JVM shutdown hook for it.</li>
	 * </ul>
	 * <p><b>Note</b>: this method does not provide a means to set active bean definition
	 * profiles for the loaded context. See {@link #loadContext(ApplicationContext, MergedContextConfiguration)}
	 * for an alternative.
	 * @return a new application context
	 * @see org.springframework.test.context.ContextLoader#loadContext
	 * @see GenericApplicationContext
	 * @see #loadContext(ApplicationContext, MergedContextConfiguration)
	 * @since 2.5
	 */
	public final ConfigurableApplicationContext loadContext(ApplicationContext parentContext, String... locations) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Loading ApplicationContext for locations [%s].",
					StringUtils.arrayToCommaDelimitedString(locations)));
		}

		GenericApplicationContext context = new GenericApplicationContext();
		if (parentContext != null) {
			context.setParent(parentContext);
		}

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
	 * Prepare the {@link GenericApplicationContext} created by this <code>ContextLoader</code>.
	 * Called <i>before</i> bean definitions are read.
	 * <p>The default implementation is empty. Can be overridden in subclasses to
	 * customize <code>GenericApplicationContext</code>'s standard settings.
	 * @param context the context that should be prepared
	 * @see #loadContext(ApplicationContext, MergedContextConfiguration)
	 * @see #loadContext(ApplicationContext, String...)
	 * @see GenericApplicationContext#setAllowBeanDefinitionOverriding
	 * @see GenericApplicationContext#setResourceLoader
	 * @see GenericApplicationContext#setId
	 * @since 2.5
	 */
	protected void prepareContext(GenericApplicationContext context) {
	}

	/**
	 * Customize the internal bean factory of the ApplicationContext created by
	 * this <code>ContextLoader</code>.
	 * <p>The default implementation is empty but can be overridden in subclasses
	 * to customize <code>DefaultListableBeanFactory</code>'s standard settings.
	 * @param beanFactory the bean factory created by this <code>ContextLoader</code>
	 * @see #loadContext(ApplicationContext, MergedContextConfiguration)
	 * @see #loadContext(ApplicationContext, String...)
	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see DefaultListableBeanFactory#setAllowEagerClassLoading
	 * @see DefaultListableBeanFactory#setAllowCircularReferences
	 * @see DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 * @since 2.5
	 */
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
	}

	/**
	 * Load bean definitions into the supplied {@link GenericApplicationContext context}
	 * from the configuration locations or classes in the supplied
	 * <code>MergedContextConfiguration</code>.</li>
	 * <p>The default implementation delegates to the {@link BeanDefinitionReader}
	 * returned by {@link #createBeanDefinitionReader()} to 
	 * {@link BeanDefinitionReader#loadBeanDefinitions(String) load} the
	 * bean definitions.
	 * <p>Subclasses must provide an appropriate implementation of
	 * {@link #createBeanDefinitionReader()}. Alternatively subclasses may
	 * provide a <em>no-op</em> implementation of {@code createBeanDefinitionReader()}
	 * and override this method to provide a custom strategy for loading or
	 * registering bean definitions.
	 * @param context the context into which the bean definitions should be loaded
	 * @param mergedConfig the merged context configuration 
	 * @see #loadContext(ApplicationContext, MergedContextConfiguration)
	 * @since 3.1
	 */
	protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
		createBeanDefinitionReader(context).loadBeanDefinitions(mergedConfig.getLocations());
	}

	/**
	 * Factory method for creating a new {@link BeanDefinitionReader} for loading
	 * bean definitions into the supplied {@link GenericApplicationContext context}.
	 * @param context the context for which the <code>BeanDefinitionReader</code>
	 * should be created
	 * @return a <code>BeanDefinitionReader</code> for the supplied context
	 * @see #loadContext(ApplicationContext, String...)
	 * @see #loadBeanDefinitions
	 * @see BeanDefinitionReader
	 * @since 2.5
	 */
	protected abstract BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context);

	/**
	 * Customize the {@link GenericApplicationContext} created by this
	 * <code>ContextLoader</code> <i>after</i> bean definitions have been
	 * loaded into the context but <i>before</i> the context is refreshed.
	 * <p>The default implementation is empty but can be overridden in subclasses
	 * to customize the application context.
	 * @param context the newly created application context
	 * @see #loadContext(ApplicationContext, MergedContextConfiguration)
	 * @see #loadContext(ApplicationContext, String...)
	 * @since 2.5
	 */
	protected void customizeContext(GenericApplicationContext context) {
	}

}
