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

package org.springframework.beans.factory;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;

/**
 * Used in {@link BeanRegistrar#register(BeanRegistry, Environment)} to expose
 * programmatic bean registration capabilities.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
public interface BeanRegistry {

	/**
	 * Register beans using the given {@link BeanRegistrar}.
	 * @param registrar the bean registrar that will be called to register
	 * additional beans
	 */
	void register(BeanRegistrar registrar);

	/**
	 * Given a name, register an alias for it.
	 * @param name the canonical name
	 * @param alias the alias to be registered
	 * @throws IllegalStateException if the alias is already in use
	 * and may not be overridden
	 */
	void registerAlias(String name, String alias);

	/**
	 * Register a bean from the given bean class, which will be instantiated using the
	 * related {@link BeanUtils#getResolvableConstructor resolvable constructor} if any.
	 * @param beanClass the class of the bean
	 * @return the generated bean name
	 */
	<T> String registerBean(Class<T> beanClass);

	/**
	 * Register a bean from the given bean class, customizing it with the customizer
	 * callback. The bean will be instantiated using the supplier that can be configured
	 * in the customizer callback, or will be tentatively instantiated with its
	 * {@link BeanUtils#getResolvableConstructor resolvable constructor} otherwise.
	 * @param beanClass the class of the bean
	 * @param customizer callback to customize other bean properties than the name
	 * @return the generated bean name
	 */
	<T> String registerBean(Class<T> beanClass, Consumer<Spec<T>> customizer);

	/**
	 * Register a bean from the given bean class, which will be instantiated using the
	 * related {@link BeanUtils#getResolvableConstructor resolvable constructor} if any.
	 * @param name the name of the bean
	 * @param beanClass the class of the bean
	 */
	<T> void registerBean(String name, Class<T> beanClass);

	/**
	 * Register a bean from the given bean class, customizing it with the customizer
	 * callback. The bean will be instantiated using the supplier that can be configured
	 * in the customizer callback, or will be tentatively instantiated with its
	 * {@link BeanUtils#getResolvableConstructor resolvable constructor} otherwise.
	 * @param name the name of the bean
	 * @param beanClass the class of the bean
	 * @param customizer callback to customize other bean properties than the name
	 */
	<T> void registerBean(String name, Class<T> beanClass, Consumer<Spec<T>> customizer);


	/**
	 * Specification for customizing a bean.
	 * @param <T> the bean type
	 */
	interface Spec<T> {

		/**
		 * Allow for instantiating this bean on a background thread.
		 * @see AbstractBeanDefinition#setBackgroundInit(boolean)
		 */
		Spec<T> backgroundInit();

		/**
		 * Set a human-readable description of this bean.
		 * @see BeanDefinition#setDescription(String)
		 */
		Spec<T> description(String description);

		/**
		 * Configure this bean as a fallback autowire candidate.
		 * @see BeanDefinition#setFallback(boolean)
		 * @see #primary
		 */
		Spec<T> fallback();

		/**
		 * Hint that this bean has an infrastructure role, meaning it has no relevance
		 * to the end-user.
		 * @see BeanDefinition#setRole(int)
		 * @see BeanDefinition#ROLE_INFRASTRUCTURE
		 */
		Spec<T> infrastructure();

		/**
		 * Configure this bean as lazily initialized.
		 * @see BeanDefinition#setLazyInit(boolean)
		 */
		Spec<T> lazyInit();

		/**
		 * Configure this bean as not a candidate for getting autowired into another bean.
		 * @see BeanDefinition#setAutowireCandidate(boolean)
		 */
		Spec<T> notAutowirable();

		/**
		 * The sort order of this bean. This is analogous to the
		 * {@code @Order} annotation.
		 * @see AbstractBeanDefinition#ORDER_ATTRIBUTE
		 */
		Spec<T> order(int order);

		/**
		 * Configure this bean as a primary autowire candidate.
		 * @see BeanDefinition#setPrimary(boolean)
		 * @see #fallback
		 */
		Spec<T> primary();

		/**
		 * Configure this bean with a prototype scope.
		 * @see BeanDefinition#setScope(String)
		 * @see BeanDefinition#SCOPE_PROTOTYPE
		 */
		Spec<T> prototype();

		/**
		 * Set the supplier to construct a bean instance.
		 * @see AbstractBeanDefinition#setInstanceSupplier(Supplier)
		 */
		Spec<T> supplier(Function<SupplierContext, T> supplier);

		/**
		 * Set a generics-containing target type of this bean.
		 * @see #targetType(ResolvableType)
		 * @see RootBeanDefinition#setTargetType(ResolvableType)
		 */
		Spec<T> targetType(ParameterizedTypeReference<? extends T> type);

		/**
		 * Set a generics-containing target type of this bean.
		 * @see #targetType(ParameterizedTypeReference)
		 * @see RootBeanDefinition#setTargetType(ResolvableType)
		 */
		Spec<T> targetType(ResolvableType type);
	}


	/**
	 * Context available from the bean instance supplier designed to give access
	 * to bean dependencies.
	 */
	interface SupplierContext {

		/**
		 * Return the bean instance that uniquely matches the given object type, if any.
		 * @param requiredType type the bean must match; can be an interface or superclass
		 * @return an instance of the single bean matching the required type
		 * @see BeanFactory#getBean(String)
		 */
		<T> T bean(Class<T> requiredType) throws BeansException;

		/**
		 * Return an instance, which may be shared or independent, of the
		 * specified bean.
		 * @param name the name of the bean to retrieve
		 * @param requiredType type the bean must match; can be an interface or superclass
		 * @return an instance of the bean.
		 * @see BeanFactory#getBean(String, Class)
		 */
		<T> T bean(String name, Class<T> requiredType) throws BeansException;

		/**
		 * Return a provider for the specified bean, allowing for lazy on-demand retrieval
		 * of instances, including availability and uniqueness options.
		 * <p>For matching a generic type, consider {@link #beanProvider(ResolvableType)}.
		 * @param requiredType type the bean must match; can be an interface or superclass
		 * @return a corresponding provider handle
		 * @see BeanFactory#getBeanProvider(Class)
		 */
		<T> ObjectProvider<T> beanProvider(Class<T> requiredType);

		/**
		 * Return a provider for the specified bean, allowing for lazy on-demand retrieval
		 * of instances, including availability and uniqueness options. This variant allows
		 * for specifying a generic type to match, similar to reflective injection points
		 * with generic type declarations in method/constructor parameters.
		 * <p>Note that collections of beans are not supported here, in contrast to reflective
		 * injection points. For programmatically retrieving a list of beans matching a
		 * specific type, specify the actual bean type as an argument here and subsequently
		 * use {@link ObjectProvider#orderedStream()} or its lazy streaming/iteration options.
		 * <p>Also, generics matching is strict here, as per the Java assignment rules.
		 * For lenient fallback matching with unchecked semantics (similar to the 'unchecked'
		 * Java compiler warning), consider calling {@link #beanProvider(Class)} with the
		 * raw type as a second step if no full generic match is
		 * {@link ObjectProvider#getIfAvailable() available} with this variant.
		 * @param requiredType type the bean must match; can be a generic type declaration
		 * @return a corresponding provider handle
		 * @see BeanFactory#getBeanProvider(ResolvableType)
		 */
		<T> ObjectProvider<T> beanProvider(ResolvableType requiredType);
	}

}
