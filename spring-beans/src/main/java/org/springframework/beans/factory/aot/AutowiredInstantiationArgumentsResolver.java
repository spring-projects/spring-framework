/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.aot;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionValueResolver;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.CollectionFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.function.ThrowingFunction;

/**
 * Resolver used to support the autowiring of constructors or factory methods.
 * Typically used in AOT-processed applications as a targeted alternative to the
 * reflection based injection.
 * <p>
 * When resolving arguments in a native image, the {@link Constructor} or
 * {@link Method} being used must be marked with an
 * {@link ExecutableMode#INTROSPECT introspection} hint so that parameter
 * annotations can be read. Full {@link ExecutableMode#INVOKE invocation} hints
 * are only required if the {@code resolveAndInstantiate} methods of this class
 * are being used (typically to support private constructors, methods or
 * classes).
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @see AutowiredArguments
 */
public final class AutowiredInstantiationArgumentsResolver extends AutowiredElementResolver {

	private final ExecutableLookup lookup;

	@Nullable
	private final String[] shortcuts;


	private AutowiredInstantiationArgumentsResolver(ExecutableLookup lookup,
			@Nullable String[] shortcuts) {

		this.lookup = lookup;
		this.shortcuts = shortcuts;
	}


	/**
	 * Create a {@link AutowiredInstantiationArgumentsResolver} that resolves
	 * arguments for the specified bean constructor.
	 * @param parameterTypes the constructor parameter types
	 * @return a new {@link AutowiredInstantiationArgumentsResolver} instance
	 */
	public static AutowiredInstantiationArgumentsResolver forConstructor(
			Class<?>... parameterTypes) {

		Assert.notNull(parameterTypes, "'parameterTypes' must not be null");
		Assert.noNullElements(parameterTypes,
				"'parameterTypes' must not contain null elements");
		return new AutowiredInstantiationArgumentsResolver(
				new ConstructorLookup(parameterTypes), null);
	}

	/**
	 * Create a new {@link AutowiredInstantiationArgumentsResolver} that
	 * resolves arguments for the specified factory method.
	 * @param declaringClass the class that declares the factory method
	 * @param methodName the factory method name
	 * @param parameterTypes the factory method parameter types
	 * @return a new {@link AutowiredInstantiationArgumentsResolver} instance
	 */
	public static AutowiredInstantiationArgumentsResolver forFactoryMethod(
			Class<?> declaringClass, String methodName, Class<?>... parameterTypes) {

		Assert.notNull(declaringClass, "'declaringClass' must not be null");
		Assert.hasText(methodName, "'methodName' must not be empty");
		Assert.notNull(parameterTypes, "'parameterTypes' must not be null");
		Assert.noNullElements(parameterTypes,
				"'parameterTypes' must not contain null elements");
		return new AutowiredInstantiationArgumentsResolver(
				new FactoryMethodLookup(declaringClass, methodName, parameterTypes),
				null);
	}


	ExecutableLookup getLookup() {
		return this.lookup;
	}

	/**
	 * Return a new {@link AutowiredInstantiationArgumentsResolver} instance
	 * that uses direct bean name injection shortcuts for specific parameters.
	 * @param beanNames the bean names to use as shortcuts (aligned with the
	 * constructor or factory method parameters)
	 * @return a new {@link AutowiredInstantiationArgumentsResolver} instance
	 * that uses the shortcuts
	 */
	public AutowiredInstantiationArgumentsResolver withShortcuts(String... beanNames) {
		return new AutowiredInstantiationArgumentsResolver(this.lookup, beanNames);
	}

	/**
	 * Resolve arguments for the specified registered bean and provide them to
	 * the given generator in order to return a result.
	 * @param registeredBean the registered bean
	 * @param generator the generator to execute with the resolved constructor
	 * or factory method arguments
	 */
	public <T> T resolve(RegisteredBean registeredBean,
			ThrowingFunction<AutowiredArguments, T> generator) {

		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		Assert.notNull(generator, "'action' must not be null");
		AutowiredArguments resolved = resolveArguments(registeredBean,
				this.lookup.get(registeredBean));
		return generator.apply(resolved);
	}

	/**
	 * Resolve arguments for the specified registered bean.
	 * @param registeredBean the registered bean
	 * @return the resolved constructor or factory method arguments
	 */
	public AutowiredArguments resolve(RegisteredBean registeredBean) {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		return resolveArguments(registeredBean, this.lookup.get(registeredBean));
	}

	/**
	 * Resolve arguments for the specified registered bean and instantiate a new
	 * instance using reflection.
	 * @param registeredBean the registered bean
	 * @return an instance of the bean
	 */
	@SuppressWarnings("unchecked")
	public <T> T resolveAndInstantiate(RegisteredBean registeredBean) {
		return (T) resolveAndInstantiate(registeredBean, Object.class);
	}

	/**
	 * Resolve arguments for the specified registered bean and instantiate a new
	 * instance using reflection.
	 * @param registeredBean the registered bean
	 * @param requiredType the required result type
	 * @return an instance of the bean
	 */
	@SuppressWarnings("unchecked")
	public <T> T resolveAndInstantiate(RegisteredBean registeredBean,
			Class<T> requiredType) {

		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		Assert.notNull(registeredBean, "'requiredType' must not be null");
		Executable executable = this.lookup.get(registeredBean);
		AutowiredArguments arguments = resolveArguments(registeredBean, executable);
		Object instance = instantiate(registeredBean.getBeanFactory(), executable,
				arguments.toArray());
		Assert.isInstanceOf(requiredType, instance);
		return (T) instance;
	}

	private AutowiredArguments resolveArguments(RegisteredBean registeredBean,
			Executable executable) {

		Assert.isInstanceOf(AbstractAutowireCapableBeanFactory.class,
				registeredBean.getBeanFactory());
		String beanName = registeredBean.getBeanName();
		Class<?> beanClass = registeredBean.getBeanClass();
		AbstractAutowireCapableBeanFactory beanFactory = (AbstractAutowireCapableBeanFactory) registeredBean
				.getBeanFactory();
		RootBeanDefinition mergedBeanDefinition = registeredBean
				.getMergedBeanDefinition();
		int startIndex = (executable instanceof Constructor<?> constructor
				&& ClassUtils.isInnerClass(constructor.getDeclaringClass())) ? 1 : 0;
		int parameterCount = executable.getParameterCount();
		Object[] resolved = new Object[parameterCount - startIndex];
		Assert.isTrue(this.shortcuts == null || this.shortcuts.length == resolved.length,
				() -> "'shortcuts' must contain " + resolved.length + " elements");
		Set<String> autowiredBeans = new LinkedHashSet<>(resolved.length);
		ConstructorArgumentValues argumentValues = resolveArgumentValues(beanFactory,
				beanName, mergedBeanDefinition);
		for (int i = startIndex; i < parameterCount; i++) {
			MethodParameter parameter = getMethodParameter(executable, i);
			DependencyDescriptor dependencyDescriptor = new DependencyDescriptor(
					parameter, true);
			String shortcut = (this.shortcuts != null) ? this.shortcuts[i - startIndex]
					: null;
			if (shortcut != null) {
				dependencyDescriptor = new ShortcutDependencyDescriptor(
						dependencyDescriptor, shortcut, beanClass);
			}
			ValueHolder argumentValue = argumentValues.getIndexedArgumentValue(i, null);
			resolved[i - startIndex] = resolveArgument(beanFactory, beanName,
					autowiredBeans, parameter, dependencyDescriptor, argumentValue);
		}
		registerDependentBeans(beanFactory, beanName, autowiredBeans);
		if (executable instanceof Method method) {
			mergedBeanDefinition.setResolvedFactoryMethod(method);
		}
		return AutowiredArguments.of(resolved);
	}

	private MethodParameter getMethodParameter(Executable executable, int index) {
		if (executable instanceof Constructor<?> constructor) {
			return new MethodParameter(constructor, index);
		}
		if (executable instanceof Method method) {
			return new MethodParameter(method, index);
		}
		throw new IllegalStateException(
				"Unsupported executable " + executable.getClass().getName());
	}

	private ConstructorArgumentValues resolveArgumentValues(
			AbstractAutowireCapableBeanFactory beanFactory, String beanName,
			RootBeanDefinition mergedBeanDefinition) {

		ConstructorArgumentValues resolved = new ConstructorArgumentValues();
		if (mergedBeanDefinition.hasConstructorArgumentValues()) {
			BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(
					beanFactory, beanName, mergedBeanDefinition,
					beanFactory.getTypeConverter());
			ConstructorArgumentValues values = mergedBeanDefinition
					.getConstructorArgumentValues();
			values.getIndexedArgumentValues().forEach((index, valueHolder) -> {
				ValueHolder resolvedValue = resolveArgumentValue(valueResolver,
						valueHolder);
				resolved.addIndexedArgumentValue(index, resolvedValue);
			});
		}
		return resolved;
	}

	private ValueHolder resolveArgumentValue(BeanDefinitionValueResolver resolver,
			ValueHolder valueHolder) {

		if (valueHolder.isConverted()) {
			return valueHolder;
		}
		Object resolvedValue = resolver.resolveValueIfNecessary("constructor argument",
				valueHolder.getValue());
		ValueHolder resolvedValueHolder = new ValueHolder(resolvedValue,
				valueHolder.getType(), valueHolder.getName());
		resolvedValueHolder.setSource(valueHolder);
		return resolvedValueHolder;
	}

	@Nullable
	private Object resolveArgument(AbstractAutowireCapableBeanFactory beanFactory,
			String beanName, Set<String> autowiredBeans, MethodParameter parameter,
			DependencyDescriptor dependencyDescriptor,
			@Nullable ValueHolder argumentValue) {

		TypeConverter typeConverter = beanFactory.getTypeConverter();
		Class<?> parameterType = parameter.getParameterType();
		if (argumentValue != null) {
			return (!argumentValue.isConverted()) ? typeConverter
					.convertIfNecessary(argumentValue.getValue(), parameterType)
					: argumentValue.getConvertedValue();
		}
		try {
			try {
				return beanFactory.resolveDependency(dependencyDescriptor, beanName,
						autowiredBeans, typeConverter);
			}
			catch (NoSuchBeanDefinitionException ex) {
				if (parameterType.isArray()) {
					return Array.newInstance(parameterType.getComponentType(), 0);
				}
				if (CollectionFactory.isApproximableCollectionType(parameterType)) {
					return CollectionFactory.createCollection(parameterType, 0);
				}
				if (CollectionFactory.isApproximableMapType(parameterType)) {
					return CollectionFactory.createMap(parameterType, 0);
				}
				throw ex;
			}
		}
		catch (BeansException ex) {
			throw new UnsatisfiedDependencyException(null, beanName,
					new InjectionPoint(parameter), ex);
		}
	}

	private Object instantiate(ConfigurableBeanFactory beanFactory, Executable executable,
			Object[] arguments) {

		try {
			if (executable instanceof Constructor<?> constructor) {
				return instantiate(constructor, arguments);
			}
			if (executable instanceof Method method) {
				return instantiate(beanFactory, method, arguments);
			}
		}
		catch (Exception ex) {
			throw new BeanCreationException(
					"Unable to instantiate bean using " + executable, ex);
		}
		throw new IllegalStateException(
				"Unsupported executable " + executable.getClass().getName());
	}

	private Object instantiate(Constructor<?> constructor, Object[] arguments)
			throws Exception {

		Class<?> declaringClass = constructor.getDeclaringClass();
		if (ClassUtils.isInnerClass(declaringClass)) {
			Object enclosingInstance = createInstance(declaringClass.getEnclosingClass());
			arguments = ObjectUtils.addObjectToArray(arguments, enclosingInstance, 0);
		}
		ReflectionUtils.makeAccessible(constructor);
		return constructor.newInstance(arguments);
	}

	private Object instantiate(ConfigurableBeanFactory beanFactory, Method method,
			Object[] arguments) {

		ReflectionUtils.makeAccessible(method);
		Object target = getFactoryMethodTarget(beanFactory, method);
		return ReflectionUtils.invokeMethod(method, target, arguments);
	}

	@Nullable
	private Object getFactoryMethodTarget(BeanFactory beanFactory, Method method) {
		if (Modifier.isStatic(method.getModifiers())) {
			return null;
		}
		Class<?> declaringClass = method.getDeclaringClass();
		return beanFactory.getBean(declaringClass);
	}

	private Object createInstance(Class<?> clazz) throws Exception {
		if (!ClassUtils.isInnerClass(clazz)) {
			Constructor<?> constructor = clazz.getDeclaredConstructor();
			ReflectionUtils.makeAccessible(constructor);
			return constructor.newInstance();
		}
		Class<?> enclosingClass = clazz.getEnclosingClass();
		Constructor<?> constructor = clazz.getDeclaredConstructor(enclosingClass);
		return constructor.newInstance(createInstance(enclosingClass));
	}


	/**
	 * Performs lookup of the {@link Executable}.
	 */
	static abstract class ExecutableLookup {

		abstract Executable get(RegisteredBean registeredBean);

		final String toCommaSeparatedNames(Class<?>... parameterTypes) {
			return Arrays.stream(parameterTypes).map(Class::getName)
					.collect(Collectors.joining(", "));
		}

	}


	/**
	 * Performs lookup of the {@link Constructor}.
	 */
	private static class ConstructorLookup extends ExecutableLookup {

		private final Class<?>[] parameterTypes;

		ConstructorLookup(Class<?>[] parameterTypes) {
			this.parameterTypes = parameterTypes;
		}

		@Override
		public Executable get(RegisteredBean registeredBean) {
			Class<?> beanClass = registeredBean.getBeanClass();
			try {
				Class<?>[] actualParameterTypes = (!ClassUtils.isInnerClass(beanClass))
						? this.parameterTypes : ObjectUtils.addObjectToArray(
								this.parameterTypes, beanClass.getEnclosingClass(), 0);
				return beanClass.getDeclaredConstructor(actualParameterTypes);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(String.format(
						"%s cannot be found on %s", this, beanClass.getName()), ex);
			}
		}

		@Override
		public String toString() {
			return String.format("Constructor with parameter types [%s]",
					toCommaSeparatedNames(this.parameterTypes));
		}

	}


	/**
	 * Performs lookup of the factory {@link Method}.
	 */
	private static class FactoryMethodLookup extends ExecutableLookup {

		private final Class<?> declaringClass;

		private final String methodName;

		private final Class<?>[] parameterTypes;


		FactoryMethodLookup(Class<?> declaringClass, String methodName,
				Class<?>[] parameterTypes) {
			this.declaringClass = declaringClass;
			this.methodName = methodName;
			this.parameterTypes = parameterTypes;
		}


		@Override
		public Executable get(RegisteredBean registeredBean) {
			Method method = ReflectionUtils.findMethod(this.declaringClass,
					this.methodName, this.parameterTypes);
			Assert.notNull(method, () -> String.format("%s cannot be found", this));
			return method;
		}

		@Override
		public String toString() {
			return String.format(
					"Factory method '%s' with parameter types [%s] declared on %s",
					this.methodName, toCommaSeparatedNames(this.parameterTypes),
					this.declaringClass);
		}

	}

}
