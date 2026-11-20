/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.convention;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.context.bean.override.BeanOverrideHandler;
import org.springframework.test.context.bean.override.BeanOverrideStrategy;
import org.springframework.util.ReflectionUtils;

/**
 * {@link BeanOverrideHandler} implementation for {@link TestBean @TestBean}.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
final class TestBeanOverrideHandler extends BeanOverrideHandler {

	private final Method factoryMethod;


	TestBeanOverrideHandler(Field field, ResolvableType beanType, @Nullable String beanName,
			String contextName, BeanOverrideStrategy strategy, Method factoryMethod) {

		super(field, beanType, beanName, contextName, strategy);
		this.factoryMethod = factoryMethod;
	}


	@Override
	protected Object createOverrideInstance(String beanName, @Nullable BeanDefinition existingBeanDefinition,
			@Nullable Object existingBeanInstance) {

		try {
			ReflectionUtils.makeAccessible(this.factoryMethod);
			return this.factoryMethod.invoke(null);
		}
		catch (Throwable ex) {
			throw new IllegalStateException(
					"Failed to invoke @TestBean factory method: " + this.factoryMethod, ex);
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
		TestBeanOverrideHandler that = (TestBeanOverrideHandler) other;
		return Objects.equals(this.factoryMethod, that.factoryMethod);
	}

	@Override
	public int hashCode() {
		return this.factoryMethod.hashCode() * 29 + super.hashCode();
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("field", getField())
				.append("beanType", getBeanType())
				.append("beanName", getBeanName())
				.append("contextName", getContextName())
				.append("strategy", getStrategy())
				.append("factoryMethod", this.factoryMethod)
				.toString();
	}

}
