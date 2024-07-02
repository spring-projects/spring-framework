/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.lang.Nullable;

/**
 * Strategy interface for determining whether a specific bean definition
 * qualifies as an autowire candidate for a specific dependency.
 * 用于确定特定bean定义是否符合特定依赖项的自动连线候选条件的策略接口
 *
 * AutowireCandidateResolver 用来判断一个给定的 bean 是否可以注入，最主要的方法是 isAutowireCandidate
 * 简单来说 isAutowireCandidate 就根据 @Qualifier 添加过滤规则来判断 bean 是否合法
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 */
public interface AutowireCandidateResolver {

	/**
	 * Determine whether the given bean definition qualifies as an
	 * autowire candidate for the given dependency.
	 * <p>The default implementation checks
	 * {@link org.springframework.beans.factory.config.BeanDefinition#isAutowireCandidate()}.
	 * 判断给定的 bdHolder 是否可以注入 descriptor，BeanDefinition#autowireCandidate 默认为 true
	 * DependencyDescriptor 是对字段、方法、参数的封装，便于统一处理，这里一般是对属性写方法参数的封装
	 *
	 * @param bdHolder   the bean definition including bean name and aliases
	 * @param descriptor the descriptor for the target method parameter or field
	 * @return whether the bean definition qualifies as autowire candidate
	 * @see org.springframework.beans.factory.config.BeanDefinition#isAutowireCandidate()
	 */
	default boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		return bdHolder.getBeanDefinition().isAutowireCandidate();
	}

	/**
	 * Determine whether the given descriptor is effectively required.
	 * <p>The default implementation checks {@link DependencyDescriptor#isRequired()}.
	 * 判断是否必须注入，如果是字段类型是 Optional 或有 @Null 注解时为 false
	 *
	 * @param descriptor the descriptor for the target method parameter or field
	 * @return whether the descriptor is marked as required or possibly indicating
	 * non-required status some other way (e.g. through a parameter annotation)
	 * @see DependencyDescriptor#isRequired()
	 * @since 5.0
	 */
	default boolean isRequired(DependencyDescriptor descriptor) {
		return descriptor.isRequired();
	}

	/**
	 * Determine whether the given descriptor declares a qualifier beyond the type
	 * (typically - but not necessarily - a specific kind of annotation).
	 * <p>The default implementation returns {@code false}.
	 * 判断是否有 @Qualifier(Spring 或 JDK) 或自定义的注解
	 *
	 * @param descriptor the descriptor for the target method parameter or field
	 * @return whether the descriptor declares a qualifier, narrowing the candidate
	 * status beyond the type match
	 * @see org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver#hasQualifier
	 * @since 5.1
	 */
	default boolean hasQualifier(DependencyDescriptor descriptor) {
		return false;
	}

	/**
	 * Determine whether a default value is suggested for the given dependency.
	 * <p>The default implementation simply returns {@code null}.
	 * 确定是否建议为给定的依赖项使用默认值
	 * 默认实现只返回{@code null}
	 *
	 * @param descriptor the descriptor for the target method parameter or field
	 * @return the value suggested (typically an expression String),
	 * or {@code null} if none found
	 * @since 3.0
	 */
	@Nullable
	default Object getSuggestedValue(DependencyDescriptor descriptor) {
		return null;
	}

	/**
	 * Build a proxy for lazy resolution of the actual dependency target,
	 * if demanded by the injection point.
	 * <p>The default implementation simply returns {@code null}.
	 * 如果注入点需要，则为实际依赖目标的延迟解析构建一个代理
	 * 默认实现只返回｛@code null｝。
	 * 如有必要，获取懒惰解析代理
	 *
	 * @param descriptor the descriptor for the target method parameter or field
	 * @param beanName   the name of the bean that contains the injection point
	 * @return the lazy resolution proxy for the actual dependency target,
	 * or {@code null} if straight resolution is to be performed
	 * @since 4.0
	 */
	@Nullable
	default Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
		return null;
	}

	/**
	 * Return a clone of this resolver instance if necessary, retaining its local
	 * configuration and allowing for the cloned instance to get associated with
	 * a new bean factory, or this original instance if there is no such state.
	 * <p>The default implementation creates a separate instance via the default
	 * class constructor, assuming no specific configuration state to copy.
	 * Subclasses may override this with custom configuration state handling
	 * or with standard {@link Cloneable} support (as implemented by Spring's
	 * own configurable {@code AutowireCandidateResolver} variants), or simply
	 * return {@code this} (as in {@link SimpleAutowireCandidateResolver}).
	 *
	 * @see GenericTypeAwareAutowireCandidateResolver#cloneIfNecessary()
	 * @see DefaultListableBeanFactory#copyConfigurationFrom
	 * @since 5.2.7
	 */
	default AutowireCandidateResolver cloneIfNecessary() {
		return BeanUtils.instantiateClass(getClass());
	}

}
