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

package org.springframework.context.support;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisherAware;
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
 * @since 10.10.2003
 * @see org.springframework.context.ResourceLoaderAware
 * @see org.springframework.context.MessageSourceAware
 * @see org.springframework.context.ApplicationEventPublisherAware
 * @see org.springframework.context.ApplicationContextAware
 * @see org.springframework.context.support.AbstractApplicationContext#refresh()
 */
class ApplicationContextAwareProcessor implements BeanPostProcessor {

	private final ApplicationContext applicationContext;


	/**
	 * Create a new ApplicationContextAwareProcessor for the given context.
	 */
	public ApplicationContextAwareProcessor(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	public Object postProcessBeforeInitialization(final Object bean, String beanName) throws BeansException {
		AccessControlContext acc = null;
		
		if (System.getSecurityManager() != null) {
			if (applicationContext instanceof ConfigurableApplicationContext) {
				ConfigurableListableBeanFactory factory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
				if (factory instanceof AbstractBeanFactory) {
					acc = ((AbstractBeanFactory) factory).getSecurityContextProvider().getAccessControlContext();
				}
			}
			// optimize - check the bean class before creating the inner class + native call
			if (bean instanceof ResourceLoaderAware || bean instanceof ApplicationEventPublisherAware 
					|| bean instanceof MessageSourceAware || bean instanceof ApplicationContextAware) {
				AccessController.doPrivileged(new PrivilegedAction<Object>() {
					public Object run() {
						doProcess(bean);
						return null;
					}
				}, acc);
			}
		}
		else {
			doProcess(bean);
		}
		
		return bean;
	}
	
	private void doProcess(Object bean) {
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
	}

	public Object postProcessAfterInitialization(Object bean, String name) {
		return bean;
	}
}