/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * implementation that passes the ServletContext to beans that implement
 * the {@link ServletContextAware} interface.
 *
 * <p>Web application contexts will automatically register this with their
 * underlying bean factory. Applications do not use this directly.
 *
 * @author Juergen Hoeller
 * @since 12.03.2004
 * @see org.springframework.web.context.ServletContextAware
 * @see org.springframework.web.context.support.XmlWebApplicationContext#postProcessBeanFactory
 */
public class ServletContextAwareProcessor implements BeanPostProcessor {

	private ServletContext servletContext;

	private ServletConfig servletConfig;


	/**
	 * Create a new ServletContextAwareProcessor for the given context.
	 */
	public ServletContextAwareProcessor(ServletContext servletContext) {
		this(servletContext, null);
	}

	/**
	 * Create a new ServletContextAwareProcessor for the given config.
	 */
	public ServletContextAwareProcessor(ServletConfig servletConfig) {
		this(null, servletConfig);
	}

	/**
	 * Create a new ServletContextAwareProcessor for the given context and config.
	 */
	public ServletContextAwareProcessor(ServletContext servletContext, ServletConfig servletConfig) {
		this.servletContext = servletContext;
		this.servletConfig = servletConfig;
		if (servletContext == null && servletConfig != null) {
			this.servletContext = servletConfig.getServletContext();
		}
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (this.servletContext != null && bean instanceof ServletContextAware) {
			((ServletContextAware) bean).setServletContext(this.servletContext);
		}
		if (this.servletConfig != null && bean instanceof ServletConfigAware) {
			((ServletConfigAware) bean).setServletConfig(this.servletConfig);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		return bean;
	}

}
