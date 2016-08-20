/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * that applies initialization and destruction callbacks to beans that
 * implement the {@link javax.servlet.Servlet} interface.
 *
 * <p>After initialization of the bean instance, the Servlet {@code init}
 * method will be called with a ServletConfig that contains the bean name
 * of the Servlet and the ServletContext that it is running in.
 *
 * <p>Before destruction of the bean instance, the Servlet {@code destroy}
 * will be called.
 *
 * <p><b>Note that this post-processor does not support Servlet initialization
 * parameters.</b> Bean instances that implement the Servlet interface are
 * supposed to be configured like any other Spring bean, that is, through
 * constructor arguments or bean properties.
 *
 * <p>For reuse of a Servlet implementation in a plain Servlet container
 * and as a bean in a Spring context, consider deriving from Spring's
 * {@link org.springframework.web.servlet.HttpServletBean} base class that
 * applies Servlet initialization parameters as bean properties, supporting
 * both the standard Servlet and the Spring bean initialization style.
 *
 * <p><b>Alternatively, consider wrapping a Servlet with Spring's
 * {@link org.springframework.web.servlet.mvc.ServletWrappingController}.</b>
 * This is particularly appropriate for existing Servlet classes,
 * allowing to specify Servlet initialization parameters etc.
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
 * @see javax.servlet.Servlet#destroy()
 * @see SimpleServletHandlerAdapter
 */
public class SimpleServletPostProcessor implements
		DestructionAwareBeanPostProcessor, ServletContextAware, ServletConfigAware {

	private boolean useSharedServletConfig = true;

	private ServletContext servletContext;

	private ServletConfig servletConfig;


	/**
	 * Set whether to use the shared ServletConfig object passed in
	 * through {@code setServletConfig}, if available.
	 * <p>Default is "true". Turn this setting to "false" to pass in
	 * a mock ServletConfig object with the bean name as servlet name,
	 * holding the current ServletContext.
	 * @see #setServletConfig
	 */
	public void setUseSharedServletConfig(boolean useSharedServletConfig) {
		this.useSharedServletConfig = useSharedServletConfig;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void setServletConfig(ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof Servlet) {
			ServletConfig config = this.servletConfig;
			if (config == null || !this.useSharedServletConfig) {
				config = new DelegatingServletConfig(beanName, this.servletContext);
			}
			try {
				((Servlet) bean).init(config);
			}
			catch (ServletException ex) {
				throw new BeanInitializationException("Servlet.init threw exception", ex);
			}
		}
		return bean;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		if (bean instanceof Servlet) {
			((Servlet) bean).destroy();
		}
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return (bean instanceof Servlet);
	}


	/**
	 * Internal implementation of the {@link ServletConfig} interface,
	 * to be passed to the wrapped servlet.
	 */
	private static class DelegatingServletConfig implements ServletConfig {

		private final String servletName;

		private final ServletContext servletContext;

		public DelegatingServletConfig(String servletName, ServletContext servletContext) {
			this.servletName = servletName;
			this.servletContext = servletContext;
		}

		@Override
		public String getServletName() {
			return this.servletName;
		}

		@Override
		public ServletContext getServletContext() {
			return this.servletContext;
		}

		@Override
		public String getInitParameter(String paramName) {
			return null;
		}

		@Override
		public Enumeration<String> getInitParameterNames() {
			return Collections.enumeration(Collections.<String>emptySet());
		}
	}

}
