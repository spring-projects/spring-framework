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

package org.springframework.beans.factory.generator.config;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.BeanDefinitionValueResolver;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.core.MethodParameter;

/**
 * An {@link InjectedElementResolver} for an {@link Executable} that creates
 * a bean instance.
 *
 * @author Stephane Nicoll
 */

class InjectedConstructionResolver implements InjectedElementResolver {

	private final Executable executable;

	private final Class<?> targetType;

	private final String beanName;

	private final Function<DefaultListableBeanFactory, BeanDefinition> beanDefinitionResolver;

	InjectedConstructionResolver(Executable executable, Class<?> targetType, String beanName,
			Function<DefaultListableBeanFactory, BeanDefinition> beanDefinitionResolver) {
		this.executable = executable;
		this.targetType = targetType;
		this.beanName = beanName;
		this.beanDefinitionResolver = beanDefinitionResolver;
	}


	Executable getExecutable() {
		return this.executable;
	}

	@Override
	public InjectedElementAttributes resolve(DefaultListableBeanFactory beanFactory, boolean required) {
		int argumentCount = this.executable.getParameterCount();
		List<Object> arguments = new ArrayList<>();
		Set<String> autowiredBeans = new LinkedHashSet<>(argumentCount);
		TypeConverter typeConverter = beanFactory.getTypeConverter();
		ConstructorArgumentValues argumentValues = resolveArgumentValues(beanFactory);
		for (int i = 0; i < argumentCount; i++) {
			MethodParameter methodParam = createMethodParameter(i);
			ValueHolder valueHolder = argumentValues.getIndexedArgumentValue(i, null);
			if (valueHolder != null) {
				if (valueHolder.isConverted()) {
					arguments.add(valueHolder.getConvertedValue());
				}
				else {
					Object userValue = beanFactory.getTypeConverter()
							.convertIfNecessary(valueHolder.getValue(), methodParam.getParameterType());
					arguments.add(userValue);
				}
			}
			else {
				DependencyDescriptor depDescriptor = new DependencyDescriptor(methodParam, true);
				depDescriptor.setContainingClass(this.targetType);
				try {
					Object arg = resolveDependency(() -> beanFactory.resolveDependency(
							depDescriptor, this.beanName, autowiredBeans, typeConverter), methodParam.getParameterType());
					arguments.add(arg);
				}
				catch (BeansException ex) {
					throw new UnsatisfiedDependencyException(null, this.beanName, new InjectionPoint(methodParam), ex);
				}
			}
		}
		return new InjectedElementAttributes(arguments);
	}

	private Object resolveDependency(Supplier<Object> resolvedDependency, Class<?> dependencyType) {
		try {
			return resolvedDependency.get();
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Single constructor or factory method -> let's return an empty array/collection
			// for e.g. a vararg or a non-null List/Set/Map parameter.
			if (dependencyType.isArray()) {
				return Array.newInstance(dependencyType.getComponentType(), 0);
			}
			else if (CollectionFactory.isApproximableCollectionType(dependencyType)) {
				return CollectionFactory.createCollection(dependencyType, 0);
			}
			else if (CollectionFactory.isApproximableMapType(dependencyType)) {
				return CollectionFactory.createMap(dependencyType, 0);
			}
			throw ex;
		}
	}

	private ConstructorArgumentValues resolveArgumentValues(DefaultListableBeanFactory beanFactory) {
		ConstructorArgumentValues resolvedValues = new ConstructorArgumentValues();
		BeanDefinition beanDefinition = this.beanDefinitionResolver.apply(beanFactory);
		if (beanDefinition == null || !beanDefinition.hasConstructorArgumentValues()) {
			return resolvedValues;
		}
		ConstructorArgumentValues argumentValues = beanDefinition.getConstructorArgumentValues();
		BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(beanFactory,
				this.beanName, beanDefinition);
		for (Map.Entry<Integer, ValueHolder> entry : argumentValues.getIndexedArgumentValues().entrySet()) {
			int index = entry.getKey();
			ValueHolder valueHolder = entry.getValue();
			if (valueHolder.isConverted()) {
				resolvedValues.addIndexedArgumentValue(index, valueHolder);
			}
			else {
				Object resolvedValue =
						valueResolver.resolveValueIfNecessary("constructor argument", valueHolder.getValue());
				ValueHolder resolvedValueHolder =
						new ValueHolder(resolvedValue, valueHolder.getType(), valueHolder.getName());
				resolvedValueHolder.setSource(valueHolder);
				resolvedValues.addIndexedArgumentValue(index, resolvedValueHolder);
			}
		}
		return resolvedValues;
	}

	private MethodParameter createMethodParameter(int index) {
		if (this.executable instanceof Constructor) {
			return new MethodParameter((Constructor<?>) this.executable, index);
		}
		else {
			return new MethodParameter((Method) this.executable, index);
		}
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", InjectedConstructionResolver.class.getSimpleName() + "[", "]")
				.add("executable=" + this.executable)
				.toString();
	}

}
