/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.factory.config;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;

/**
 * Adapter that implements all methods on {@link SmartInstantiationAwareBeanPostProcessor}
 * as no-ops, which will not change normal processing of each bean instantiated
 * by the container. Subclasses may override merely those methods that they are
 * actually interested in.
 *
 * <p>Note that this base class is only recommendable if you actually require
 * {@link InstantiationAwareBeanPostProcessor} functionality. If all you need
 * is plain {@link BeanPostProcessor} functionality, prefer a straight
 * implementation of that (simpler) interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class InstantiationAwareBeanPostProcessorAdapter implements SmartInstantiationAwareBeanPostProcessor {

	@Override
	public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
		return null;
	}

	@Override
	public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	@Override
	public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

		return pvs;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
