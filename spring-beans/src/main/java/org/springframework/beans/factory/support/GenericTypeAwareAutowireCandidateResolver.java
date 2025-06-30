/*
 * Copyright 2002-present the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Properties;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

/**
 * Basic {@link AutowireCandidateResolver} that performs a full generic type
 * match with the candidate's type if the dependency is declared as a generic type
 * (for example, {@code Repository<Customer>}).
 *
 * <p>This is the base class for
 * {@link org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver},
 * providing an implementation all non-annotation-based resolution steps at this level.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class GenericTypeAwareAutowireCandidateResolver extends SimpleAutowireCandidateResolver
		implements BeanFactoryAware, Cloneable {

	private @Nullable BeanFactory beanFactory;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	protected final @Nullable BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
	public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		if (!super.isAutowireCandidate(bdHolder, descriptor)) {
			// If explicitly false, do not proceed with any other checks...
			return false;
		}
		return checkGenericTypeMatch(bdHolder, descriptor);
	}

	/**
	 * Match the given dependency type with its generic type information against the given
	 * candidate bean definition.
	 */
	@SuppressWarnings("NullAway") // Dataflow analysis limitation
	protected boolean checkGenericTypeMatch(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		ResolvableType dependencyType = descriptor.getResolvableType();
		if (dependencyType.getType() instanceof Class) {
			// No generic type -> we know it's a Class type-match, so no need to check again.
			return true;
		}

		ResolvableType targetType = null;
		boolean cacheType = false;
		RootBeanDefinition rbd = null;
		if (bdHolder.getBeanDefinition() instanceof RootBeanDefinition rootBeanDef) {
			rbd = rootBeanDef;
		}
		if (rbd != null) {
			targetType = rbd.targetType;
			if (targetType == null) {
				cacheType = true;
				// First, check factory method return type, if applicable
				targetType = getReturnTypeForFactoryMethod(rbd, descriptor);
				if (targetType == null) {
					RootBeanDefinition dbd = getResolvedDecoratedDefinition(rbd);
					if (dbd != null) {
						targetType = dbd.targetType;
						if (targetType == null) {
							targetType = getReturnTypeForFactoryMethod(dbd, descriptor);
						}
					}
				}
			}
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
		if (cacheType) {
			rbd.targetType = targetType;
		}

		// Pre-declared target type: In case of a generic FactoryBean type,
		// unwrap nested generic type when matching a non-FactoryBean type.
		Class<?> targetClass = targetType.resolve();
		if (targetClass != null && FactoryBean.class.isAssignableFrom(targetClass)) {
			Class<?> classToMatch = dependencyType.resolve();
			if (classToMatch != null && !FactoryBean.class.isAssignableFrom(classToMatch) &&
					!classToMatch.isAssignableFrom(targetClass)) {
				targetType = targetType.getGeneric();
				if (descriptor.fallbackMatchAllowed()) {
					// Matching the Class-based type determination for FactoryBean
					// objects in the lazy-determination getType code path above.
					targetType = ResolvableType.forClass(targetType.resolve());
				}
			}
		}

		if (descriptor.fallbackMatchAllowed()) {
			// Fallback matches allow unresolvable generics, for example, plain HashMap to Map<String,String>;
			// and pragmatically also java.util.Properties to any Map (since despite formally being a
			// Map<Object,Object>, java.util.Properties is usually perceived as a Map<String,String>).
			if (targetType.hasUnresolvableGenerics()) {
				return dependencyType.isAssignableFromResolvedPart(targetType);
			}
			else if (targetType.resolve() == Properties.class) {
				return true;
			}
		}
		// Full check for complex generic type match...
		return dependencyType.isAssignableFrom(targetType);
	}

	protected @Nullable RootBeanDefinition getResolvedDecoratedDefinition(RootBeanDefinition rbd) {
		BeanDefinitionHolder decDef = rbd.getDecoratedDefinition();
		if (decDef != null && this.beanFactory instanceof ConfigurableListableBeanFactory clbf) {
			if (clbf.containsBeanDefinition(decDef.getBeanName())) {
				BeanDefinition dbd = clbf.getMergedBeanDefinition(decDef.getBeanName());
				if (dbd instanceof RootBeanDefinition rootBeanDef) {
					return rootBeanDef;
				}
			}
		}
		return null;
	}

	protected @Nullable ResolvableType getReturnTypeForFactoryMethod(RootBeanDefinition rbd, DependencyDescriptor descriptor) {
		// Should typically be set for any kind of factory method, since the BeanFactory
		// pre-resolves them before reaching out to the AutowireCandidateResolver...
		ResolvableType returnType = rbd.factoryMethodReturnType;
		if (returnType == null) {
			Method factoryMethod = rbd.getResolvedFactoryMethod();
			if (factoryMethod != null) {
				returnType = ResolvableType.forMethodReturnType(factoryMethod);
			}
		}
		if (returnType != null) {
			Class<?> resolvedClass = returnType.resolve();
			if (resolvedClass != null && descriptor.getDependencyType().isAssignableFrom(resolvedClass)) {
				// Only use factory method metadata if the return type is actually expressive enough
				// for our dependency. Otherwise, the returned instance type may have matched instead
				// in case of a singleton instance having been registered with the container already.
				return returnType;
			}
		}
		return null;
	}


	/**
	 * This implementation clones all instance fields through standard
	 * {@link Cloneable} support, allowing for subsequent reconfiguration
	 * of the cloned instance through a fresh {@link #setBeanFactory} call.
	 * @see #clone()
	 */
	@Override
	public AutowireCandidateResolver cloneIfNecessary() {
		try {
			return (AutowireCandidateResolver) clone();
		}
		catch (CloneNotSupportedException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
