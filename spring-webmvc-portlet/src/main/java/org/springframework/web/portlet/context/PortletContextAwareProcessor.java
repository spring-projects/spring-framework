/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * implementation that passes the PortletContext to beans that implement
 * the {@link PortletContextAware} interface.
 *
 * <p>Portlet application contexts will automatically register this with their
 * underlying bean factory. Applications do not use this directly.
 *
 * @author Juergen Hoeller
 * @author John A. Lewis
 * @since 2.0
 * @see org.springframework.web.portlet.context.PortletContextAware
 * @see org.springframework.web.portlet.context.XmlPortletApplicationContext#postProcessBeanFactory
 */
public class PortletContextAwareProcessor implements BeanPostProcessor {

	private PortletContext portletContext;

	private PortletConfig portletConfig;


	/**
	 * Create a new PortletContextAwareProcessor for the given context.
	 */
	public PortletContextAwareProcessor(PortletContext portletContext) {
		this(portletContext, null);
	}

	/**
	 * Create a new PortletContextAwareProcessor for the given config.
	 */
	public PortletContextAwareProcessor(PortletConfig portletConfig) {
		this(null, portletConfig);
	}

	/**
	 * Create a new PortletContextAwareProcessor for the given context and config.
	 */
	public PortletContextAwareProcessor(PortletContext portletContext, PortletConfig portletConfig) {
		this.portletContext = portletContext;
		this.portletConfig = portletConfig;
		if (portletContext == null && portletConfig != null) {
			this.portletContext = portletConfig.getPortletContext();
		}
	}


	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (this.portletContext != null && bean instanceof PortletContextAware) {
			((PortletContextAware) bean).setPortletContext(this.portletContext);
		}
		if (this.portletConfig != null && bean instanceof PortletConfigAware) {
			((PortletConfigAware) bean).setPortletConfig(this.portletConfig);
		}
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) {
		return bean;
	}

}
