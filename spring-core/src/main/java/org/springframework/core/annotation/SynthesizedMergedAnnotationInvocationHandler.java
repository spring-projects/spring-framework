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
			return this.annotation.toString();
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
			Object otherValue = ReflectionUtils.invokeMethod(attribute, other);
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
		if (value instanceof boolean[]) {
			return Arrays.hashCode((boolean[]) value);
		}
		if (value instanceof byte[]) {
			return Arrays.hashCode((byte[]) value);
		}
		if (value instanceof char[]) {
			return Arrays.hashCode((char[]) value);
		}
		if (value instanceof double[]) {
			return Arrays.hashCode((double[]) value);
		}
		if (value instanceof float[]) {
			return Arrays.hashCode((float[]) value);
		}
		if (value instanceof int[]) {
			return Arrays.hashCode((int[]) value);
		}
		if (value instanceof long[]) {
			return Arrays.hashCode((long[]) value);
		}
		if (value instanceof short[]) {
			return Arrays.hashCode((short[]) value);
		}
		if (value instanceof Object[]) {
			return Arrays.hashCode((Object[]) value);
		}
		return value.hashCode();
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
	 * Clone the provided array, ensuring that original component type is retained.
	 * @param array the array to clone
	 */
	private Object cloneArray(Object array) {
		if (array instanceof boolean[]) {
			return ((boolean[]) array).clone();
		}
		if (array instanceof byte[]) {
			return ((byte[]) array).clone();
		}
		if (array instanceof char[]) {
			return ((char[]) array).clone();
		}
		if (array instanceof double[]) {
			return ((double[]) array).clone();
		}
		if (array instanceof float[]) {
			return ((float[]) array).clone();
		}
		if (array instanceof int[]) {
			return ((int[]) array).clone();
		}
		if (array instanceof long[]) {
			return ((long[]) array).clone();
		}
		if (array instanceof short[]) {
			return ((short[]) array).clone();
		}

		// else
		return ((Object[]) array).clone();
	}

	@SuppressWarnings("unchecked")
	static <A extends Annotation> A createProxy(MergedAnnotation<A> annotation, Class<A> type) {
		ClassLoader classLoader = type.getClassLoader();
		InvocationHandler handler = new SynthesizedMergedAnnotationInvocationHandler<>(annotation, type);
		Class<?>[] interfaces = isVisible(classLoader, SynthesizedAnnotation.class) ?
				new Class<?>[] {type, SynthesizedAnnotation.class} : new Class<?>[] {type};
		return (A) Proxy.newProxyInstance(classLoader, interfaces, handler);
	}


	private static boolean isVisible(ClassLoader classLoader, Class<?> interfaceClass) {
		if (classLoader == interfaceClass.getClassLoader()) {
			return true;
		}
		try {
			return Class.forName(interfaceClass.getName(), false, classLoader) == interfaceClass;
		}
		catch (ClassNotFoundException ex) {
			return false;
		}
	}

}
