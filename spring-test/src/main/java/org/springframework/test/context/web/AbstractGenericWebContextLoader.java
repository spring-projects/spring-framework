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

package org.springframework.test.context.web;

import jakarta.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextLoadException;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.aot.AotContextLoader;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * Abstract, generic extension of {@link AbstractContextLoader} that loads a
 * {@link GenericWebApplicationContext}.
 *
 * <p>If instances of concrete subclasses are invoked via the
 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader}
 * SPI, the context will be loaded from the {@link MergedContextConfiguration}
 * provided to {@link #loadContext(MergedContextConfiguration)}. In such cases, a
 * {@code SmartContextLoader} will decide whether to load the context from
 * <em>locations</em> or <em>annotated classes</em>. Note that {@code
 * AbstractGenericWebContextLoader} does not support the {@code
 * loadContext(String... locations)} method from the legacy
 * {@link org.springframework.test.context.ContextLoader ContextLoader} SPI.
 *
 * <p>Concrete subclasses must provide an appropriate implementation of
 * {@link #loadBeanDefinitions}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.2
 * @see #loadContext(MergedContextConfiguration)
 * @see #loadContext(String...)
 */
public abstract class AbstractGenericWebContextLoader extends AbstractContextLoader implements AotContextLoader {

	protected static final Log logger = LogFactory.getLog(AbstractGenericWebContextLoader.class);


	/**
	 * Load a {@link GenericWebApplicationContext} for the supplied
	 * {@link MergedContextConfiguration}.
	 * <p>Implementation details:
	 * <ul>
	 * <li>Calls {@link #validateMergedContextConfiguration(WebMergedContextConfiguration)}
	 * to allow subclasses to validate the supplied configuration before proceeding.</li>
	 * <li>Creates a {@link GenericWebApplicationContext} instance.</li>
	 * <li>If the supplied {@code MergedContextConfiguration} references a
	 * {@linkplain MergedContextConfiguration#getParent() parent configuration},
	 * the corresponding {@link MergedContextConfiguration#getParentApplicationContext()
	 * ApplicationContext} will be retrieved and
	 * {@linkplain GenericWebApplicationContext#setParent(ApplicationContext) set as the parent}
	 * for the context created by this method.</li>
	 * <li>Delegates to {@link #configureWebResources} to create the
	 * {@link MockServletContext} and set it in the {@code WebApplicationContext}.</li>
	 * <li>Calls {@link #prepareContext} to allow for customizing the context
	 * before bean definitions are loaded.</li>
	 * <li>Calls {@link #customizeBeanFactory} to allow for customizing the
	 * context's {@code DefaultListableBeanFactory}.</li>
	 * <li>Delegates to {@link #loadBeanDefinitions} to populate the context
	 * from the locations or classes in the supplied {@code MergedContextConfiguration}.</li>
	 * <li>Delegates to {@link AnnotationConfigUtils} for
	 * {@linkplain AnnotationConfigUtils#registerAnnotationConfigProcessors registering}
	 * annotation configuration processors.</li>
	 * <li>Calls {@link #customizeContext} to allow for customizing the context
	 * before it is refreshed.</li>
	 * <li>{@link ConfigurableApplicationContext#refresh Refreshes} the
	 * context and registers a JVM shutdown hook for it.</li>
	 * </ul>
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context
	 * @return a new web application context
	 * @see org.springframework.test.context.SmartContextLoader#loadContext(MergedContextConfiguration)
	 */
	@Override
	public final ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
		return loadContext(mergedConfig, false);
	}

	/**
	 * Load a {@link GenericWebApplicationContext} for AOT build-time processing based
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
	 * @return a new web application context
	 * @throws Exception if context loading failed
	 * @since 6.0
	 * @see AotContextLoader#loadContextForAotProcessing(MergedContextConfiguration)
	 */
	@Override
	public final GenericWebApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig)
			throws Exception {
		return loadContext(mergedConfig, true);
	}

	/**
	 * Load a {@link GenericWebApplicationContext} for AOT run-time execution based on
	 * the supplied {@link MergedContextConfiguration} and
	 * {@link ApplicationContextInitializer}.
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context
	 * @param initializer the {@code ApplicationContextInitializer} that should
	 * be applied to the context in order to recreate bean definitions
	 * @return a new web application context
	 * @throws Exception if context loading failed
	 * @since 6.0
	 * @see AotContextLoader#loadContextForAotRuntime(MergedContextConfiguration, ApplicationContextInitializer)
	 */
	@Override
	public final GenericWebApplicationContext loadContextForAotRuntime(MergedContextConfiguration mergedConfig,
			ApplicationContextInitializer<ConfigurableApplicationContext> initializer) throws Exception {

		Assert.notNull(mergedConfig, "MergedContextConfiguration must not be null");
		Assert.notNull(initializer, "ApplicationContextInitializer must not be null");
		if (!(mergedConfig instanceof WebMergedContextConfiguration webMergedConfig)) {
			throw new IllegalArgumentException("""
					Cannot load WebApplicationContext from non-web merged context configuration %s. \
					Consider annotating your test class with @WebAppConfiguration."""
						.formatted(mergedConfig));
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Loading WebApplicationContext for AOT runtime for " + mergedConfig);
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("Loading WebApplicationContext for AOT runtime for test class " +
					mergedConfig.getTestClass().getName());
		}

		validateMergedContextConfiguration(webMergedConfig);

		GenericWebApplicationContext context = createContext();
		try {
			configureWebResources(context, webMergedConfig);
			prepareContext(context, webMergedConfig);
			initializer.initialize(context);
			customizeContext(context, webMergedConfig);
			context.refresh();
			return context;
		}
		catch (Exception ex) {
			throw new ContextLoadException(context, ex);
		}
	}

	/**
	 * Load a {@link GenericWebApplicationContext} for the supplied
	 * {@link MergedContextConfiguration}.
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context
	 * @param forAotProcessing {@code true} if the context is being loaded for
	 * AOT processing, meaning not to refresh the {@code ApplicationContext} or
	 * register a JVM shutdown hook for it
	 * @return a new web application context
	 * @see org.springframework.test.context.SmartContextLoader#loadContext(MergedContextConfiguration)
	 * @see org.springframework.test.context.aot.AotContextLoader#loadContextForAotProcessing(MergedContextConfiguration)
	 */
	private GenericWebApplicationContext loadContext(
			MergedContextConfiguration mergedConfig, boolean forAotProcessing) throws Exception {

		if (!(mergedConfig instanceof WebMergedContextConfiguration webMergedConfig)) {
			throw new IllegalArgumentException("""
					Cannot load WebApplicationContext from non-web merged context configuration %s. \
					Consider annotating your test class with @WebAppConfiguration."""
						.formatted(mergedConfig));
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Loading WebApplicationContext %sfor %s".formatted(
					(forAotProcessing ? "for AOT processing " : ""), mergedConfig));
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("Loading WebApplicationContext %sfor test class %s".formatted(
					(forAotProcessing ? "for AOT processing " : ""), mergedConfig.getTestClass().getName()));
		}

		validateMergedContextConfiguration(webMergedConfig);

		GenericWebApplicationContext context = createContext();
		try {
			ApplicationContext parent = mergedConfig.getParentApplicationContext();
			if (parent != null) {
				context.setParent(parent);
			}
			configureWebResources(context, webMergedConfig);
			prepareContext(context, webMergedConfig);
			customizeBeanFactory(context.getDefaultListableBeanFactory(), webMergedConfig);
			loadBeanDefinitions(context, webMergedConfig);
			AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
			customizeContext(context, webMergedConfig);

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
	 * Validate the supplied {@link WebMergedContextConfiguration} with respect to
	 * what this context loader supports.
	 * <p>The default implementation is a <em>no-op</em> but can be overridden by
	 * subclasses as appropriate.
	 * @param mergedConfig the merged configuration to validate
	 * @throws IllegalStateException if the supplied configuration is not valid
	 * for this context loader
	 * @since 4.0.4
	 */
	protected void validateMergedContextConfiguration(WebMergedContextConfiguration mergedConfig) {
		// no-op
	}

	/**
	 * Factory method for creating the {@link GenericWebApplicationContext} used
	 * by this {@code ContextLoader}.
	 * <p>The default implementation creates a {@code GenericWebApplicationContext}
	 * using the default constructor. This method may be overridden &mdash; for
	 * example, to use a custom context subclass or to create a
	 * {@code GenericWebApplicationContext} with a custom
	 * {@link DefaultListableBeanFactory} implementation.
	 * @return a newly instantiated {@code GenericWebApplicationContext}
	 * @since 5.3.23
	 */
	protected GenericWebApplicationContext createContext() {
		return new GenericWebApplicationContext();
	}

	/**
	 * Configures web resources for the supplied web application context (WAC).
	 * <h4>Implementation Details</h4>
	 * <p>If the supplied WAC has no parent or its parent is not a WAC, the
	 * supplied WAC will be configured as the Root WAC (see "<em>Root WAC
	 * Configuration</em>" below).
	 * <p>Otherwise the context hierarchy of the supplied WAC will be traversed
	 * to find the top-most WAC (i.e., the root); and the {@link ServletContext}
	 * of the Root WAC will be set as the {@code ServletContext} for the supplied
	 * WAC.
	 * <h4>Root WAC Configuration</h4>
	 * <ul>
	 * <li>The resource base path is retrieved from the supplied
	 * {@code WebMergedContextConfiguration}.</li>
	 * <li>A {@link ResourceLoader} is instantiated for the {@link MockServletContext}:
	 * if the resource base path is prefixed with "{@code classpath:}", a
	 * {@link DefaultResourceLoader} will be used; otherwise, a
	 * {@link FileSystemResourceLoader} will be used.</li>
	 * <li>A {@code MockServletContext} will be created using the resource base
	 * path and resource loader.</li>
	 * <li>The supplied {@link GenericWebApplicationContext} is then stored in
	 * the {@code MockServletContext} under the
	 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} key.</li>
	 * <li>Finally, the {@code MockServletContext} is set in the
	 * {@code WebApplicationContext}.</li>
	 * </ul>
	 * @param context the web application context for which to configure the web resources
	 * @param webMergedConfig the merged context configuration to use to load the web application context
	 */
	protected void configureWebResources(GenericWebApplicationContext context,
			WebMergedContextConfiguration webMergedConfig) {

		ApplicationContext parent = context.getParent();

		// If the WebApplicationContext has no parent or the parent is not a WebApplicationContext,
		// set the current context as the root WebApplicationContext:
		if (!(parent instanceof WebApplicationContext)) {
			String resourceBasePath = webMergedConfig.getResourceBasePath();
			ResourceLoader resourceLoader = (resourceBasePath.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX) ?
					new DefaultResourceLoader() : new FileSystemResourceLoader());
			ServletContext servletContext = new MockServletContext(resourceBasePath, resourceLoader);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
			context.setServletContext(servletContext);
		}
		else {
			ServletContext servletContext = null;
			// Find the root WebApplicationContext
			while (parent != null) {
				if (parent instanceof WebApplicationContext parentWac &&
						!(parent.getParent() instanceof WebApplicationContext)) {
					servletContext = parentWac.getServletContext();
					break;
				}
				parent = parent.getParent();
			}
			Assert.state(servletContext != null, "Failed to find root WebApplicationContext in the context hierarchy");
			context.setServletContext(servletContext);
		}
	}

	/**
	 * Customize the internal bean factory of the {@code WebApplicationContext}
	 * created by this context loader.
	 * <p>The default implementation is empty but can be overridden in subclasses
	 * to customize {@code DefaultListableBeanFactory}'s standard settings.
	 * @param beanFactory the bean factory created by this context loader
	 * @param webMergedConfig the merged context configuration to use to load the
	 * web application context
	 * @see #loadContext(MergedContextConfiguration)
	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see DefaultListableBeanFactory#setAllowEagerClassLoading
	 * @see DefaultListableBeanFactory#setAllowCircularReferences
	 * @see DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 */
	protected void customizeBeanFactory(
			DefaultListableBeanFactory beanFactory, WebMergedContextConfiguration webMergedConfig) {
	}

	/**
	 * Load bean definitions into the supplied {@link GenericWebApplicationContext context}
	 * from the locations or classes in the supplied {@code WebMergedContextConfiguration}.
	 * <p>Concrete subclasses must provide an appropriate implementation.
	 * @param context the context into which the bean definitions should be loaded
	 * @param webMergedConfig the merged context configuration to use to load the
	 * web application context
	 * @see #loadContext(MergedContextConfiguration)
	 */
	protected abstract void loadBeanDefinitions(
			GenericWebApplicationContext context, WebMergedContextConfiguration webMergedConfig);

	/**
	 * Customize the {@link GenericWebApplicationContext} created by this context
	 * loader <i>after</i> bean definitions have been loaded into the context but
	 * <i>before</i> the context is refreshed.
	 * <p>The default implementation simply delegates to
	 * {@link AbstractContextLoader#customizeContext(ConfigurableApplicationContext, MergedContextConfiguration)}.
	 * @param context the newly created web application context
	 * @param webMergedConfig the merged context configuration to use to load the
	 * web application context
	 * @see #loadContext(MergedContextConfiguration)
	 * @see #customizeContext(ConfigurableApplicationContext, MergedContextConfiguration)
	 */
	protected void customizeContext(
			GenericWebApplicationContext context, WebMergedContextConfiguration webMergedConfig) {

		super.customizeContext(context, webMergedConfig);
	}


	/**
	 * {@code AbstractGenericWebContextLoader} should be used as a
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * not as a legacy {@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * Consequently, this method is not supported.
	 * @throws UnsupportedOperationException in this implementation
	 * @see org.springframework.test.context.ContextLoader#loadContext(java.lang.String[])
	 */
	@Override
	@SuppressWarnings("deprecation")
	public final ApplicationContext loadContext(String... locations) throws Exception {
		throw new UnsupportedOperationException(
				"AbstractGenericWebContextLoader does not support the loadContext(String... locations) method");
	}

}
