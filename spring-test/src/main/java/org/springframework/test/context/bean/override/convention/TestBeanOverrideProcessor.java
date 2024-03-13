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
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.BeanOverrideProcessor;
import org.springframework.test.context.bean.override.BeanOverrideStrategy;
import org.springframework.test.context.bean.override.OverrideMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link BeanOverrideProcessor} implementation primarily made to work with
 * fields annotated with {@link TestBean @TestBean}, but can also work with
 * arbitrary test bean override annotations provided the annotated field's
 * declaring class declares an appropriate test bean factory method according
 * to the conventions documented in {@link TestBean}.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
public class TestBeanOverrideProcessor implements BeanOverrideProcessor {

	/**
	 * Find a test bean factory {@link Method} in the given {@link Class} which
	 * meets the following criteria.
	 * <ul>
	 * <li>The method is static.
	 * <li>The method does not accept any arguments.
	 * <li>The method's return type matches the supplied {@code methodReturnType}.
	 * <li>The method's name is one of the supplied {@code methodNames}.
	 * </ul>
	 * @param clazz the class in which to search for the factory method
	 * @param methodReturnType the return type for the factory method
	 * @param methodNames a set of supported names for the factory method
	 * @return the corresponding factory method
	 * @throws IllegalStateException if a single matching factory method cannot
	 * be found
	 */
	public static Method findTestBeanFactoryMethod(Class<?> clazz, Class<?> methodReturnType, String... methodNames) {
		Assert.isTrue(methodNames.length > 0, "At least one candidate method name is required");
		Set<String> supportedNames = new LinkedHashSet<>(Arrays.asList(methodNames));
		List<Method> methods = Arrays.stream(clazz.getDeclaredMethods())
				.filter(method -> Modifier.isStatic(method.getModifiers()) &&
						supportedNames.contains(method.getName()) &&
						methodReturnType.isAssignableFrom(method.getReturnType()))
				.toList();

		Assert.state(!methods.isEmpty(), () -> """
				Failed to find a static test bean factory method in %s with return type %s \
				whose name matches one of the supported candidates %s""".formatted(
						clazz.getName(), methodReturnType.getName(), supportedNames));

		Assert.state(methods.size() == 1, () -> """
				Found %d competing static test bean factory methods in %s with return type %s \
				whose name matches one of the supported candidates %s""".formatted(
						methods.size(), clazz.getName(), methodReturnType.getName(), supportedNames));

		return methods.get(0);
	}

	@Override
	public OverrideMetadata createMetadata(Field field, Annotation overrideAnnotation, ResolvableType typeToOverride) {
		Class<?> declaringClass = field.getDeclaringClass();
		// If we can, get an explicit method name right away; fail fast if it doesn't match.
		if (overrideAnnotation instanceof TestBean testBeanAnnotation) {
			Method overrideMethod = null;
			String beanName = null;
			if (!testBeanAnnotation.methodName().isBlank()) {
				overrideMethod = findTestBeanFactoryMethod(declaringClass, field.getType(), testBeanAnnotation.methodName());
			}
			if (!testBeanAnnotation.name().isBlank()) {
				beanName = testBeanAnnotation.name();
			}
			return new MethodConventionOverrideMetadata(field, overrideMethod, beanName,
					overrideAnnotation, typeToOverride);
		}
		// Otherwise defer the resolution of the static method until OverrideMetadata#createOverride.
		return new MethodConventionOverrideMetadata(field, null, null, overrideAnnotation, typeToOverride);
	}


	static final class MethodConventionOverrideMetadata extends OverrideMetadata {

		@Nullable
		private final Method overrideMethod;

		@Nullable
		private final String beanName;

		public MethodConventionOverrideMetadata(Field field, @Nullable Method overrideMethod, @Nullable String beanName,
				Annotation overrideAnnotation, ResolvableType typeToOverride) {

			super(field, overrideAnnotation, typeToOverride, BeanOverrideStrategy.REPLACE_DEFINITION);
			this.overrideMethod = overrideMethod;
			this.beanName = beanName;
		}

		@Override
		protected String getExpectedBeanName() {
			if (StringUtils.hasText(this.beanName)) {
				return this.beanName;
			}
			return super.getExpectedBeanName();
		}

		@Override
		public String getBeanOverrideDescription() {
			return "method convention";
		}

		@Override
		protected Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition,
				@Nullable Object existingBeanInstance) {

			Method methodToInvoke = this.overrideMethod;
			if (methodToInvoke == null) {
				methodToInvoke = findTestBeanFactoryMethod(field().getDeclaringClass(), field().getType(),
						beanName + TestBean.CONVENTION_SUFFIX,
						field().getName() + TestBean.CONVENTION_SUFFIX);
			}

			try {
				ReflectionUtils.makeAccessible(methodToInvoke);
				return methodToInvoke.invoke(null);
			}
			catch (IllegalAccessException | InvocationTargetException ex) {
				throw new IllegalArgumentException("Could not invoke bean overriding method " + methodToInvoke.getName() +
						"; a static method with no formal parameters is expected", ex);
			}
		}
	}

}
