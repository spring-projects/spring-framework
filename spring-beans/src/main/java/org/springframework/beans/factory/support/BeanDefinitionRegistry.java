/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.AliasRegistry;

/**
 * Interface for registries that hold bean definitions, for example RootBeanDefinition
 * and ChildBeanDefinition instances. Typically implemented by BeanFactories that
 * internally work with the AbstractBeanDefinition hierarchy.
 *
 * <p>This is the only interface in Spring's bean factory packages that encapsulates
 * <i>registration</i> of bean definitions. The standard BeanFactory interfaces
 * only cover access to a <i>fully configured factory instance</i>.
 *
 * <p>Spring's bean definition readers expect to work on an implementation of this
 * interface. Known implementors within the Spring core are DefaultListableBeanFactory
 * and GenericApplicationContext.
 *
 * <p>bean定义注册表(BeanDefinitionRegistry)
 * <p>用于保存bean定义的注册表的接口，例如RootBeanDefinition和ChildBeanDefinition实例。
 * 通常由内部使用AbstractBeanDefinition层次结构的BeanFactory实现
 * <p>这是Spring的bean工厂包中唯一封装了bean定义的<i>注册<i>的接口。标准BeanFactory接口仅涵盖对完全配置的<i>工厂实例<i>的访问
 * <p>Spring的bean定义读者希望开发此接口的实现。Spring核心中已知的实现者是DefaultListableBeanFactory和GenericApplicationContext。
 * <p>对BeanDefinition进行管理, 对bean定义的增删改功能
 *
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.config.BeanDefinition
 * @see AbstractBeanDefinition
 * @see RootBeanDefinition
 * @see ChildBeanDefinition
 * @see DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 * @see PropertiesBeanDefinitionReader
 * @since 26.11.2003
 */
public interface BeanDefinitionRegistry extends AliasRegistry {

	/**
	 * Register a new bean definition with this registry.
	 * Must support RootBeanDefinition and ChildBeanDefinition.
	 * <p>在此注册表中注册新的bean定义。必须支持RootBeanDefinition和ChildBeanDefinition。
	 *
	 * @param beanName       the name of the bean instance to register
	 * @param beanDefinition definition of the bean instance to register
	 * @throws BeanDefinitionStoreException    if the BeanDefinition is invalid
	 * @throws BeanDefinitionOverrideException if there is already a BeanDefinition
	 *                                         for the specified bean name and we are not allowed to override it
	 * @see GenericBeanDefinition
	 * @see RootBeanDefinition
	 * @see ChildBeanDefinition
	 */
	void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException;

	/**
	 * Remove the BeanDefinition for the given name.
	 * <p>删除给定名称的BeanDefinition
	 *
	 * @param beanName the name of the bean instance to register
	 * @throws NoSuchBeanDefinitionException if there is no such bean definition
	 */
	void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * Return the BeanDefinition for the given bean name.
	 * <p>返回给定bean名称的BeanDefinition
	 *
	 * @param beanName name of the bean to find a definition for
	 * @return the BeanDefinition for the given name (never {@code null})
	 * @throws NoSuchBeanDefinitionException if there is no such bean definition
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * Check if this registry contains a bean definition with the given name.
	 * <p>检查此注册表是否包含具有给定名称的bean定义
	 *
	 * @param beanName the name of the bean to look for
	 * @return if this registry contains a bean definition with the given name
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * Return the names of all beans defined in this registry.
	 * <p>返回此注册表中定义的所有bean的名称
	 *
	 * @return the names of all beans defined in this registry,
	 * or an empty array if none defined
	 */
	String[] getBeanDefinitionNames();

	/**
	 * Return the number of beans defined in the registry.
	 * <p>返回注册表中定义的bean数量
	 *
	 * @return the number of beans defined in the registry
	 */
	int getBeanDefinitionCount();

	/**
	 * Determine whether the given bean name is already in use within this registry,
	 * i.e. whether there is a local bean or alias registered under this name.
	 * <p>确定给定的bean名称是否已在此注册表中使用，即是否有在此名称下注册的本地bean或别名。
	 *
	 * @param beanName the name to check
	 * @return whether the given bean name is already in use
	 */
	boolean isBeanNameInUse(String beanName);

}
