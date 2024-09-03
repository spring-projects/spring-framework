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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
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
 *
 * <p>Generates code in the following form:<pre class="code">
 * beanDefinition.setPrimary(true);
 * beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
 * ...
 * </pre>
 *
 * <p>The generated code expects the following variables to be available:
 * <ul>
 * <li>{@code beanDefinition}: the {@link RootBeanDefinition} to configure</li>
 * </ul>
 *
 * <p>Note that this generator does <b>not</b> set the {@link InstanceSupplier}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.0
 */
class BeanDefinitionPropertiesCodeGenerator {

	private static final RootBeanDefinition DEFAULT_BEAN_DEFINITION = new RootBeanDefinition();

	private static final String BEAN_DEFINITION_VARIABLE = BeanRegistrationCodeFragments.BEAN_DEFINITION_VARIABLE;

	private final RuntimeHints hints;

	private final Predicate<String> attributeFilter;

	private final BeanDefinitionPropertyValueCodeGenerator valueCodeGenerator;


	BeanDefinitionPropertiesCodeGenerator(RuntimeHints hints,
			Predicate<String> attributeFilter, GeneratedMethods generatedMethods,
			BiFunction<String, Object, CodeBlock> customValueCodeGenerator) {

		this.hints = hints;
		this.attributeFilter = attributeFilter;
		this.valueCodeGenerator = new BeanDefinitionPropertyValueCodeGenerator(generatedMethods,
				(object, type) -> customValueCodeGenerator.apply(PropertyNamesStack.peek(), object));
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
		addQualifiers(code, beanDefinition);
		return code.build();
	}

	private void addInitDestroyMethods(Builder code, AbstractBeanDefinition beanDefinition,
			@Nullable String[] methodNames, String format) {
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
		Class<?> methodDeclaringClass = beanUserClass;

		// Parse fully-qualified method name if necessary.
		int indexOfDot = methodName.lastIndexOf('.');
		if (indexOfDot > 0) {
			String className = methodName.substring(0, indexOfDot);
			methodName = methodName.substring(indexOfDot + 1);
			if (!beanUserClass.getName().equals(className)) {
				try {
					methodDeclaringClass = ClassUtils.forName(className, beanUserClass.getClassLoader());
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Failed to load Class [" + className +
							"] from ClassLoader [" + beanUserClass.getClassLoader() + "]", ex);
				}
			}
		}

		Method method = ReflectionUtils.findMethod(methodDeclaringClass, methodName);
		if (method != null) {
			this.hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
		}
	}

	private void addConstructorArgumentValues(CodeBlock.Builder code, BeanDefinition beanDefinition) {
		Map<Integer, ValueHolder> argumentValues =
				beanDefinition.getConstructorArgumentValues().getIndexedArgumentValues();
		if (!argumentValues.isEmpty()) {
			argumentValues.forEach((index, valueHolder) -> {
				CodeBlock valueCode = generateValue(valueHolder.getName(), valueHolder.getValue());
				code.addStatement(
						"$L.getConstructorArgumentValues().addIndexedArgumentValue($L, $L)",
						BEAN_DEFINITION_VARIABLE, index, valueCode);
			});
		}
	}

	private void addPropertyValues(CodeBlock.Builder code, RootBeanDefinition beanDefinition) {
		MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
		if (!propertyValues.isEmpty()) {
			for (PropertyValue propertyValue : propertyValues) {
				String name = propertyValue.getName();
				CodeBlock valueCode = generateValue(name, propertyValue.getValue());
				code.addStatement("$L.getPropertyValues().addPropertyValue($S, $L)",
						BEAN_DEFINITION_VARIABLE, propertyValue.getName(), valueCode);
			}
			Class<?> infrastructureType = getInfrastructureType(beanDefinition);
			if (infrastructureType != Object.class) {
				Map<String, Method> writeMethods = getWriteMethods(infrastructureType);
				for (PropertyValue propertyValue : propertyValues) {
					Method writeMethod = writeMethods.get(propertyValue.getName());
					if (writeMethod != null) {
						this.hints.reflection().registerMethod(writeMethod, ExecutableMode.INVOKE);
					}
				}
			}
		}
	}

	private void addQualifiers(CodeBlock.Builder code, RootBeanDefinition beanDefinition) {
		Set<AutowireCandidateQualifier> qualifiers = beanDefinition.getQualifiers();
		if (!qualifiers.isEmpty()) {
			for (AutowireCandidateQualifier qualifier : qualifiers) {
				Collection<CodeBlock> arguments = new ArrayList<>();
				arguments.add(CodeBlock.of("$S", qualifier.getTypeName()));
				Object qualifierValue = qualifier.getAttribute(AutowireCandidateQualifier.VALUE_KEY);
				if (qualifierValue != null) {
					arguments.add(generateValue("value", qualifierValue));
				}
				code.addStatement("$L.addQualifier(new $T($L))", BEAN_DEFINITION_VARIABLE,
						AutowireCandidateQualifier.class, CodeBlock.join(arguments, ", "));
			}
		}
	}

	private CodeBlock generateValue(@Nullable String name, @Nullable Object value) {
		try {
			PropertyNamesStack.push(name);
			return this.valueCodeGenerator.generateCode(value);
		}
		finally {
			PropertyNamesStack.pop();
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

	private Map<String, Method> getWriteMethods(Class<?> clazz) {
		Map<String, Method> writeMethods = new HashMap<>();
		for (PropertyDescriptor propertyDescriptor : BeanUtils.getPropertyDescriptors(clazz)) {
			writeMethods.put(propertyDescriptor.getName(), propertyDescriptor.getWriteMethod());
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
		return StringUtils.hasText(actualValue) &&
				!ConfigurableBeanFactory.SCOPE_SINGLETON.equals(actualValue);
	}

	private boolean hasDependsOn(String[] defaultValue, String[] actualValue) {
		return !ObjectUtils.isEmpty(actualValue);
	}

	private boolean hasRole(int defaultValue, int actualValue) {
		return actualValue != BeanDefinition.ROLE_APPLICATION;
	}

	private CodeBlock toStringVarArgs(String[] strings) {
		return Arrays.stream(strings).map(string -> CodeBlock.of("$S", string)).collect(CodeBlock.joining(","));
	}

	private Object toRole(int value) {
		return switch (value) {
			case BeanDefinition.ROLE_INFRASTRUCTURE ->
				CodeBlock.builder().add("$T.ROLE_INFRASTRUCTURE", BeanDefinition.class).build();
			case BeanDefinition.ROLE_SUPPORT ->
				CodeBlock.builder().add("$T.ROLE_SUPPORT", BeanDefinition.class).build();
			default -> value;
		};
	}

	private <B extends BeanDefinition, T> void addStatementForValue(
			CodeBlock.Builder code, BeanDefinition beanDefinition,
			Function<B, T> getter, String format) {

		addStatementForValue(code, beanDefinition, getter,
				(defaultValue, actualValue) -> !Objects.equals(defaultValue, actualValue), format);
	}

	private <B extends BeanDefinition, T> void addStatementForValue(
			CodeBlock.Builder code, BeanDefinition beanDefinition,
			Function<B, T> getter, BiPredicate<T, T> filter, String format) {

		addStatementForValue(code, beanDefinition, getter, filter, format, actualValue -> actualValue);
	}

	@SuppressWarnings("unchecked")
	private <B extends BeanDefinition, T> void addStatementForValue(
			CodeBlock.Builder code, BeanDefinition beanDefinition,
			Function<B, T> getter, BiPredicate<T, T> filter, String format,
			Function<T, Object> formatter) {

		T defaultValue = getter.apply((B) DEFAULT_BEAN_DEFINITION);
		T actualValue = getter.apply((B) beanDefinition);
		if (filter.test(defaultValue, actualValue)) {
			code.addStatement(format, BEAN_DEFINITION_VARIABLE, formatter.apply(actualValue));
		}
	}

	static class PropertyNamesStack {

		private static final ThreadLocal<ArrayDeque<String>> threadLocal = ThreadLocal.withInitial(ArrayDeque::new);

		static void push(@Nullable String name) {
			String valueToSet = (name != null) ? name : "";
			threadLocal.get().push(valueToSet);
		}

		static void pop() {
			threadLocal.get().pop();
		}

		@Nullable
		static String peek() {
			String value = threadLocal.get().peek();
			return ("".equals(value) ? null : value);
		}

	}

}
