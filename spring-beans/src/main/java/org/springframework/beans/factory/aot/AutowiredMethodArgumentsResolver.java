/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.function.ThrowingConsumer;

/**
 * Resolver used to support the autowiring of methods. Typically used in
 * AOT-processed applications as a targeted alternative to the
 * {@link org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
 * AutowiredAnnotationBeanPostProcessor}.
 *
 * <p>When resolving arguments in a native image, the {@link Method} being used
 * must be marked with an {@link ExecutableMode#INTROSPECT introspection} hint
 * so that field annotations can be read. Full {@link ExecutableMode#INVOKE
 * invocation} hints are only required if the
 * {@link #resolveAndInvoke(RegisteredBean, Object)} method of this class is
 * being used (typically to support private methods).
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class AutowiredMethodArgumentsResolver extends AutowiredElementResolver {

	private final String methodName;

	private final Class<?>[] parameterTypes;

	private final boolean required;

	@Nullable
	private final String[] shortcuts;


	private AutowiredMethodArgumentsResolver(String methodName, Class<?>[] parameterTypes,
			boolean required, @Nullable String[] shortcuts) {

		Assert.hasText(methodName, "'methodName' must not be empty");
		this.methodName = methodName;
		this.parameterTypes = parameterTypes;
		this.required = required;
		this.shortcuts = shortcuts;
	}


	/**
	 * Create a new {@link AutowiredMethodArgumentsResolver} for the specified
	 * method where injection is optional.
	 * @param methodName the method name
	 * @param parameterTypes the factory method parameter types
	 * @return a new {@link AutowiredFieldValueResolver} instance
	 */
	public static AutowiredMethodArgumentsResolver forMethod(String methodName, Class<?>... parameterTypes) {
		return new AutowiredMethodArgumentsResolver(methodName, parameterTypes, false, null);
	}

	/**
	 * Create a new {@link AutowiredMethodArgumentsResolver} for the specified
	 * method where injection is required.
	 * @param methodName the method name
	 * @param parameterTypes the factory method parameter types
	 * @return a new {@link AutowiredFieldValueResolver} instance
	 */
	public static AutowiredMethodArgumentsResolver forRequiredMethod(String methodName, Class<?>... parameterTypes) {
		return new AutowiredMethodArgumentsResolver(methodName, parameterTypes, true, null);
	}

	/**
	 * Return a new {@link AutowiredMethodArgumentsResolver} instance
	 * that uses direct bean name injection shortcuts for specific parameters.
	 * @param beanNames the bean names to use as shortcuts (aligned with the
	 * method parameters)
	 * @return a new {@link AutowiredMethodArgumentsResolver} instance that uses
	 * the shortcuts
	 */
	public AutowiredMethodArgumentsResolver withShortcut(String... beanNames) {
		return new AutowiredMethodArgumentsResolver(this.methodName, this.parameterTypes, this.required, beanNames);
	}

	/**
	 * Resolve the method arguments for the specified registered bean and
	 * provide it to the given action.
	 * @param registeredBean the registered bean
	 * @param action the action to execute with the resolved method arguments
	 */
	public void resolve(RegisteredBean registeredBean, ThrowingConsumer<AutowiredArguments> action) {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		Assert.notNull(action, "'action' must not be null");
		AutowiredArguments resolved = resolve(registeredBean);
		if (resolved != null) {
			action.accept(resolved);
		}
	}

	/**
	 * Resolve the method arguments for the specified registered bean.
	 * @param registeredBean the registered bean
	 * @return the resolved method arguments
	 */
	@Nullable
	public AutowiredArguments resolve(RegisteredBean registeredBean) {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		return resolveArguments(registeredBean, getMethod(registeredBean));
	}

	/**
	 * Resolve the method arguments for the specified registered bean and invoke
	 * the method using reflection.
	 * @param registeredBean the registered bean
	 * @param instance the bean instance
	 */
	public void resolveAndInvoke(RegisteredBean registeredBean, Object instance) {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		Assert.notNull(instance, "'instance' must not be null");
		Method method = getMethod(registeredBean);
		AutowiredArguments resolved = resolveArguments(registeredBean, method);
		if (resolved != null) {
			ReflectionUtils.makeAccessible(method);
			ReflectionUtils.invokeMethod(method, instance, resolved.toArray());
		}
	}

	@Nullable
	private AutowiredArguments resolveArguments(RegisteredBean registeredBean,
			Method method) {

		String beanName = registeredBean.getBeanName();
		Class<?> beanClass = registeredBean.getBeanClass();
		ConfigurableBeanFactory beanFactory = registeredBean.getBeanFactory();
		Assert.isInstanceOf(AutowireCapableBeanFactory.class, beanFactory);
		AutowireCapableBeanFactory autowireCapableBeanFactory = (AutowireCapableBeanFactory) beanFactory;
		int argumentCount = method.getParameterCount();
		Object[] arguments = new Object[argumentCount];
		Set<String> autowiredBeanNames = new LinkedHashSet<>(argumentCount);
		TypeConverter typeConverter = beanFactory.getTypeConverter();
		for (int i = 0; i < argumentCount; i++) {
			MethodParameter parameter = new MethodParameter(method, i);
			DependencyDescriptor descriptor = new DependencyDescriptor(parameter, this.required);
			descriptor.setContainingClass(beanClass);
			String shortcut = (this.shortcuts != null ? this.shortcuts[i] : null);
			if (shortcut != null) {
				descriptor = new ShortcutDependencyDescriptor(descriptor, shortcut);
			}
			try {
				Object argument = autowireCapableBeanFactory.resolveDependency(
						descriptor, beanName, autowiredBeanNames, typeConverter);
				if (argument == null && !this.required) {
					return null;
				}
				arguments[i] = argument;
			}
			catch (BeansException ex) {
				throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(parameter), ex);
			}
		}
		registerDependentBeans(beanFactory, beanName, autowiredBeanNames);
		return AutowiredArguments.of(arguments);
	}

	private Method getMethod(RegisteredBean registeredBean) {
		Method method = ReflectionUtils.findMethod(registeredBean.getBeanClass(),
				this.methodName, this.parameterTypes);
		Assert.notNull(method, () ->
				"Method '%s' with parameter types [%s] declared on %s could not be found.".formatted(
						this.methodName, toCommaSeparatedNames(this.parameterTypes),
						registeredBean.getBeanClass().getName()));
		return method;
	}

	private String toCommaSeparatedNames(Class<?>... parameterTypes) {
		return Arrays.stream(parameterTypes).map(Class::getName)
				.collect(Collectors.joining(", "));
	}

}
