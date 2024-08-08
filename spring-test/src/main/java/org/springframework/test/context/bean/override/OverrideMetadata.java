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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import static org.springframework.core.annotation.MergedAnnotations.SearchStrategy.DIRECT;

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
 * @author Stephane Nicoll
 * @since 6.2
 */
public abstract class OverrideMetadata {

	private final Field field;

	private final ResolvableType beanType;

	@Nullable
	private final String beanName;

	private final BeanOverrideStrategy strategy;


	protected OverrideMetadata(Field field, ResolvableType beanType, @Nullable String beanName,
			BeanOverrideStrategy strategy) {
		this.field = field;
		this.beanType = beanType;
		this.beanName = beanName;
		this.strategy = strategy;
	}

	/**
	 * Parse the given {@code testClass} and build the corresponding list of
	 * bean {@code OverrideMetadata}.
	 * @param testClass the class to parse
	 * @return a list of {@code OverrideMetadata}
	 */
	public static List<OverrideMetadata> forTestClass(Class<?> testClass) {
		List<OverrideMetadata> metadata = new LinkedList<>();
		ReflectionUtils.doWithFields(testClass, field -> parseField(field, testClass, metadata));
		return metadata;
	}

	private static void parseField(Field field, Class<?> testClass, List<OverrideMetadata> metadataList) {
		AtomicBoolean overrideAnnotationFound = new AtomicBoolean();
		MergedAnnotations.from(field, DIRECT).stream(BeanOverride.class).forEach(mergedAnnotation -> {
			MergedAnnotation<?> metaSource = mergedAnnotation.getMetaSource();
			Assert.state(metaSource != null, "@BeanOverride annotation must be meta-present");

			BeanOverride beanOverride = mergedAnnotation.synthesize();
			BeanOverrideProcessor processor = BeanUtils.instantiateClass(beanOverride.value());
			Annotation composedAnnotation = metaSource.synthesize();

			Assert.state(overrideAnnotationFound.compareAndSet(false, true),
					() -> "Multiple @BeanOverride annotations found on field: " + field);
			OverrideMetadata metadata = processor.createMetadata(composedAnnotation, testClass, field);
			metadataList.add(metadata);
		});
	}


	/**
	 * Get the annotated {@link Field}.
	 */
	public final Field getField() {
		return this.field;
	}

	/**
	 * Get the bean {@linkplain ResolvableType type} to override.
	 */
	public final ResolvableType getBeanType() {
		return this.beanType;
	}

	/**
	 * Get the bean name to override, or {@code null} to look for a single
	 * matching bean of type {@link #getBeanType()}.
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
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
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		OverrideMetadata that = (OverrideMetadata) other;
		if (!Objects.equals(this.beanType.getType(), that.beanType.getType()) ||
				!Objects.equals(this.beanName, that.beanName) ||
				!Objects.equals(this.strategy, that.strategy)) {
			return false;
		}
		if (this.beanName != null) {
			return true;
		}
		// by type lookup
		return Objects.equals(this.field.getName(), that.field.getName()) &&
				Arrays.equals(this.field.getAnnotations(), that.field.getAnnotations());
	}

	@Override
	public int hashCode() {
		int hash = Objects.hash(getClass(), this.beanType.getType(), this.beanName, this.strategy);
		return (this.beanName != null ? hash : hash +
				Objects.hash(this.field.getName(), Arrays.hashCode(this.field.getAnnotations())));
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("field", this.field)
				.append("beanType", this.beanType)
				.append("beanName", this.beanName)
				.append("strategy", this.strategy)
				.toString();
	}

}
