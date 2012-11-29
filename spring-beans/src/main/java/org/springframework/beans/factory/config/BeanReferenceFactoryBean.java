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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartFactoryBean;

/**
 * FactoryBean that exposes an arbitrary target bean under a different name.
 *
 * <p>Usually, the target bean will reside in a different bean definition file,
 * using this FactoryBean to link it in and expose it under a different name.
 * Effectively, this corresponds to an alias for the target bean.
 *
 * <p><b>NOTE:</b> For XML bean definition files, an <code>&lt;alias&gt;</code>
 * tag is available that effectively achieves the same.
 *
 * <p>A special capability of this FactoryBean is enabled through its configuration
 * as bean definition: The "targetBeanName" can be substituted through a placeholder,
 * in combination with Spring's {@link PropertyPlaceholderConfigurer}.
 * Thanks to Marcus Bristav for pointing this out!
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setTargetBeanName
 * @see PropertyPlaceholderConfigurer
 * @deprecated as of Spring 3.2, in favor of using regular bean name aliases
 * (which support placeholder parsing since Spring 2.5)
 */
@Deprecated
public class BeanReferenceFactoryBean implements SmartFactoryBean, BeanFactoryAware {

	private String targetBeanName;

	private BeanFactory beanFactory;


	/**
	 * Set the name of the target bean.
	 * <p>This property is required. The value for this property can be
	 * substituted through a placeholder, in combination with Spring's
	 * PropertyPlaceholderConfigurer.
	 * @param targetBeanName the name of the target bean
	 * @see PropertyPlaceholderConfigurer
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		if (this.targetBeanName == null) {
			throw new IllegalArgumentException("'targetBeanName' is required");
		}
		if (!this.beanFactory.containsBean(this.targetBeanName)) {
			throw new NoSuchBeanDefinitionException(this.targetBeanName, this.beanFactory.toString());
		}
	}


	public Object getObject() throws BeansException {
		if (this.beanFactory == null) {
			throw new FactoryBeanNotInitializedException();
		}
		return this.beanFactory.getBean(this.targetBeanName);
	}

	public Class getObjectType() {
		if (this.beanFactory == null) {
			return null;
		}
		return this.beanFactory.getType(this.targetBeanName);
	}

	public boolean isSingleton() {
		if (this.beanFactory == null) {
			throw new FactoryBeanNotInitializedException();
		}
		return this.beanFactory.isSingleton(this.targetBeanName);
	}

	public boolean isPrototype() {
		if (this.beanFactory == null) {
			throw new FactoryBeanNotInitializedException();
		}
		return this.beanFactory.isPrototype(this.targetBeanName);
	}

	public boolean isEagerInit() {
		return false;
	}

}
