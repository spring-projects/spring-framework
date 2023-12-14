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

package org.springframework.test.bean.override.convention;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.test.bean.override.BeanOverrideProcessor;
import org.springframework.test.bean.override.BeanOverrideStrategy;
import org.springframework.test.bean.override.OverrideMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Simple {@link BeanOverrideProcessor} primarily made to work with the
 * {@link TestBean} annotation but can work with arbitrary override annotations
 * provided the annotated class has a relevant method according to the
 * convention documented in {@link TestBean}.
 *
 * @author Simon Baslé
 * @since 6.2
 */
public class TestBeanOverrideProcessor implements BeanOverrideProcessor {

	/**
	 * Ensures the {@code enclosingClass} has a static, no-arguments method with
	 * the provided {@code expectedMethodReturnType} and exactly one of the
	 * {@code expectedMethodNames}.
	 */
	public static Method ensureMethod(Class<?> enclosingClass, Class<?> expectedMethodReturnType,
			String... expectedMethodNames) {
		Assert.isTrue(expectedMethodNames.length > 0, "At least one expectedMethodName is required");
		Set<String> expectedNames = new LinkedHashSet<>(Arrays.asList(expectedMethodNames));
		final List<Method> found = Arrays.stream(enclosingClass.getDeclaredMethods())
				.filter(m -> Modifier.isStatic(m.getModifiers()))
				.filter(m -> expectedNames.contains(m.getName()) && expectedMethodReturnType
						.isAssignableFrom(m.getReturnType()))
				.collect(Collectors.toList());

		Assert.state(found.size() == 1, () -> "Found " + found.size() + " static methods " +
				"instead of exactly one, matching a name in " + expectedNames + " with return type " +
				expectedMethodReturnType.getName() + " on class " + enclosingClass.getName());

		return found.get(0);
	}

	@Override
	public OverrideMetadata createMetadata(Field field, Annotation overrideAnnotation, ResolvableType typeToOverride) {
		final Class<?> enclosingClass = field.getDeclaringClass();
		// if we can get an explicit method name right away, fail fast if it doesn't match
		if (overrideAnnotation instanceof TestBean testBeanAnnotation) {
			Method overrideMethod = null;
			String beanName = null;
			if (!testBeanAnnotation.methodName().isBlank()) {
				overrideMethod = ensureMethod(enclosingClass, field.getType(), testBeanAnnotation.methodName());
			}
			if (!testBeanAnnotation.name().isBlank()) {
				beanName = testBeanAnnotation.name();
			}
			return new MethodConventionOverrideMetadata(field, overrideMethod, beanName,
					overrideAnnotation, typeToOverride);
		}
		// otherwise defer the resolution of the static method until OverrideMetadata#createOverride
		return new MethodConventionOverrideMetadata(field, null, null, overrideAnnotation,
					typeToOverride);
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
				methodToInvoke = ensureMethod(field().getDeclaringClass(), field().getType(),
						beanName + TestBean.CONVENTION_SUFFIX,
						field().getName() + TestBean.CONVENTION_SUFFIX);
			}

			methodToInvoke.setAccessible(true);
			Object override;
			try {
				override = methodToInvoke.invoke(null);
			}
			catch (IllegalAccessException | InvocationTargetException ex) {
				throw new IllegalArgumentException("Could not invoke bean overriding method " + methodToInvoke.getName() +
						", a static method with no input parameters is expected", ex);
			}

			return override;
		}
	}

}
