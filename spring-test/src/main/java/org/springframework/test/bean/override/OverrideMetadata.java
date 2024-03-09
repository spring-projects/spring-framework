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

package org.springframework.test.bean.override;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * Metadata for Bean Overrides.
 *
 * @author Simon Baslé
 * @since 6.2
 */
public abstract class OverrideMetadata {

	private final Field field;

	private final Annotation overrideAnnotation;

	private final ResolvableType typeToOverride;

	private final BeanOverrideStrategy strategy;

	protected OverrideMetadata(Field field, Annotation overrideAnnotation,
			ResolvableType typeToOverride, BeanOverrideStrategy strategy) {
		this.field = field;
		this.overrideAnnotation = overrideAnnotation;
		this.typeToOverride = typeToOverride;
		this.strategy = strategy;
	}

	/**
	 * Return a short human-readable description of the kind of override this
	 * instance handles.
	 */
	public abstract String getBeanOverrideDescription();

	/**
	 * Return the expected bean name to override. Typically, this is either
	 * explicitly set in the concrete annotations or defined by the annotated
	 * field's name.
	 * @return the expected bean name
	 */
	protected String getExpectedBeanName() {
		return this.field.getName();
	}

	/**
	 * Return the annotated {@link Field}.
	 */
	public Field field() {
		return this.field;
	}

	/**
	 * Return the concrete override annotation, that is the one meta-annotated
	 * with {@link BeanOverride}.
	 */
	public Annotation overrideAnnotation() {
		return this.overrideAnnotation;
	}

	/**
	 * Return the bean {@link ResolvableType type} to override.
	 */
	public ResolvableType typeToOverride() {
		return this.typeToOverride;
	}

	/**
	 * Return the {@link BeanOverrideStrategy} for this instance, as a hint on
	 * how and when the override instance should be created.
	 */
	public final BeanOverrideStrategy getBeanOverrideStrategy() {
		return this.strategy;
	}

	/**
	 * Create an override instance from this {@link OverrideMetadata},
	 * optionally provided with an existing {@link BeanDefinition} and/or an
	 * original instance, that is a singleton or an early wrapped instance.
	 * @param beanName the name of the bean being overridden
	 * @param existingBeanDefinition an existing bean definition for that bean
	 * name, or {@code null} if not relevant
	 * @param existingBeanInstance an existing instance for that bean name,
	 * for wrapping purpose, or {@code null} if irrelevant
	 * @return the instance with which to override the bean
	 */
	protected abstract Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition,
			@Nullable Object existingBeanInstance);

	/**
	 * Optionally track objects created by this {@link OverrideMetadata}
	 * (default is no tracking).
	 * @param override the bean override instance to track
	 * @param trackingBeanRegistry the registry in which trackers could
	 * optionally be registered
	 */
	protected void track(Object override, SingletonBeanRegistry trackingBeanRegistry) {
		//NO-OP
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
		return Objects.equals(this.field, that.field) &&
				Objects.equals(this.overrideAnnotation, that.overrideAnnotation) &&
				Objects.equals(this.strategy, that.strategy) &&
				Objects.equals(this.typeToOverride, that.typeToOverride);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.field, this.overrideAnnotation, this.strategy, this.typeToOverride);
	}

	@Override
	public String toString() {
		return "OverrideMetadata[" +
				"category=" + this.getBeanOverrideDescription() + ", " +
				"field=" + this.field + ", " +
				"overrideAnnotation=" + this.overrideAnnotation + ", " +
				"strategy=" + this.strategy + ", " +
				"typeToOverride=" + this.typeToOverride + ']';
	}
}
