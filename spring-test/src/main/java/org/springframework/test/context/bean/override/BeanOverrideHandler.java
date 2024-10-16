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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
 * Handler for Bean Override injection points that is responsible for creating
 * the bean override instance for a given set of metadata and potentially for
 * tracking the created instance.
 *
 * <p><strong>WARNING</strong>: Implementations are used as a cache key and must
 * implement proper {@code equals()} and {@code hashCode()} methods based on the
 * unique set of metadata used to identify the bean to override. Overridden
 * {@code equals()} and {@code hashCode()} methods should also delegate to the
 * {@code super} implementations in this class in order to support the basic
 * metadata used by all bean overrides.
 *
 * <p>Concrete implementations of {@code BeanOverrideHandler} can store additional
 * metadata to use during override {@linkplain #createOverrideInstance instance
 * creation} &mdash; for example, based on further processing of the annotation
 * or the annotated field.
 *
 * <p><strong>NOTE</strong>: Only <em>singleton</em> beans can be overridden.
 * Any attempt to override a non-singleton bean will result in an exception.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
public abstract class BeanOverrideHandler {

	private final Field field;

	private final Set<Annotation> fieldAnnotations;

	private final ResolvableType beanType;

	@Nullable
	private final String beanName;

	private final BeanOverrideStrategy strategy;


	protected BeanOverrideHandler(Field field, ResolvableType beanType, @Nullable String beanName,
			BeanOverrideStrategy strategy) {

		this.field = field;
		this.fieldAnnotations = annotationSet(field);
		this.beanType = beanType;
		this.beanName = beanName;
		this.strategy = strategy;
	}

	/**
	 * Process the given {@code testClass} and build the corresponding
	 * {@code BeanOverrideHandler} list derived from {@link BeanOverride @BeanOverride}
	 * fields in the test class, its type hierarchy, and its enclosing class hierarchy.
	 * @param testClass the test class to process
	 * @return a list of bean override handlers
	 */
	public static List<BeanOverrideHandler> forTestClass(Class<?> testClass) {
		List<BeanOverrideHandler> handlers = new LinkedList<>();
		ReflectionUtils.doWithFields(testClass, field -> processField(field, testClass, handlers));
		return handlers;
	}

	private static void processField(Field field, Class<?> testClass, List<BeanOverrideHandler> handlers) {
		AtomicBoolean overrideAnnotationFound = new AtomicBoolean();
		MergedAnnotations.from(field, DIRECT).stream(BeanOverride.class).forEach(mergedAnnotation -> {
			MergedAnnotation<?> metaSource = mergedAnnotation.getMetaSource();
			Assert.state(metaSource != null, "@BeanOverride annotation must be meta-present");

			BeanOverride beanOverride = mergedAnnotation.synthesize();
			BeanOverrideProcessor processor = BeanUtils.instantiateClass(beanOverride.value());
			Annotation composedAnnotation = metaSource.synthesize();

			Assert.state(overrideAnnotationFound.compareAndSet(false, true),
					() -> "Multiple @BeanOverride annotations found on field: " + field);
			BeanOverrideHandler handler = processor.createHandler(composedAnnotation, testClass, field);
			handlers.add(handler);
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
	public final String getBeanName() {
		return this.beanName;
	}

	/**
	 * Get the {@link BeanOverrideStrategy} for this {@code BeanOverrideHandler},
	 * which influences how and when the bean override instance should be created.
	 */
	public final BeanOverrideStrategy getStrategy() {
		return this.strategy;
	}

	/**
	 * {@linkplain #createOverrideInstance Create} and
	 * {@linkplain #trackOverrideInstance track} a bean override instance for an
	 * existing {@link BeanDefinition} or an existing singleton bean, based on the
	 * metadata in this {@code BeanOverrideHandler}.
	 * @param beanName the name of the bean being overridden
	 * @param existingBeanDefinition an existing bean definition for the supplied
	 * bean name, or {@code null} if not available or not relevant
	 * @param existingBeanInstance an existing instance for the supplied bean name
	 * for wrapping purposes, or {@code null} if not available or not relevant
	 * @param singletonBeanRegistry a registry in which this handler can store
	 * tracking state in the form of a singleton bean
	 * @return the instance with which to override the bean
	 * @see #trackOverrideInstance(Object, SingletonBeanRegistry)
	 * @see #createOverrideInstance(String, BeanDefinition, Object)
	 */
	final Object createOverrideInstance(
			String beanName, @Nullable BeanDefinition existingBeanDefinition,
			@Nullable Object existingBeanInstance, SingletonBeanRegistry singletonBeanRegistry) {

		Object override = createOverrideInstance(beanName, existingBeanDefinition, existingBeanInstance);
		trackOverrideInstance(override, singletonBeanRegistry);
		return override;
	}

	/**
	 * Create a bean override instance for an existing {@link BeanDefinition} or
	 * an existing singleton bean, based on the metadata in this
	 * {@code BeanOverrideHandler}.
	 * @param beanName the name of the bean being overridden
	 * @param existingBeanDefinition an existing bean definition for the supplied
	 * bean name, or {@code null} if not available or not relevant
	 * @param existingBeanInstance an existing instance for the supplied bean name
	 * for wrapping purposes, or {@code null} if not available or not relevant
	 * @return the instance with which to override the bean
	 * @see #trackOverrideInstance(Object, SingletonBeanRegistry)
	 */
	protected abstract Object createOverrideInstance(String beanName,
			@Nullable BeanDefinition existingBeanDefinition, @Nullable Object existingBeanInstance);

	/**
	 * Track the supplied bean override instance that was created by this
	 * {@code BeanOverrideHandler}.
	 * <p>The default implementation does not track the supplied instance, but
	 * this can be overridden in subclasses as appropriate.
	 * @param override the bean override instance to track
	 * @param singletonBeanRegistry a registry in which this handler can store
	 * tracking state in the form of a singleton bean
	 * @see #createOverrideInstance(String, BeanDefinition, Object)
	 */
	protected void trackOverrideInstance(Object override, SingletonBeanRegistry singletonBeanRegistry) {
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
		BeanOverrideHandler that = (BeanOverrideHandler) other;
		if (!Objects.equals(this.beanType.getType(), that.beanType.getType()) ||
				!Objects.equals(this.beanName, that.beanName) ||
				!Objects.equals(this.strategy, that.strategy)) {
			return false;
		}
		if (this.beanName != null) {
			return true;
		}

		// by-type lookup
		return (Objects.equals(this.field.getName(), that.field.getName()) &&
				this.fieldAnnotations.equals(that.fieldAnnotations));
	}

	@Override
	public int hashCode() {
		int hash = Objects.hash(getClass(), this.beanType.getType(), this.beanName, this.strategy);
		return (this.beanName != null ? hash : hash +
				Objects.hash(this.field.getName(), this.fieldAnnotations));
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

	private static Set<Annotation> annotationSet(Field field) {
		Annotation[] annotations = field.getAnnotations();
		return (annotations.length != 0 ? new HashSet<>(Arrays.asList(annotations)) : Collections.emptySet());
	}

}
