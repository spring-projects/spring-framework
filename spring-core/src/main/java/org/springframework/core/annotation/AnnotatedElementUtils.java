/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * General utility methods for finding annotations, meta-annotations, and
 * repeatable annotations on {@link AnnotatedElement AnnotatedElements}.
 *
 * <p>{@code AnnotatedElementUtils} defines the public API for Spring's
 * meta-annotation programming model with support for <em>annotation attribute
 * overrides</em>. If you do not need support for annotation attribute
 * overrides, consider using {@link AnnotationUtils} instead.
 *
 * <p>Note that the features of this class are not provided by the JDK's
 * introspection facilities themselves.
 *
 * <h3>Annotation Attribute Overrides</h3>
 * <p>Support for meta-annotations with <em>attribute overrides</em> in
 * <em>composed annotations</em> is provided by all variants of the
 * {@code getMergedAnnotationAttributes()}, {@code getMergedAnnotation()},
 * {@code getAllMergedAnnotations()}, {@code getMergedRepeatableAnnotations()},
 * {@code findMergedAnnotationAttributes()}, {@code findMergedAnnotation()},
 * {@code findAllMergedAnnotations()}, and {@code findMergedRepeatableAnnotations()}
 * methods.
 *
 * <h3>Find vs. Get Semantics</h3>
 * <p>The search algorithms used by methods in this class follow either
 * <em>find</em> or <em>get</em> semantics. Consult the javadocs for each
 * individual method for details on which search algorithm is used.
 *
 * <p><strong>Get semantics</strong> are limited to searching for annotations
 * that are either <em>present</em> on an {@code AnnotatedElement} (i.e. declared
 * locally or {@linkplain java.lang.annotation.Inherited inherited}) or declared
 * within the annotation hierarchy <em>above</em> the {@code AnnotatedElement}.
 *
 * <p><strong>Find semantics</strong> are much more exhaustive, providing
 * <em>get semantics</em> plus support for the following:
 *
 * <ul>
 * <li>Searching on interfaces, if the annotated element is a class
 * <li>Searching on superclasses, if the annotated element is a class
 * <li>Resolving bridged methods, if the annotated element is a method
 * <li>Searching on methods in interfaces, if the annotated element is a method
 * <li>Searching on methods in superclasses, if the annotated element is a method
 * </ul>
 *
 * <h3>Support for {@code @Inherited}</h3>
 * <p>Methods following <em>get semantics</em> will honor the contract of Java's
 * {@link java.lang.annotation.Inherited @Inherited} annotation except that locally
 * declared annotations (including custom composed annotations) will be favored over
 * inherited annotations. In contrast, methods following <em>find semantics</em>
 * will completely ignore the presence of {@code @Inherited} since the <em>find</em>
 * search algorithm manually traverses type and method hierarchies and thereby
 * implicitly supports annotation inheritance without a need for {@code @Inherited}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 * @see AliasFor
 * @see AnnotationAttributes
 * @see AnnotationUtils
 * @see BridgeMethodResolver
 */
public abstract class AnnotatedElementUtils {

	/**
	 * Build an adapted {@link AnnotatedElement} for the given annotations,
	 * typically for use with other methods on {@link AnnotatedElementUtils}.
	 * @param annotations the annotations to expose through the {@code AnnotatedElement}
	 * @since 4.3
	 */
	public static AnnotatedElement forAnnotations(Annotation... annotations) {
		return new AnnotatedElementForAnnotations(annotations);
	}

	/**
	 * Get the fully qualified class names of all meta-annotation types
	 * <em>present</em> on the annotation (of the specified {@code annotationType})
	 * on the supplied {@link AnnotatedElement}.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationType the annotation type on which to find meta-annotations
	 * @return the names of all meta-annotations present on the annotation,
	 * or an empty set if not found
	 * @since 4.2
	 * @see #getMetaAnnotationTypes(AnnotatedElement, String)
	 * @see #hasMetaAnnotationTypes
	 */
	public static Set<String> getMetaAnnotationTypes(AnnotatedElement element,
			Class<? extends Annotation> annotationType) {

		return getMetaAnnotationTypes(element, element.getAnnotation(annotationType));
	}

	/**
	 * Get the fully qualified class names of all meta-annotation
	 * types <em>present</em> on the annotation (of the specified
	 * {@code annotationName}) on the supplied {@link AnnotatedElement}.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the annotation
	 * type on which to find meta-annotations
	 * @return the names of all meta-annotations present on the annotation,
	 * or an empty set if none found
	 * @see #getMetaAnnotationTypes(AnnotatedElement, Class)
	 * @see #hasMetaAnnotationTypes
	 */
	public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, String annotationName) {
		for (Annotation annotation : element.getAnnotations()) {
			if (annotation.annotationType().getName().equals(annotationName)) {
				return getMetaAnnotationTypes(element, annotation);
			}
		}
		return Collections.emptySet();
	}

	private static Set<String> getMetaAnnotationTypes(AnnotatedElement element, @Nullable Annotation annotation) {
		if (annotation == null) {
			return Collections.emptySet();
		}
		return getAnnotations(annotation.annotationType()).stream()
				.map(mergedAnnotation -> mergedAnnotation.getType().getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Determine if the supplied {@link AnnotatedElement} is annotated with
	 * a <em>composed annotation</em> that is meta-annotated with an
	 * annotation of the specified {@code annotationType}.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationType the meta-annotation type to find
	 * @return {@code true} if a matching meta-annotation is present
	 * @since 4.2.3
	 * @see #getMetaAnnotationTypes
	 */
	public static boolean hasMetaAnnotationTypes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		return getAnnotations(element).stream(annotationType).anyMatch(MergedAnnotation::isMetaPresent);
	}

	/**
	 * Determine if the supplied {@link AnnotatedElement} is annotated with a
	 * <em>composed annotation</em> that is meta-annotated with an annotation
	 * of the specified {@code annotationName}.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the
	 * meta-annotation type to find
	 * @return {@code true} if a matching meta-annotation is present
	 * @see #getMetaAnnotationTypes
	 */
	public static boolean hasMetaAnnotationTypes(AnnotatedElement element, String annotationName) {
		return getAnnotations(element).stream(annotationName).anyMatch(MergedAnnotation::isMetaPresent);
	}

	/**
	 * Determine if an annotation of the specified {@code annotationType}
	 * is <em>present</em> on the supplied {@link AnnotatedElement} or
	 * within the annotation hierarchy <em>above</em> the specified element.
	 * <p>If this method returns {@code true}, then {@link #getMergedAnnotationAttributes}
	 * will return a non-null value.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @return {@code true} if a matching annotation is present
	 * @since 4.2.3
	 * @see #hasAnnotation(AnnotatedElement, Class)
	 */
	public static boolean isAnnotated(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		// Shortcut: directly present on the element, with no merging needed?
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
			return element.isAnnotationPresent(annotationType);
		}
		// Exhaustive retrieval of merged annotations...
		return getAnnotations(element).isPresent(annotationType);
	}

	/**
	 * Determine if an annotation of the specified {@code annotationName} is
	 * <em>present</em> on the supplied {@link AnnotatedElement} or within the
	 * annotation hierarchy <em>above</em> the specified element.
	 * <p>If this method returns {@code true}, then {@link #getMergedAnnotationAttributes}
	 * will return a non-null value.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the annotation type to find
	 * @return {@code true} if a matching annotation is present
	 */
	public static boolean isAnnotated(AnnotatedElement element, String annotationName) {
		return getAnnotations(element).isPresent(annotationName);
	}

	/**
	 * Get the first annotation of the specified {@code annotationType} within
	 * the annotation hierarchy <em>above</em> the supplied {@code element} and
	 * merge that annotation's attributes with <em>matching</em> attributes from
	 * annotations in lower levels of the annotation hierarchy.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both
	 * within a single annotation and within the annotation hierarchy.
	 * <p>This method delegates to {@link #getMergedAnnotationAttributes(AnnotatedElement, String)}.
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @return the merged {@code AnnotationAttributes}, or {@code null} if not found
	 * @since 4.2
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 */
	@Nullable
	public static AnnotationAttributes getMergedAnnotationAttributes(
			AnnotatedElement element, Class<? extends Annotation> annotationType) {

		MergedAnnotation<?> mergedAnnotation = getAnnotations(element)
				.get(annotationType, null, MergedAnnotationSelectors.firstDirectlyDeclared());
		return getAnnotationAttributes(mergedAnnotation, false, false);
	}

	/**
	 * Get the first annotation of the specified {@code annotationName} within
	 * the annotation hierarchy <em>above</em> the supplied {@code element} and
	 * merge that annotation's attributes with <em>matching</em> attributes from
	 * annotations in lower levels of the annotation hierarchy.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both
	 * within a single annotation and within the annotation hierarchy.
	 * <p>This method delegates to {@link #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)},
	 * supplying {@code false} for {@code classValuesAsString} and {@code nestedAnnotationsAsMap}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the annotation type to find
	 * @return the merged {@code AnnotationAttributes}, or {@code null} if not found
	 * @since 4.2
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String)
	 */
	@Nullable
	public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element,
			String annotationName) {

		return getMergedAnnotationAttributes(element, annotationName, false, false);
	}

	/**
	 * Get the first annotation of the specified {@code annotationName} within
	 * the annotation hierarchy <em>above</em> the supplied {@code element} and
	 * merge that annotation's attributes with <em>matching</em> attributes from
	 * annotations in lower levels of the annotation hierarchy.
	 * <p>Attributes from lower levels in the annotation hierarchy override attributes
	 * of the same name from higher levels, and {@link AliasFor @AliasFor} semantics are
	 * fully supported, both within a single annotation and within the annotation hierarchy.
	 * <p>In contrast to {@link #getAllAnnotationAttributes}, the search algorithm used by
	 * this method will stop searching the annotation hierarchy once the first annotation
	 * of the specified {@code annotationName} has been found. As a consequence,
	 * additional annotations of the specified {@code annotationName} will be ignored.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the annotation type to find
	 * @param classValuesAsString whether to convert Class references into Strings or to
	 * preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested Annotation instances
	 * into {@code AnnotationAttributes} maps or to preserve them as Annotation instances
	 * @return the merged {@code AnnotationAttributes}, or {@code null} if not found
	 * @since 4.2
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	@Nullable
	public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element,
			String annotationName, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		MergedAnnotation<?> mergedAnnotation = getAnnotations(element)
				.get(annotationName, null, MergedAnnotationSelectors.firstDirectlyDeclared());
		return getAnnotationAttributes(mergedAnnotation, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * Get the first annotation of the specified {@code annotationType} within
	 * the annotation hierarchy <em>above</em> the supplied {@code element},
	 * merge that annotation's attributes with <em>matching</em> attributes from
	 * annotations in lower levels of the annotation hierarchy, and synthesize
	 * the result back into an annotation of the specified {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both
	 * within a single annotation and within the annotation hierarchy.
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @return the merged, synthesized {@code Annotation}, or {@code null} if not found
	 * @since 4.2
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 */
	@Nullable
	public static <A extends Annotation> A getMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
		// Shortcut: directly present on the element, with no merging needed?
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
			return element.getDeclaredAnnotation(annotationType);
		}
		// Exhaustive retrieval of merged annotations...
		return getAnnotations(element)
				.get(annotationType, null, MergedAnnotationSelectors.firstDirectlyDeclared())
				.synthesize(MergedAnnotation::isPresent).orElse(null);
	}

	/**
	 * Get <strong>all</strong> annotations of the specified {@code annotationType}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the annotation
	 * hierarchy and synthesize the results back into an annotation of the specified
	 * {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element (never {@code null})
	 * @param annotationType the annotation type to find (never {@code null})
	 * @return the set of all merged, synthesized {@code Annotations} found,
	 * or an empty set if none were found
	 * @since 4.3
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String)
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Set<A> getAllMergedAnnotations(
			AnnotatedElement element, Class<A> annotationType) {

		return getAnnotations(element).stream(annotationType)
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * Get <strong>all</strong> annotations of the specified {@code annotationTypes}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the
	 * annotation hierarchy and synthesize the results back into an annotation
	 * of the corresponding {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element (never {@code null})
	 * @param annotationTypes the annotation types to find
	 * @return the set of all merged, synthesized {@code Annotations} found,
	 * or an empty set if none were found
	 * @since 5.1
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static Set<Annotation> getAllMergedAnnotations(AnnotatedElement element,
			Set<Class<? extends Annotation>> annotationTypes) {

		return getAnnotations(element).stream()
				.filter(MergedAnnotationPredicates.typeIn(annotationTypes))
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * Get all <em>repeatable annotations</em> of the specified {@code annotationType}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the annotation
	 * hierarchy and synthesize the results back into an annotation of the specified
	 * {@code annotationType}.
	 * <p>The container type that holds the repeatable annotations will be looked up
	 * via {@link java.lang.annotation.Repeatable}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element (never {@code null})
	 * @param annotationType the annotation type to find (never {@code null})
	 * @return the set of all merged repeatable {@code Annotations} found,
	 * or an empty set if none were found
	 * @throws IllegalArgumentException if the {@code element} or {@code annotationType}
	 * is {@code null}, or if the container type cannot be resolved
	 * @since 4.3
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 * @see #getMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
	 */
	public static <A extends Annotation> Set<A> getMergedRepeatableAnnotations(
			AnnotatedElement element, Class<A> annotationType) {

		return getMergedRepeatableAnnotations(element, annotationType, null);
	}

	/**
	 * Get all <em>repeatable annotations</em> of the specified {@code annotationType}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the annotation
	 * hierarchy and synthesize the results back into an annotation of the specified
	 * {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * <p><strong>WARNING</strong>: if the supplied {@code containerType} is not
	 * {@code null}, the search will be restricted to supporting only repeatable
	 * annotations whose container is the supplied {@code containerType}. This
	 * prevents the search from finding repeatable annotations declared as
	 * meta-annotations on other types of repeatable annotations. If you need to
	 * support such a use case, favor {@link #getMergedRepeatableAnnotations(AnnotatedElement, Class)}
	 * over this method or alternatively use the {@link MergedAnnotations} API
	 * directly in conjunction with {@link RepeatableContainers} that are
	 * {@linkplain RepeatableContainers#and(Class, Class) composed} to support
	 * multiple repeatable annotation types.
	 * @param element the annotated element (never {@code null})
	 * @param annotationType the annotation type to find (never {@code null})
	 * @param containerType the type of the container that holds the annotations;
	 * may be {@code null} if the container type should be looked up via
	 * {@link java.lang.annotation.Repeatable}
	 * @return the set of all merged repeatable {@code Annotations} found,
	 * or an empty set if none were found
	 * @throws IllegalArgumentException if the {@code element} or {@code annotationType}
	 * is {@code null}, or if the container type cannot be resolved
	 * @throws AnnotationConfigurationException if the supplied {@code containerType}
	 * is not a valid container annotation for the supplied {@code annotationType}
	 * @since 4.3
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Set<A> getMergedRepeatableAnnotations(
			AnnotatedElement element, Class<A> annotationType,
			@Nullable Class<? extends Annotation> containerType) {

		return getRepeatableAnnotations(element, containerType, annotationType)
				.stream(annotationType)
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * Get the annotation attributes of <strong>all</strong> annotations of the specified
	 * {@code annotationName} in the annotation hierarchy above the supplied
	 * {@link AnnotatedElement} and store the results in a {@link MultiValueMap}.
	 * <p>Note: in contrast to {@link #getMergedAnnotationAttributes(AnnotatedElement, String)},
	 * this method does <em>not</em> support attribute overrides.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the annotation type to find
	 * @return a {@link MultiValueMap} keyed by attribute name, containing the annotation
	 * attributes from all annotations found, or {@code null} if not found
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	@Nullable
	public static MultiValueMap<String, Object> getAllAnnotationAttributes(
			AnnotatedElement element, String annotationName) {

		return getAllAnnotationAttributes(element, annotationName, false, false);
	}

	/**
	 * Get the annotation attributes of <strong>all</strong> annotations of
	 * the specified {@code annotationName} in the annotation hierarchy above
	 * the supplied {@link AnnotatedElement} and store the results in a
	 * {@link MultiValueMap}.
	 * <p>Note: in contrast to {@link #getMergedAnnotationAttributes(AnnotatedElement, String)},
	 * this method does <em>not</em> support attribute overrides.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the annotation type to find
	 * @param classValuesAsString whether to convert Class references into Strings or to
	 * preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested Annotation instances into
	 * {@code AnnotationAttributes} maps or to preserve them as Annotation instances
	 * @return a {@link MultiValueMap} keyed by attribute name, containing the annotation
	 * attributes from all annotations found, or {@code null} if not found
	 */
	@Nullable
	public static MultiValueMap<String, Object> getAllAnnotationAttributes(AnnotatedElement element,
			String annotationName, final boolean classValuesAsString, final boolean nestedAnnotationsAsMap) {

		Adapt[] adaptations = Adapt.values(classValuesAsString, nestedAnnotationsAsMap);
		return getAnnotations(element).stream(annotationName)
				.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
				.map(MergedAnnotation::withNonMergedAttributes)
				.collect(MergedAnnotationCollectors.toMultiValueMap(AnnotatedElementUtils::nullIfEmpty, adaptations));
	}

	/**
	 * Determine if an annotation of the specified {@code annotationType}
	 * is <em>available</em> on the supplied {@link AnnotatedElement} or
	 * within the annotation hierarchy <em>above</em> the specified element.
	 * <p>If this method returns {@code true}, then {@link #findMergedAnnotationAttributes}
	 * will return a non-null value.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @return {@code true} if a matching annotation is present
	 * @since 4.3
	 * @see #isAnnotated(AnnotatedElement, Class)
	 */
	public static boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		// Shortcut: directly present on the element, with no merging needed?
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
			return element.isAnnotationPresent(annotationType);
		}
		// Exhaustive retrieval of merged annotations...
		return findAnnotations(element).isPresent(annotationType);
	}

	/**
	 * Find the first annotation of the specified {@code annotationType} within
	 * the annotation hierarchy <em>above</em> the supplied {@code element} and
	 * merge that annotation's attributes with <em>matching</em> attributes from
	 * annotations in lower levels of the annotation hierarchy.
	 * <p>Attributes from lower levels in the annotation hierarchy override
	 * attributes of the same name from higher levels, and
	 * {@link AliasFor @AliasFor} semantics are fully supported, both
	 * within a single annotation and within the annotation hierarchy.
	 * <p>In contrast to {@link #getAllAnnotationAttributes}, the search algorithm
	 * used by this method will stop searching the annotation hierarchy once the
	 * first annotation of the specified {@code annotationType} has been found.
	 * As a consequence, additional annotations of the specified
	 * {@code annotationType} will be ignored.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @param classValuesAsString whether to convert Class references into
	 * Strings or to preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested Annotation instances into
	 * {@code AnnotationAttributes} maps or to preserve them as Annotation instances
	 * @return the merged {@code AnnotationAttributes}, or {@code null} if not found
	 * @since 4.2
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	@Nullable
	public static AnnotationAttributes findMergedAnnotationAttributes(AnnotatedElement element,
			Class<? extends Annotation> annotationType, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		MergedAnnotation<?> mergedAnnotation = findAnnotations(element)
				.get(annotationType, null, MergedAnnotationSelectors.firstDirectlyDeclared());
		return getAnnotationAttributes(mergedAnnotation, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * Find the first annotation of the specified {@code annotationName} within
	 * the annotation hierarchy <em>above</em> the supplied {@code element} and
	 * merge that annotation's attributes with <em>matching</em> attributes from
	 * annotations in lower levels of the annotation hierarchy.
	 * <p>Attributes from lower levels in the annotation hierarchy override
	 * attributes of the same name from higher levels, and
	 * {@link AliasFor @AliasFor} semantics are fully supported, both
	 * within a single annotation and within the annotation hierarchy.
	 * <p>In contrast to {@link #getAllAnnotationAttributes}, the search
	 * algorithm used by this method will stop searching the annotation
	 * hierarchy once the first annotation of the specified
	 * {@code annotationName} has been found. As a consequence, additional
	 * annotations of the specified {@code annotationName} will be ignored.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the annotation type to find
	 * @param classValuesAsString whether to convert Class references into Strings or to
	 * preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested Annotation instances into
	 * {@code AnnotationAttributes} maps or to preserve them as Annotation instances
	 * @return the merged {@code AnnotationAttributes}, or {@code null} if not found
	 * @since 4.2
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	@Nullable
	public static AnnotationAttributes findMergedAnnotationAttributes(AnnotatedElement element,
			String annotationName, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		MergedAnnotation<?> mergedAnnotation = findAnnotations(element)
				.get(annotationName, null, MergedAnnotationSelectors.firstDirectlyDeclared());
		return getAnnotationAttributes(mergedAnnotation, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * Find the first annotation of the specified {@code annotationType} within
	 * the annotation hierarchy <em>above</em> the supplied {@code element},
	 * merge that annotation's attributes with <em>matching</em> attributes from
	 * annotations in lower levels of the annotation hierarchy, and synthesize
	 * the result back into an annotation of the specified {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both
	 * within a single annotation and within the annotation hierarchy.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @return the merged, synthesized {@code Annotation}, or {@code null} if not found
	 * @since 4.2
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, Class)
	 */
	@Nullable
	public static <A extends Annotation> A findMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
		// Shortcut: directly present on the element, with no merging needed?
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
			return element.getDeclaredAnnotation(annotationType);
		}
		// Exhaustive retrieval of merged annotations...
		return findAnnotations(element)
				.get(annotationType, null, MergedAnnotationSelectors.firstDirectlyDeclared())
				.synthesize(MergedAnnotation::isPresent).orElse(null);
	}

	/**
	 * Find <strong>all</strong> annotations of the specified {@code annotationType}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the annotation
	 * hierarchy and synthesize the results back into an annotation of the specified
	 * {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element (never {@code null})
	 * @param annotationType the annotation type to find (never {@code null})
	 * @return the set of all merged, synthesized {@code Annotations} found,
	 * or an empty set if none were found
	 * @since 4.3
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Set<A> findAllMergedAnnotations(AnnotatedElement element, Class<A> annotationType) {
		return findAnnotations(element).stream(annotationType)
				.sorted(highAggregateIndexesFirst())
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * Find <strong>all</strong> annotations of the specified {@code annotationTypes}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the
	 * annotation hierarchy and synthesize the results back into an annotation
	 * of the corresponding {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element (never {@code null})
	 * @param annotationTypes the annotation types to find
	 * @return the set of all merged, synthesized {@code Annotations} found,
	 * or an empty set if none were found
	 * @since 5.1
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static Set<Annotation> findAllMergedAnnotations(AnnotatedElement element, Set<Class<? extends Annotation>> annotationTypes) {
		return findAnnotations(element).stream()
				.filter(MergedAnnotationPredicates.typeIn(annotationTypes))
				.sorted(highAggregateIndexesFirst())
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * Find all <em>repeatable annotations</em> of the specified {@code annotationType}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the annotation
	 * hierarchy and synthesize the results back into an annotation of the specified
	 * {@code annotationType}.
	 * <p>The container type that holds the repeatable annotations will be looked up
	 * via {@link java.lang.annotation.Repeatable}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element (never {@code null})
	 * @param annotationType the annotation type to find (never {@code null})
	 * @return the set of all merged repeatable {@code Annotations} found,
	 * or an empty set if none were found
	 * @throws IllegalArgumentException if the {@code element} or {@code annotationType}
	 * is {@code null}, or if the container type cannot be resolved
	 * @since 4.3
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 * @see #findMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
	 */
	public static <A extends Annotation> Set<A> findMergedRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType) {

		return findMergedRepeatableAnnotations(element, annotationType, null);
	}

	/**
	 * Find all <em>repeatable annotations</em> of the specified {@code annotationType}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the annotation
	 * hierarchy and synthesize the results back into an annotation of the specified
	 * {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * <p><strong>WARNING</strong>: if the supplied {@code containerType} is not
	 * {@code null}, the search will be restricted to supporting only repeatable
	 * annotations whose container is the supplied {@code containerType}. This
	 * prevents the search from finding repeatable annotations declared as
	 * meta-annotations on other types of repeatable annotations. If you need to
	 * support such a use case, favor {@link #findMergedRepeatableAnnotations(AnnotatedElement, Class)}
	 * over this method or alternatively use the {@link MergedAnnotations} API
	 * directly in conjunction with {@link RepeatableContainers} that are
	 * {@linkplain RepeatableContainers#and(Class, Class) composed} to support
	 * multiple repeatable annotation types.
	 * @param element the annotated element (never {@code null})
	 * @param annotationType the annotation type to find (never {@code null})
	 * @param containerType the type of the container that holds the annotations;
	 * may be {@code null} if the container type should be looked up via
	 * {@link java.lang.annotation.Repeatable}
	 * @return the set of all merged repeatable {@code Annotations} found,
	 * or an empty set if none were found
	 * @throws IllegalArgumentException if the {@code element} or {@code annotationType}
	 * is {@code null}, or if the container type cannot be resolved
	 * @throws AnnotationConfigurationException if the supplied {@code containerType}
	 * is not a valid container annotation for the supplied {@code annotationType}
	 * @since 4.3
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Set<A> findMergedRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType, @Nullable Class<? extends Annotation> containerType) {

		return findRepeatableAnnotations(element, containerType, annotationType)
				.stream(annotationType)
				.sorted(highAggregateIndexesFirst())
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	private static MergedAnnotations getAnnotations(AnnotatedElement element) {
		return MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none());
	}

	private static MergedAnnotations getRepeatableAnnotations(AnnotatedElement element,
			@Nullable Class<? extends Annotation> containerType, Class<? extends Annotation> annotationType) {

		RepeatableContainers repeatableContainers;
		if (containerType == null) {
			// Invoke RepeatableContainers.of() in order to adhere to the contract of
			// getMergedRepeatableAnnotations() which states that an IllegalArgumentException
			// will be thrown if the container cannot be resolved.
			//
			// In any case, we use standardRepeatables() in order to support repeatable
			// annotations on other types of repeatable annotations (i.e., nested repeatable
			// annotation types).
			//
			// See https://github.com/spring-projects/spring-framework/issues/20279
			RepeatableContainers.of(annotationType, null);
			repeatableContainers = RepeatableContainers.standardRepeatables();
		}
		else {
			repeatableContainers = RepeatableContainers.of(annotationType, containerType);
		}
		return MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS, repeatableContainers);
	}

	private static MergedAnnotations findAnnotations(AnnotatedElement element) {
		return MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none());
	}

	private static MergedAnnotations findRepeatableAnnotations(AnnotatedElement element,
			@Nullable Class<? extends Annotation> containerType, Class<? extends Annotation> annotationType) {

		RepeatableContainers repeatableContainers;
		if (containerType == null) {
			// Invoke RepeatableContainers.of() in order to adhere to the contract of
			// findMergedRepeatableAnnotations() which states that an IllegalArgumentException
			// will be thrown if the container cannot be resolved.
			//
			// In any case, we use standardRepeatables() in order to support repeatable
			// annotations on other types of repeatable annotations (i.e., nested repeatable
			// annotation types).
			//
			// See https://github.com/spring-projects/spring-framework/issues/20279
			RepeatableContainers.of(annotationType, null);
			repeatableContainers = RepeatableContainers.standardRepeatables();
		}
		else {
			repeatableContainers = RepeatableContainers.of(annotationType, containerType);
		}
		return MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY, repeatableContainers);
	}

	@Nullable
	private static MultiValueMap<String, Object> nullIfEmpty(MultiValueMap<String, Object> map) {
		return (map.isEmpty() ? null : map);
	}

	private static <A extends Annotation> Comparator<MergedAnnotation<A>> highAggregateIndexesFirst() {
		return Comparator.<MergedAnnotation<A>> comparingInt(MergedAnnotation::getAggregateIndex).reversed();
	}

	@Nullable
	private static AnnotationAttributes getAnnotationAttributes(MergedAnnotation<?> annotation,
			boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		if (!annotation.isPresent()) {
			return null;
		}
		return annotation.asAnnotationAttributes(Adapt.values(classValuesAsString, nestedAnnotationsAsMap));
	}


	/**
	 * Adapted {@link AnnotatedElement} that holds specific annotations.
	 */
	private static class AnnotatedElementForAnnotations implements AnnotatedElement {

		private final Annotation[] annotations;

		AnnotatedElementForAnnotations(Annotation... annotations) {
			this.annotations = annotations;
		}

		@Override
		@SuppressWarnings("unchecked")
		@Nullable
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			for (Annotation annotation : this.annotations) {
				if (annotation.annotationType() == annotationClass) {
					return (T) annotation;
				}
			}
			return null;
		}

		@Override
		public Annotation[] getAnnotations() {
			return this.annotations.clone();
		}

		@Override
		public Annotation[] getDeclaredAnnotations() {
			return this.annotations.clone();
		}

	}

}
