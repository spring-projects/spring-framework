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

package org.springframework.beans.factory.aot;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionValueResolver;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.SimpleInstantiationStrategy;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.function.ThrowingBiFunction;
import org.springframework.util.function.ThrowingFunction;
import org.springframework.util.function.ThrowingSupplier;

/**
 * Specialized {@link InstanceSupplier} that provides the factory {@link Method}
 * used to instantiate the underlying bean instance, if any. Transparently
 * handles resolution of {@link AutowiredArguments} if necessary. Typically used
 * in AOT-processed applications as a targeted alternative to the reflection
 * based injection.
 *
 * <p>If no {@code generator} is provided, reflection is used to instantiate the
 * bean instance, and full {@link ExecutableMode#INVOKE invocation} hints are
 * contributed. Multiple generator callback styles are supported:
 * <ul>
 * <li>A function with the {@code registeredBean} and resolved {@code arguments}
 * for executables that require arguments resolution. An
 * {@link ExecutableMode#INTROSPECT introspection} hint is added so that
 * parameter annotations can be read </li>
 * <li>A function with only the {@code registeredBean} for simpler cases that
 * do not require resolution of arguments</li>
 * <li>A supplier when a method reference can be used</li>
 * </ul>
 * Generator callbacks handle checked exceptions so that the caller does not
 * have to deal with them.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @param <T> the type of instance supplied by this supplier
 * @see AutowiredArguments
 */
public final class BeanInstanceSupplier<T> extends AutowiredElementResolver implements InstanceSupplier<T> {

	private final ExecutableLookup lookup;

	@Nullable
	private final ThrowingBiFunction<RegisteredBean, AutowiredArguments, T> generator;

	@Nullable
	private final String[] shortcuts;


	private BeanInstanceSupplier(ExecutableLookup lookup,
			@Nullable ThrowingBiFunction<RegisteredBean, AutowiredArguments, T> generator,
			@Nullable String[] shortcuts) {

		this.lookup = lookup;
		this.generator = generator;
		this.shortcuts = shortcuts;
	}


	/**
	 * Create a {@link BeanInstanceSupplier} that resolves
	 * arguments for the specified bean constructor.
	 * @param <T> the type of instance supplied
	 * @param parameterTypes the constructor parameter types
	 * @return a new {@link BeanInstanceSupplier} instance
	 */
	public static <T> BeanInstanceSupplier<T> forConstructor(Class<?>... parameterTypes) {
		Assert.notNull(parameterTypes, "'parameterTypes' must not be null");
		Assert.noNullElements(parameterTypes, "'parameterTypes' must not contain null elements");
		return new BeanInstanceSupplier<>(new ConstructorLookup(parameterTypes), null, null);
	}

	/**
	 * Create a new {@link BeanInstanceSupplier} that
	 * resolves arguments for the specified factory method.
	 * @param <T> the type of instance supplied
	 * @param declaringClass the class that declares the factory method
	 * @param methodName the factory method name
	 * @param parameterTypes the factory method parameter types
	 * @return a new {@link BeanInstanceSupplier} instance
	 */
	public static <T> BeanInstanceSupplier<T> forFactoryMethod(
			Class<?> declaringClass, String methodName, Class<?>... parameterTypes) {

		Assert.notNull(declaringClass, "'declaringClass' must not be null");
		Assert.hasText(methodName, "'methodName' must not be empty");
		Assert.notNull(parameterTypes, "'parameterTypes' must not be null");
		Assert.noNullElements(parameterTypes, "'parameterTypes' must not contain null elements");
		return new BeanInstanceSupplier<>(
				new FactoryMethodLookup(declaringClass, methodName, parameterTypes),
				null, null);
	}


	ExecutableLookup getLookup() {
		return this.lookup;
	}

	/**
	 * Return a new {@link BeanInstanceSupplier} instance that uses the specified
	 * {@code generator} bi-function to instantiate the underlying bean.
	 * @param generator a {@link ThrowingBiFunction} that uses the
	 * {@link RegisteredBean} and resolved {@link AutowiredArguments} to
	 * instantiate the underlying bean
	 * @return a new {@link BeanInstanceSupplier} instance with the specified generator
	 */
	public BeanInstanceSupplier<T> withGenerator(
			ThrowingBiFunction<RegisteredBean, AutowiredArguments, T> generator) {

		Assert.notNull(generator, "'generator' must not be null");
		return new BeanInstanceSupplier<>(this.lookup, generator, this.shortcuts);
	}

	/**
	 * Return a new {@link BeanInstanceSupplier} instance that uses the specified
	 * {@code generator} function to instantiate the underlying bean.
	 * @param generator a {@link ThrowingFunction} that uses the
	 * {@link RegisteredBean} to instantiate the underlying bean
	 * @return a new {@link BeanInstanceSupplier} instance with the specified generator
	 */
	public BeanInstanceSupplier<T> withGenerator(ThrowingFunction<RegisteredBean, T> generator) {
		Assert.notNull(generator, "'generator' must not be null");
		return new BeanInstanceSupplier<>(this.lookup,
				(registeredBean, args) -> generator.apply(registeredBean), this.shortcuts);
	}

	/**
	 * Return a new {@link BeanInstanceSupplier} instance that uses the specified
	 * {@code generator} supplier to instantiate the underlying bean.
	 * @param generator a {@link ThrowingSupplier} to instantiate the underlying bean
	 * @return a new {@link BeanInstanceSupplier} instance with the specified generator
	 * @deprecated in favor of {@link #withGenerator(ThrowingFunction)}
	 */
	@Deprecated(since = "6.0.11", forRemoval = true)
	public BeanInstanceSupplier<T> withGenerator(ThrowingSupplier<T> generator) {
		Assert.notNull(generator, "'generator' must not be null");
		return new BeanInstanceSupplier<>(this.lookup,
				(registeredBean, args) -> generator.get(), this.shortcuts);
	}

	/**
	 * Return a new {@link BeanInstanceSupplier} instance
	 * that uses direct bean name injection shortcuts for specific parameters.
	 * @param beanNames the bean names to use as shortcuts (aligned with the
	 * constructor or factory method parameters)
	 * @return a new {@link BeanInstanceSupplier} instance
	 * that uses the shortcuts
	 */
	public BeanInstanceSupplier<T> withShortcuts(String... beanNames) {
		return new BeanInstanceSupplier<>(this.lookup, this.generator, beanNames);
	}

	@Override
	public T get(RegisteredBean registeredBean) throws Exception {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		Executable executable = this.lookup.get(registeredBean);
		AutowiredArguments arguments = resolveArguments(registeredBean, executable);
		if (this.generator != null) {
			return invokeBeanSupplier(executable, () -> this.generator.apply(registeredBean, arguments));
		}
		return invokeBeanSupplier(executable,
				() -> instantiate(registeredBean.getBeanFactory(), executable, arguments.toArray()));
	}

	private T invokeBeanSupplier(Executable executable, ThrowingSupplier<T> beanSupplier) {
		if (!(executable instanceof Method method)) {
			return beanSupplier.get();
		}
		return SimpleInstantiationStrategy.instantiateWithFactoryMethod(method, beanSupplier::get);
	}

	@Nullable
	@Override
	public Method getFactoryMethod() {
		if (this.lookup instanceof FactoryMethodLookup factoryMethodLookup) {
			return factoryMethodLookup.get();
		}
		return null;
	}

	/**
	 * Resolve arguments for the specified registered bean.
	 * @param registeredBean the registered bean
	 * @return the resolved constructor or factory method arguments
	 */
	AutowiredArguments resolveArguments(RegisteredBean registeredBean) {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		return resolveArguments(registeredBean, this.lookup.get(registeredBean));
	}

	private AutowiredArguments resolveArguments(RegisteredBean registeredBean, Executable executable) {
		Assert.isInstanceOf(AbstractAutowireCapableBeanFactory.class, registeredBean.getBeanFactory());

		int startIndex = (executable instanceof Constructor<?> constructor &&
				ClassUtils.isInnerClass(constructor.getDeclaringClass())) ? 1 : 0;
		int parameterCount = executable.getParameterCount();
		Object[] resolved = new Object[parameterCount - startIndex];
		Assert.isTrue(this.shortcuts == null || this.shortcuts.length == resolved.length,
				() -> "'shortcuts' must contain " + resolved.length + " elements");

		ValueHolder[] argumentValues = resolveArgumentValues(registeredBean, executable);
		Set<String> autowiredBeanNames = new LinkedHashSet<>(resolved.length * 2);
		for (int i = startIndex; i < parameterCount; i++) {
			MethodParameter parameter = getMethodParameter(executable, i);
			DependencyDescriptor descriptor = new DependencyDescriptor(parameter, true);
			String shortcut = (this.shortcuts != null ? this.shortcuts[i - startIndex] : null);
			if (shortcut != null) {
				descriptor = new ShortcutDependencyDescriptor(descriptor, shortcut);
			}
			ValueHolder argumentValue = argumentValues[i];
			resolved[i - startIndex] = resolveAutowiredArgument(
					registeredBean, descriptor, argumentValue, autowiredBeanNames);
		}
		registerDependentBeans(registeredBean.getBeanFactory(), registeredBean.getBeanName(), autowiredBeanNames);

		return AutowiredArguments.of(resolved);
	}

	private MethodParameter getMethodParameter(Executable executable, int index) {
		if (executable instanceof Constructor<?> constructor) {
			return new MethodParameter(constructor, index);
		}
		if (executable instanceof Method method) {
			return new MethodParameter(method, index);
		}
		throw new IllegalStateException("Unsupported executable: " + executable.getClass().getName());
	}

	private ValueHolder[] resolveArgumentValues(RegisteredBean registeredBean, Executable executable) {
		Parameter[] parameters = executable.getParameters();
		ValueHolder[] resolved = new ValueHolder[parameters.length];
		RootBeanDefinition beanDefinition = registeredBean.getMergedBeanDefinition();
		if (beanDefinition.hasConstructorArgumentValues() &&
				registeredBean.getBeanFactory() instanceof AbstractAutowireCapableBeanFactory beanFactory) {
			BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(
					beanFactory, registeredBean.getBeanName(), beanDefinition, beanFactory.getTypeConverter());
			ConstructorArgumentValues values = resolveConstructorArguments(
					valueResolver, beanDefinition.getConstructorArgumentValues());
			Set<ValueHolder> usedValueHolders = CollectionUtils.newHashSet(parameters.length);
			for (int i = 0; i < parameters.length; i++) {
				Class<?> parameterType = parameters[i].getType();
				String parameterName = (parameters[i].isNamePresent() ? parameters[i].getName() : null);
				ValueHolder valueHolder = values.getArgumentValue(
						i, parameterType, parameterName, usedValueHolders);
				if (valueHolder != null) {
					resolved[i] = valueHolder;
					usedValueHolders.add(valueHolder);
				}
			}
		}
		return resolved;
	}

	private ConstructorArgumentValues resolveConstructorArguments(
			BeanDefinitionValueResolver valueResolver, ConstructorArgumentValues constructorArguments) {

		ConstructorArgumentValues resolvedConstructorArguments = new ConstructorArgumentValues();
		for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry : constructorArguments.getIndexedArgumentValues().entrySet()) {
			resolvedConstructorArguments.addIndexedArgumentValue(entry.getKey(), resolveArgumentValue(valueResolver, entry.getValue()));
		}
		for (ConstructorArgumentValues.ValueHolder valueHolder : constructorArguments.getGenericArgumentValues()) {
			resolvedConstructorArguments.addGenericArgumentValue(resolveArgumentValue(valueResolver, valueHolder));
		}
		return resolvedConstructorArguments;
	}

	private ValueHolder resolveArgumentValue(BeanDefinitionValueResolver resolver, ValueHolder valueHolder) {
		if (valueHolder.isConverted()) {
			return valueHolder;
		}
		Object value = resolver.resolveValueIfNecessary("constructor argument", valueHolder.getValue());
		ValueHolder resolvedHolder = new ValueHolder(value, valueHolder.getType(), valueHolder.getName());
		resolvedHolder.setSource(valueHolder);
		return resolvedHolder;
	}

	@Nullable
	private Object resolveAutowiredArgument(RegisteredBean registeredBean, DependencyDescriptor descriptor,
			@Nullable ValueHolder argumentValue, Set<String> autowiredBeanNames) {

		TypeConverter typeConverter = registeredBean.getBeanFactory().getTypeConverter();
		if (argumentValue != null) {
			return (argumentValue.isConverted() ? argumentValue.getConvertedValue() :
					typeConverter.convertIfNecessary(argumentValue.getValue(),
							descriptor.getDependencyType(), descriptor.getMethodParameter()));
		}
		try {
			return registeredBean.resolveAutowiredArgument(descriptor, typeConverter, autowiredBeanNames);
		}
		catch (BeansException ex) {
			throw new UnsatisfiedDependencyException(null, registeredBean.getBeanName(), descriptor, ex);
		}
	}

	@SuppressWarnings("unchecked")
	private T instantiate(ConfigurableBeanFactory beanFactory, Executable executable, Object[] args) {
		if (executable instanceof Constructor<?> constructor) {
			try {
				return (T) instantiate(constructor, args);
			}
			catch (Exception ex) {
				throw new BeanInstantiationException(constructor, ex.getMessage(), ex);
			}
		}
		if (executable instanceof Method method) {
			try {
				return (T) instantiate(beanFactory, method, args);
			}
			catch (Exception ex) {
				throw new BeanInstantiationException(method, ex.getMessage(), ex);
			}
		}
		throw new IllegalStateException("Unsupported executable " + executable.getClass().getName());
	}

	private Object instantiate(Constructor<?> constructor, Object[] args) throws Exception {
		Class<?> declaringClass = constructor.getDeclaringClass();
		if (ClassUtils.isInnerClass(declaringClass)) {
			Object enclosingInstance = createInstance(declaringClass.getEnclosingClass());
			args = ObjectUtils.addObjectToArray(args, enclosingInstance, 0);
		}
		return BeanUtils.instantiateClass(constructor, args);
	}

	private Object instantiate(ConfigurableBeanFactory beanFactory, Method method, Object[] args) throws Exception {
		Object target = getFactoryMethodTarget(beanFactory, method);
		ReflectionUtils.makeAccessible(method);
		return method.invoke(target, args);
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


	private static String toCommaSeparatedNames(Class<?>... parameterTypes) {
		return Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.joining(", "));
	}


	/**
	 * Performs lookup of the {@link Executable}.
	 */
	abstract static class ExecutableLookup {

		abstract Executable get(RegisteredBean registeredBean);
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
				Class<?>[] actualParameterTypes = (!ClassUtils.isInnerClass(beanClass)) ?
						this.parameterTypes : ObjectUtils.addObjectToArray(
								this.parameterTypes, beanClass.getEnclosingClass(), 0);
				return beanClass.getDeclaredConstructor(actualParameterTypes);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(
						"%s cannot be found on %s".formatted(this, beanClass.getName()), ex);
			}
		}

		@Override
		public String toString() {
			return "Constructor with parameter types [%s]".formatted(toCommaSeparatedNames(this.parameterTypes));
		}
	}


	/**
	 * Performs lookup of the factory {@link Method}.
	 */
	private static class FactoryMethodLookup extends ExecutableLookup {

		private final Class<?> declaringClass;

		private final String methodName;

		private final Class<?>[] parameterTypes;

		FactoryMethodLookup(Class<?> declaringClass, String methodName, Class<?>[] parameterTypes) {
			this.declaringClass = declaringClass;
			this.methodName = methodName;
			this.parameterTypes = parameterTypes;
		}

		@Override
		public Executable get(RegisteredBean registeredBean) {
			return get();
		}

		Method get() {
			Method method = ReflectionUtils.findMethod(this.declaringClass, this.methodName, this.parameterTypes);
			Assert.notNull(method, () -> "%s cannot be found".formatted(this));
			return method;
		}

		@Override
		public String toString() {
			return "Factory method '%s' with parameter types [%s] declared on %s".formatted(
					this.methodName, toCommaSeparatedNames(this.parameterTypes),
					this.declaringClass);
		}
	}

}
