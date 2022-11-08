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
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link InvocationHandler} for an {@link Annotation} that Spring has
 * <em>synthesized</em> (i.e. wrapped in a dynamic proxy) with additional
 * functionality such as attribute alias handling.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 5.2
 * @param <A> the annotation type
 * @see Annotation
 * @see AnnotationUtils#synthesizeAnnotation(Annotation, AnnotatedElement)
 */
final class SynthesizedMergedAnnotationInvocationHandler<A extends Annotation> implements InvocationHandler {

	private final MergedAnnotation<?> annotation;

	private final Class<A> type;

	private final AttributeMethods attributes;

	private final Map<String, Object> valueCache = new ConcurrentHashMap<>(8);

	@Nullable
	private volatile Integer hashCode;

	@Nullable
	private volatile String string;


	private SynthesizedMergedAnnotationInvocationHandler(MergedAnnotation<A> annotation, Class<A> type) {
		Assert.notNull(annotation, "MergedAnnotation must not be null");
		Assert.notNull(type, "Type must not be null");
		Assert.isTrue(type.isAnnotation(), "Type must be an annotation");
		this.annotation = annotation;
		this.type = type;
		this.attributes = AttributeMethods.forAnnotationType(type);
	}


	@Override
	public Object invoke(Object proxy, Method method, Object[] args) {
		if (ReflectionUtils.isEqualsMethod(method)) {
			return annotationEquals(args[0]);
		}
		if (ReflectionUtils.isHashCodeMethod(method)) {
			return annotationHashCode();
		}
		if (ReflectionUtils.isToStringMethod(method)) {
			return annotationToString();
		}
		if (isAnnotationTypeMethod(method)) {
			return this.type;
		}
		if (this.attributes.indexOf(method.getName()) != -1) {
			return getAttributeValue(method);
		}
		throw new AnnotationConfigurationException(String.format(
				"Method [%s] is unsupported for synthesized annotation type [%s]", method, this.type));
	}

	private boolean isAnnotationTypeMethod(Method method) {
		return (method.getName().equals("annotationType") && method.getParameterCount() == 0);
	}

	/**
	 * See {@link Annotation#equals(Object)} for a definition of the required algorithm.
	 * @param other the other object to compare against
	 */
	private boolean annotationEquals(Object other) {
		if (this == other) {
			return true;
		}
		if (!this.type.isInstance(other)) {
			return false;
		}
		for (int i = 0; i < this.attributes.size(); i++) {
			Method attribute = this.attributes.get(i);
			Object thisValue = getAttributeValue(attribute);
			Object otherValue = AnnotationUtils.invokeAnnotationMethod(attribute, other);
			if (!ObjectUtils.nullSafeEquals(thisValue, otherValue)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * See {@link Annotation#hashCode()} for a definition of the required algorithm.
	 */
	private int annotationHashCode() {
		Integer hashCode = this.hashCode;
		if (hashCode == null) {
			hashCode = computeHashCode();
			this.hashCode = hashCode;
		}
		return hashCode;
	}

	private Integer computeHashCode() {
		int hashCode = 0;
		for (int i = 0; i < this.attributes.size(); i++) {
			Method attribute = this.attributes.get(i);
			Object value = getAttributeValue(attribute);
			hashCode += (127 * attribute.getName().hashCode()) ^ getValueHashCode(value);
		}
		return hashCode;
	}

	private int getValueHashCode(Object value) {
		// Use Arrays.hashCode(...) since Spring's ObjectUtils doesn't comply
		// with the requirements specified in Annotation#hashCode().
		if (value instanceof boolean[] booleans) {
			return Arrays.hashCode(booleans);
		}
		if (value instanceof byte[] bytes) {
			return Arrays.hashCode(bytes);
		}
		if (value instanceof char[] chars) {
			return Arrays.hashCode(chars);
		}
		if (value instanceof double[] doubles) {
			return Arrays.hashCode(doubles);
		}
		if (value instanceof float[] floats) {
			return Arrays.hashCode(floats);
		}
		if (value instanceof int[] ints) {
			return Arrays.hashCode(ints);
		}
		if (value instanceof long[] longs) {
			return Arrays.hashCode(longs);
		}
		if (value instanceof short[] shorts) {
			return Arrays.hashCode(shorts);
		}
		if (value instanceof Object[] objects) {
			return Arrays.hashCode(objects);
		}
		return value.hashCode();
	}

	private String annotationToString() {
		String string = this.string;
		if (string == null) {
			StringBuilder builder = new StringBuilder("@").append(getName(this.type)).append('(');
			for (int i = 0; i < this.attributes.size(); i++) {
				Method attribute = this.attributes.get(i);
				if (i > 0) {
					builder.append(", ");
				}
				builder.append(attribute.getName());
				builder.append('=');
				builder.append(toString(getAttributeValue(attribute)));
			}
			builder.append(')');
			string = builder.toString();
			this.string = string;
		}
		return string;
	}

	/**
	 * This method currently does not address the following issues which we may
	 * choose to address at a later point in time.
	 *
	 * <ul>
	 * <li>non-ASCII, non-visible, and non-printable characters within a character
	 * or String literal are not escaped.</li>
	 * <li>formatting for float and double values does not take into account whether
	 * a value is not a number (NaN) or infinite.</li>
	 * </ul>
	 * @param value the attribute value to format
	 * @return the formatted string representation
	 */
	private String toString(Object value) {
		if (value instanceof String str) {
			return '"' + str + '"';
		}
		if (value instanceof Character) {
			return '\'' + value.toString() + '\'';
		}
		if (value instanceof Byte) {
			return String.format("(byte) 0x%02X", value);
		}
		if (value instanceof Long longValue) {
			return Long.toString(longValue) + 'L';
		}
		if (value instanceof Float floatValue) {
			return Float.toString(floatValue) + 'f';
		}
		if (value instanceof Double doubleValue) {
			return Double.toString(doubleValue) + 'd';
		}
		if (value instanceof Enum<?> e) {
			return e.name();
		}
		if (value instanceof Class<?> clazz) {
			return getName(clazz) + ".class";
		}
		if (value.getClass().isArray()) {
			StringBuilder builder = new StringBuilder("{");
			for (int i = 0; i < Array.getLength(value); i++) {
				if (i > 0) {
					builder.append(", ");
				}
				builder.append(toString(Array.get(value, i)));
			}
			builder.append('}');
			return builder.toString();
		}
		return String.valueOf(value);
	}

	private Object getAttributeValue(Method method) {
		Object value = this.valueCache.computeIfAbsent(method.getName(), attributeName -> {
			Class<?> type = ClassUtils.resolvePrimitiveIfNecessary(method.getReturnType());
			return this.annotation.getValue(attributeName, type).orElseThrow(
					() -> new NoSuchElementException("No value found for attribute named '" + attributeName +
							"' in merged annotation " + this.annotation.getType().getName()));
		});

		// Clone non-empty arrays so that users cannot alter the contents of values in our cache.
		if (value.getClass().isArray() && Array.getLength(value) > 0) {
			value = cloneArray(value);
		}

		return value;
	}

	/**
	 * Clone the provided array, ensuring that the original component type is retained.
	 * @param array the array to clone
	 */
	private Object cloneArray(Object array) {
		if (array instanceof boolean[] booleans) {
			return booleans.clone();
		}
		if (array instanceof byte[] bytes) {
			return bytes.clone();
		}
		if (array instanceof char[] chars) {
			return chars.clone();
		}
		if (array instanceof double[] doubles) {
			return doubles.clone();
		}
		if (array instanceof float[] floats) {
			return floats.clone();
		}
		if (array instanceof int[] ints) {
			return ints.clone();
		}
		if (array instanceof long[] longs) {
			return longs.clone();
		}
		if (array instanceof short[] shorts) {
			return shorts.clone();
		}

		// else
		return ((Object[]) array).clone();
	}

	@SuppressWarnings("unchecked")
	static <A extends Annotation> A createProxy(MergedAnnotation<A> annotation, Class<A> type) {
		ClassLoader classLoader = type.getClassLoader();
		Class<?>[] interfaces = new Class<?>[] {type};
		InvocationHandler handler = new SynthesizedMergedAnnotationInvocationHandler<>(annotation, type);
		return (A) Proxy.newProxyInstance(classLoader, interfaces, handler);
	}

	private static String getName(Class<?> clazz) {
		String canonicalName = clazz.getCanonicalName();
		return (canonicalName != null ? canonicalName : clazz.getName());
	}

}
