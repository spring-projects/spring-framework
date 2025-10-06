/*
 * Copyright 2002-present the original author or authors.
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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.context.TestContextAnnotationUtils;
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
 * metadata used by all bean overrides. In addition, it is recommended that
 * implementations override {@code toString()} to include all relevant metadata
 * in order to enhance diagnostics.
 *
 * <p>Concrete implementations of {@code BeanOverrideHandler} can store additional
 * metadata to use during override {@linkplain #createOverrideInstance instance
 * creation} &mdash; for example, based on further processing of the annotation,
 * the annotated field, or the annotated class.
 *
 * <h3>Singleton Semantics</h3>
 *
 * <p>When replacing a non-singleton bean, the non-singleton bean will be replaced
 * with a singleton bean corresponding to bean override instance created by the
 * handler, and the corresponding bean definition will be converted to a singleton.
 * Consequently, if a handler overrides a prototype or custom scoped bean, the
 * overridden bean will be treated as a singleton.
 *
 * <p>When replacing a bean created by a
 * {@link org.springframework.beans.factory.FactoryBean FactoryBean}, the
 * {@code FactoryBean} itself will be replaced with a singleton bean corresponding
 * to bean override instance created by the handler.
 *
 * <p>When wrapping a bean created by a
 * {@link org.springframework.beans.factory.FactoryBean FactoryBean}, the object
 * created by the {@code FactoryBean} will be wrapped, not the {@code FactoryBean}
 * itself.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 * @see BeanOverrideStrategy
 */
public abstract class BeanOverrideHandler {

	private static final Comparator<MergedAnnotation<? extends Annotation>> reversedMetaDistance =
			Comparator.<MergedAnnotation<? extends Annotation>> comparingInt(MergedAnnotation::getDistance).reversed();


	private final @Nullable Field field;

	private final Set<Annotation> qualifierAnnotations;

	private final ResolvableType beanType;

	private final @Nullable String beanName;

	private final String contextName;

	private final BeanOverrideStrategy strategy;


	/**
	 * Construct a new {@code BeanOverrideHandler} from the supplied values.
	 * <p>To provide proper support for
	 * {@link org.springframework.test.context.ContextHierarchy @ContextHierarchy},
	 * invoke {@link #BeanOverrideHandler(Field, ResolvableType, String, String, BeanOverrideStrategy)}
	 * instead.
	 * @param field the {@link Field} annotated with {@link BeanOverride @BeanOverride},
	 * or {@code null} if {@code @BeanOverride} was declared at the type level
	 * @param beanType the {@linkplain ResolvableType type} of bean to override
	 * @param beanName the name of the bean to override, or {@code null} to look
	 * for a single matching bean by type
	 * @param strategy the {@link BeanOverrideStrategy} to use
	 * @deprecated As of Spring Framework 6.2.6, in favor of
	 * {@link #BeanOverrideHandler(Field, ResolvableType, String, String, BeanOverrideStrategy)}
	 */
	@Deprecated(since = "6.2.6", forRemoval = true)
	protected BeanOverrideHandler(@Nullable Field field, ResolvableType beanType, @Nullable String beanName,
			BeanOverrideStrategy strategy) {

		this(field, beanType, beanName, "", strategy);
	}

	/**
	 * Construct a new {@code BeanOverrideHandler} from the supplied values.
	 * @param field the {@link Field} annotated with {@link BeanOverride @BeanOverride},
	 * or {@code null} if {@code @BeanOverride} was declared at the type level
	 * @param beanType the {@linkplain ResolvableType type} of bean to override
	 * @param beanName the name of the bean to override, or {@code null} to look
	 * for a single matching bean by type
	 * @param contextName the name of the context hierarchy level in which the
	 * handler should be applied, or an empty string to indicate that the handler
	 * should be applied to all application contexts within a context hierarchy
	 * @param strategy the {@link BeanOverrideStrategy} to use
	 * @since 6.2.6
	 */
	protected BeanOverrideHandler(@Nullable Field field, ResolvableType beanType, @Nullable String beanName,
			String contextName, BeanOverrideStrategy strategy) {

		this.field = field;
		this.qualifierAnnotations = getQualifierAnnotations(field);
		this.beanType = beanType;
		this.beanName = beanName;
		this.strategy = strategy;
		this.contextName = contextName;
	}

	/**
	 * Process the given {@code testClass} and build the corresponding
	 * {@code BeanOverrideHandler} list derived from {@link BeanOverride @BeanOverride}
	 * fields in the test class and its type hierarchy.
	 * <p>This method does not search the enclosing class hierarchy and does not
	 * search for {@code @BeanOverride} declarations on classes or interfaces.
	 * @param testClass the test class to process
	 * @return a list of bean override handlers
	 * @see #findAllHandlers(Class)
	 */
	public static List<BeanOverrideHandler> forTestClass(Class<?> testClass) {
		return findHandlers(testClass, true);
	}

	/**
	 * Process the given {@code testClass} and build the corresponding
	 * {@code BeanOverrideHandler} list derived from {@link BeanOverride @BeanOverride}
	 * fields in the test class and in its type hierarchy as well as from
	 * {@code @BeanOverride} declarations on classes and interfaces.
	 * <p>This method additionally searches for {@code @BeanOverride} declarations
	 * in the enclosing class hierarchy based on
	 * {@link TestContextAnnotationUtils#searchEnclosingClass(Class)} semantics.
	 * @param testClass the test class to process
	 * @return a list of bean override handlers
	 * @since 6.2.2
	 */
	static List<BeanOverrideHandler> findAllHandlers(Class<?> testClass) {
		return findHandlers(testClass, false);
	}

	private static List<BeanOverrideHandler> findHandlers(Class<?> testClass, boolean localFieldsOnly) {
		List<BeanOverrideHandler> handlers = new ArrayList<>();
		findHandlers(testClass, testClass, handlers, localFieldsOnly, new HashSet<>());
		return handlers;
	}

	/**
	 * Find handlers using tail recursion to ensure that "locally declared" bean overrides
	 * take precedence over inherited bean overrides.
	 * <p>Note: the search algorithm is effectively the inverse of the algorithm used in
	 * {@link org.springframework.test.context.TestContextAnnotationUtils#findAnnotationDescriptor(Class, Class)},
	 * but with tail recursion the semantics should be the same.
	 * @param clazz the class in/on which to search
	 * @param testClass the original test class
	 * @param handlers the list of handlers found
	 * @param localFieldsOnly whether to search only on local fields within the type hierarchy
	 * @param visitedTypes the set of types already visited
	 * @since 6.2.2
	 */
	private static void findHandlers(Class<?> clazz, Class<?> testClass, List<BeanOverrideHandler> handlers,
			boolean localFieldsOnly, Set<Class<?>> visitedTypes) {

		// 0) Ensure that we do not process the same class or interface multiple times.
		if (!visitedTypes.add(clazz)) {
			return;
		}

		// 1) Search enclosing class hierarchy.
		if (!localFieldsOnly && TestContextAnnotationUtils.searchEnclosingClass(clazz)) {
			findHandlers(clazz.getEnclosingClass(), testClass, handlers, localFieldsOnly, visitedTypes);
		}

		// 2) Search class hierarchy.
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null && superclass != Object.class) {
			findHandlers(superclass, testClass, handlers, localFieldsOnly, visitedTypes);
		}

		if (!localFieldsOnly) {
			// 3) Search interfaces.
			for (Class<?> ifc : clazz.getInterfaces()) {
				findHandlers(ifc, testClass, handlers, localFieldsOnly, visitedTypes);
			}

			// 4) Process current class.
			processClass(clazz, testClass, handlers);
		}

		// 5) Process fields in current class.
		ReflectionUtils.doWithLocalFields(clazz, field -> processField(field, testClass, handlers));
	}

	private static void processClass(Class<?> clazz, Class<?> testClass, List<BeanOverrideHandler> handlers) {
		processElement(clazz, testClass, (processor, composedAnnotation) ->
				processor.createHandlers(composedAnnotation, testClass).forEach(handlers::add));
	}

	private static void processField(Field field, Class<?> testClass, List<BeanOverrideHandler> handlers) {
		AtomicBoolean overrideAnnotationFound = new AtomicBoolean();
		processElement(field, testClass, (processor, composedAnnotation) -> {
			Assert.state(!Modifier.isStatic(field.getModifiers()),
					() -> "@BeanOverride field must not be static: " + field);
			Assert.state(overrideAnnotationFound.compareAndSet(false, true),
					() -> "Multiple @BeanOverride annotations found on field: " + field);
			handlers.add(processor.createHandler(composedAnnotation, testClass, field));
		});
	}

	private static void processElement(AnnotatedElement element, Class<?> testClass,
			BiConsumer<BeanOverrideProcessor, Annotation> consumer) {

		MergedAnnotations.from(element, DIRECT)
				.stream(BeanOverride.class)
				.sorted(reversedMetaDistance)
				.forEach(mergedAnnotation -> {
					MergedAnnotation<?> metaSource = mergedAnnotation.getMetaSource();
					Assert.state(metaSource != null, "@BeanOverride annotation must be meta-present");

					BeanOverride beanOverride = mergedAnnotation.synthesize();
					BeanOverrideProcessor processor = BeanUtils.instantiateClass(beanOverride.value());
					Annotation composedAnnotation = metaSource.synthesize();
					consumer.accept(processor, composedAnnotation);
				});
	}


	/**
	 * Get the {@link Field} annotated with {@link BeanOverride @BeanOverride}.
	 */
	public final @Nullable Field getField() {
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
	public final @Nullable String getBeanName() {
		return this.beanName;
	}

	/**
	 * Get the name of the context hierarchy level in which this handler should
	 * be applied.
	 * <p>An empty string indicates that this handler should be applied to all
	 * application contexts.
	 * <p>If a context name is configured for this handler, it must match a name
	 * configured via {@code @ContextConfiguration(name=...)}.
	 * @since 6.2.6
	 * @see org.springframework.test.context.ContextHierarchy @ContextHierarchy
	 * @see org.springframework.test.context.ContextConfiguration#name()
	 */
	public final String getContextName() {
		return this.contextName;
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
				!Objects.equals(this.contextName, that.contextName) ||
				!Objects.equals(this.strategy, that.strategy)) {
			return false;
		}

		// by-name lookup
		if (this.beanName != null) {
			return true;
		}

		// by-type lookup
		if (this.field == null) {
			return (that.field == null);
		}
		return (that.field != null && this.field.getName().equals(that.field.getName()) &&
				this.qualifierAnnotations.equals(that.qualifierAnnotations));
	}

	@Override
	public int hashCode() {
		int hash = Objects.hash(getClass(), this.beanType.getType(), this.beanName, this.contextName, this.strategy);
		return (this.beanName != null ? hash : hash +
				Objects.hash((this.field != null ? this.field.getName() : null), this.qualifierAnnotations));
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("field", this.field)
				.append("beanType", this.beanType)
				.append("beanName", this.beanName)
				.append("contextName", this.contextName)
				.append("strategy", this.strategy)
				.toString();
	}


	private static Set<Annotation> getQualifierAnnotations(@Nullable Field field) {
		if (field == null) {
			return Collections.emptySet();
		}
		Annotation[] candidates = field.getDeclaredAnnotations();
		if (candidates.length == 0) {
			return Collections.emptySet();
		}
		Set<Annotation> annotations = new HashSet<>(candidates.length - 1);
		for (Annotation candidate : candidates) {
			// Assume all non-BeanOverride annotations are "qualifiers".
			if (!isBeanOverrideAnnotation(candidate.annotationType())) {
				annotations.add(candidate);
			}
		}
		return annotations;
	}

	private static boolean isBeanOverrideAnnotation(Class<? extends Annotation> type) {
		return MergedAnnotations.from(type).isPresent(BeanOverride.class);
	}

}
