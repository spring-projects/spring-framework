/*
 * Copyright 2002-2019 the original author or authors.
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
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;

/**
 * A single merged annotation returned from a {@link MergedAnnotations}
 * collection. Presents a view onto an annotation where attribute values may
 * have been "merged" from different source values.
 *
 * <p>Attribute values may be accessed using the various {@code get} methods.
 * For example, to access an {@code int} attribute the {@link #getInt(String)}
 * method would be used.
 *
 * <p>Note that attribute values are <b>not</b> converted when accessed.
 * For example, it is not possible to call {@link #getString(String)} if the
 * underlying attribute is an {@code int}. The only exception to this rule is
 * {@code Class} and {@code Class[]} values which may be accessed as
 * {@code String} and {@code String[]} respectively to prevent potential early
 * class initialization.
 *
 * <p>If necessary, a {@link MergedAnnotation} can be {@link #synthesize()
 * synthesized} back into an actual {@link java.lang.annotation.Annotation}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 5.2
 * @param <A> the annotation type
 * @see MergedAnnotations
 * @see MergedAnnotationPredicates
 */
public interface MergedAnnotation<A extends Annotation> {

	/**
	 * The attribute name for annotations with a single element.
	 */
	String VALUE = "value";


	/**
	 * Return the {@code Class} reference for the actual annotation type.
	 * @return the annotation type
	 */
	Class<A> getType();

	/**
	 * Return if the annotation is present on the source. Considers
	 * {@link #isDirectlyPresent() direct annotations}, and
	 * {@link #isMetaPresent() meta-annotation} annotations within the context
	 * of the {@link SearchStrategy} used.
	 * @return {@code true} if the annotation is present
	 */
	boolean isPresent();

	/**
	 * Return if the annotation is directly present on the source. A directly
	 * present annotation is one that the user has explicitly defined and not
	 * one that is {@link #isMetaPresent() meta-present} or
	 * {@link Inherited @Inherited}.
	 * @return {@code true} if the annotation is directly present
	 */
	boolean isDirectlyPresent();

	/**
	 * Return if the annotation is meta-present on the source. A meta-present
	 * annotation is an annotation that the user hasn't explicitly defined, but
	 * has been used as a meta-annotation somewhere in the annotation hierarchy.
	 * @return {@code true} if the annotation is meta-present
	 */
	boolean isMetaPresent();

	/**
	 * Return the depth of this annotation related to its use as a
	 * meta-annotation. A directly declared annotation has a depth of {@code 0},
	 * a meta-annotation has a depth of {@code 1}, a meta-annotation on a
	 * meta-annotation has a depth of {@code 2}, etc. A {@link #missing()
	 * missing} annotation will always return a depth of {@code -1}.
	 * @return the annotation depth or {@code -1} if the annotation is missing
	 */
	int getDepth();

	/**
	 * Return the index of the aggregate collection containing this annotation.
	 * Can be used to reorder a stream of annotations, for example, to give a
	 * higher priority to annotations declared on a superclass or interface. A
	 * {@link #missing() missing} annotation will always return an aggregate
	 * index of {@code -1}.
	 * @return the aggregate index (starting at {@code 0}) or {@code -1} if the
	 * annotation is missing
	 */
	int getAggregateIndex();

	/**
	 * Return the source that ultimately declared the annotation, or
	 * {@code null} if the source is not known. If this merged annotation was
	 * created {@link MergedAnnotations#from(java.lang.reflect.AnnotatedElement)
	 * from} an {@link AnnotatedElement} then this source will be an element of
	 * the same type. If the annotation was loaded without using reflection, the
	 * source can be of any type, but should have a sensible {@code toString()}.
	 * Meta-annotations will return the same source as the {@link #getParent()}.
	 * @return the source, or {@code null}
	 */
	@Nullable
	Object getSource();

	/**
	 * Return the parent of the meta-annotation, or {@code null} if the
	 * annotation is not {@link #isMetaPresent() meta-present}.
	 * @return the parent annotation or {@code null}
	 */
	@Nullable
	MergedAnnotation<?> getParent();

	/**
	 * Return if the specified attribute name as a non-default value when
	 * compared to the annotation declaration.
	 * @param attributeName the attribute name
	 * @return {@code true} if the attribute value is different from the default
	 * value
	 */
	boolean hasNonDefaultValue(String attributeName);

	/**
	 * Return if the specified attribute name as a default value when compared
	 * to the annotation declaration.
	 * @param attributeName the attribute name
	 * @return {@code true} if the attribute value is the same as the default
	 * value
	 */
	boolean hasDefaultValue(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required byte attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a byte
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	byte getByte(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required byte array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a byte array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	byte[] getByteArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required boolean attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a boolean
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	boolean getBoolean(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required boolean array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a boolean array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	boolean[] getBooleanArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required char attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a char
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	char getChar(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required char array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a char array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	char[] getCharArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required short attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a short
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	short getShort(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required short array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a short array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	short[] getShortArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required int attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as an int
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	int getInt(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required int array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as an int array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	int[] getIntArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required long attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a long
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	long getLong(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required long array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a long array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	long[] getLongArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required double attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a double
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	double getDouble(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required double array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a double array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	double[] getDoubleArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required float attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a float
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	float getFloat(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required float array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a float array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	float[] getFloatArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required string attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a string
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	String getString(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required string array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a string array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	String[] getStringArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required class attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a class
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	Class<?> getClass(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required class array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a class array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	Class<?>[] getClassArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required enum attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @param type the enum type
	 * @return the value as a enum
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<E extends Enum<E>> E getEnum(String attributeName, Class<E> type) throws NoSuchElementException;

	/**
	 * Return a required enum array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @param type the enum type
	 * @return the value as a enum array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<E extends Enum<E>> E[] getEnumArray(String attributeName, Class<E> type) throws NoSuchElementException;

	/**
	 * Return a required annotation attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @param type the annotation type
	 * @return the value as a {@link MergedAnnotation}
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName, Class<T> type)
			throws NoSuchElementException;

	/**
	 * Return a required annotation array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @param type the annotation type
	 * @return the value as a {@link MergedAnnotation} array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(String attributeName, Class<T> type)
			throws NoSuchElementException;

	/**
	 * Return an optional attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return an optional value or {@link Optional#empty()} if there is no
	 * matching attribute
	 */
	Optional<Object> getValue(String attributeName);

	/**
	 * Return an optional attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @param type the attribute type. Must be compatible with the underlying
	 * attribute type or {@code Object.class}.
	 * @return an optional value or {@link Optional#empty()} if there is no
	 * matching attribute
	 */
	<T> Optional<T> getValue(String attributeName, Class<T> type);

	/**
	 * Return the default attribute value from the annotation as specified in
	 * the annotation declaration.
	 * @param attributeName the attribute name
	 * @return an optional of the default value or {@link Optional#empty()} if
	 * there is no matching attribute or no defined default
	 */
	Optional<Object> getDefaultValue(String attributeName);

	/**
	 * Return the default attribute value from the annotation as specified in
	 * the annotation declaration.
	 * @param attributeName the attribute name
	 * @param type the attribute type. Must be compatible with the underlying
	 * attribute type or {@code Object.class}.
	 * @return an optional of the default value or {@link Optional#empty()} if
	 * there is no matching attribute or no defined default
	 */
	<T> Optional<T> getDefaultValue(String attributeName, Class<T> type);

	/**
	 * Return a new view of the annotation with all attributes that have default
	 * values removed.
	 * @return a filtered view of the annotation without any attributes that
	 * have a default value
	 * @see #filterAttributes(Predicate)
	 */
	MergedAnnotation<A> filterDefaultValues();

	/**
	 * Return a new view of the annotation with only attributes that match the
	 * given predicate.
	 * @param predicate a predicate used to filter attribute names
	 * @return a filtered view of the annotation
	 * @see #filterDefaultValues()
	 * @see MergedAnnotationPredicates
	 */
	MergedAnnotation<A> filterAttributes(Predicate<String> predicate);

	/**
	 * Return a new view of the annotation that exposes non-merged attribute values.
	 * Methods from this view will return attribute values with only alias mirroring
	 * rules applied. Aliases to parent attributes will not be applied.
	 * @return a non-merged view of the annotation
	 */
	MergedAnnotation<A> withNonMergedAttributes();

	/**
	 * Return an immutable {@link Map} that contains all the annotation attributes.
	 * The {@link MapValues} options may be used to change the way that values are added.
	 * @param options map value options
	 * @return an immutable map containing the attributes and values
	 */
	Map<String, Object> asMap(MapValues... options);

	/**
	 * Return a {@link Map} of the given type that contains all the annotation attributes.
	 * The {@link MapValues} options may be used to change the way that values are added.
	 * @param factory a map factory
	 * @param options map value options
	 * @return a map containing the attributes and values
	 */
	<T extends Map<String, Object>> T asMap(Function<MergedAnnotation<?>, T> factory, MapValues... options);

	/**
	 * Return a type-safe synthesized version of this annotation that can be
	 * used directly in code. The result is synthesized using a JDK {@link Proxy}
	 * and as a result may incur a computational cost when first invoked.
	 * @return a synthesized version of the annotation.
	 * @throws NoSuchElementException on a missing annotation
	 */
	A synthesize() throws NoSuchElementException;

	/**
	 * Optionally return type-safe synthesized version of this annotation based
	 * on a condition predicate. The result is synthesized using a JDK {@link Proxy}
	 * and as a result may incur a computational cost when first invoked.
	 * @param condition the test to determine if the annotation can be synthesized
	 * @return a optional containing the synthesized version of the annotation or
	 * an empty optional if the condition doesn't match
	 * @throws NoSuchElementException on a missing annotation
	 * @see MergedAnnotationPredicates
	 */
	Optional<A> synthesize(Predicate<? super MergedAnnotation<A>> condition) throws NoSuchElementException;


	/**
	 * Return an {@link MergedAnnotation} that represents a missing annotation
	 * (i.e. one that is not present).
	 * @return an instance representing a missing annotation
	 */
	static <A extends Annotation> MergedAnnotation<A> missing() {
		return MissingMergedAnnotation.getInstance();
	}

	/**
	 * Create a new {@link MergedAnnotation} instance from the specified
	 * annotation.
	 * @param annotation the annotation to include
	 * @return a {@link MergedAnnotation} instance containing the annotation
	 */
	static <A extends Annotation> MergedAnnotation<A> from(A annotation) {
		return from(null, annotation);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * annotation.
	 * @param source the source for the annotation. This source is used only for
	 * information and logging. It does not need to <em>actually</em> contain
	 * the specified annotations and it will not be searched.
	 * @param annotation the annotation to include
	 * @return a {@link MergedAnnotation} instance for the annotation
	 */
	static <A extends Annotation> MergedAnnotation<A> from(@Nullable Object source, A annotation) {
		return TypeMappedAnnotation.from(source, annotation);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * annotation type. The resulting annotation will not have any attribute
	 * values, but may still be used to query default values.
	 * @param annotationType the annotation type
	 * @return a {@link MergedAnnotation} instance for the annotation
	 */
	static <A extends Annotation> MergedAnnotation<A> from(Class<A> annotationType) {
		return from(null, annotationType, null);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * annotation type and attributes maps.
	 * @param annotationType the annotation type
	 * @param attributes the annotation attributes or {@code null} if just default
	 * values should be used
	 * @return a {@link MergedAnnotation} instance for the annotation and attributes
	 * @see #from(AnnotatedElement, Class, Map)
	 */
	static <A extends Annotation> MergedAnnotation<A> from(
			Class<A> annotationType, @Nullable Map<String, ?> attributes) {

		return from(null, annotationType, attributes);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * annotation type and attributes maps.
	 * @param source the source for the annotation. This source is used only for
	 * information and logging. It does not need to <em>actually</em> contain
	 * the specified annotations and it will not be searched.
	 * @param annotationType the annotation type
	 * @param attributes the annotation attributes or {@code null} if just default
	 * values should be used
	 * @return a {@link MergedAnnotation} instance for the annotation and attributes
	 */
	static <A extends Annotation> MergedAnnotation<A> from(
			@Nullable AnnotatedElement source, Class<A> annotationType, @Nullable Map<String, ?> attributes) {

		return TypeMappedAnnotation.from(source, annotationType, attributes);
	}


	/**
	 * Options that effect the way map values are
	 * {@link MergedAnnotation#asMap(MapValues...) converted}.
	 */
	enum MapValues {

		/**
		 * Add class or class array attributes as strings.
		 */
		CLASS_TO_STRING,

		/**
		 * Convert any nested annotation or annotation arrays to maps rather
		 * than synthesizing the values.
		 */
		ANNOTATION_TO_MAP;

		protected final boolean isIn(MapValues... options) {
			for (MapValues candidate : options) {
				if (candidate == this) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Factory method to create a {@link MapValues} array from a set of boolean flags.
		 * @param classToString if {@link MapValues#CLASS_TO_STRING} is included
		 * @param annotationsToMap if {@link MapValues#ANNOTATION_TO_MAP} is included
		 * @return a new {@link MapValues} array
		 */
		public static MapValues[] of(boolean classToString, boolean annotationsToMap) {
			EnumSet<MapValues> result = EnumSet.noneOf(MapValues.class);
			addIfTrue(result, MapValues.CLASS_TO_STRING, classToString);
			addIfTrue(result, MapValues.ANNOTATION_TO_MAP, annotationsToMap);
			return result.toArray(new MapValues[0]);
		}

		private static <T> void addIfTrue(Set<T> result, T value, boolean test) {
			if (test) {
				result.add(value);
			}
		}
	}

}
