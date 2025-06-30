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

package org.springframework.core.type;

import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.core.annotation.MergedAnnotationCollectors;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.MergedAnnotationSelectors;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.MultiValueMap;

/**
 * Defines access to the annotations of a specific type ({@link AnnotationMetadata class}
 * or {@link MethodMetadata method}), in a form that does not necessarily require
 * class loading of the types being inspected. Note, however, that classes for
 * encountered annotations will be loaded.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Mark Pollack
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @see AnnotationMetadata
 * @see MethodMetadata
 */
public interface AnnotatedTypeMetadata {

	/**
	 * Get annotation details based on the direct annotations and meta-annotations
	 * of the underlying element.
	 * @return merged annotations based on the direct annotations and meta-annotations
	 * @since 5.2
	 */
	MergedAnnotations getAnnotations();

	/**
	 * Determine whether the underlying element has an annotation or meta-annotation
	 * of the given type defined.
	 * <p>If this method returns {@code true}, then
	 * {@link #getAnnotationAttributes} will return a non-null Map.
	 * @param annotationName the fully-qualified class name of the annotation
	 * type to look for
	 * @return whether a matching annotation is defined
	 */
	default boolean isAnnotated(String annotationName) {
		return getAnnotations().isPresent(annotationName);
	}

	/**
	 * Retrieve the attributes of the annotation of the given type, if any (i.e. if
	 * defined on the underlying element, as direct annotation or meta-annotation).
	 * <p>{@link org.springframework.core.annotation.AliasFor @AliasFor} semantics
	 * are fully supported, both within a single annotation and within annotation
	 * hierarchies.
	 * @param annotationName the fully-qualified class name of the annotation
	 * type to look for
	 * @return a {@link Map} of attributes, with each annotation attribute name
	 * as map key (for example, "location") and the attribute's value as map value; or
	 * {@code null} if no matching annotation is found
	 */
	default @Nullable Map<String, @Nullable Object> getAnnotationAttributes(String annotationName) {
		return getAnnotationAttributes(annotationName, false);
	}

	/**
	 * Retrieve the attributes of the annotation of the given type, if any (i.e. if
	 * defined on the underlying element, as direct annotation or meta-annotation).
	 * <p>{@link org.springframework.core.annotation.AliasFor @AliasFor} semantics
	 * are fully supported, both within a single annotation and within annotation
	 * hierarchies.
	 * @param annotationName the fully-qualified class name of the annotation
	 * type to look for
	 * @param classValuesAsString whether to convert class references to String
	 * class names for exposure as values in the returned Map, instead of Class
	 * references which might potentially have to be loaded first
	 * @return a {@link Map} of attributes, with each annotation attribute name
	 * as map key (for example, "location") and the attribute's value as map value; or
	 * {@code null} if no matching annotation is found
	 */
	default @Nullable Map<String, @Nullable Object> getAnnotationAttributes(String annotationName,
			boolean classValuesAsString) {

		MergedAnnotation<Annotation> annotation = getAnnotations().get(annotationName,
				null, MergedAnnotationSelectors.firstDirectlyDeclared());
		if (!annotation.isPresent()) {
			return null;
		}
		return annotation.asAnnotationAttributes(Adapt.values(classValuesAsString, true));
	}

	/**
	 * Retrieve all attributes of all annotations of the given type, if any (i.e. if
	 * defined on the underlying element, as direct annotation or meta-annotation).
	 * <p>Note: this method does <i>not</i> take attribute overrides on composed
	 * annotations into account.
	 * @param annotationName the fully-qualified class name of the annotation
	 * type to look for
	 * @return a {@link MultiValueMap} of attributes, with each annotation attribute
	 * name as map key (for example, "location") and a list of the attribute's values as
	 * map value; or {@code null} if no matching annotation is found
	 * @see #getAllAnnotationAttributes(String, boolean)
	 */
	default @Nullable MultiValueMap<String, @Nullable Object> getAllAnnotationAttributes(String annotationName) {
		return getAllAnnotationAttributes(annotationName, false);
	}

	/**
	 * Retrieve all attributes of all annotations of the given type, if any (i.e. if
	 * defined on the underlying element, as direct annotation or meta-annotation).
	 * <p>Note: this method does <i>not</i> take attribute overrides on composed
	 * annotations into account.
	 * @param annotationName the fully-qualified class name of the annotation
	 * type to look for
	 * @param classValuesAsString whether to convert class references to String
	 * class names for exposure as values in the returned Map, instead of Class
	 * references which might potentially have to be loaded first
	 * @return a {@link MultiValueMap} of attributes, with each annotation attribute
	 * name as map key (for example, "location") and a list of the attribute's values as
	 * map value; or {@code null} if no matching annotation is found
	 * @see #getAllAnnotationAttributes(String)
	 */
	default @Nullable MultiValueMap<String, @Nullable Object> getAllAnnotationAttributes(
			String annotationName, boolean classValuesAsString) {

		Adapt[] adaptations = Adapt.values(classValuesAsString, true);
		return getAnnotations().stream(annotationName)
				.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
				.map(MergedAnnotation::withNonMergedAttributes)
				.collect(MergedAnnotationCollectors.toMultiValueMap(
						(MultiValueMap<String, @Nullable Object> map) -> (map.isEmpty() ? null : map), adaptations));
	}

	/**
	 * Retrieve all <em>repeatable annotations</em> of the given type within the
	 * annotation hierarchy <em>above</em> the underlying element (as direct
	 * annotation or meta-annotation); and for each annotation found, merge that
	 * annotation's attributes with <em>matching</em> attributes from annotations
	 * in lower levels of the annotation hierarchy and store the results in an
	 * instance of {@link AnnotationAttributes}.
	 * <p>{@link org.springframework.core.annotation.AliasFor @AliasFor} semantics
	 * are fully supported, both within a single annotation and within annotation
	 * hierarchies.
	 * @param annotationType the annotation type to find
	 * @param containerType the type of the container that holds the annotations
	 * @param classValuesAsString whether to convert class references to {@code String}
	 * class names for exposure as values in the returned {@code AnnotationAttributes},
	 * instead of {@code Class} references which might potentially have to be loaded
	 * first
	 * @return the set of all merged repeatable {@code AnnotationAttributes} found,
	 * or an empty set if none were found
	 * @since 6.1
	 * @see #getMergedRepeatableAnnotationAttributes(Class, Class, boolean, boolean)
	 * @see #getMergedRepeatableAnnotationAttributes(Class, Class, Predicate, boolean, boolean)
	 */
	default Set<AnnotationAttributes> getMergedRepeatableAnnotationAttributes(
			Class<? extends Annotation> annotationType, Class<? extends Annotation> containerType,
			boolean classValuesAsString) {

		return getMergedRepeatableAnnotationAttributes(annotationType, containerType, classValuesAsString, false);
	}

	/**
	 * Retrieve all <em>repeatable annotations</em> of the given type within the
	 * annotation hierarchy <em>above</em> the underlying element (as direct
	 * annotation or meta-annotation); and for each annotation found, merge that
	 * annotation's attributes with <em>matching</em> attributes from annotations
	 * in lower levels of the annotation hierarchy and store the results in an
	 * instance of {@link AnnotationAttributes}.
	 * <p>{@link org.springframework.core.annotation.AliasFor @AliasFor} semantics
	 * are fully supported, both within a single annotation and within annotation
	 * hierarchies.
	 * <p>If the {@code sortByReversedMetaDistance} flag is set to {@code true},
	 * the results will be sorted in {@link Comparator#reversed() reversed} order
	 * based on each annotation's {@linkplain MergedAnnotation#getDistance()
	 * meta distance}, which effectively orders meta-annotations before annotations
	 * that are declared directly on the underlying element.
	 * @param annotationType the annotation type to find
	 * @param containerType the type of the container that holds the annotations
	 * @param classValuesAsString whether to convert class references to {@code String}
	 * class names for exposure as values in the returned {@code AnnotationAttributes},
	 * instead of {@code Class} references which might potentially have to be loaded
	 * first
	 * @param sortByReversedMetaDistance {@code true} if the results should be
	 * sorted in reversed order based on each annotation's meta distance
	 * @return the set of all merged repeatable {@code AnnotationAttributes} found,
	 * or an empty set if none were found
	 * @since 6.1
	 * @see #getMergedRepeatableAnnotationAttributes(Class, Class, boolean)
	 * @see #getMergedRepeatableAnnotationAttributes(Class, Class, Predicate, boolean, boolean)
	 */
	default Set<AnnotationAttributes> getMergedRepeatableAnnotationAttributes(
			Class<? extends Annotation> annotationType, Class<? extends Annotation> containerType,
			boolean classValuesAsString, boolean sortByReversedMetaDistance) {

		return getMergedRepeatableAnnotationAttributes(annotationType, containerType,
				mergedAnnotation -> true, classValuesAsString, sortByReversedMetaDistance);
	}

	/**
	 * Retrieve all <em>repeatable annotations</em> of the given type within the
	 * annotation hierarchy <em>above</em> the underlying element (as direct
	 * annotation or meta-annotation); and for each annotation found, merge that
	 * annotation's attributes with <em>matching</em> attributes from annotations
	 * in lower levels of the annotation hierarchy and store the results in an
	 * instance of {@link AnnotationAttributes}.
	 * <p>{@link org.springframework.core.annotation.AliasFor @AliasFor} semantics
	 * are fully supported, both within a single annotation and within annotation
	 * hierarchies.
	 * <p>The supplied {@link Predicate} will be used to filter the results. For
	 * example, supply {@code mergedAnnotation -> true} to include all annotations
	 * in the results; supply {@code MergedAnnotation::isDirectlyPresent} to limit
	 * the results to directly declared annotations, etc.
	 * <p>If the {@code sortByReversedMetaDistance} flag is set to {@code true},
	 * the results will be sorted in {@link Comparator#reversed() reversed} order
	 * based on each annotation's {@linkplain MergedAnnotation#getDistance()
	 * meta distance}, which effectively orders meta-annotations before annotations
	 * that are declared directly on the underlying element.
	 * @param annotationType the annotation type to find
	 * @param containerType the type of the container that holds the annotations
	 * @param predicate a {@code Predicate} to apply to each {@code MergedAnnotation}
	 * to determine if it should be included in the results
	 * @param classValuesAsString whether to convert class references to {@code String}
	 * class names for exposure as values in the returned {@code AnnotationAttributes},
	 * instead of {@code Class} references which might potentially have to be loaded
	 * first
	 * @param sortByReversedMetaDistance {@code true} if the results should be
	 * sorted in reversed order based on each annotation's meta distance
	 * @return the set of all merged repeatable {@code AnnotationAttributes} found,
	 * or an empty set if none were found
	 * @since 6.1.2
	 * @see #getMergedRepeatableAnnotationAttributes(Class, Class, boolean)
	 * @see #getMergedRepeatableAnnotationAttributes(Class, Class, boolean, boolean)
	 */
	default Set<AnnotationAttributes> getMergedRepeatableAnnotationAttributes(
			Class<? extends Annotation> annotationType, Class<? extends Annotation> containerType,
			Predicate<MergedAnnotation<? extends Annotation>> predicate, boolean classValuesAsString,
			boolean sortByReversedMetaDistance) {

		Stream<MergedAnnotation<Annotation>> stream = getAnnotations().stream()
				.filter(predicate)
				.filter(MergedAnnotationPredicates.typeIn(containerType, annotationType));

		if (sortByReversedMetaDistance) {
			stream = stream.sorted(reversedMetaDistance());
		}

		Adapt[] adaptations = Adapt.values(classValuesAsString, true);
		return stream
				.map(annotation -> annotation.asAnnotationAttributes(adaptations))
				.flatMap(attributes -> {
					if (containerType.equals(attributes.annotationType())) {
						return Stream.of(attributes.getAnnotationArray(MergedAnnotation.VALUE));
					}
					return Stream.of(attributes);
				})
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}


	private static Comparator<MergedAnnotation<Annotation>> reversedMetaDistance() {
		return Comparator.<MergedAnnotation<Annotation>> comparingInt(MergedAnnotation::getDistance).reversed();
	}

}
