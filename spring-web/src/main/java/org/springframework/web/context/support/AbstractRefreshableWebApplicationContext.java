/*
 * Copyright 2002-2025 the original author or authors.
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
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link org.springframework.context.support.AbstractRefreshableApplicationContext}
 * subclass which implements the
 * {@link org.springframework.web.context.ConfigurableWebApplicationContext}
 * interface for web environments. Provides a "configLocations" property,
 * to be populated through the ConfigurableWebApplicationContext interface
 * on web application startup.
 *
 * <p>This class is as easy to subclass as AbstractRefreshableApplicationContext:
 * All you need to implement is the {@link #loadBeanDefinitions} method;
 * see the superclass javadoc for details. Note that implementations are supposed
 * to load bean definitions from the files specified by the locations returned
 * by the {@link #getConfigLocations} method.
 *
 * <p>Interprets resource paths as servlet context resources, i.e. as paths beneath
 * the web application root. Absolute paths, for example, for files outside the web app root,
 * can be accessed via "file:" URLs, as implemented by
 * {@link org.springframework.core.io.DefaultResourceLoader}.
 *
 * <p><b>This is the web context to be subclassed for a different bean definition format.</b>
 * Such a context implementation can be specified as "contextClass" context-param
 * for {@link org.springframework.web.context.ContextLoader} or as "contextClass"
 * init-param for {@link org.springframework.web.servlet.FrameworkServlet},
 * replacing the default {@link XmlWebApplicationContext}. It will then automatically
 * receive the "contextConfigLocation" context-param or init-param, respectively.
 *
 * <p>Note that WebApplicationContext implementations are generally supposed
 * to configure themselves based on the configuration received through the
 * {@link ConfigurableWebApplicationContext} interface. In contrast, a standalone
 * application context might allow for configuration in custom startup code
 * (for example, {@link org.springframework.context.support.GenericApplicationContext}).
 *
 * @author Juergen Hoeller
 * @since 1.1.3
 * @see #loadBeanDefinitions
 * @see org.springframework.web.context.ConfigurableWebApplicationContext#setConfigLocations
 * @see XmlWebApplicationContext
 */
@SuppressWarnings("deprecation")
public abstract class AbstractRefreshableWebApplicationContext extends AbstractRefreshableConfigApplicationContext
		implements ConfigurableWebApplicationContext {

	/** Servlet context that this context runs in. */
	private @Nullable ServletContext servletContext;

	/** Servlet config that this context runs in, if any. */
	private @Nullable ServletConfig servletConfig;

	/** Namespace of this context, or {@code null} if root. */
	private @Nullable String namespace;


	public AbstractRefreshableWebApplicationContext() {
		setDisplayName("Root WebApplicationContext");
	}


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
			setServletContext(servletConfig.getServletContext());
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

	@Override
	public String @Nullable [] getConfigLocations() {
		return super.getConfigLocations();
	}

	@Override
	public String getApplicationName() {
		return (this.servletContext != null ? this.servletContext.getContextPath() : "");
	}

	/**
	 * Create and return a new {@link StandardServletEnvironment}. Subclasses may override
	 * in order to configure the environment or specialize the environment type returned.
	 */
	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
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
	 * {@inheritDoc}
	 * <p>Replace {@code Servlet}-related property sources.
	 */
	@Override
	protected void initPropertySources() {
		ConfigurableEnvironment env = getEnvironment();
		if (env instanceof ConfigurableWebEnvironment configurableWebEnv) {
			configurableWebEnv.initPropertySources(this.servletContext, this.servletConfig);
		}
	}

}
