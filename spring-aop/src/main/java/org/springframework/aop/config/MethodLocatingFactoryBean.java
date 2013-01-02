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

package org.springframework.aop.config;

import java.lang.reflect.Method;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.StringUtils;

/**
 * {@link FactoryBean} implementation that locates a {@link Method} on a specified bean.
 *
 * @author Rob Harrop
 * @since 2.0
 */
public class MethodLocatingFactoryBean implements FactoryBean<Method>, BeanFactoryAware {

	private String targetBeanName;

	private String methodName;

	private Method method;


	/**
	 * Set the name of the bean to locate the {@link Method} on.
	 * <p>This property is required.
	 * @param targetBeanName the name of the bean to locate the {@link Method} on
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	/**
	 * Set the name of the {@link Method} to locate.
	 * <p>This property is required.
	 * @param methodName the name of the {@link Method} to locate
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		if (!StringUtils.hasText(this.targetBeanName)) {
			throw new IllegalArgumentException("Property 'targetBeanName' is required");
		}
		if (!StringUtils.hasText(this.methodName)) {
			throw new IllegalArgumentException("Property 'methodName' is required");
		}

		Class beanClass = beanFactory.getType(this.targetBeanName);
		if (beanClass == null) {
			throw new IllegalArgumentException("Can't determine type of bean with name '" + this.targetBeanName + "'");
		}
		this.method = BeanUtils.resolveSignature(this.methodName, beanClass);

		if (this.method == null) {
			throw new IllegalArgumentException("Unable to locate method [" + this.methodName +
					"] on bean [" + this.targetBeanName + "]");
		}
	}


	public Method getObject() throws Exception {
		return this.method;
	}

	public Class<Method> getObjectType() {
		return Method.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
