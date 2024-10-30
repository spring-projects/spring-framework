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

import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Post-processor callback interface for <i>merged</i> bean definitions at runtime.
 * {@link BeanPostProcessor} implementations may implement this sub-interface in order
 * to post-process the merged bean definition (a processed copy of the original bean
 * definition) that the Spring {@code BeanFactory} uses to create a bean instance.
 *
 * <p>The {@link #postProcessMergedBeanDefinition} method may for example introspect
 * the bean definition in order to prepare some cached metadata before post-processing
 * actual instances of a bean. It is also allowed to modify the bean definition but
 * <i>only</i> for definition properties which are actually intended for concurrent
 * modification. Essentially, this only applies to operations defined on the
 * {@link RootBeanDefinition} itself but not to the properties of its base classes.
 *
 * <p>合并的Bean定义后处理器(postProcessMergedBeanDefinition)
 *
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getMergedBeanDefinition
 * @since 2.5
 */
public interface MergedBeanDefinitionPostProcessor extends BeanPostProcessor {

	/**
	 * Post-process the given merged bean definition for the specified bean.
	 *
	 * @param beanDefinition the merged bean definition for the bean
	 * @param beanType       the actual type of the managed bean instance
	 * @param beanName       the name of the bean
	 * @see AbstractAutowireCapableBeanFactory#applyMergedBeanDefinitionPostProcessors
	 */
	void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName);

	/**
	 * A notification that the bean definition for the specified name has been reset,
	 * and that this post-processor should clear any metadata for the affected bean.
	 * <p>The default implementation is empty.
	 *
	 * <p>重置bean定义(resetBeanDefinition)
	 * <p>通知指定名称的bean定义已被重置，并且此后处理器应清除受影响bean的所有元数据。
	 * <p>默认实现为空
	 *
	 * @param beanName the name of the bean
	 * @see DefaultListableBeanFactory#resetBeanDefinition
	 * @since 5.1
	 */
	default void resetBeanDefinition(String beanName) {
	}

}
