/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.context.annotation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;

/**
 * Complete implementation of the
 * {@link org.springframework.beans.factory.support.AutowireCandidateResolver} strategy
 * interface, providing support for qualifier annotations as well as for lazy resolution
 * driven by the {@link Lazy} annotation in the {@code context.annotation} package.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ContextAnnotationAutowireCandidateResolver extends QualifierAnnotationAutowireCandidateResolver {

	@Override
	@Nullable
	public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
		return (isLazy(descriptor) ? buildLazyResolutionProxy(descriptor, beanName) : null);
	}

	@Override
	@Nullable
	public Class<?> getLazyResolutionProxyClass(DependencyDescriptor descriptor, @Nullable String beanName) {
		return (isLazy(descriptor) ? (Class<?>) buildLazyResolutionProxy(descriptor, beanName, true) : null);
	}

	protected boolean isLazy(DependencyDescriptor descriptor) {
		for (Annotation ann : descriptor.getAnnotations()) {
			Lazy lazy = AnnotationUtils.getAnnotation(ann, Lazy.class);
			if (lazy != null && lazy.value()) {
				return true;
			}
		}
		MethodParameter methodParam = descriptor.getMethodParameter();
		if (methodParam != null) {
			Method method = methodParam.getMethod();
			if (method == null || void.class == method.getReturnType()) {
				Lazy lazy = AnnotationUtils.getAnnotation(methodParam.getAnnotatedElement(), Lazy.class);
				if (lazy != null && lazy.value()) {
					return true;
				}
			}
		}
		return false;
	}

	protected Object buildLazyResolutionProxy(DependencyDescriptor descriptor, @Nullable String beanName) {
		return buildLazyResolutionProxy(descriptor, beanName, false);
	}

	private Object buildLazyResolutionProxy(
			DependencyDescriptor descriptor, @Nullable String beanName, boolean classOnly) {

		if (!(getBeanFactory() instanceof DefaultListableBeanFactory dlbf)) {
			throw new IllegalStateException("Lazy resolution only supported with DefaultListableBeanFactory");
		}

		TargetSource ts = new LazyDependencyTargetSource(dlbf, descriptor, beanName);

		ProxyFactory pf = new ProxyFactory();
		pf.setTargetSource(ts);
		Class<?> dependencyType = descriptor.getDependencyType();
		if (dependencyType.isInterface()) {
			pf.addInterface(dependencyType);
		}
		ClassLoader classLoader = dlbf.getBeanClassLoader();
		return (classOnly ? pf.getProxyClass(classLoader) : pf.getProxy(classLoader));
	}


	@SuppressWarnings("serial")
	private static class LazyDependencyTargetSource implements TargetSource, Serializable {

		private final DefaultListableBeanFactory beanFactory;

		private final DependencyDescriptor descriptor;

		@Nullable
		private final String beanName;

		@Nullable
		private transient volatile Object cachedTarget;

		public LazyDependencyTargetSource(DefaultListableBeanFactory beanFactory,
				DependencyDescriptor descriptor, @Nullable String beanName) {

			this.beanFactory = beanFactory;
			this.descriptor = descriptor;
			this.beanName = beanName;
		}

		@Override
		public Class<?> getTargetClass() {
			return this.descriptor.getDependencyType();
		}

		@Override
		@SuppressWarnings("NullAway")
		public Object getTarget() {
			Object cachedTarget = this.cachedTarget;
			if (cachedTarget != null) {
				return cachedTarget;
			}

			Set<String> autowiredBeanNames = new LinkedHashSet<>(2);
			Object target = this.beanFactory.doResolveDependency(
					this.descriptor, this.beanName, autowiredBeanNames, null);

			if (target == null) {
				Class<?> type = getTargetClass();
				if (Map.class == type) {
					target = Collections.emptyMap();
				}
				else if (List.class == type) {
					target = Collections.emptyList();
				}
				else if (Set.class == type || Collection.class == type) {
					target = Collections.emptySet();
				}
				else {
					throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType(),
							"Optional dependency not present for lazy injection point");
				}
			}
			else {
				if (target instanceof Map<?, ?> map && Map.class == getTargetClass()) {
					target = Collections.unmodifiableMap(map);
				}
				else if (target instanceof List<?> list && List.class == getTargetClass()) {
					target = Collections.unmodifiableList(list);
				}
				else if (target instanceof Set<?> set && Set.class == getTargetClass()) {
					target = Collections.unmodifiableSet(set);
				}
				else if (target instanceof Collection<?> coll && Collection.class == getTargetClass()) {
					target = Collections.unmodifiableCollection(coll);
				}
			}

			boolean cacheable = true;
			for (String autowiredBeanName : autowiredBeanNames) {
				if (!this.beanFactory.containsBean(autowiredBeanName)) {
					cacheable = false;
				}
				else {
					if (this.beanName != null) {
						this.beanFactory.registerDependentBean(autowiredBeanName, this.beanName);
					}
					if (!this.beanFactory.isSingleton(autowiredBeanName)) {
						cacheable = false;
					}
				}
				if (cacheable) {
					this.cachedTarget = target;
				}
			}

			return target;
		}
	}

}
