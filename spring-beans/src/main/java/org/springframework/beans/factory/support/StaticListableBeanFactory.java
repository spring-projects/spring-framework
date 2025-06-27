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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanIsNotAFactoryException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Static {@link org.springframework.beans.factory.BeanFactory} implementation
 * which allows one to register existing singleton instances programmatically.
 *
 * <p>Does not have support for prototype beans or aliases.
 *
 * <p>Serves as an example for a simple implementation of the
 * {@link org.springframework.beans.factory.ListableBeanFactory} interface,
 * managing existing bean instances rather than creating new ones based on bean
 * definitions, and not implementing any extended SPI interfaces (such as
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}).
 *
 * <p>For a full-fledged factory based on bean definitions, have a look at
 * {@link DefaultListableBeanFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 06.01.2003
 * @see DefaultListableBeanFactory
 */
public class StaticListableBeanFactory implements ListableBeanFactory {

	/** Map from bean name to bean instance. */
	private final Map<String, Object> beans;


	/**
	 * Create a regular {@code StaticListableBeanFactory}, to be populated
	 * with singleton bean instances through {@link #addBean} calls.
	 */
	public StaticListableBeanFactory() {
		this.beans = new LinkedHashMap<>();
	}

	/**
	 * Create a {@code StaticListableBeanFactory} wrapping the given {@code Map}.
	 * <p>Note that the given {@code Map} may be pre-populated with beans;
	 * or new, still allowing for beans to be registered via {@link #addBean};
	 * or {@link java.util.Collections#emptyMap()} for a dummy factory which
	 * enforces operating against an empty set of beans.
	 * @param beans a {@code Map} for holding this factory's beans, with the
	 * bean name as key and the corresponding singleton object as value
	 * @since 4.3
	 */
	public StaticListableBeanFactory(Map<String, Object> beans) {
		Assert.notNull(beans, "Beans Map must not be null");
		this.beans = beans;
	}


	/**
	 * Add a new singleton bean.
	 * <p>Will overwrite any existing instance for the given name.
	 * @param name the name of the bean
	 * @param bean the bean instance
	 */
	public void addBean(String name, Object bean) {
		this.beans.put(name, bean);
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		return getBean(name, (Class<?>) null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getBean(String name, @Nullable Class<T> requiredType) throws BeansException {
		String beanName = BeanFactoryUtils.transformedBeanName(name);
		Object bean = obtainBean(beanName);

		if (BeanFactoryUtils.isFactoryDereference(name)) {
			if (!(bean instanceof FactoryBean)) {
				throw new BeanIsNotAFactoryException(beanName, bean.getClass());
			}
		}
		else if (bean instanceof FactoryBean<?> factoryBean) {
			try {
				Object exposedObject =
						(factoryBean instanceof SmartFactoryBean<?> smartFactoryBean && requiredType != null ?
								smartFactoryBean.getObject(requiredType) : factoryBean.getObject());
				if (exposedObject == null) {
					throw new BeanCreationException(beanName, "FactoryBean exposed null object");
				}
				bean = exposedObject;
			}
			catch (Exception ex) {
				throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
			}
		}

		if (requiredType != null && !requiredType.isInstance(bean)) {
			throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
		}
		return (T) bean;
	}

	@Override
	public Object getBean(String name, @Nullable Object @Nullable ... args) throws BeansException {
		if (!ObjectUtils.isEmpty(args)) {
			throw new UnsupportedOperationException(
					"StaticListableBeanFactory does not support explicit bean creation arguments");
		}
		return getBean(name);
	}

	private Object obtainBean(String beanName) {
		Object bean = this.beans.get(beanName);
		if (bean == null) {
			throw new NoSuchBeanDefinitionException(beanName,
					"Defined beans are [" + StringUtils.collectionToCommaDelimitedString(this.beans.keySet()) + "]");
		}
		return bean;
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		String[] beanNames = getBeanNamesForType(requiredType);
		if (beanNames.length == 1) {
			return getBean(beanNames[0], requiredType);
		}
		else if (beanNames.length > 1) {
			throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
		}
		else {
			throw new NoSuchBeanDefinitionException(requiredType);
		}
	}

	@Override
	public <T> T getBean(Class<T> requiredType, @Nullable Object @Nullable ... args) throws BeansException {
		if (!ObjectUtils.isEmpty(args)) {
			throw new UnsupportedOperationException(
					"StaticListableBeanFactory does not support explicit bean creation arguments");
		}
		return getBean(requiredType);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) throws BeansException {
		return getBeanProvider(ResolvableType.forRawClass(requiredType), true);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		return getBeanProvider(requiredType, true);
	}

	@Override
	public boolean containsBean(String name) {
		return this.beans.containsKey(name);
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		String beanName = BeanFactoryUtils.transformedBeanName(name);
		Object bean = obtainBean(beanName);
		if (bean instanceof FactoryBean<?> factoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
			return factoryBean.isSingleton();
		}
		return true;
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		String beanName = BeanFactoryUtils.transformedBeanName(name);
		Object bean = obtainBean(beanName);
		return (!BeanFactoryUtils.isFactoryDereference(name) &&
				((bean instanceof SmartFactoryBean<?> smartFactoryBean && smartFactoryBean.isPrototype()) ||
				(bean instanceof FactoryBean<?> factoryBean && !factoryBean.isSingleton())));
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		String beanName = BeanFactoryUtils.transformedBeanName(name);
		Object bean = obtainBean(beanName);
		if (bean instanceof FactoryBean<?> factoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
			return isTypeMatch(factoryBean, typeToMatch.toClass());
		}
		return typeToMatch.isInstance(bean);
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		String beanName = BeanFactoryUtils.transformedBeanName(name);
		Object bean = obtainBean(beanName);
		if (bean instanceof FactoryBean<?> factoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
			return isTypeMatch(factoryBean, typeToMatch);
		}
		return typeToMatch.isInstance(bean);
	}

	private boolean isTypeMatch(FactoryBean<?> factoryBean, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		if (factoryBean instanceof SmartFactoryBean<?> smartFactoryBean) {
			return smartFactoryBean.supportsType(typeToMatch);
		}
		Class<?> objectType = factoryBean.getObjectType();
		return (objectType != null && typeToMatch.isAssignableFrom(objectType));
	}

	@Override
	public @Nullable Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return getType(name, true);
	}

	@Override
	public @Nullable Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
		String beanName = BeanFactoryUtils.transformedBeanName(name);
		Object bean = obtainBean(beanName);
		if (bean instanceof FactoryBean<?> factoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
			return factoryBean.getObjectType();
		}
		return bean.getClass();
	}

	@Override
	public String[] getAliases(String name) {
		return new String[0];
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String name) {
		return this.beans.containsKey(name);
	}

	@Override
	public int getBeanDefinitionCount() {
		return this.beans.size();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return StringUtils.toStringArray(this.beans.keySet());
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
		return getBeanProvider(ResolvableType.forRawClass(requiredType), allowEagerInit);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
		return new ObjectProvider<>() {
			@Override
			public T getObject() throws BeansException {
				String[] beanNames = getBeanNamesForType(requiredType);
				if (beanNames.length == 1) {
					return (T) getBean(beanNames[0], requiredType.toClass());
				}
				else if (beanNames.length > 1) {
					throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
				}
				else {
					throw new NoSuchBeanDefinitionException(requiredType);
				}
			}
			@Override
			public T getObject(@Nullable Object... args) throws BeansException {
				String[] beanNames = getBeanNamesForType(requiredType);
				if (beanNames.length == 1) {
					return (T) getBean(beanNames[0], args);
				}
				else if (beanNames.length > 1) {
					throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
				}
				else {
					throw new NoSuchBeanDefinitionException(requiredType);
				}
			}
			@Override
			public @Nullable T getIfAvailable() throws BeansException {
				String[] beanNames = getBeanNamesForType(requiredType);
				if (beanNames.length == 1) {
					return (T) getBean(beanNames[0], requiredType.toClass());
				}
				else if (beanNames.length > 1) {
					throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
				}
				else {
					return null;
				}
			}
			@Override
			public @Nullable T getIfUnique() throws BeansException {
				String[] beanNames = getBeanNamesForType(requiredType);
				if (beanNames.length == 1) {
					return (T) getBean(beanNames[0], requiredType.toClass());
				}
				else {
					return null;
				}
			}
			@Override
			public Stream<T> stream() {
				return Arrays.stream(getBeanNamesForType(requiredType))
						.map(name -> (T) getBean(name, requiredType.toClass()));
			}
		};
	}

	@Override
	public String[] getBeanNamesForType(@Nullable ResolvableType type) {
		return getBeanNamesForType(type, true, true);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable ResolvableType type,
			boolean includeNonSingletons, boolean allowEagerInit) {

		Class<?> resolved = (type != null ? type.resolve() : null);
		boolean isFactoryType = (resolved != null && FactoryBean.class.isAssignableFrom(resolved));
		List<String> matches = new ArrayList<>();

		for (Map.Entry<String, Object> entry : this.beans.entrySet()) {
			String beanName = entry.getKey();
			Object beanInstance = entry.getValue();
			if (beanInstance instanceof FactoryBean<?> factoryBean && !isFactoryType) {
				if ((includeNonSingletons || factoryBean.isSingleton()) &&
						(type == null || isTypeMatch(factoryBean, type.toClass()))) {
					matches.add(beanName);
				}
			}
			else {
				if (type == null || type.isInstance(beanInstance)) {
					matches.add(beanName);
				}
			}
		}
		return StringUtils.toStringArray(matches);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type) {
		return getBeanNamesForType(ResolvableType.forClass(type));
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		return getBeanNamesForType(ResolvableType.forClass(type), includeNonSingletons, allowEagerInit);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
		return getBeansOfType(type, true, true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		boolean isFactoryType = (type != null && FactoryBean.class.isAssignableFrom(type));
		Map<String, T> matches = new LinkedHashMap<>();

		for (Map.Entry<String, Object> entry : this.beans.entrySet()) {
			String beanName = entry.getKey();
			Object beanInstance = entry.getValue();
			if (beanInstance instanceof FactoryBean<?> factoryBean && !isFactoryType) {
				if ((includeNonSingletons || factoryBean.isSingleton()) &&
						(type == null || isTypeMatch(factoryBean, type))) {
					matches.put(beanName, getBean(beanName, type));
				}
			}
			else {
				if (type == null || type.isInstance(beanInstance)) {
					if (isFactoryType) {
						beanName = FACTORY_BEAN_PREFIX + beanName;
					}
					matches.put(beanName, (T) beanInstance);
				}
			}
		}
		return matches;
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		List<String> results = new ArrayList<>();
		for (String beanName : this.beans.keySet()) {
			if (findAnnotationOnBean(beanName, annotationType) != null) {
				results.add(beanName);
			}
		}
		return StringUtils.toStringArray(results);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {

		Map<String, Object> results = new LinkedHashMap<>();
		for (String beanName : this.beans.keySet()) {
			if (findAnnotationOnBean(beanName, annotationType) != null) {
				results.put(beanName, getBean(beanName));
			}
		}
		return results;
	}

	@Override
	public <A extends Annotation> @Nullable A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {

		return findAnnotationOnBean(beanName, annotationType, true);
	}

	@Override
	public <A extends Annotation> @Nullable A findAnnotationOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {

		Class<?> beanType = getType(beanName, allowFactoryBeanInit);
		return (beanType != null ? AnnotatedElementUtils.findMergedAnnotation(beanType, annotationType) : null);
	}

	@Override
	public <A extends Annotation> Set<A> findAllAnnotationsOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {

		Class<?> beanType = getType(beanName, allowFactoryBeanInit);
		return (beanType != null ?
				AnnotatedElementUtils.findAllMergedAnnotations(beanType, annotationType) : Collections.emptySet());
	}

}
