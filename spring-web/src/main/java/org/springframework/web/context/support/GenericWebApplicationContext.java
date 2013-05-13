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

package org.springframework.web.context.support;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ServletContextAware;

/**
 * Subclass of {@link GenericApplicationContext}, suitable for web environments.
 *
 * <p>Implements the
 * {@link org.springframework.web.context.ConfigurableWebApplicationContext},
 * but is not intended for declarative setup in {@code web.xml}. Instead,
 * it is designed for programmatic setup, for example for building nested contexts or
 * for use within Spring 3.1 {@link org.springframework.web.WebApplicationInitializer}s.
 *
 * <p><b>If you intend to implement a WebApplicationContext that reads bean definitions
 * from configuration files, consider deriving from AbstractRefreshableWebApplicationContext,
 * reading the bean definitions in an implementation of the {@code loadBeanDefinitions}
 * method.</b>
 *
 * <p>Interprets resource paths as servlet context resources, i.e. as paths beneath
 * the web application root. Absolute paths, e.g. for files outside the web app root,
 * can be accessed via "file:" URLs, as implemented by AbstractApplicationContext.
 *
 * <p>In addition to the special beans detected by
 * {@link org.springframework.context.support.AbstractApplicationContext},
 * this class detects a ThemeSource bean in the context, with the name "themeSource".
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 1.2
 */
public class GenericWebApplicationContext extends GenericApplicationContext
		implements ConfigurableWebApplicationContext, ThemeSource {

	private ServletContext servletContext;

	private ThemeSource themeSource;


	/**
	 * Create a new GenericWebApplicationContext.
	 * @see #setServletContext
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericWebApplicationContext() {
		super();
	}

	/**
	 * Create a new GenericWebApplicationContext for the given ServletContext.
	 * @param servletContext the ServletContext to run in
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericWebApplicationContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * Create a new GenericWebApplicationContext with the given DefaultListableBeanFactory.
	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
	 * @see #setServletContext
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericWebApplicationContext(DefaultListableBeanFactory beanFactory) {
		super(beanFactory);
	}

	/**
	 * Create a new GenericWebApplicationContext with the given DefaultListableBeanFactory.
	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
	 * @param servletContext the ServletContext to run in
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericWebApplicationContext(DefaultListableBeanFactory beanFactory, ServletContext servletContext) {
		super(beanFactory);
		this.servletContext = servletContext;
	}


	/**
	 * Set the ServletContext that this WebApplicationContext runs in.
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	@Override
	public String getApplicationName() {
		if (this.servletContext == null) {
			return "";
		}
		if (this.servletContext.getMajorVersion() == 2 && this.servletContext.getMinorVersion() < 5) {
			String name = this.servletContext.getServletContextName();
			return (name != null ? name : "");
		}
		else {
			// Servlet 2.5 available
			return this.servletContext.getContextPath();
		}
	}

	/**
	 * Create and return a new {@link StandardServletEnvironment}.
	 */
	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * Register ServletContextAwareProcessor.
	 * @see ServletContextAwareProcessor
	 */
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext));
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);

		WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
		WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext);
	}

	/**
	 * This implementation supports file paths beneath the root of the ServletContext.
	 * @see ServletContextResource
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		return new ServletContextResource(this.servletContext, path);
	}

	/**
	 * This implementation supports pattern matching in unexpanded WARs too.
	 * @see ServletContextResourcePatternResolver
	 */
	@Override
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new ServletContextResourcePatternResolver(this);
	}

	/**
	 * Initialize the theme capability.
	 */
	@Override
	protected void onRefresh() {
		this.themeSource = UiApplicationContextUtils.initThemeSource(this);
	}

	/**
	 * {@inheritDoc}
	 * <p>Replace {@code Servlet}-related property sources.
	 */
	@Override
	protected void initPropertySources() {
		super.initPropertySources();
		ConfigurableEnvironment env = this.getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment)env).initPropertySources(
					this.servletContext, null);
		}
	}

	@Override
	public Theme getTheme(String themeName) {
		return this.themeSource.getTheme(themeName);
	}


	// ---------------------------------------------------------------------
	// Pseudo-implementation of ConfigurableWebApplicationContext
	// ---------------------------------------------------------------------

	@Override
	public void setServletConfig(ServletConfig servletConfig) {
		// no-op
	}

	@Override
	public ServletConfig getServletConfig() {
		throw new UnsupportedOperationException(
				"GenericWebApplicationContext does not support getServletConfig()");
	}

	@Override
	public void setNamespace(String namespace) {
		// no-op
	}

	@Override
	public String getNamespace() {
		throw new UnsupportedOperationException(
				"GenericWebApplicationContext does not support getNamespace()");
	}

	@Override
	public void setConfigLocation(String configLocation) {
		if (StringUtils.hasText(configLocation)) {
			throw new UnsupportedOperationException(
					"GenericWebApplicationContext does not support setConfigLocation(). " +
					"Do you still have an 'contextConfigLocations' init-param set?");
		}
	}

	@Override
	public void setConfigLocations(String[] configLocations) {
		if (!ObjectUtils.isEmpty(configLocations)) {
			throw new UnsupportedOperationException(
					"GenericWebApplicationContext does not support setConfigLocations(). " +
					"Do you still have an 'contextConfigLocations' init-param set?");
		}
	}

	@Override
	public String[] getConfigLocations() {
		throw new UnsupportedOperationException(
				"GenericWebApplicationContext does not support getConfigLocations()");
	}

}
