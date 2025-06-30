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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Simple object instantiation strategy for use in a BeanFactory.
 *
 * <p>Does not support Method Injection, although it provides hooks for subclasses
 * to override to add Method Injection support, for example by overriding methods.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 1.1
 */
public class SimpleInstantiationStrategy implements InstantiationStrategy {

	private static final ThreadLocal<Method> currentlyInvokedFactoryMethod = new ThreadLocal<>();


	/**
	 * Return the factory method currently being invoked or {@code null} if none.
	 * <p>Allows factory method implementations to determine whether the current
	 * caller is the container itself as opposed to user code.
	 */
	public static @Nullable Method getCurrentlyInvokedFactoryMethod() {
		return currentlyInvokedFactoryMethod.get();
	}

	/**
	 * Invoke the given {@code instanceSupplier} with the factory method exposed
	 * as being invoked.
	 * @param method the factory method to expose
	 * @param instanceSupplier the instance supplier
	 * @param <T> the type of the instance
	 * @return the result of the instance supplier
	 * @since 6.2
	 */
	public static <T> T instantiateWithFactoryMethod(Method method, Supplier<T> instanceSupplier) {
		Method priorInvokedFactoryMethod = currentlyInvokedFactoryMethod.get();
		try {
			currentlyInvokedFactoryMethod.set(method);
			return instanceSupplier.get();
		}
		finally {
			if (priorInvokedFactoryMethod != null) {
				currentlyInvokedFactoryMethod.set(priorInvokedFactoryMethod);
			}
			else {
				currentlyInvokedFactoryMethod.remove();
			}
		}
	}


	@Override
	public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
		// Don't override the class with CGLIB if no overrides.
		if (!bd.hasMethodOverrides()) {
			Constructor<?> constructorToUse;
			synchronized (bd.constructorArgumentLock) {
				constructorToUse = (Constructor<?>) bd.resolvedConstructorOrFactoryMethod;
				if (constructorToUse == null) {
					Class<?> clazz = bd.getBeanClass();
					if (clazz.isInterface()) {
						throw new BeanInstantiationException(clazz, "Specified class is an interface");
					}
					try {
						constructorToUse = clazz.getDeclaredConstructor();
						bd.resolvedConstructorOrFactoryMethod = constructorToUse;
					}
					catch (Throwable ex) {
						throw new BeanInstantiationException(clazz, "No default constructor found", ex);
					}
				}
			}
			return BeanUtils.instantiateClass(constructorToUse);
		}
		else {
			// Must generate CGLIB subclass.
			return instantiateWithMethodInjection(bd, beanName, owner);
		}
	}

	/**
	 * Subclasses can override this method, which is implemented to throw
	 * UnsupportedOperationException, if they can instantiate an object with
	 * the Method Injection specified in the given RootBeanDefinition.
	 * Instantiation should use a no-arg constructor.
	 */
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
		throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
	}

	@Override
	public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
			Constructor<?> ctor, Object... args) {

		if (!bd.hasMethodOverrides()) {
			return BeanUtils.instantiateClass(ctor, args);
		}
		else {
			return instantiateWithMethodInjection(bd, beanName, owner, ctor, args);
		}
	}

	/**
	 * Subclasses can override this method, which is implemented to throw
	 * UnsupportedOperationException, if they can instantiate an object with
	 * the Method Injection specified in the given RootBeanDefinition.
	 * Instantiation should use the given constructor and parameters.
	 */
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName,
			BeanFactory owner, @Nullable Constructor<?> ctor, Object... args) {

		throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
	}

	@Override
	public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
			@Nullable Object factoryBean, Method factoryMethod, @Nullable Object... args) {

		return instantiateWithFactoryMethod(factoryMethod, () -> {
			try {
				ReflectionUtils.makeAccessible(factoryMethod);
				Object result = factoryMethod.invoke(factoryBean, args);
				if (result == null) {
					result = new NullBean();
				}
				return result;
			}
			catch (IllegalArgumentException ex) {
				if (factoryBean != null && !factoryMethod.getDeclaringClass().isAssignableFrom(factoryBean.getClass())) {
					throw new BeanInstantiationException(factoryMethod,
							"Illegal factory instance for factory method '" + factoryMethod.getName() + "'; " +
									"instance: " + factoryBean.getClass().getName(), ex);
				}
				throw new BeanInstantiationException(factoryMethod,
						"Illegal arguments to factory method '" + factoryMethod.getName() + "'; " +
								"args: " + StringUtils.arrayToCommaDelimitedString(args), ex);
			}
			catch (IllegalAccessException ex) {
				throw new BeanInstantiationException(factoryMethod,
						"Cannot access factory method '" + factoryMethod.getName() + "'; is it public?", ex);
			}
			catch (InvocationTargetException ex) {
				String msg = "Factory method '" + factoryMethod.getName() + "' threw exception with message: " +
						ex.getTargetException().getMessage();
				if (bd.getFactoryBeanName() != null && owner instanceof ConfigurableBeanFactory cbf &&
						cbf.isCurrentlyInCreation(bd.getFactoryBeanName())) {
					msg = "Circular reference involving containing bean '" + bd.getFactoryBeanName() + "' - consider " +
							"declaring the factory method as static for independence from its containing instance. " + msg;
				}
				throw new BeanInstantiationException(factoryMethod, msg, ex.getTargetException());
			}
		});
	}

}
