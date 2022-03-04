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

package org.springframework.beans.factory.generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionValueResolver;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * Default {@link BeanRegistrationContributionProvider} implementation.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class DefaultBeanRegistrationContributionProvider implements BeanRegistrationContributionProvider {

	private final DefaultListableBeanFactory beanFactory;

	private final ExecutableProvider executableProvider;

	private final Supplier<List<AotContributingBeanPostProcessor>> beanPostProcessors;

	public DefaultBeanRegistrationContributionProvider(DefaultListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.executableProvider = new ExecutableProvider(beanFactory);
		this.beanPostProcessors = new SingletonSupplier<>(null,
				() -> loadAotContributingBeanPostProcessors(beanFactory));
	}

	private static List<AotContributingBeanPostProcessor> loadAotContributingBeanPostProcessors(
			DefaultListableBeanFactory beanFactory) {
		String[] postProcessorNames = beanFactory.getBeanNamesForType(AotContributingBeanPostProcessor.class, true, false);
		List<AotContributingBeanPostProcessor> postProcessors = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			postProcessors.add(beanFactory.getBean(ppName, AotContributingBeanPostProcessor.class));
		}
		sortPostProcessors(postProcessors, beanFactory);
		return postProcessors;
	}

	@Override
	public BeanRegistrationBeanFactoryContribution getContributionFor(
			String beanName, RootBeanDefinition beanDefinition) {
		BeanInstantiationGenerator beanInstantiationGenerator = getBeanInstantiationGenerator(
				beanName, beanDefinition);
		return new BeanRegistrationBeanFactoryContribution(beanName, beanDefinition, beanInstantiationGenerator, this);
	}

	public BeanInstantiationGenerator getBeanInstantiationGenerator(
			String beanName, RootBeanDefinition beanDefinition) {
		return new DefaultBeanInstantiationGenerator(determineExecutable(beanDefinition),
				determineBeanInstanceContributions(beanName, beanDefinition));
	}

	/**
	 * Return a {@link BeanRegistrationBeanFactoryContribution} that is capable of
	 * contributing the specified inner {@link BeanDefinition}.
	 * @param parent the contribution of the parent bean definition
	 * @param innerBeanDefinition the inner bean definition
	 * @return a contribution for the specified inner bean definition
	 */
	BeanRegistrationBeanFactoryContribution getInnerBeanRegistrationContribution(
			BeanRegistrationBeanFactoryContribution parent, BeanDefinition innerBeanDefinition) {
		BeanDefinitionValueResolver bdvr = new BeanDefinitionValueResolver(this.beanFactory,
				parent.getBeanName(), parent.getBeanDefinition());
		return bdvr.resolveInnerBean(null, innerBeanDefinition, (beanName, bd) ->
				new InnerBeanRegistrationBeanFactoryContribution(beanName, bd,
						getBeanInstantiationGenerator(beanName, bd), this));
	}

	private Executable determineExecutable(RootBeanDefinition beanDefinition) {
		Executable executable = this.executableProvider.detectBeanInstanceExecutable(beanDefinition);
		if (executable == null) {
			throw new IllegalStateException("No suitable executor found for " + beanDefinition);
		}
		return executable;
	}

	private List<BeanInstantiationContribution> determineBeanInstanceContributions(
			String beanName, RootBeanDefinition beanDefinition) {
		List<BeanInstantiationContribution> contributions = new ArrayList<>();
		for (AotContributingBeanPostProcessor pp : this.beanPostProcessors.get()) {
			BeanInstantiationContribution contribution = pp.contribute(beanDefinition,
					beanDefinition.getResolvableType().toClass(), beanName);
			if (contribution != null) {
				contributions.add(contribution);
			}
		}
		return contributions;
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		if (postProcessors.size() <= 1) {
			return;
		}
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	// FIXME: copy-paste from Spring Native that should go away in favor of ConstructorResolver
	private static class ExecutableProvider {

		private static final Log logger = LogFactory.getLog(ExecutableProvider.class);

		private final ConfigurableBeanFactory beanFactory;

		private final ClassLoader classLoader;

		ExecutableProvider(ConfigurableBeanFactory beanFactory) {
			this.beanFactory = beanFactory;
			this.classLoader = (beanFactory.getBeanClassLoader() != null
					? beanFactory.getBeanClassLoader() : getClass().getClassLoader());
		}

		@Nullable
		Executable detectBeanInstanceExecutable(BeanDefinition beanDefinition) {
			Supplier<ResolvableType> beanType = () -> getBeanType(beanDefinition);
			List<ResolvableType> valueTypes = beanDefinition.hasConstructorArgumentValues()
					? determineParameterValueTypes(beanDefinition.getConstructorArgumentValues()) : Collections.emptyList();
			Method resolvedFactoryMethod = resolveFactoryMethod(beanDefinition, valueTypes);
			if (resolvedFactoryMethod != null) {
				return resolvedFactoryMethod;
			}
			Class<?> factoryBeanClass = getFactoryBeanClass(beanDefinition);
			if (factoryBeanClass != null && !factoryBeanClass.equals(beanDefinition.getResolvableType().toClass())) {
				ResolvableType resolvableType = beanDefinition.getResolvableType();
				boolean isCompatible = ResolvableType.forClass(factoryBeanClass).as(FactoryBean.class)
						.getGeneric(0).isAssignableFrom(resolvableType);
				if (isCompatible) {
					return resolveConstructor(() -> ResolvableType.forClass(factoryBeanClass), valueTypes);
				}
				else {
					throw new IllegalStateException(String.format("Incompatible target type '%s' for factory bean '%s'",
							resolvableType.toClass().getName(), factoryBeanClass.getName()));
				}
			}
			Executable resolvedConstructor = resolveConstructor(beanType, valueTypes);
			if (resolvedConstructor != null) {
				return resolvedConstructor;
			}
			Executable resolvedConstructorOrFactoryMethod = getField(beanDefinition,
					"resolvedConstructorOrFactoryMethod", Executable.class);
			if (resolvedConstructorOrFactoryMethod != null) {
				logger.error("resolvedConstructorOrFactoryMethod required for " + beanDefinition);
				return resolvedConstructorOrFactoryMethod;
			}
			return null;
		}

		private List<ResolvableType> determineParameterValueTypes(ConstructorArgumentValues constructorArgumentValues) {
			List<ResolvableType> parameterTypes = new ArrayList<>();
			for (ValueHolder valueHolder : constructorArgumentValues.getIndexedArgumentValues().values()) {
				if (valueHolder.getType() != null) {
					parameterTypes.add(ResolvableType.forClass(loadClass(valueHolder.getType())));
				}
				else {
					Object value = valueHolder.getValue();
					if (value instanceof BeanReference) {
						parameterTypes.add(ResolvableType.forClass(
								this.beanFactory.getType(((BeanReference) value).getBeanName(), false)));
					}
					else if (value instanceof BeanDefinition) {
						parameterTypes.add(extractTypeFromBeanDefinition(getBeanType((BeanDefinition) value)));
					}
					else {
						parameterTypes.add(ResolvableType.forInstance(value));
					}
				}
			}
			return parameterTypes;
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
				List<Method> methods = new ArrayList<>();
				Class<?> beanClass = getBeanClass(beanDefinition);
				if (beanClass == null) {
					throw new IllegalStateException("Failed to determine bean class of " + beanDefinition);
				}
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
							valueTypes, FallbackMode.NONE)).toList();
			if (matches.size() == 1) {
				return matches.get(0);
			}
			List<? extends Executable> assignableElementFallbackMatches = Arrays.stream(constructors)
					.filter(executable -> match(parameterTypesFactory.apply(executable),
							valueTypes, FallbackMode.ASSIGNABLE_ELEMENT)).toList();
			if (assignableElementFallbackMatches.size() == 1) {
				return assignableElementFallbackMatches.get(0);
			}
			List<? extends Executable> typeConversionFallbackMatches = Arrays.stream(constructors)
					.filter(executable -> match(parameterTypesFactory.apply(executable),
							valueTypes, ExecutableProvider.FallbackMode.TYPE_CONVERSION)).toList();
			return (typeConversionFallbackMatches.size() == 1) ? typeConversionFallbackMatches.get(0) : null;
		}

		private Executable resolveFactoryMethod(List<Method> executables,
				Function<Method, List<ResolvableType>> parameterTypesFactory, List<ResolvableType> valueTypes) {
			List<? extends Executable> matches = executables.stream()
					.filter(executable -> match(parameterTypesFactory.apply(executable),
							valueTypes, ExecutableProvider.FallbackMode.NONE)).toList();
			if (matches.size() == 1) {
				return matches.get(0);
			}
			List<? extends Executable> assignableElementFallbackMatches = executables.stream()
					.filter(executable -> match(parameterTypesFactory.apply(executable),
							valueTypes, ExecutableProvider.FallbackMode.ASSIGNABLE_ELEMENT)).toList();
			if (assignableElementFallbackMatches.size() == 1) {
				return assignableElementFallbackMatches.get(0);
			}
			List<? extends Executable> typeConversionFallbackMatches = executables.stream()
					.filter(executable -> match(parameterTypesFactory.apply(executable),
							valueTypes, ExecutableProvider.FallbackMode.TYPE_CONVERSION)).toList();
			if (typeConversionFallbackMatches.size() > 1) {
				throw new IllegalStateException("Multiple matches with parameters '"
						+ valueTypes + "': " + typeConversionFallbackMatches);
			}
			return (typeConversionFallbackMatches.size() == 1) ? typeConversionFallbackMatches.get(0) : null;
		}

		private boolean match(List<ResolvableType> parameterTypes, List<ResolvableType> valueTypes,
				ExecutableProvider.FallbackMode fallbackMode) {
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

		private boolean isMatch(ResolvableType parameterType, ResolvableType valueType,
				ExecutableProvider.FallbackMode fallbackMode) {
			if (isAssignable(valueType).test(parameterType)) {
				return true;
			}
			return switch (fallbackMode) {
				case ASSIGNABLE_ELEMENT -> isAssignable(valueType).test(extractElementType(parameterType));
				case TYPE_CONVERSION -> typeConversionFallback(valueType).test(parameterType);
				default -> false;
			};
		}

		private Predicate<ResolvableType> isAssignable(ResolvableType valueType) {
			return parameterType -> {
				if (valueType.hasUnresolvableGenerics()) {
					return parameterType.toClass().isAssignableFrom(valueType.toClass());
				}
				else {
					return parameterType.isAssignableFrom(valueType);
				}
			};
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
				return valueOrCollection(valueType, this::isSimpleConvertibleType).test(parameterType);
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
		 * Return a {@link Predicate} for a parameter type that checks if its target value
		 * is a {@link Class} and the value type is a {@link String}. This is a regular use
		 * cases where a {@link Class} is defined in the bean definition as an FQN.
		 * @param valueType the type of the value
		 * @return a predicate to indicate a fallback match for a String to Class parameter
		 */
		private Predicate<ResolvableType> isStringForClassFallback(ResolvableType valueType) {
			return parameterType -> (valueType.isAssignableFrom(String.class)
					&& parameterType.isAssignableFrom(Class.class));
		}

		private Predicate<ResolvableType> isSimpleConvertibleType(ResolvableType valueType) {
			return parameterType -> isSimpleConvertibleType(parameterType.toClass())
					&& isSimpleConvertibleType(valueType.toClass());
		}

		@Nullable
		private Class<?> getFactoryBeanClass(BeanDefinition beanDefinition) {
			if (beanDefinition instanceof RootBeanDefinition rbd) {
				if (rbd.hasBeanClass()) {
					Class<?> beanClass = rbd.getBeanClass();
					return FactoryBean.class.isAssignableFrom(beanClass) ? beanClass : null;
				}
			}
			return null;
		}

		@Nullable
		private Class<?> getBeanClass(BeanDefinition beanDefinition) {
			if (beanDefinition instanceof AbstractBeanDefinition abd) {
				return abd.hasBeanClass() ? abd.getBeanClass() : loadClass(abd.getBeanClassName());
			}
			return (beanDefinition.getBeanClassName() != null) ? loadClass(beanDefinition.getBeanClassName()) : null;
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
			throw new IllegalStateException("Failed to determine bean class of " + beanDefinition);
		}

		private Class<?> loadClass(String beanClassName) {
			try {
				return ClassUtils.forName(beanClassName, this.classLoader);
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException("Failed to load class " + beanClassName);
			}
		}

		@Nullable
		private <T> T getField(BeanDefinition beanDefinition, String fieldName, Class<T> targetType) {
			Field field = ReflectionUtils.findField(RootBeanDefinition.class, fieldName);
			ReflectionUtils.makeAccessible(field);
			return targetType.cast(ReflectionUtils.getField(field, beanDefinition));
		}

		public static boolean isSimpleConvertibleType(Class<?> type) {
			return (type.isPrimitive() && type != void.class) ||
					type == Double.class || type == Float.class || type == Long.class ||
					type == Integer.class || type == Short.class || type == Character.class ||
					type == Byte.class || type == Boolean.class || type == String.class;
		}


		enum FallbackMode {

			NONE,

			ASSIGNABLE_ELEMENT,

			TYPE_CONVERSION

		}

	}

}
