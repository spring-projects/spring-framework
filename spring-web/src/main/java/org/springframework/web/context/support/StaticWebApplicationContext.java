/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.context.support;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

/**
 * Static {@link org.springframework.web.context.WebApplicationContext}
 * implementation for testing. Not intended for use in production applications.
 *
 * <p>Implements the {@link org.springframework.web.context.ConfigurableWebApplicationContext}
 * interface to allow for direct replacement of an {@link XmlWebApplicationContext},
 * despite not actually supporting external configuration files.
 *
 * <p>Interprets resource paths as servlet context resources, i.e. as paths beneath
 * the web application root. Absolute paths, for example, for files outside the web app root,
 * can be accessed via "file:" URLs, as implemented by
 * {@link org.springframework.core.io.DefaultResourceLoader}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class StaticWebApplicationContext extends StaticApplicationContext
		implements ConfigurableWebApplicationContext {

	private @Nullable ServletContext servletContext;

	private @Nullable ServletConfig servletConfig;

	private @Nullable String namespace;


	public StaticWebApplicationContext() {
		setDisplayName("Root WebApplicationContext");
	}


	/**
	 * Set the ServletContext that this WebApplicationContext runs in.
	 */
	@Override
	public void setServletContext(@Nullable ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public @Nullable ServletContext getServletContext() {
		return this.servletContext;
	}

	@Override
	public void setServletConfig(@Nullable ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
		if (servletConfig != null && this.servletContext == null) {
			this.servletContext = servletConfig.getServletContext();
		}
	}

	@Override
	public @Nullable ServletConfig getServletConfig() {
		return this.servletConfig;
	}

	@Override
	public void setNamespace(@Nullable String namespace) {
		this.namespace = namespace;
		if (namespace != null) {
			setDisplayName("WebApplicationContext for namespace '" + namespace + "'");
		}
	}

	@Override
	public @Nullable String getNamespace() {
		return this.namespace;
	}

	/**
	 * The {@link StaticWebApplicationContext} class does not support this method.
	 * @throws UnsupportedOperationException <b>always</b>
	 */
	@Override
	public void setConfigLocation(String configLocation) {
		throw new UnsupportedOperationException("StaticWebApplicationContext does not support config locations");
	}

	/**
	 * The {@link StaticWebApplicationContext} class does not support this method.
	 * @throws UnsupportedOperationException <b>always</b>
	 */
	@Override
	public void setConfigLocations(String... configLocations) {
		throw new UnsupportedOperationException("StaticWebApplicationContext does not support config locations");
	}

	@Override
	public String @Nullable [] getConfigLocations() {
		return null;
	}


	/**
	 * Register request/session scopes, a {@link ServletContextAwareProcessor}, etc.
	 */
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext, this.servletConfig));
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
		beanFactory.ignoreDependencyInterface(ServletConfigAware.class);

		WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
		WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext, this.servletConfig);
	}

	/**
	 * This implementation supports file paths beneath the root of the ServletContext.
	 * @see ServletContextResource
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		Assert.state(this.servletContext != null, "No ServletContext available");
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
	 * Create and return a new {@link StandardServletEnvironment}.
	 */
	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	@Override
	protected void initPropertySources() {
		WebApplicationContextUtils.initServletPropertySources(getEnvironment().getPropertySources(),
				this.servletContext, this.servletConfig);
	}

}
