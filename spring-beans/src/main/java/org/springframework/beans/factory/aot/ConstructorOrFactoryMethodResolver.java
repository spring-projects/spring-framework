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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Resolves the {@link Executable} (factory method or constructor) that should
 * be used to create a bean. This class is similar to
 * {@code org.springframework.beans.factory.support.ConstructorResolver} but it
 * doesn't need bean initialization.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 */
class ConstructorOrFactoryMethodResolver {

	private final ConfigurableBeanFactory beanFactory;

	@Nullable
	private final ClassLoader classLoader;


	ConstructorOrFactoryMethodResolver(ConfigurableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.classLoader = (beanFactory.getBeanClassLoader() != null ?
				beanFactory.getBeanClassLoader() : ClassUtils.getDefaultClassLoader());
	}


	Executable resolve(BeanDefinition beanDefinition) {
		Supplier<ResolvableType> beanType = () -> getBeanType(beanDefinition);
		List<ResolvableType> valueTypes = (beanDefinition.hasConstructorArgumentValues() ?
				determineParameterValueTypes(beanDefinition.getConstructorArgumentValues()) :
				Collections.emptyList());
		Method resolvedFactoryMethod = resolveFactoryMethod(beanDefinition, valueTypes);
		if (resolvedFactoryMethod != null) {
			return resolvedFactoryMethod;
		}

		Class<?> factoryBeanClass = getFactoryBeanClass(beanDefinition);
		if (factoryBeanClass != null && !factoryBeanClass.equals(beanDefinition.getResolvableType().toClass())) {
			ResolvableType resolvableType = beanDefinition.getResolvableType();
			boolean isCompatible = ResolvableType.forClass(factoryBeanClass)
					.as(FactoryBean.class).getGeneric(0).isAssignableFrom(resolvableType);
			Assert.state(isCompatible, () -> String.format(
					"Incompatible target type '%s' for factory bean '%s'",
					resolvableType.toClass().getName(), factoryBeanClass.getName()));
			Executable executable = resolveConstructor(() -> ResolvableType.forClass(factoryBeanClass), valueTypes);
			if (executable != null) {
				return executable;
			}
			throw new IllegalStateException("No suitable FactoryBean constructor found for "
					+ beanDefinition + " and argument types " + valueTypes);

		}

		Executable resolvedConstructor = resolveConstructor(beanType, valueTypes);
		if (resolvedConstructor != null) {
			return resolvedConstructor;
		}

		throw new IllegalStateException("No constructor or factory method candidate found for "
				+ beanDefinition + " and argument types " + valueTypes);
	}

	private List<ResolvableType> determineParameterValueTypes(
			ConstructorArgumentValues constructorArgumentValues) {

		List<ResolvableType> parameterTypes = new ArrayList<>();
		for (ValueHolder valueHolder : constructorArgumentValues
				.getIndexedArgumentValues().values()) {
			parameterTypes.add(determineParameterValueType(valueHolder));
		}
		return parameterTypes;
	}

	private ResolvableType determineParameterValueType(ValueHolder valueHolder) {
		if (valueHolder.getType() != null) {
			return ResolvableType.forClass(loadClass(valueHolder.getType()));
		}
		Object value = valueHolder.getValue();
		if (value instanceof BeanReference br) {
			if (value instanceof RuntimeBeanReference rbr) {
				if (rbr.getBeanType() != null) {
					return ResolvableType.forClass(rbr.getBeanType());
				}
			}
			return ResolvableType.forClass(this.beanFactory.getType(br.getBeanName(), false));
		}
		if (value instanceof BeanDefinition bd) {
			return extractTypeFromBeanDefinition(getBeanType(bd));
		}
		if (value instanceof Class<?> clazz) {
			return ResolvableType.forClassWithGenerics(Class.class, clazz);
		}
		return ResolvableType.forInstance(value);
	}

	private ResolvableType extractTypeFromBeanDefinition(ResolvableType type) {
		if (FactoryBean.class.isAssignableFrom(type.toClass())) {
			return type.as(FactoryBean.class).getGeneric(0);
		}
		return type;
	}

	@Nullable
	private Method resolveFactoryMethod(BeanDefinition beanDefinition, List<ResolvableType> valueTypes) {
		if (beanDefinition instanceof RootBeanDefinition rbd) {
			Method resolvedFactoryMethod = rbd.getResolvedFactoryMethod();
			if (resolvedFactoryMethod != null) {
				return resolvedFactoryMethod;
			}
		}
		String factoryMethodName = beanDefinition.getFactoryMethodName();
		if (factoryMethodName != null) {
			String factoryBeanName = beanDefinition.getFactoryBeanName();
			Class<?> beanClass = getBeanClass(factoryBeanName != null ?
					this.beanFactory.getMergedBeanDefinition(factoryBeanName) : beanDefinition);
			List<Method> methods = new ArrayList<>();
			Assert.state(beanClass != null,
					() -> "Failed to determine bean class of " + beanDefinition);
			ReflectionUtils.doWithMethods(beanClass, methods::add,
					method -> isFactoryMethodCandidate(beanClass, method, factoryMethodName));
			if (methods.size() >= 1) {
				Function<Method, List<ResolvableType>> parameterTypesFactory = method -> {
					List<ResolvableType> types = new ArrayList<>();
					for (int i = 0; i < method.getParameterCount(); i++) {
						types.add(ResolvableType.forMethodParameter(method, i));
					}
					return types;
				};
				return (Method) resolveFactoryMethod(methods, parameterTypesFactory, valueTypes);
			}
		}
		return null;
	}

	private boolean isFactoryMethodCandidate(Class<?> beanClass, Method method, String factoryMethodName) {
		if (method.getName().equals(factoryMethodName)) {
			if (Modifier.isStatic(method.getModifiers())) {
				return method.getDeclaringClass().equals(beanClass);
			}
			return !Modifier.isPrivate(method.getModifiers());
		}
		return false;
	}

	@Nullable
	private Executable resolveConstructor(Supplier<ResolvableType> beanType, List<ResolvableType> valueTypes) {
		Class<?> type = ClassUtils.getUserClass(beanType.get().toClass());
		Constructor<?>[] constructors = type.getDeclaredConstructors();
		if (constructors.length == 1) {
			return constructors[0];
		}
		for (Constructor<?> constructor : constructors) {
			if (MergedAnnotations.from(constructor).isPresent(Autowired.class)) {
				return constructor;
			}
		}
		Function<Constructor<?>, List<ResolvableType>> parameterTypesFactory = executable -> {
			List<ResolvableType> types = new ArrayList<>();
			for (int i = 0; i < executable.getParameterCount(); i++) {
				types.add(ResolvableType.forConstructorParameter(executable, i));
			}
			return types;
		};
		List<? extends Executable> matches = Arrays.stream(constructors)
				.filter(executable -> match(parameterTypesFactory.apply(executable),
						valueTypes, FallbackMode.NONE))
				.toList();
		if (matches.size() == 1) {
			return matches.get(0);
		}
		List<? extends Executable> assignableElementFallbackMatches = Arrays
				.stream(constructors)
				.filter(executable -> match(parameterTypesFactory.apply(executable),
						valueTypes, FallbackMode.ASSIGNABLE_ELEMENT))
				.toList();
		if (assignableElementFallbackMatches.size() == 1) {
			return assignableElementFallbackMatches.get(0);
		}
		List<? extends Executable> typeConversionFallbackMatches = Arrays
				.stream(constructors)
				.filter(executable -> match(parameterTypesFactory.apply(executable),
						valueTypes, FallbackMode.TYPE_CONVERSION))
				.toList();
		return (typeConversionFallbackMatches.size() == 1)
				? typeConversionFallbackMatches.get(0) : null;
	}

	@Nullable
	private Executable resolveFactoryMethod(List<Method> executables,
			Function<Method, List<ResolvableType>> parameterTypesFactory,
			List<ResolvableType> valueTypes) {

		List<? extends Executable> matches = executables.stream()
				.filter(executable -> match(parameterTypesFactory.apply(executable), valueTypes, FallbackMode.NONE))
				.toList();
		if (matches.size() == 1) {
			return matches.get(0);
		}
		List<? extends Executable> assignableElementFallbackMatches = executables.stream()
				.filter(executable -> match(parameterTypesFactory.apply(executable),
						valueTypes, FallbackMode.ASSIGNABLE_ELEMENT))
				.toList();
		if (assignableElementFallbackMatches.size() == 1) {
			return assignableElementFallbackMatches.get(0);
		}
		List<? extends Executable> typeConversionFallbackMatches = executables.stream()
				.filter(executable -> match(parameterTypesFactory.apply(executable),
						valueTypes, FallbackMode.TYPE_CONVERSION))
				.toList();
		Assert.state(typeConversionFallbackMatches.size() <= 1,
				() -> "Multiple matches with parameters '" + valueTypes + "': " + typeConversionFallbackMatches);
		return (typeConversionFallbackMatches.size() == 1 ? typeConversionFallbackMatches.get(0) : null);
	}

	private boolean match(
			List<ResolvableType> parameterTypes, List<ResolvableType> valueTypes, FallbackMode fallbackMode) {

		if (parameterTypes.size() != valueTypes.size()) {
			return false;
		}
		for (int i = 0; i < parameterTypes.size(); i++) {
			if (!isMatch(parameterTypes.get(i), valueTypes.get(i), fallbackMode)) {
				return false;
			}
		}
		return true;
	}

	private boolean isMatch(ResolvableType parameterType, ResolvableType valueType, FallbackMode fallbackMode) {
		if (isAssignable(valueType).test(parameterType)) {
			return true;
		}
		return switch (fallbackMode) {
			case ASSIGNABLE_ELEMENT ->
					isAssignable(valueType).test(extractElementType(parameterType));
			case TYPE_CONVERSION -> typeConversionFallback(valueType).test(parameterType);
			default -> false;
		};
	}

	private Predicate<ResolvableType> isAssignable(ResolvableType valueType) {
		return parameterType -> parameterType.isAssignableFrom(valueType);
	}

	private ResolvableType extractElementType(ResolvableType parameterType) {
		if (parameterType.isArray()) {
			return parameterType.getComponentType();
		}
		if (Collection.class.isAssignableFrom(parameterType.toClass())) {
			return parameterType.as(Collection.class).getGeneric(0);
		}
		return ResolvableType.NONE;
	}

	private Predicate<ResolvableType> typeConversionFallback(ResolvableType valueType) {
		return parameterType -> {
			if (valueOrCollection(valueType, this::isStringForClassFallback).test(parameterType)) {
				return true;
			}
			return valueOrCollection(valueType, this::isSimpleValueType).test(parameterType);
		};
	}

	private Predicate<ResolvableType> valueOrCollection(ResolvableType valueType,
			Function<ResolvableType, Predicate<ResolvableType>> predicateProvider) {

		return parameterType -> {
			if (predicateProvider.apply(valueType).test(parameterType)) {
				return true;
			}
			if (predicateProvider.apply(extractElementType(valueType)).test(extractElementType(parameterType))) {
				return true;
			}
			return (predicateProvider.apply(valueType).test(extractElementType(parameterType)));
		};
	}

	/**
	 * Return a {@link Predicate} for a parameter type that checks if its target
	 * value is a {@link Class} and the value type is a {@link String}. This is
	 * a regular use cases where a {@link Class} is defined in the bean
	 * definition as an FQN.
	 * @param valueType the type of the value
	 * @return a predicate to indicate a fallback match for a String to Class
	 * parameter
	 */
	private Predicate<ResolvableType> isStringForClassFallback(ResolvableType valueType) {
		return parameterType -> (valueType.isAssignableFrom(String.class) &&
				parameterType.isAssignableFrom(Class.class));
	}

	private Predicate<ResolvableType> isSimpleValueType(ResolvableType valueType) {
		return parameterType -> (BeanUtils.isSimpleValueType(parameterType.toClass()) &&
				BeanUtils.isSimpleValueType(valueType.toClass()));
	}

	@Nullable
	private Class<?> getFactoryBeanClass(BeanDefinition beanDefinition) {
		if (beanDefinition instanceof RootBeanDefinition rbd) {
			if (rbd.hasBeanClass()) {
				Class<?> beanClass = rbd.getBeanClass();
				return (FactoryBean.class.isAssignableFrom(beanClass) ? beanClass : null);
			}
		}
		return null;
	}

	@Nullable
	private Class<?> getBeanClass(BeanDefinition beanDefinition) {
		if (beanDefinition instanceof AbstractBeanDefinition abd && abd.hasBeanClass()) {
			return abd.getBeanClass();
		}
		return (beanDefinition.getBeanClassName() != null ? loadClass(beanDefinition.getBeanClassName()) : null);
	}

	private ResolvableType getBeanType(BeanDefinition beanDefinition) {
		ResolvableType resolvableType = beanDefinition.getResolvableType();
		if (resolvableType != ResolvableType.NONE) {
			return resolvableType;
		}
		if (beanDefinition instanceof RootBeanDefinition rbd) {
			if (rbd.hasBeanClass()) {
				return ResolvableType.forClass(rbd.getBeanClass());
			}
		}
		String beanClassName = beanDefinition.getBeanClassName();
		if (beanClassName != null) {
			return ResolvableType.forClass(loadClass(beanClassName));
		}
		throw new IllegalStateException(
				"Failed to determine bean class of " + beanDefinition);
	}

	private Class<?> loadClass(String beanClassName) {
		try {
			return ClassUtils.forName(beanClassName, this.classLoader);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Failed to load class " + beanClassName);
		}
	}

	static Executable resolve(RegisteredBean registeredBean) {
		return new ConstructorOrFactoryMethodResolver(registeredBean.getBeanFactory())
				.resolve(registeredBean.getMergedBeanDefinition());
	}


	enum FallbackMode {

		NONE,

		ASSIGNABLE_ELEMENT,

		TYPE_CONVERSION
	}

}
