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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.BeanOverrideStrategy;
import org.springframework.test.context.bean.override.OverrideMetadata;
import org.springframework.util.ReflectionUtils;

/**
 * {@link OverrideMetadata} implementation for {@link TestBean}.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @since 6.2
 */
final class TestBeanOverrideMetadata extends OverrideMetadata {

	private final Method overrideMethod;


	TestBeanOverrideMetadata(Field field, ResolvableType beanType, @Nullable String beanName,
			Method overrideMethod) {

		super(field, beanType, beanName, BeanOverrideStrategy.REPLACE_DEFINITION);
		this.overrideMethod = overrideMethod;
	}


	@Override
	protected Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition,
			@Nullable Object existingBeanInstance) {

		try {
			ReflectionUtils.makeAccessible(this.overrideMethod);
			return this.overrideMethod.invoke(null);
		}
		catch (IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException("Failed to invoke bean overriding method " + this.overrideMethod.getName() +
					"; a static method with no formal parameters is expected", ex);
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		if (!super.equals(other)) {
			return false;
		}
		TestBeanOverrideMetadata that = (TestBeanOverrideMetadata) other;
		return Objects.equals(this.overrideMethod, that.overrideMethod);
	}

	@Override
	public int hashCode() {
		return this.overrideMethod.hashCode() * 29 + super.hashCode();
	}
}
