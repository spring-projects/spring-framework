/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.aop.scope;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.lang.Nullable;

/**
 * Utility class for creating a scoped proxy.
 * Used by ScopedProxyBeanDefinitionDecorator and ClassPathBeanDefinitionScanner.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.5
 */
public abstract class ScopedProxyUtils {

	private static final String TARGET_NAME_PREFIX = "scopedTarget.";


	/**
	 * Generate a scoped proxy for the supplied target bean, registering the target
	 * bean with an internal name and setting 'targetBeanName' on the scoped proxy.
	 *
	 * 为提供的目标bean生成一个作用域代理，用一个内部名称注册目标bean，并在作用域代理上设置'targetBeanName'。
	 *
	 * @param definition the original bean definition
	 * @param registry the bean definition registry
	 * @param proxyTargetClass whether to create a target class proxy
	 * @return the scoped proxy definition
	 */
	public static BeanDefinitionHolder createScopedProxy(BeanDefinitionHolder definition,
			BeanDefinitionRegistry registry, boolean proxyTargetClass) {

		//获得beanName
		String originalBeanName = definition.getBeanName();
		//从BeanDefinitionHolder对象中获得BeanDefinition
		BeanDefinition targetDefinition = definition.getBeanDefinition();
		//targetBeanName = “scopedTarget.”+ beanName
		String targetBeanName = getTargetBeanName(originalBeanName);

		// Create a scoped proxy definition for the original bean name,
		// "hiding" the target bean in an internal target definition.

		//为原始bean名称创建一个范围代理定义，
		//将目标bean隐藏在内部目标定义中。
		//最终都使用RootBeanDefinition对象
		RootBeanDefinition proxyDefinition = new RootBeanDefinition(ScopedProxyFactoryBean.class);
		proxyDefinition.setDecoratedDefinition(new BeanDefinitionHolder(targetDefinition, targetBeanName));
		//设置proxyDefinition的代理对象
		proxyDefinition.setOriginatingBeanDefinition(targetDefinition);
		//设置proxyDefinition的原对象
		proxyDefinition.setSource(definition.getSource());
		//设置proxyDefinition的
		proxyDefinition.setRole(targetDefinition.getRole());

		//设置proxyDefinition的键值对中设置，"targetBeanName" : targetBeanName
		proxyDefinition.getPropertyValues().add("targetBeanName", targetBeanName);
		if (proxyTargetClass) {
			//如果是cglib的代理模式
			//设置属性“org.springframework.aop.framework.autoproxy.AutoProxyUtils.preserveTargetClass”：true
			targetDefinition.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
			// ScopedProxyFactoryBean's "proxyTargetClass" default is TRUE, so we don't need to set it explicitly here.
			// ScopedProxyFactoryBean的“proxyTargetClass”默认值为TRUE，所以我们不需要在这里明确地设置它。
		}
		else {
			//代理对象设置属性"proxyTargetClass"：false
			proxyDefinition.getPropertyValues().add("proxyTargetClass", Boolean.FALSE);
		}

		// Copy autowire settings from original bean definition.
		//从原始bean定义复制autowire设置。
		proxyDefinition.setAutowireCandidate(targetDefinition.isAutowireCandidate());
		proxyDefinition.setPrimary(targetDefinition.isPrimary());
		if (targetDefinition instanceof AbstractBeanDefinition) {
			proxyDefinition.copyQualifiersFrom((AbstractBeanDefinition) targetDefinition);
		}

		// The target bean should be ignored in favor of the scoped proxy.
		//目标bean应该被忽略以支持作用域代理。
		targetDefinition.setAutowireCandidate(false);
		targetDefinition.setPrimary(false);

		// Register the target bean as separate bean in the factory.
		//在工厂中将目标bean注册为单独的bean。
		registry.registerBeanDefinition(targetBeanName, targetDefinition);

		// Return the scoped proxy definition as primary bean definition
		// (potentially an inner bean).
		//返回作用域代理定义作为主bean定义
		//（可能是一个内部bean）。
		return new BeanDefinitionHolder(proxyDefinition, originalBeanName, definition.getAliases());
	}

	/**
	 * Generate the bean name that is used within the scoped proxy to reference the target bean.
	 *
	 * 生成在作用域代理中使用的bean名称以引用目标bean。
	 *
	 * @param originalBeanName the original name of bean
	 * @return the generated bean to be used to reference the target bean
	 */
	public static String getTargetBeanName(String originalBeanName) {
		return TARGET_NAME_PREFIX + originalBeanName;
	}

	/**
	 * Specify if the {@code beanName} is the name of a bean that references the target
	 * bean within a scoped proxy.
	 * @since 4.1.4
	 */
	public static boolean isScopedTarget(@Nullable String beanName) {
		return (beanName != null && beanName.startsWith(TARGET_NAME_PREFIX));
	}

}
