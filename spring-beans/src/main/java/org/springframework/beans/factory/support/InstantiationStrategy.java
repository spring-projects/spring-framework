/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;

/**
 * Interface responsible for creating instances corresponding to a root bean definition.
 *
 * <p>This is pulled out into a strategy as various approaches are possible,
 * including using CGLIB to create subclasses on the fly to support Method Injection.
 *
 * @author Rod Johnson
 * @since 1.1
 */
public interface InstantiationStrategy {

	/**
	 * Return an instance of the bean with the given name in this factory.
	 * @param beanDefinition the bean definition
	 * @param beanName name of the bean when it's created in this context.
	 * The name can be {@code null} if we're autowiring a bean that
	 * doesn't belong to the factory.
	 * @param owner owning BeanFactory
	 * @return a bean instance for this bean definition
	 * @throws BeansException if the instantiation failed
	 */
	Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner)
			throws BeansException;

	/**
	 * Return an instance of the bean with the given name in this factory,
	 * creating it via the given constructor.
	 * @param beanDefinition the bean definition
	 * @param beanName name of the bean when it's created in this context.
	 * The name can be {@code null} if we're autowiring a bean
	 * that doesn't belong to the factory.
	 * @param owner owning BeanFactory
	 * @param ctor the constructor to use
	 * @param args the constructor arguments to apply
	 * @return a bean instance for this bean definition
	 * @throws BeansException if the instantiation failed
	 */
	Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner,
			Constructor<?> ctor, Object[] args) throws BeansException;

	/**
	 * Return an instance of the bean with the given name in this factory,
	 * creating it via the given factory method.
	 * @param beanDefinition bean definition
	 * @param beanName name of the bean when it's created in this context.
	 * The name can be {@code null} if we're autowiring a bean
	 * that doesn't belong to the factory.
	 * @param owner owning BeanFactory
	 * @param factoryBean the factory bean instance to call the factory method on,
	 * or {@code null} in case of a static factory method
	 * @param factoryMethod the factory method to use
	 * @param args the factory method arguments to apply
	 * @return a bean instance for this bean definition
	 * @throws BeansException if the instantiation failed
	 */
	Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner,
			Object factoryBean, Method factoryMethod, Object[] args) throws BeansException;

}
