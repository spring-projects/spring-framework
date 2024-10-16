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

package org.springframework.test.context.bean.override.mockito;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.mockito.Answers;
import org.mockito.MockSettings;
import org.mockito.Mockito;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.BeanOverrideHandler;
import org.springframework.test.context.bean.override.BeanOverrideStrategy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import static org.springframework.test.context.bean.override.BeanOverrideStrategy.REPLACE;
import static org.springframework.test.context.bean.override.BeanOverrideStrategy.REPLACE_OR_CREATE;

/**
 * {@link BeanOverrideHandler} implementation for Mockito {@code mock} support.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
class MockitoBeanOverrideHandler extends AbstractMockitoBeanOverrideHandler {

	private final Set<Class<?>> extraInterfaces;

	private final Answers answers;

	private final boolean serializable;


	MockitoBeanOverrideHandler(Field field, ResolvableType typeToMock, MockitoBean mockitoBean) {
		this(field, typeToMock, (!mockitoBean.name().isBlank() ? mockitoBean.name() : null),
			(mockitoBean.enforceOverride() ? REPLACE : REPLACE_OR_CREATE),
			mockitoBean.reset(), mockitoBean.extraInterfaces(), mockitoBean.answers(), mockitoBean.serializable());
	}

	private MockitoBeanOverrideHandler(Field field, ResolvableType typeToMock, @Nullable String beanName,
			BeanOverrideStrategy strategy, MockReset reset, Class<?>[] extraInterfaces, @Nullable Answers answers,
			boolean serializable) {

		super(field, typeToMock, beanName, strategy, reset, false);
		Assert.notNull(typeToMock, "'typeToMock' must not be null");
		this.extraInterfaces = asClassSet(extraInterfaces);
		this.answers = (answers != null ? answers : Answers.RETURNS_DEFAULTS);
		this.serializable = serializable;
	}


	private static Set<Class<?>> asClassSet(Class<?>[] classes) {
		if (classes.length == 0) {
			return Collections.emptySet();
		}
		Set<Class<?>> classSet = new LinkedHashSet<>(Arrays.asList(classes));
		return Collections.unmodifiableSet(classSet);
	}


	/**
	 * Return the extra interfaces.
	 * @return the extra interfaces or an empty set
	 */
	Set<Class<?>> getExtraInterfaces() {
		return this.extraInterfaces;
	}

	/**
	 * Return the {@link Answers}.
	 * @return the answers mode
	 */
	Answers getAnswers() {
		return this.answers;
	}

	/**
	 * Determine if the mock is serializable.
	 * @return {@code true} if the mock is serializable
	 */
	boolean isSerializable() {
		return this.serializable;
	}

	@Override
	protected Object createOverrideInstance(String beanName, @Nullable BeanDefinition existingBeanDefinition, @Nullable Object existingBeanInstance) {
		return createMock(beanName);
	}

	@SuppressWarnings("unchecked")
	private <T> T createMock(String name) {
		MockSettings settings = MockReset.withSettings(getReset());
		if (StringUtils.hasLength(name)) {
			settings.name(name);
		}
		if (!this.extraInterfaces.isEmpty()) {
			settings.extraInterfaces(ClassUtils.toClassArray(this.extraInterfaces));
		}
		settings.defaultAnswer(this.answers);
		if (this.serializable) {
			settings.serializable();
		}
		Class<?> targetType = getBeanType().resolve();
		return (T) Mockito.mock(targetType, settings);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		return (other instanceof MockitoBeanOverrideHandler that && super.equals(that) &&
				(this.serializable == that.serializable) && (this.answers == that.answers) &&
				Objects.equals(this.extraInterfaces, that.extraInterfaces));
	}

	@Override
	public int hashCode() {
		return super.hashCode() + Objects.hash(this.extraInterfaces, this.answers, this.serializable);
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("field", getField())
				.append("beanType", getBeanType())
				.append("beanName", getBeanName())
				.append("strategy", getStrategy())
				.append("reset", getReset())
				.append("extraInterfaces", getExtraInterfaces())
				.append("answers", getAnswers())
				.append("serializable", isSerializable())
				.toString();
	}

}
