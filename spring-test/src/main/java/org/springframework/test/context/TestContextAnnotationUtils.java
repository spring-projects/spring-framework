/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationCollectors;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentLruCache;
import org.springframework.util.ObjectUtils;

/**
 * {@code TestContextAnnotationUtils} is a collection of utility methods that
 * complements the standard support already available in {@link AnnotationUtils}
 * and {@link AnnotatedElementUtils}, while transparently honoring
 * {@link NestedTestConfiguration @NestedTestConfiguration} semantics.
 *
 * <p>Mainly for internal use within the <em>Spring TestContext Framework</em>.
 *
 * <p>Whereas {@code AnnotationUtils} and {@code AnnotatedElementUtils} provide
 * utilities for <em>getting</em> or <em>finding</em> annotations,
 * {@code TestContextAnnotationUtils} goes a step further by providing support
 * for determining the <em>root class</em> on which an annotation is declared,
 * either directly or indirectly via a <em>composed annotation</em>. This
 * additional information is encapsulated in an {@link AnnotationDescriptor}.
 *
 * <p>The additional information provided by an {@code AnnotationDescriptor} is
 * required by the <em>Spring TestContext Framework</em> in order to be able to
 * support class inheritance and enclosing class hierarchy traversals for
 * annotations such as {@link ContextConfiguration @ContextConfiguration},
 * {@link TestExecutionListeners @TestExecutionListeners}, and
 * {@link ActiveProfiles @ActiveProfiles} which offer support for merging and
 * overriding various <em>inherited</em> annotation attributes &mdash; for
 * example, {@link ContextConfiguration#inheritLocations}.
 *
 * @author Sam Brannen
 * @since 5.3, though originally since 4.0 as {@link org.springframework.test.util.MetaAnnotationUtils}
 * @see AnnotationUtils
 * @see AnnotatedElementUtils
 * @see AnnotationDescriptor
 */
public abstract class TestContextAnnotationUtils {

	private static final ConcurrentLruCache<Class<?>, EnclosingConfiguration> cachedEnclosingConfigurationModes =
			new ConcurrentLruCache<>(32, TestContextAnnotationUtils::lookUpEnclosingConfiguration);

	@Nullable
	private static volatile EnclosingConfiguration defaultEnclosingConfigurationMode;


	/**
	 * Determine if an annotation of the specified {@code annotationType} is
	 * present or meta-present on the supplied {@link Class} according to the
	 * search algorithm used in {@link #findMergedAnnotation(Class, Class)}.
	 * <p>If this method returns {@code true}, then {@code findMergedAnnotation(...)}
	 * will return a non-null value.
	 * @param clazz the class to look for annotations on
	 * @param annotationType the type of annotation to look for
	 * @return {@code true} if a matching annotation is present
	 * @since 5.3.3
	 * @see #findMergedAnnotation(Class, Class)
	 */
	public static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotationType) {
		return (findMergedAnnotation(clazz, annotationType) != null);
	}

	/**
	 * Find the first annotation of the specified {@code annotationType} within
	 * the annotation hierarchy <em>above</em> the supplied class, merge that
	 * annotation's attributes with <em>matching</em> attributes from annotations
	 * in lower levels of the annotation hierarchy, and synthesize the result back
	 * into an annotation of the specified {@code annotationType}.
	 * <p>In the context of this method, the term "above" means within the
	 * {@linkplain Class#getSuperclass() superclass} hierarchy or within the
	 * {@linkplain Class#getEnclosingClass() enclosing class} hierarchy of the
	 * supplied class. The enclosing class hierarchy will only be searched
	 * according to {@link NestedTestConfiguration @NestedTestConfiguration}
	 * semantics.
	 * <p>{@link org.springframework.core.annotation.AliasFor @AliasFor} semantics
	 * are fully supported, both within a single annotation and within annotation
	 * hierarchies.
	 * @param clazz the class to look for annotations on
	 * @param annotationType the type of annotation to look for
	 * @return the merged, synthesized {@code Annotation}, or {@code null} if not found
	 * @see AnnotatedElementUtils#findMergedAnnotation(java.lang.reflect.AnnotatedElement, Class)
	 * @see #findAnnotationDescriptor(Class, Class)
	 * @see #searchEnclosingClass(Class)
	 */
	@Nullable
	public static <T extends Annotation> T findMergedAnnotation(Class<?> clazz, Class<T> annotationType) {
		return findMergedAnnotation(clazz, annotationType, TestContextAnnotationUtils::searchEnclosingClass);
	}

	@Nullable
	private static <T extends Annotation> T findMergedAnnotation(Class<?> clazz, Class<T> annotationType,
			Predicate<Class<?>> searchEnclosingClass) {

		AnnotationDescriptor<T> descriptor =
				findAnnotationDescriptor(clazz, annotationType, searchEnclosingClass, new HashSet<>());
		return (descriptor != null ? descriptor.getAnnotation() : null);
	}

	/**
	 * Get all <em>repeatable annotations</em> of the specified {@code annotationType}
	 * within the annotation hierarchy <em>above</em> the supplied class; and for
	 * each annotation found, merge that annotation's attributes with <em>matching</em>
	 * attributes from annotations in lower levels of the annotation hierarchy and
	 * synthesize the results back into an annotation of the specified {@code annotationType}.
	 * <p>This method will find {@link java.lang.annotation.Inherited @Inherited}
	 * annotations declared on superclasses if the supplied class does not have
	 * any local declarations of the repeatable annotation. If no inherited
	 * annotations are found, this method will search within the
	 * {@linkplain Class#getEnclosingClass() enclosing class} hierarchy of the
	 * supplied class. The enclosing class hierarchy will only be searched
	 * according to {@link NestedTestConfiguration @NestedTestConfiguration}
	 * semantics.
	 * <p>The container type that holds the repeatable annotations will be looked up
	 * via {@link java.lang.annotation.Repeatable}.
	 * <p>{@link org.springframework.core.annotation.AliasFor @AliasFor} semantics
	 * are fully supported, both within a single annotation and within annotation
	 * hierarchies.
	 * @param clazz the class on which to search for annotations (never {@code null})
	 * @param annotationType the annotation type to find (never {@code null})
	 * @return the set of all merged repeatable annotations found, or an empty set
	 * if none were found
	 * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(java.lang.reflect.AnnotatedElement, Class)
	 * @see #searchEnclosingClass(Class)
	 */
	public static <T extends Annotation> Set<T> getMergedRepeatableAnnotations(
			Class<?> clazz, Class<T> annotationType) {

		// Present (via @Inherited semantics), directly present, or meta-present?
		Set<T> mergedAnnotations = MergedAnnotations.from(clazz, SearchStrategy.INHERITED_ANNOTATIONS)
				.stream(annotationType)
				.collect(MergedAnnotationCollectors.toAnnotationSet());

		if (!mergedAnnotations.isEmpty()) {
			return mergedAnnotations;
		}

		// Declared on an enclosing class of an inner class?
		if (searchEnclosingClass(clazz)) {
			// Then mimic @Inherited semantics within the enclosing class hierarchy.
			return getMergedRepeatableAnnotations(clazz.getEnclosingClass(), annotationType);
		}

		return Collections.emptySet();
	}

	/**
	 * Find the {@link AnnotationDescriptor} for the supplied {@code annotationType}
	 * on the supplied {@link Class}, traversing its annotations, interfaces,
	 * superclasses, and enclosing classes if no annotation can be found on the
	 * given class itself.
	 * <p>This method explicitly handles class-level annotations which are not
	 * declared as {@linkplain java.lang.annotation.Inherited inherited} <em>as
	 * well as meta-annotations</em>.
	 * <p>The algorithm operates as follows:
	 * <ol>
	 * <li>Search for the annotation on the given class and return a corresponding
	 * {@code AnnotationDescriptor} if found.
	 * <li>Recursively search through all annotations that the given class declares.</li>
	 * <li>Recursively search through all interfaces implemented by the given class.</li>
	 * <li>Recursively search through the superclass hierarchy of the given class.</li>
	 * <li>Recursively search through the enclosing class hierarchy of the given class
	 * if appropriate according to {@link NestedTestConfiguration @NestedTestConfiguration}
	 * semantics.</li>
	 * </ol>
	 * <p>In this context, the term <em>recursively</em> means that the search
	 * process continues by returning to step #1 with the current annotation,
	 * interface, superclass, or enclosing class as the class to look for
	 * annotations on.
	 * @param clazz the class to look for annotations on
	 * @param annotationType the type of annotation to look for
	 * @return the corresponding annotation descriptor if the annotation was found;
	 * otherwise {@code null}
	 * @see #findAnnotationDescriptorForTypes(Class, Class...)
	 */
	@Nullable
	public static <T extends Annotation> AnnotationDescriptor<T> findAnnotationDescriptor(
			Class<?> clazz, Class<T> annotationType) {

		Assert.notNull(annotationType, "Annotation type must not be null");
		return findAnnotationDescriptor(clazz, annotationType, TestContextAnnotationUtils::searchEnclosingClass,
			new HashSet<>());
	}

	/**
	 * Perform the search algorithm for {@link #findAnnotationDescriptor(Class, Class)},
	 * avoiding endless recursion by tracking which annotations have already been
	 * <em>visited</em>.
	 * @param clazz the class to look for annotations on
	 * @param annotationType the type of annotation to look for
	 * @param searchEnclosingClass a predicate which evaluates to {@code true}
	 * if a search should be performed on the enclosing class
	 * @param visited the set of annotations that have already been visited
	 * @return the corresponding annotation descriptor if the annotation was found;
	 * otherwise {@code null}
	 */
	@Nullable
	private static <T extends Annotation> AnnotationDescriptor<T> findAnnotationDescriptor(
			@Nullable Class<?> clazz, Class<T> annotationType, Predicate<Class<?>> searchEnclosingClass,
			Set<Annotation> visited) {

		if (clazz == null || Object.class == clazz) {
			return null;
		}

		// Declared locally?
		if (AnnotationUtils.isAnnotationDeclaredLocally(annotationType, clazz)) {
			return new AnnotationDescriptor<>(clazz, clazz.getAnnotation(annotationType));
		}

		AnnotationDescriptor<T> descriptor = null;

		// Declared on a composed annotation (i.e., as a meta-annotation)?
		for (Annotation composedAnn : clazz.getDeclaredAnnotations()) {
			Class<? extends Annotation> composedType = composedAnn.annotationType();
			if (!AnnotationUtils.isInJavaLangAnnotationPackage(composedType.getName()) && visited.add(composedAnn)) {
				descriptor = findAnnotationDescriptor(composedType, annotationType, searchEnclosingClass, visited);
				if (descriptor != null) {
					return new AnnotationDescriptor<>(clazz, descriptor.getDeclaringClass(), descriptor.getAnnotation());
				}
			}
		}

		// Declared on an interface?
		for (Class<?> ifc : clazz.getInterfaces()) {
			descriptor = findAnnotationDescriptor(ifc, annotationType, searchEnclosingClass, visited);
			if (descriptor != null) {
				return new AnnotationDescriptor<>(clazz, descriptor.getDeclaringClass(), descriptor.getAnnotation());
			}
		}

		// Declared on a superclass?
		descriptor = findAnnotationDescriptor(clazz.getSuperclass(), annotationType, searchEnclosingClass, visited);
		if (descriptor != null) {
			return descriptor;
		}

		// Declared on an enclosing class of an inner class?
		if (searchEnclosingClass.test(clazz)) {
			descriptor = findAnnotationDescriptor(clazz.getEnclosingClass(), annotationType, searchEnclosingClass, visited);
			if (descriptor != null) {
				return descriptor;
			}
		}

		return null;
	}

	/**
	 * Find the {@link UntypedAnnotationDescriptor} for the first {@link Class}
	 * in the inheritance hierarchy of the specified {@code clazz} (including
	 * the specified {@code clazz} itself) which declares at least one of the
	 * specified {@code annotationTypes}.
	 * <p>This method traverses the annotations, interfaces, superclasses, and
	 * enclosing classes of the specified {@code clazz} if no annotation can be
	 * found on the given class itself.
	 * <p>This method explicitly handles class-level annotations which are not
	 * declared as {@linkplain java.lang.annotation.Inherited inherited} <em>as
	 * well as meta-annotations</em>.
	 * <p>The algorithm operates as follows:
	 * <ol>
	 * <li>Search for a local declaration of one of the annotation types on the
	 * given class and return a corresponding {@code UntypedAnnotationDescriptor}
	 * if found.
	 * <li>Recursively search through all annotations that the given class declares.</li>
	 * <li>Recursively search through all interfaces implemented by the given class.</li>
	 * <li>Recursively search through the superclass hierarchy of the given class.</li>
	 * <li>Recursively search through the enclosing class hierarchy of the given class
	 * if appropriate according to {@link NestedTestConfiguration @NestedTestConfiguration}
	 * semantics.</li>
	 * </ol>
	 * <p>In this context, the term <em>recursively</em> means that the search
	 * process continues by returning to step #1 with the current annotation,
	 * interface, superclass, or enclosing class as the class to look for
	 * annotations on.
	 * @param clazz the class to look for annotations on
	 * @param annotationTypes the types of annotations to look for
	 * @return the corresponding annotation descriptor if one of the annotations
	 * was found; otherwise {@code null}
	 * @see #findAnnotationDescriptor(Class, Class)
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static UntypedAnnotationDescriptor findAnnotationDescriptorForTypes(
			Class<?> clazz, Class<? extends Annotation>... annotationTypes) {

		assertNonEmptyAnnotationTypeArray(annotationTypes, "The list of annotation types must not be empty");
		return findAnnotationDescriptorForTypes(clazz, annotationTypes, new HashSet<>());
	}

	/**
	 * Perform the search algorithm for {@link #findAnnotationDescriptorForTypes(Class, Class...)},
	 * avoiding endless recursion by tracking which annotations have already been
	 * <em>visited</em>.
	 * @param clazz the class to look for annotations on
	 * @param annotationTypes the types of annotations to look for
	 * @param visited the set of annotations that have already been visited
	 * @return the corresponding annotation descriptor if one of the annotations
	 * was found; otherwise {@code null}
	 */
	@Nullable
	private static UntypedAnnotationDescriptor findAnnotationDescriptorForTypes(@Nullable Class<?> clazz,
			Class<? extends Annotation>[] annotationTypes, Set<Annotation> visited) {

		if (clazz == null || Object.class == clazz) {
			return null;
		}

		// Declared locally?
		for (Class<? extends Annotation> annotationType : annotationTypes) {
			if (AnnotationUtils.isAnnotationDeclaredLocally(annotationType, clazz)) {
				return new UntypedAnnotationDescriptor(clazz, clazz.getAnnotation(annotationType), annotationTypes);
			}
		}

		// Declared on a composed annotation (i.e., as a meta-annotation)?
		for (Annotation composedAnnotation : clazz.getDeclaredAnnotations()) {
			if (!AnnotationUtils.isInJavaLangAnnotationPackage(composedAnnotation) && visited.add(composedAnnotation)) {
				UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
						composedAnnotation.annotationType(), annotationTypes, visited);
				if (descriptor != null) {
					return new UntypedAnnotationDescriptor(clazz, descriptor.getDeclaringClass(),
							descriptor.getAnnotation(), annotationTypes);
				}
			}
		}

		// Declared on an interface?
		for (Class<?> ifc : clazz.getInterfaces()) {
			UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(ifc, annotationTypes, visited);
			if (descriptor != null) {
				return new UntypedAnnotationDescriptor(clazz, descriptor.getDeclaringClass(),
						descriptor.getAnnotation(), annotationTypes);
			}
		}

		// Declared on a superclass?
		UntypedAnnotationDescriptor descriptor =
				findAnnotationDescriptorForTypes(clazz.getSuperclass(), annotationTypes, visited);
		if (descriptor != null) {
			return descriptor;
		}

		// Declared on an enclosing class of an inner class?
		if (searchEnclosingClass(clazz)) {
			descriptor = findAnnotationDescriptorForTypes(clazz.getEnclosingClass(), annotationTypes, visited);
			if (descriptor != null) {
				return descriptor;
			}
		}

		return null;
	}

	/**
	 * Determine if annotations on the enclosing class of the supplied class
	 * should be searched by annotation search algorithms within the <em>Spring
	 * TestContext Framework</em>.
	 * @param clazz the class whose enclosing class should potentially be searched
	 * @return {@code true} if the supplied class is an inner class whose enclosing
	 * class should be searched
	 * @see ClassUtils#isInnerClass(Class)
	 * @see NestedTestConfiguration @NestedTestConfiguration
	 */
	public static boolean searchEnclosingClass(Class<?> clazz) {
		return (ClassUtils.isInnerClass(clazz) &&
				getEnclosingConfiguration(clazz) == EnclosingConfiguration.INHERIT);
	}

	static void clearCaches() {
		cachedEnclosingConfigurationModes.clear();
		defaultEnclosingConfigurationMode = null;
	}

	/**
	 * Get the {@link EnclosingConfiguration} mode for the supplied class.
	 * @param clazz the class for which the enclosing configuration mode should
	 * be resolved
	 * @return the resolved enclosing configuration mode
	 */
	private static EnclosingConfiguration getEnclosingConfiguration(Class<?> clazz) {
		return cachedEnclosingConfigurationModes.get(clazz);
	}

	private static EnclosingConfiguration lookUpEnclosingConfiguration(Class<?> clazz) {
		// @NestedTestConfiguration should not be discovered on an enclosing class
		// for a nested interface (which is always static), so our predicate simply
		// ensures that the candidate class is an inner class.
		Predicate<Class<?>> searchEnclosingClass = ClassUtils::isInnerClass;
		NestedTestConfiguration nestedTestConfiguration =
				findMergedAnnotation(clazz, NestedTestConfiguration.class, searchEnclosingClass);
		return (nestedTestConfiguration != null ? nestedTestConfiguration.value() : getDefaultEnclosingConfigurationMode());
	}

	private static EnclosingConfiguration getDefaultEnclosingConfigurationMode() {
		EnclosingConfiguration defaultConfigurationMode = defaultEnclosingConfigurationMode;
		if (defaultConfigurationMode == null) {
			String value = SpringProperties.getProperty(NestedTestConfiguration.ENCLOSING_CONFIGURATION_PROPERTY_NAME);
			EnclosingConfiguration enclosingConfigurationMode = EnclosingConfiguration.from(value);
			defaultConfigurationMode =
					(enclosingConfigurationMode != null ? enclosingConfigurationMode : EnclosingConfiguration.INHERIT);
			defaultEnclosingConfigurationMode = defaultConfigurationMode;
		}
		return defaultConfigurationMode;
	}

	private static void assertNonEmptyAnnotationTypeArray(Class<?>[] annotationTypes, String message) {
		if (ObjectUtils.isEmpty(annotationTypes)) {
			throw new IllegalArgumentException(message);
		}
		for (Class<?> clazz : annotationTypes) {
			if (!Annotation.class.isAssignableFrom(clazz)) {
				throw new IllegalArgumentException("Array elements must be of type Annotation");
			}
		}
	}


	/**
	 * Descriptor for an {@link Annotation}, including the {@linkplain
	 * #getDeclaringClass() class} on which the annotation is <em>declared</em>
	 * as well as the {@linkplain #getAnnotation() merged annotation} instance.
	 * <p>If the annotation is used as a meta-annotation, the <em>root declaring
	 * class</em> is not directly annotated with the annotation but rather
	 * indirectly via a composed annotation.
	 * <p>Given the following example, if we are searching for the {@code @Transactional}
	 * annotation <em>on</em> the {@code TransactionalTests} class, then the
	 * properties of the {@code AnnotationDescriptor} would be as follows.
	 * <ul>
	 * <li>rootDeclaringClass: {@code TransactionalTests} class object</li>
	 * <li>declaringClass: {@code TransactionalTests} class object</li>
	 * <li>annotation: instance of the {@code Transactional} annotation</li>
	 * </ul>
	 * <p><pre style="code">
	 * &#064;Transactional
	 * &#064;ContextConfiguration({"/test-datasource.xml", "/repository-config.xml"})
	 * public class TransactionalTests { }
	 * </pre>
	 * <p>Given the following example, if we are searching for the {@code @Transactional}
	 * annotation <em>on</em> the {@code UserRepositoryTests} class, then the
	 * properties of the {@code AnnotationDescriptor} would be as follows.
	 * <ul>
	 * <li>rootDeclaringClass: {@code UserRepositoryTests} class object</li>
	 * <li>declaringClass: {@code RepositoryTests} class object</li>
	 * <li>annotation: instance of the {@code Transactional} annotation</li>
	 * </ul>
	 * <p><pre style="code">
	 * &#064;Transactional
	 * &#064;ContextConfiguration({"/test-datasource.xml", "/repository-config.xml"})
	 * &#064;Retention(RetentionPolicy.RUNTIME)
	 * public &#064;interface RepositoryTests { }
	 *
	 * &#064;RepositoryTests
	 * public class UserRepositoryTests { }
	 * </pre>
	 *
	 * @param <T> the annotation type
	 */
	public static class AnnotationDescriptor<T extends Annotation> {

		private final Class<?> rootDeclaringClass;

		private final Class<?> declaringClass;

		private final T annotation;

		AnnotationDescriptor(Class<?> rootDeclaringClass, T annotation) {
			this(rootDeclaringClass, rootDeclaringClass, annotation);
		}

		@SuppressWarnings("unchecked")
		AnnotationDescriptor(Class<?> rootDeclaringClass, Class<?> declaringClass, T annotation) {
			Assert.notNull(rootDeclaringClass, "'rootDeclaringClass' must not be null");
			Assert.notNull(declaringClass, "'declaringClass' must not be null");
			Assert.notNull(annotation, "Annotation must not be null");
			this.rootDeclaringClass = rootDeclaringClass;
			this.declaringClass = declaringClass;
			T mergedAnnotation = (T) AnnotatedElementUtils.findMergedAnnotation(
					rootDeclaringClass, annotation.annotationType());
			Assert.state(mergedAnnotation != null,
					() -> "Failed to find merged annotation for " + annotation);
			this.annotation = mergedAnnotation;
		}

		public Class<?> getRootDeclaringClass() {
			return this.rootDeclaringClass;
		}

		public Class<?> getDeclaringClass() {
			return this.declaringClass;
		}

		/**
		 * Get the merged annotation for this descriptor.
		 */
		public T getAnnotation() {
			return this.annotation;
		}

		@SuppressWarnings("unchecked")
		Class<T> getAnnotationType() {
			return (Class<T>) this.annotation.annotationType();
		}

		/**
		 * Find the next {@link AnnotationDescriptor} for the specified annotation
		 * type in the hierarchy above the {@linkplain #getRootDeclaringClass()
		 * root declaring class} of this descriptor.
		 * <p>If a corresponding annotation is found in the superclass hierarchy
		 * of the root declaring class, that will be returned. Otherwise, an
		 * attempt will be made to find a corresponding annotation in the
		 * {@linkplain Class#getEnclosingClass() enclosing class} hierarchy of
		 * the root declaring class if {@linkplain #searchEnclosingClass appropriate}.
		 * @return the next corresponding annotation descriptor if the annotation
		 * was found; otherwise {@code null}
		 */
		@Nullable
		public AnnotationDescriptor<T> next() {
			// Declared on a superclass?
			AnnotationDescriptor<T> descriptor =
					findAnnotationDescriptor(getRootDeclaringClass().getSuperclass(), getAnnotationType());
			// Declared on an enclosing class of an inner class?
			if (descriptor == null && searchEnclosingClass(getRootDeclaringClass())) {
				descriptor = findAnnotationDescriptor(getRootDeclaringClass().getEnclosingClass(), getAnnotationType());
			}
			return descriptor;
		}

		/**
		 * Find <strong>all</strong> annotations of the specified annotation type
		 * that are present or meta-present on the {@linkplain #getRootDeclaringClass()
		 * root declaring class} of this descriptor or on any interfaces that the
		 * root declaring class implements.
		 * @return the set of all merged, synthesized {@code Annotations} found,
		 * or an empty set if none were found
		 */
		public Set<T> findAllLocalMergedAnnotations() {
			SearchStrategy searchStrategy = SearchStrategy.TYPE_HIERARCHY;
			return MergedAnnotations.from(getRootDeclaringClass(), searchStrategy, RepeatableContainers.none())
					.stream(getAnnotationType())
					.filter(MergedAnnotationPredicates.firstRunOf(MergedAnnotation::getAggregateIndex))
					.collect(MergedAnnotationCollectors.toAnnotationSet());
		}

		/**
		 * Provide a textual representation of this {@code AnnotationDescriptor}.
		 */
		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("rootDeclaringClass", this.rootDeclaringClass.getName())
					.append("declaringClass", this.declaringClass.getName())
					.append("annotation", this.annotation)
					.toString();
		}
	}


	/**
	 * <em>Untyped</em> extension of {@link AnnotationDescriptor} that is used
	 * to describe the declaration of one of several candidate annotation types
	 * where the actual annotation type cannot be predetermined.
	 */
	public static class UntypedAnnotationDescriptor extends AnnotationDescriptor<Annotation> {

		private final Class<? extends Annotation>[] annotationTypes;

		UntypedAnnotationDescriptor(Class<?> rootDeclaringClass, Annotation annotation,
				Class<? extends Annotation>[] annotationTypes) {

			this(rootDeclaringClass, rootDeclaringClass, annotation, annotationTypes);
		}

		UntypedAnnotationDescriptor(Class<?> rootDeclaringClass, Class<?> declaringClass,
				Annotation annotation, Class<? extends Annotation>[] annotationTypes) {

			super(rootDeclaringClass, declaringClass, annotation);
			this.annotationTypes = annotationTypes;
		}

		/**
		 * Find the next {@link UntypedAnnotationDescriptor} for the specified
		 * annotation types in the hierarchy above the
		 * {@linkplain #getRootDeclaringClass() root declaring class} of this
		 * descriptor.
		 * <p>If one of the corresponding annotations is found in the superclass
		 * hierarchy of the root declaring class, that will be returned. Otherwise,
		 * an attempt will be made to find a corresponding annotation in the
		 * {@linkplain Class#getEnclosingClass() enclosing class} hierarchy of
		 * the root declaring class if {@linkplain #searchEnclosingClass appropriate}.
		 * @return the next corresponding annotation descriptor if one of the
		 * annotations was found; otherwise {@code null}
		 * @see AnnotationDescriptor#next()
		 */
		@Override
		@Nullable
		public UntypedAnnotationDescriptor next() {
			// Declared on a superclass?
			UntypedAnnotationDescriptor descriptor =
					findAnnotationDescriptorForTypes(getRootDeclaringClass().getSuperclass(), this.annotationTypes);
			// Declared on an enclosing class of an inner class?
			if (descriptor == null && searchEnclosingClass(getRootDeclaringClass())) {
				descriptor = findAnnotationDescriptorForTypes(getRootDeclaringClass().getEnclosingClass(), this.annotationTypes);
			}
			return descriptor;
		}

		/**
		 * Throws an {@link UnsupportedOperationException} since the type of annotation
		 * represented by an {@code UntypedAnnotationDescriptor} is unknown.
		 */
		@Override
		public Set<Annotation> findAllLocalMergedAnnotations() {
			throw new UnsupportedOperationException(
					"findAllLocalMergedAnnotations() is unsupported in UntypedAnnotationDescriptor");
		}
	}

}
