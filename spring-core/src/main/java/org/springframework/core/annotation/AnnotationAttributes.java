/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link LinkedHashMap} subclass representing annotation attribute
 * <em>key-value</em> pairs as read by {@link AnnotationUtils},
 * {@link AnnotatedElementUtils}, and Spring's reflection- and ASM-based
 * {@link org.springframework.core.type.AnnotationMetadata} implementations.
 *
 * <p>Provides 'pseudo-reification' to avoid noisy Map generics in the calling
 * code as well as convenience methods for looking up annotation attributes
 * in a type-safe fashion.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 3.1.1
 * @see AnnotationUtils#getAnnotationAttributes
 * @see AnnotatedElementUtils
 */
@SuppressWarnings("serial")
public class AnnotationAttributes extends LinkedHashMap<String, Object> {

	private static final String UNKNOWN = "unknown";

	private final Class<? extends Annotation> annotationType;

	private final String displayName;

	boolean validated = false;


	/**
	 * Create a new, empty {@link AnnotationAttributes} instance.
	 */
	public AnnotationAttributes() {
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * Create a new, empty {@link AnnotationAttributes} instance with the
	 * given initial capacity to optimize performance.
	 * @param initialCapacity initial size of the underlying map
	 */
	public AnnotationAttributes(int initialCapacity) {
		super(initialCapacity);
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * Create a new {@link AnnotationAttributes} instance, wrapping the provided
	 * map and all its <em>key-value</em> pairs.
	 * @param map original source of annotation attribute <em>key-value</em> pairs
	 * @see #fromMap(Map)
	 */
	public AnnotationAttributes(Map<String, Object> map) {
		super(map);
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * Create a new {@link AnnotationAttributes} instance, wrapping the provided
	 * map and all its <em>key-value</em> pairs.
	 * @param other original source of annotation attribute <em>key-value</em> pairs
	 * @see #fromMap(Map)
	 */
	public AnnotationAttributes(AnnotationAttributes other) {
		super(other);
		this.annotationType = other.annotationType;
		this.displayName = other.displayName;
		this.validated = other.validated;
	}

	/**
	 * Create a new, empty {@link AnnotationAttributes} instance for the
	 * specified {@code annotationType}.
	 * @param annotationType the type of annotation represented by this
	 * {@code AnnotationAttributes} instance; never {@code null}
	 * @since 4.2
	 */
	public AnnotationAttributes(Class<? extends Annotation> annotationType) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = annotationType;
		this.displayName = annotationType.getName();
	}

	/**
	 * Create a new, empty {@link AnnotationAttributes} instance for the
	 * specified {@code annotationType}.
	 * @param annotationType the annotation type name represented by this
	 * {@code AnnotationAttributes} instance; never {@code null}
	 * @param classLoader the ClassLoader to try to load the annotation type on,
	 * or {@code null} to just store the annotation type name
	 * @since 4.3.2
	 */
	public AnnotationAttributes(String annotationType, ClassLoader classLoader) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = getAnnotationType(annotationType, classLoader);
		this.displayName = annotationType;
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends Annotation> getAnnotationType(String annotationType, ClassLoader classLoader) {
		if (classLoader != null) {
			try {
				return (Class<? extends Annotation>) classLoader.loadClass(annotationType);
			}
			catch (ClassNotFoundException ex) {
				// Annotation Class not resolvable
			}
		}
		return null;
	}


	/**
	 * Get the type of annotation represented by this
	 * {@code AnnotationAttributes} instance.
	 * @return the annotation type, or {@code null} if unknown
	 * @since 4.2
	 */
	public Class<? extends Annotation> annotationType() {
		return this.annotationType;
	}

	/**
	 * Get the value stored under the specified {@code attributeName} as a
	 * string.
	 * @param attributeName the name of the attribute to get; never
	 * {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	public String getString(String attributeName) {
		return getRequiredAttribute(attributeName, String.class);
	}

	/**
	 * Get the value stored under the specified {@code attributeName} as an
	 * array of strings.
	 * <p>If the value stored under the specified {@code attributeName} is
	 * a string, it will be wrapped in a single-element array before
	 * returning it.
	 * @param attributeName the name of the attribute to get; never
	 * {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	public String[] getStringArray(String attributeName) {
		return getRequiredAttribute(attributeName, String[].class);
	}

	/**
	 * Get the value stored under the specified {@code attributeName} as a
	 * boolean.
	 * @param attributeName the name of the attribute to get; never
	 * {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	public boolean getBoolean(String attributeName) {
		return getRequiredAttribute(attributeName, Boolean.class);
	}

	/**
	 * Get the value stored under the specified {@code attributeName} as a
	 * number.
	 * @param attributeName the name of the attribute to get; never
	 * {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	@SuppressWarnings("unchecked")
	public <N extends Number> N getNumber(String attributeName) {
		return (N) getRequiredAttribute(attributeName, Number.class);
	}

	/**
	 * Get the value stored under the specified {@code attributeName} as an
	 * enum.
	 * @param attributeName the name of the attribute to get; never
	 * {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	@SuppressWarnings("unchecked")
	public <E extends Enum<?>> E getEnum(String attributeName) {
		return (E) getRequiredAttribute(attributeName, Enum.class);
	}

	/**
	 * Get the value stored under the specified {@code attributeName} as a
	 * class.
	 * @param attributeName the name of the attribute to get; never
	 * {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	@SuppressWarnings("unchecked")
	public <T> Class<? extends T> getClass(String attributeName) {
		return getRequiredAttribute(attributeName, Class.class);
	}

	/**
	 * Get the value stored under the specified {@code attributeName} as an
	 * array of classes.
	 * <p>If the value stored under the specified {@code attributeName} is
	 * a class, it will be wrapped in a single-element array before
	 * returning it.
	 * @param attributeName the name of the attribute to get; never
	 * {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	public Class<?>[] getClassArray(String attributeName) {
		return getRequiredAttribute(attributeName, Class[].class);
	}

	/**
	 * Get the {@link AnnotationAttributes} stored under the specified
	 * {@code attributeName}.
	 * <p>Note: if you expect an actual annotation, invoke
	 * {@link #getAnnotation(String, Class)} instead.
	 * @param attributeName the name of the attribute to get; never
	 * {@code null} or empty
	 * @return the {@code AnnotationAttributes}
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	public AnnotationAttributes getAnnotation(String attributeName) {
		return getRequiredAttribute(attributeName, AnnotationAttributes.class);
	}

	/**
	 * Get the annotation of type {@code annotationType} stored under the
	 * specified {@code attributeName}.
	 * @param attributeName the name of the attribute to get; never
	 * {@code null} or empty
	 * @param annotationType the expected annotation type; never {@code null}
	 * @return the annotation
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 * @since 4.2
	 */
	public <A extends Annotation> A getAnnotation(String attributeName, Class<A> annotationType) {
		return getRequiredAttribute(attributeName, annotationType);
	}

	/**
	 * Get the array of {@link AnnotationAttributes} stored under the specified
	 * {@code attributeName}.
	 * <p>If the value stored under the specified {@code attributeName} is
	 * an instance of {@code AnnotationAttributes}, it will be wrapped in
	 * a single-element array before returning it.
	 * <p>Note: if you expect an actual array of annotations, invoke
	 * {@link #getAnnotationArray(String, Class)} instead.
	 * @param attributeName the name of the attribute to get; never
	 * {@code null} or empty
	 * @return the array of {@code AnnotationAttributes}
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	public AnnotationAttributes[] getAnnotationArray(String attributeName) {
		return getRequiredAttribute(attributeName, AnnotationAttributes[].class);
	}

	/**
	 * Get the array of type {@code annotationType} stored under the specified
	 * {@code attributeName}.
	 * <p>If the value stored under the specified {@code attributeName} is
	 * an {@code Annotation}, it will be wrapped in a single-element array
	 * before returning it.
	 * @param attributeName the name of the attribute to get; never
	 * {@code null} or empty
	 * @param annotationType the expected annotation type; never {@code null}
	 * @return the annotation array
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 * @since 4.2
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A[] getAnnotationArray(String attributeName, Class<A> annotationType) {
		Object array = Array.newInstance(annotationType, 0);
		return (A[]) getRequiredAttribute(attributeName, array.getClass());
	}

	/**
	 * Get the value stored under the specified {@code attributeName},
	 * ensuring that the value is of the {@code expectedType}.
	 * <p>If the {@code expectedType} is an array and the value stored
	 * under the specified {@code attributeName} is a single element of the
	 * component type of the expected array type, the single element will be
	 * wrapped in a single-element array of the appropriate type before
	 * returning it.
	 * @param attributeName the name of the attribute to get; never
	 * {@code null} or empty
	 * @param expectedType the expected type; never {@code null}
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	@SuppressWarnings("unchecked")
	private <T> T getRequiredAttribute(String attributeName, Class<T> expectedType) {
		Assert.hasText(attributeName, "'attributeName' must not be null or empty");
		Object value = get(attributeName);
		assertAttributePresence(attributeName, value);
		assertNotException(attributeName, value);
		if (!expectedType.isInstance(value) && expectedType.isArray() &&
				expectedType.getComponentType().isInstance(value)) {
			Object array = Array.newInstance(expectedType.getComponentType(), 1);
			Array.set(array, 0, value);
			value = array;
		}
		assertAttributeType(attributeName, value, expectedType);
		return (T) value;
	}

	private void assertAttributePresence(String attributeName, Object attributeValue) {
		Assert.notNull(attributeValue, () -> String.format("Attribute '%s' not found in attributes for annotation [%s]",
				attributeName, this.displayName));
	}

	private void assertNotException(String attributeName, Object attributeValue) {
		if (attributeValue instanceof Exception) {
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' for annotation [%s] was not resolvable due to exception [%s]",
					attributeName, this.displayName, attributeValue), (Exception) attributeValue);
		}
	}

	private void assertAttributeType(String attributeName, Object attributeValue, Class<?> expectedType) {
		if (!expectedType.isInstance(attributeValue)) {
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' is of type [%s], but [%s] was expected in attributes for annotation [%s]",
					attributeName, attributeValue.getClass().getSimpleName(), expectedType.getSimpleName(),
					this.displayName));
		}
	}

	/**
	 * Store the supplied {@code value} in this map under the specified
	 * {@code key}, unless a value is already stored under the key.
	 * @param key the key under which to store the value
	 * @param value the value to store
	 * @return the current value stored in this map, or {@code null} if no
	 * value was previously stored in this map
	 * @see #get
	 * @see #put
	 * @since 4.2
	 */
	@Override
	public Object putIfAbsent(String key, Object value) {
		Object obj = get(key);
		if (obj == null) {
			obj = put(key, value);
		}
		return obj;
	}

	@Override
	public String toString() {
		Iterator<Map.Entry<String, Object>> entries = entrySet().iterator();
		StringBuilder sb = new StringBuilder("{");
		while (entries.hasNext()) {
			Map.Entry<String, Object> entry = entries.next();
			sb.append(entry.getKey());
			sb.append('=');
			sb.append(valueToString(entry.getValue()));
			sb.append(entries.hasNext() ? ", " : "");
		}
		sb.append("}");
		return sb.toString();
	}

	private String valueToString(Object value) {
		if (value == this) {
			return "(this Map)";
		}
		if (value instanceof Object[]) {
			return "[" + StringUtils.arrayToDelimitedString((Object[]) value, ", ") + "]";
		}
		return String.valueOf(value);
	}


	/**
	 * Return an {@link AnnotationAttributes} instance based on the given map.
	 * <p>If the map is already an {@code AnnotationAttributes} instance, it
	 * will be cast and returned immediately without creating a new instance.
	 * Otherwise a new instance will be created by passing the supplied map
	 * to the {@link #AnnotationAttributes(Map)} constructor.
	 * @param map original source of annotation attribute <em>key-value</em> pairs
	 */
	public static AnnotationAttributes fromMap(Map<String, Object> map) {
		if (map == null) {
			return null;
		}
		if (map instanceof AnnotationAttributes) {
			return (AnnotationAttributes) map;
		}
		return new AnnotationAttributes(map);
	}

}
