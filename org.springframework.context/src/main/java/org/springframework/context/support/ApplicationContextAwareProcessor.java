/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.context.support;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.ResourceLoaderAware;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * implementation that passes the ApplicationContext to beans that
 * implement the {@link ResourceLoaderAware}, {@link MessageSourceAware},
 * {@link ApplicationEventPublisherAware} and/or
 * {@link ApplicationContextAware} interfaces.
 * If all of them are implemented, they are satisfied in the given order.
 *
 * <p>Application contexts will automatically register this with their
 * underlying bean factory. Applications do not use this directly.
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 10.10.2003
 * @see org.springframework.context.ResourceLoaderAware
 * @see org.springframework.context.MessageSourceAware
 * @see org.springframework.context.ApplicationEventPublisherAware
 * @see org.springframework.context.ApplicationContextAware
 * @see org.springframework.context.support.AbstractApplicationContext#refresh()
 */
class ApplicationContextAwareProcessor implements MergedBeanDefinitionPostProcessor {

	private final Log logger = LogFactory.getLog(getClass());

	private final ConfigurableApplicationContext applicationContext;

	private final Map<String, Boolean> singletonNames = new ConcurrentHashMap<String, Boolean>();


	/**
	 * Create a new ApplicationContextAwareProcessor for the given context.
	 */
	public ApplicationContextAwareProcessor(ConfigurableApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class beanType, String beanName) {
		if (!this.applicationContext.containsBean(beanName) && beanDefinition.isSingleton()) {
			this.singletonNames.put(beanName, Boolean.TRUE);
		}
	}

	public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
		AccessControlContext acc = null;

		if (System.getSecurityManager() != null &&
				(bean instanceof ResourceLoaderAware || bean instanceof ApplicationEventPublisherAware ||
						bean instanceof MessageSourceAware || bean instanceof ApplicationContextAware)) {
			acc = this.applicationContext.getBeanFactory().getAccessControlContext();
		}

		if (acc != null) {
			AccessController.doPrivileged(new PrivilegedAction<Object>() {
				public Object run() {
					doProcess(bean, beanName);
					return null;
				}
			}, acc);
		}
		else {
			doProcess(bean, beanName);
		}
		
		return bean;
	}
	
	private void doProcess(Object bean, String beanName) {
		if (bean instanceof ResourceLoaderAware) {
			((ResourceLoaderAware) bean).setResourceLoader(this.applicationContext);
		}
		if (bean instanceof ApplicationEventPublisherAware) {
			((ApplicationEventPublisherAware) bean).setApplicationEventPublisher(this.applicationContext);
		}
		if (bean instanceof MessageSourceAware) {
			((MessageSourceAware) bean).setMessageSource(this.applicationContext);
		}
		if (bean instanceof ApplicationContextAware) {
			((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);
		}

		if (bean instanceof ApplicationListener) {
			if (!this.applicationContext.containsBean(beanName)) {
				// not a top-level bean - not detected as a listener by getBeanNamesForType retrieval
				Boolean flag = this.singletonNames.get(beanName);
				if (Boolean.TRUE.equals(flag)) {
					// inner singleton bean: register on the fly
					this.applicationContext.addApplicationListener((ApplicationListener) bean);
				}
				else if (flag == null) {
					// inner bean with other scope - can't reliably process events
					if (logger.isWarnEnabled()) {
						logger.warn("Inner bean '" + beanName + "' implements ApplicationListener interface " +
								"but is not reachable for event multicasting by its containing ApplicationContext " +
								"because it does not have singleton scope. Only top-level listener beans are allowed " +
								"to be of non-singleton scope.");
					}
					this.singletonNames.put(beanName, Boolean.FALSE);
				}
			}
		}
	}

	public Object postProcessAfterInitialization(Object bean, String name) {
		return bean;
	}

}
