/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory;

import org.springframework.lang.Nullable;

/**
 * Sub-interface implemented by bean factories that can be part
 * of a hierarchy.
 *
 * <p>The corresponding {@code setParentBeanFactory} method for bean
 * factories that allow setting the parent in a configurable
 * fashion can be found in the ConfigurableBeanFactory interface.
 *
 * <p>层级bean工厂(HierarchicalBeanFactory)
 * <p>由bean工厂实现的子接口, 可以是层次结构的一部分
 * <p>可以在ConfigurableBeanFactory接口中找到允许以可配置方式设置父级的bean工厂的相应{@code setParentBeanFactory}方法。
 * <p>继承自BeanFactory, 也就是在BeanFactory定义的功能基础上增加对parentFactory的支持
 * <p>实现了Bean工厂的分层。这个工厂接口也是继承自BeanFactory，也是一个二级接口，相对于父接口，它只扩展了一个重要的功能: 工厂分层
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setParentBeanFactory
 * @since 07.07.2003
 */
public interface HierarchicalBeanFactory extends BeanFactory {

	/**
	 * Return the parent bean factory, or {@code null} if there is none.
	 * 获取父级 BeanFactory
	 */
	@Nullable
	BeanFactory getParentBeanFactory();

	/**
	 * Return whether the local bean factory contains a bean of the given name,
	 * ignoring beans defined in ancestor contexts.
	 * <p>This is an alternative to {@code containsBean}, ignoring a bean
	 * of the given name from an ancestor bean factory.
	 *
	 * <p>返回本地bean工厂是否包含给定名称的bean，忽略祖先上下文中定义的bean
	 * <p>这是{@code containsBean}的替代方案，忽略祖先bean工厂中给定名称的bean。
	 * <p>跟 BeanFactory#containsBean 类似, 该方法只检索当前 BeanFactory, 而不会层级检索
	 * <p>本地工厂(容器)是否包含这个Bean
	 *
	 * @param name the name of the bean to query
	 * @return whether a bean with the given name is defined in the local factory
	 * @see BeanFactory#containsBean
	 */
	boolean containsLocalBean(String name);

}
