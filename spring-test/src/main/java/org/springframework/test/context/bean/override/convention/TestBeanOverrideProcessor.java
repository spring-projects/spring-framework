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

package org.springframework.test.context.bean.override.convention;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.bean.override.BeanOverrideProcessor;
import org.springframework.test.context.bean.override.BeanOverrideStrategy;
import org.springframework.test.context.bean.override.OverrideMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link BeanOverrideProcessor} implementation for {@link TestBean @TestBean}
 * support. It creates metadata for annotated fields in a given class and
 * ensures that a corresponding static factory method exists, according to the
 * {@linkplain TestBean documented conventions}.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
class TestBeanOverrideProcessor implements BeanOverrideProcessor {

	/**
	 * Find a test bean factory {@link Method} in the given {@link Class} or one
	 * of its parent classes, which meets the following criteria.
	 * <ul>
	 * <li>The method is static.</li>
	 * <li>The method does not accept any arguments.</li>
	 * <li>The method's return type matches the supplied {@code methodReturnType}.</li>
	 * <li>The method's name is one of the supplied {@code methodNames}.</li>
	 * </ul>
	 * <p>If the test class inherits from another class, the class hierarchy is
	 * searched for factory methods. Matching factory methods are prioritized
	 * from closest to furthest from the test class in the class hierarchy,
	 * provided they have the same name. However, if multiple methods are found
	 * that match distinct candidate names, an exception is thrown.
	 * @param clazz the class in which to search for the factory method
	 * @param methodReturnType the return type for the factory method
	 * @param methodNames a set of supported names for the factory method
	 * @return the corresponding factory method
	 * @throws IllegalStateException if a matching factory method cannot
	 * be found or multiple methods match
	 */
	static Method findTestBeanFactoryMethod(Class<?> clazz, Class<?> methodReturnType, String... methodNames) {
		Assert.isTrue(methodNames.length > 0, "At least one candidate method name is required");
		Set<String> supportedNames = new LinkedHashSet<>(Arrays.asList(methodNames));
		List<Method> methods = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz))
				.filter(method -> Modifier.isStatic(method.getModifiers()) &&
						supportedNames.contains(method.getName()) &&
						methodReturnType.isAssignableFrom(method.getReturnType()))
				.toList();

		if (methods.isEmpty() && TestContextAnnotationUtils.searchEnclosingClass(clazz)) {
			methods = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz.getEnclosingClass()))
					.filter(method -> Modifier.isStatic(method.getModifiers()) &&
							supportedNames.contains(method.getName()) &&
							methodReturnType.isAssignableFrom(method.getReturnType()))
					.toList();
		}

		Assert.state(!methods.isEmpty(), () -> """
				Failed to find a static test bean factory method in %s with return type %s \
				whose name matches one of the supported candidates %s""".formatted(
						clazz.getName(), methodReturnType.getName(), supportedNames));

		long nameCount = methods.stream().map(Method::getName).distinct().count();
		int methodCount = methods.size();
		Assert.state(nameCount == 1, () -> """
				Found %d competing static test bean factory methods in %s with return type %s \
				whose name matches one of the supported candidates %s""".formatted(
					methodCount, clazz.getName(), methodReturnType.getName(), supportedNames));

		return methods.get(0);
	}

	@Override
	public TestBeanOverrideMetadata createMetadata(Annotation overrideAnnotation, Class<?> testClass, Field field) {
		if (!(overrideAnnotation instanceof TestBean testBeanAnnotation)) {
			throw new IllegalStateException(String.format("Invalid annotation passed to %s: expected @TestBean on field %s.%s",
					TestBeanOverrideProcessor.class.getSimpleName(), field.getDeclaringClass().getName(),
					field.getName()));
		}
		// If the user specified a method explicitly, search for that.
		// Otherwise, search candidate factory methods using the convention suffix
		// and the explicit bean name (if any) or field name.
		Method explicitOverrideMethod;
		if (!testBeanAnnotation.methodName().isBlank()) {
			explicitOverrideMethod = findTestBeanFactoryMethod(testClass, field.getType(), testBeanAnnotation.methodName());
		}
		else {
			String beanName = testBeanAnnotation.name();
			if (!StringUtils.hasText(beanName)) {
				explicitOverrideMethod = findTestBeanFactoryMethod(testClass, field.getType(),
					field.getName() + TestBean.CONVENTION_SUFFIX);
			}
			else {
				explicitOverrideMethod = findTestBeanFactoryMethod(testClass, field.getType(),
					beanName + TestBean.CONVENTION_SUFFIX,
						field.getName() + TestBean.CONVENTION_SUFFIX);
			}
		}

		return new TestBeanOverrideMetadata(field, explicitOverrideMethod, testBeanAnnotation, ResolvableType.forField(field, testClass));
	}


	static final class TestBeanOverrideMetadata extends OverrideMetadata {

		private final Method overrideMethod;

		private final String beanName;

		public TestBeanOverrideMetadata(Field field, Method overrideMethod, TestBean overrideAnnotation,
				ResolvableType typeToOverride) {

			super(field, typeToOverride, BeanOverrideStrategy.REPLACE_DEFINITION);
			this.beanName = overrideAnnotation.name();
			this.overrideMethod = overrideMethod;
		}

		@Override
		@Nullable
		protected String getBeanName() {
			return StringUtils.hasText(this.beanName) ? this.beanName : super.getBeanName();
		}

		@Override
		protected Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition,
				@Nullable Object existingBeanInstance) {

			try {
				ReflectionUtils.makeAccessible(this.overrideMethod);
				return this.overrideMethod.invoke(null);
			}
			catch (IllegalAccessException | InvocationTargetException ex) {
				throw new IllegalArgumentException("Could not invoke bean overriding method " + this.overrideMethod.getName() +
						"; a static method with no formal parameters is expected", ex);
			}
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}
			TestBeanOverrideMetadata that = (TestBeanOverrideMetadata) o;
			return Objects.equals(this.overrideMethod, that.overrideMethod)
					&& Objects.equals(this.beanName, that.beanName);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), this.overrideMethod, this.beanName);
		}
	}

}
