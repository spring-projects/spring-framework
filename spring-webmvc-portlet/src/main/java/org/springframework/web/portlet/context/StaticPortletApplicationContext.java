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

package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ServletContextAwareProcessor;

/**
 * Static Portlet-based {@link org.springframework.context.ApplicationContext}
 * implementation for testing. Not intended for use in production applications.
 *
 * <p>Implements the
 * {@link org.springframework.web.portlet.context.ConfigurablePortletApplicationContext}
 * interface to allow for direct replacement of an {@link XmlPortletApplicationContext},
 * despite not actually supporting external configuration files.
 *
 * <p>Interprets resource paths as portlet context resources, that is, as paths
 * beneath the portlet application root. Absolute paths, for example for files
 * outside the portlet app root, can be accessed via "file:" URLs, as implemented
 * by {@link org.springframework.core.io.DefaultResourceLoader}.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.0
 */
public class StaticPortletApplicationContext extends StaticApplicationContext
		implements ConfigurablePortletApplicationContext {

	private ServletContext servletContext;

	private PortletContext portletContext;

	private PortletConfig portletConfig;

	private String namespace;


	public StaticPortletApplicationContext() {
		setDisplayName("Root Portlet ApplicationContext");
	}


	/**
	 * Return a new {@link StandardPortletEnvironment}
	 */
	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardPortletEnvironment();
	}

	/**
	 * {@inheritDoc}
	 * <p>Replace {@code Portlet}- and {@code Servlet}-related property sources.
	 */
	@Override
	protected void initPropertySources() {
		PortletApplicationContextUtils.initPortletPropertySources(getEnvironment().getPropertySources(),
				this.servletContext, this.portletContext, this.portletConfig);
	}

	/**
	 * {@inheritDoc}
	 * <p>The parent {@linkplain #getEnvironment() environment} is
	 * delegated to this (child) context if the parent is a
	 * {@link org.springframework.context.ConfigurableApplicationContext} implementation.
	 * <p>The parent {@linkplain #getServletContext() servlet context} is
	 * delegated to this (child) context if the parent is a {@link WebApplicationContext}
	 * implementation.
	 */
	@Override
	public void setParent(ApplicationContext parent) {
		super.setParent(parent);
		if (parent instanceof WebApplicationContext) {
			this.servletContext = ((WebApplicationContext) parent).getServletContext();
		}
	}

	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	@Override
	public void setPortletContext(PortletContext portletContext) {
		this.portletContext = portletContext;
	}

	@Override
	public PortletContext getPortletContext() {
		return this.portletContext;
	}

	@Override
	public void setPortletConfig(PortletConfig portletConfig) {
		this.portletConfig = portletConfig;
		if (portletConfig != null && this.portletContext == null) {
			this.portletContext = portletConfig.getPortletContext();
		}
	}

	@Override
	public PortletConfig getPortletConfig() {
		return this.portletConfig;
	}

	@Override
	public void setNamespace(String namespace) {
		this.namespace = namespace;
		if (namespace != null) {
			setDisplayName("Portlet ApplicationContext for namespace '" + namespace + "'");
		}
	}

	@Override
	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * The {@link StaticPortletApplicationContext} class does not support this method.
	 * @throws UnsupportedOperationException <b>always</b>
	 */
	@Override
	public void setConfigLocation(String configLocation) {
		if (configLocation != null) {
			throw new UnsupportedOperationException("StaticPortletApplicationContext does not support config locations");
		}
	}

	/**
	 * The {@link StaticPortletApplicationContext} class does not support this method.
	 * @throws UnsupportedOperationException <b>always</b>
	 */
	@Override
	public void setConfigLocations(String[] configLocations) {
		if (configLocations != null) {
			throw new UnsupportedOperationException("StaticPortletApplicationContext does not support config locations");
		}
	}

	@Override
	public String[] getConfigLocations() {
		return null;
	}


	/**
	 * Register request/session scopes, a {@link PortletContextAwareProcessor}, etc.
	 */
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext));
		beanFactory.addBeanPostProcessor(new PortletContextAwareProcessor(this.portletContext, this.portletConfig));
		beanFactory.ignoreDependencyInterface(PortletContextAware.class);
		beanFactory.ignoreDependencyInterface(PortletConfigAware.class);

		PortletApplicationContextUtils.registerPortletApplicationScopes(beanFactory, this.portletContext);
		PortletApplicationContextUtils.registerEnvironmentBeans(
				beanFactory, this.servletContext, this.portletContext, this.portletConfig);
	}

	/**
	 * This implementation supports file paths beneath the root of the PortletContext.
	 * @see PortletContextResource
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		return new PortletContextResource(this.portletContext, path);
	}

	/**
	 * This implementation supports pattern matching in unexpanded WARs too.
	 * @see PortletContextResourcePatternResolver
	 */
	@Override
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new PortletContextResourcePatternResolver(this);
	}

}
