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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.BeanInfoFactory;
import org.springframework.beans.ExtendedBeanInfoFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Internal code generator to set {@link RootBeanDefinition} properties.
 * <p>
 * Generates code in the following form:<blockquote><pre class="code">
 * beanDefinition.setPrimary(true);
 * beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
 * ...
 * </pre></blockquote>
 * <p>
 * The generated code expects the following variables to be available:
 * <p>
 * <ul>
 * <li>{@code beanDefinition} - The {@link RootBeanDefinition} to
 * configure.</li>
 * </ul>
 * <p>
 * Note that this generator does <b>not</b> set the {@link InstanceSupplier}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
class BeanDefinitionPropertiesCodeGenerator {

	private static final RootBeanDefinition DEFAULT_BEAN_DEFINITION = new RootBeanDefinition();

	private static final String BEAN_DEFINITION_VARIABLE = BeanRegistrationCodeFragments.BEAN_DEFINITION_VARIABLE;

	private static final BeanInfoFactory beanInfoFactory = new ExtendedBeanInfoFactory();

	private final RuntimeHints hints;

	private final Predicate<String> attributeFilter;

	private final BiFunction<String, Object, CodeBlock> customValueCodeGenerator;

	private final BeanDefinitionPropertyValueCodeGenerator valueCodeGenerator;


	BeanDefinitionPropertiesCodeGenerator(RuntimeHints hints,
			Predicate<String> attributeFilter, GeneratedMethods generatedMethods,
			BiFunction<String, Object, CodeBlock> customValueCodeGenerator) {

		this.hints = hints;
		this.attributeFilter = attributeFilter;
		this.customValueCodeGenerator = customValueCodeGenerator;
		this.valueCodeGenerator = new BeanDefinitionPropertyValueCodeGenerator(
				generatedMethods);
	}


	CodeBlock generateCode(RootBeanDefinition beanDefinition) {
		CodeBlock.Builder code = CodeBlock.builder();
		addStatementForValue(code, beanDefinition, BeanDefinition::isPrimary,
				"$L.setPrimary($L)");
		addStatementForValue(code, beanDefinition, BeanDefinition::getScope,
				this::hasScope, "$L.setScope($S)");
		addStatementForValue(code, beanDefinition, BeanDefinition::getDependsOn,
				this::hasDependsOn, "$L.setDependsOn($L)", this::toStringVarArgs);
		addStatementForValue(code, beanDefinition, BeanDefinition::isAutowireCandidate,
				"$L.setAutowireCandidate($L)");
		addStatementForValue(code, beanDefinition, BeanDefinition::getRole,
				this::hasRole, "$L.setRole($L)", this::toRole);
		addStatementForValue(code, beanDefinition, AbstractBeanDefinition::getLazyInit,
				"$L.setLazyInit($L)");
		addStatementForValue(code, beanDefinition, AbstractBeanDefinition::isSynthetic,
				"$L.setSynthetic($L)");
		addInitDestroyMethods(code, beanDefinition, beanDefinition.getInitMethodNames(),
				"$L.setInitMethodNames($L)");
		addInitDestroyMethods(code, beanDefinition, beanDefinition.getDestroyMethodNames(),
				"$L.setDestroyMethodNames($L)");
		addConstructorArgumentValues(code, beanDefinition);
		addPropertyValues(code, beanDefinition);
		addAttributes(code, beanDefinition);
		return code.build();
	}

	private void addInitDestroyMethods(Builder code,
			AbstractBeanDefinition beanDefinition, @Nullable String[] methodNames, String format) {
		if (!ObjectUtils.isEmpty(methodNames)) {
			Class<?> beanType = ClassUtils.getUserClass(beanDefinition.getResolvableType().toClass());
			Arrays.stream(methodNames).forEach(methodName -> addInitDestroyHint(beanType, methodName));
			CodeBlock arguments = Arrays.stream(methodNames)
					.map(name -> CodeBlock.of("$S", name))
					.collect(CodeBlock.joining(", "));
			code.addStatement(format, BEAN_DEFINITION_VARIABLE, arguments);
		}
	}

	private void addInitDestroyHint(Class<?> beanUserClass, String methodName) {
		Method method = ReflectionUtils.findMethod(beanUserClass, methodName);
		if (method != null) {
			this.hints.reflection().registerMethod(method);
		}
	}

	private void addConstructorArgumentValues(CodeBlock.Builder code,
			BeanDefinition beanDefinition) {

		Map<Integer, ValueHolder> argumentValues = beanDefinition
				.getConstructorArgumentValues().getIndexedArgumentValues();
		if (!argumentValues.isEmpty()) {
			argumentValues.forEach((index, valueHolder) -> {
				String name = valueHolder.getName();
				Object value = valueHolder.getValue();
				CodeBlock valueCode = this.customValueCodeGenerator.apply(name, value);
				if (valueCode == null) {
					valueCode = this.valueCodeGenerator.generateCode(value);
				}
				code.addStatement(
						"$L.getConstructorArgumentValues().addIndexedArgumentValue($L, $L)",
						BEAN_DEFINITION_VARIABLE, index, valueCode);
			});
		}
	}

	private void addPropertyValues(CodeBlock.Builder code,
			RootBeanDefinition beanDefinition) {

		MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
		if (!propertyValues.isEmpty()) {
			for (PropertyValue propertyValue : propertyValues) {
				String name = propertyValue.getName();
				Object value = propertyValue.getValue();
				CodeBlock valueCode = this.customValueCodeGenerator.apply(name, value);
				if (valueCode == null) {
					valueCode = this.valueCodeGenerator.generateCode(value);
				}
				code.addStatement("$L.getPropertyValues().addPropertyValue($S, $L)",
						BEAN_DEFINITION_VARIABLE, propertyValue.getName(), valueCode);
			}
			Class<?> infrastructureType = getInfrastructureType(beanDefinition);
			BeanInfo beanInfo = (infrastructureType != Object.class) ? getBeanInfo(infrastructureType) : null;
			if (beanInfo != null) {
				Map<String, Method> writeMethods = getWriteMethods(beanInfo);
				for (PropertyValue propertyValue : propertyValues) {
					Method writeMethod = writeMethods.get(propertyValue.getName());
					if (writeMethod != null) {
						this.hints.reflection().registerMethod(writeMethod, ExecutableMode.INVOKE);
					}
				}
			}
		}
	}

	private Class<?> getInfrastructureType(RootBeanDefinition beanDefinition) {
		if (beanDefinition.hasBeanClass()) {
			Class<?> beanClass = beanDefinition.getBeanClass();
			if (FactoryBean.class.isAssignableFrom(beanClass)) {
				return beanClass;
			}
		}
		return ClassUtils.getUserClass(beanDefinition.getResolvableType().toClass());
	}

	@Nullable
	private BeanInfo getBeanInfo(Class<?> beanType) {
		try {
			BeanInfo beanInfo = beanInfoFactory.getBeanInfo(beanType);
			if (beanInfo != null) {
				return beanInfo;
			}
			return Introspector.getBeanInfo(beanType, Introspector.IGNORE_ALL_BEANINFO);
		}
		catch (IntrospectionException ex) {
			return null;
		}
	}

	private Map<String, Method> getWriteMethods(BeanInfo beanInfo) {
		Map<String, Method> writeMethods = new HashMap<>();
		for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
			writeMethods.put(propertyDescriptor.getName(),
					propertyDescriptor.getWriteMethod());
		}
		return Collections.unmodifiableMap(writeMethods);
	}

	private void addAttributes(CodeBlock.Builder code, BeanDefinition beanDefinition) {
		String[] attributeNames = beanDefinition.attributeNames();
		if (!ObjectUtils.isEmpty(attributeNames)) {
			for (String attributeName : attributeNames) {
				if (this.attributeFilter.test(attributeName)) {
					CodeBlock value = this.valueCodeGenerator
							.generateCode(beanDefinition.getAttribute(attributeName));
					code.addStatement("$L.setAttribute($S, $L)",
							BEAN_DEFINITION_VARIABLE, attributeName, value);
				}
			}
		}
	}

	private boolean hasScope(String defaultValue, String actualValue) {
		return StringUtils.hasText(actualValue)
				&& !ConfigurableBeanFactory.SCOPE_SINGLETON.equals(actualValue);
	}

	private boolean hasDependsOn(String[] defaultValue, String[] actualValue) {
		return !ObjectUtils.isEmpty(actualValue);
	}

	private boolean hasRole(int defaultValue, int actualValue) {
		return actualValue != BeanDefinition.ROLE_APPLICATION;
	}

	private CodeBlock toStringVarArgs(String[] strings) {
		return Arrays.stream(strings).map(string -> CodeBlock.of("$S", string))
				.collect(CodeBlock.joining(","));
	}

	private Object toRole(int value) {
		return switch (value) {
			case BeanDefinition.ROLE_INFRASTRUCTURE -> CodeBlock.builder()
					.add("$T.ROLE_INFRASTRUCTURE", BeanDefinition.class).build();
			case BeanDefinition.ROLE_SUPPORT -> CodeBlock.builder()
					.add("$T.ROLE_SUPPORT", BeanDefinition.class).build();
			default -> value;
		};
	}

	private <B extends BeanDefinition, T> void addStatementForValue(
			CodeBlock.Builder code, BeanDefinition beanDefinition,
			Function<B, T> getter, String format) {

		addStatementForValue(code, beanDefinition, getter,
				(defaultValue, actualValue) -> !Objects.equals(defaultValue, actualValue),
				format);
	}

	private <B extends BeanDefinition, T> void addStatementForValue(
			CodeBlock.Builder code, BeanDefinition beanDefinition,
			Function<B, T> getter, BiPredicate<T, T> filter, String format) {

		addStatementForValue(code, beanDefinition, getter, filter, format,
				actualValue -> actualValue);
	}

	@SuppressWarnings("unchecked")
	private <B extends BeanDefinition, T> void addStatementForValue(
			CodeBlock.Builder code, BeanDefinition beanDefinition,
			Function<B, T> getter, BiPredicate<T, T> filter, String format,
			Function<T, Object> formatter) {

		T defaultValue = getter.apply((B) DEFAULT_BEAN_DEFINITION);
		T actualValue = getter.apply((B) beanDefinition);
		if (filter.test(defaultValue, actualValue)) {
			code.addStatement(format, BEAN_DEFINITION_VARIABLE,
					formatter.apply(actualValue));
		}
	}

}
