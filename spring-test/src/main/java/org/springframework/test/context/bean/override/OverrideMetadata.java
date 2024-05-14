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

package org.springframework.test.context.bean.override;

import java.lang.reflect.Field;
import java.util.Objects;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;

/**
 * Metadata for Bean Override injection points, also responsible for creation of
 * the overriding instance.
 *
 * <p><strong>WARNING</strong>: implementations are used as a cache key and
 * must implement proper {@code equals()} and {@code hashCode()} methods.
 *
 * <p>Specific implementations of metadata can have state to be used during
 * override {@linkplain #createOverride(String, BeanDefinition, Object)
 * instance creation} &mdash; for example, based on further parsing of the
 * annotation or the annotated field.
 *
 * @author Simon Baslé
 * @since 6.2
 */
public abstract class OverrideMetadata {

	private final Field field;

	private final ResolvableType beanType;

	private final BeanOverrideStrategy strategy;


	protected OverrideMetadata(Field field, ResolvableType beanType,
			BeanOverrideStrategy strategy) {

		this.field = field;
		this.beanType = beanType;
		this.strategy = strategy;
	}

	/**
	 * Get the bean name to override, or {@code null} to look for a single
	 * matching bean of type {@link #getBeanType()}.
	 * <p>Defaults to {@code null}.
	 */
	@Nullable
	protected String getBeanName() {
		return null;
	}

	/**
	 * Get the bean {@linkplain ResolvableType type} to override.
	 */
	public final ResolvableType getBeanType() {
		return this.beanType;
	}

	/**
	 * Get the annotated {@link Field}.
	 */
	public final Field getField() {
		return this.field;
	}

	/**
	 * Get the {@link BeanOverrideStrategy} for this instance, as a hint on
	 * how and when the override instance should be created.
	 */
	public final BeanOverrideStrategy getStrategy() {
		return this.strategy;
	}

	/**
	 * Create an override instance from this {@link OverrideMetadata},
	 * optionally provided with an existing {@link BeanDefinition} and/or an
	 * original instance, that is a singleton or an early wrapped instance.
	 * @param beanName the name of the bean being overridden
	 * @param existingBeanDefinition an existing bean definition for the supplied
	 * bean name, or {@code null} if not relevant
	 * @param existingBeanInstance an existing instance for the supplied bean name
	 * for wrapping purposes, or {@code null} if irrelevant
	 * @return the instance with which to override the bean
	 */
	protected abstract Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition,
			@Nullable Object existingBeanInstance);

	/**
	 * Optionally track objects created by this {@link OverrideMetadata}.
	 * <p>The default is not to track, but this can be overridden in subclasses.
	 * @param override the bean override instance to track
	 * @param trackingBeanRegistry the registry in which trackers can
	 * optionally be registered
	 */
	protected void track(Object override, SingletonBeanRegistry trackingBeanRegistry) {
		// NO-OP
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || !getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		OverrideMetadata that = (OverrideMetadata) obj;
		return Objects.equals(this.strategy, that.strategy) &&
				Objects.equals(this.field, that.field) &&
				Objects.equals(this.beanType, that.beanType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.strategy, this.field, this.beanType);
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("strategy", this.strategy)
				.append("field", this.field)
				.append("beanType", this.beanType)
				.toString();
	}

}
