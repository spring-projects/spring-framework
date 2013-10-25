/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

/**
 * Basic {@link AutowireCandidateResolver} that performs a full generic type
 * match with the candidate's type if the dependency is declared as a generic type
 * (e.g. Repository&lt;Customer&gt;).
 *
 * <p>This is the base class for
 * {@link org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver},
 * providing an implementation all non-annotation-based resolution steps at this level.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class GenericTypeAwareAutowireCandidateResolver implements AutowireCandidateResolver, BeanFactoryAware {

	private BeanFactory beanFactory;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	protected final BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
	public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		if (!bdHolder.getBeanDefinition().isAutowireCandidate()) {
			// if explicitly false, do not proceed with any other checks
			return false;
		}
		return (descriptor == null || checkGenericTypeMatch(bdHolder, descriptor));
	}

	/**
	 * Match the given dependency type with its generic type information
	 * against the given candidate bean definition.
	 */
	protected boolean checkGenericTypeMatch(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		ResolvableType dependencyType = descriptor.getResolvableType();
		if (!dependencyType.hasGenerics()) {
			// No generic type -> we know it's a Class type-match, so no need to check again.
			return true;
		}
		ResolvableType targetType = null;
		RootBeanDefinition rbd = null;
		if (bdHolder.getBeanDefinition() instanceof RootBeanDefinition) {
			rbd = (RootBeanDefinition) bdHolder.getBeanDefinition();
		}
		if (rbd != null && rbd.getResolvedFactoryMethod() != null) {
			// Should typically be set for any kind of factory method, since the BeanFactory
			// pre-resolves them before reaching out to the AutowireCandidateResolver...
			targetType = ResolvableType.forMethodReturnType(rbd.getResolvedFactoryMethod());
		}
		if (targetType == null) {
			// Regular case: straight bean instance, with BeanFactory available.
			if (this.beanFactory != null) {
				Class<?> beanType = this.beanFactory.getType(bdHolder.getBeanName());
				if (beanType != null) {
					targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanType));
				}
			}
			// Fallback: no BeanFactory set, or no type resolvable through it
			// -> best-effort match against the target class if applicable.
			if (targetType == null && rbd != null && rbd.hasBeanClass() && rbd.getFactoryMethodName() == null) {
				Class<?> beanClass = rbd.getBeanClass();
				if (!FactoryBean.class.isAssignableFrom(beanClass)) {
					targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanClass));
				}
			}
		}
		if (targetType == null) {
			return true;
		}
		if (descriptor.fallbackMatchAllowed() && targetType.hasUnresolvableGenerics()) {
			return descriptor.getDependencyType().isAssignableFrom(targetType.getRawClass());
		}
		return dependencyType.isAssignableFrom(targetType);
	}


	/**
	 * This implementation always returns {@code null},
	 * leaving suggested value support up to subclasses.
	 */
	@Override
	public Object getSuggestedValue(DependencyDescriptor descriptor) {
		return null;
	}

	/**
	 * This implementation always returns {@code null},
	 * leaving lazy resolution support up to subclasses.
	 */
	@Override
	public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, String beanName) {
		return null;
	}

}
