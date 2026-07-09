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

package org.springframework.core.type.classreading;

import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.AnnotationFilter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ClassUtils;

/**
 * Parse {@link RuntimeVisibleAnnotationsAttribute} into {@link MergedAnnotations}
 * instances.
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 7.0
 */
abstract class ClassFileAnnotationDelegate {

	static MergedAnnotations createMergedAnnotations(
			String className, RuntimeVisibleAnnotationsAttribute annotationAttribute, @Nullable ClassLoader classLoader) {

		List<MergedAnnotation<?>> annotations = annotationAttribute.annotations()
				.stream()
				.map(ann -> createMergedAnnotation(className, ann, classLoader))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		return MergedAnnotations.of(annotations);
	}

	@SuppressWarnings("unchecked")
	private static <A extends java.lang.annotation.Annotation> @Nullable MergedAnnotation<A> createMergedAnnotation(
			String className, Annotation annotation, @Nullable ClassLoader classLoader) {

		String typeName = ClassFileAnnotationMetadata.resolveTypeName(annotation.classSymbol());
		if (AnnotationFilter.PLAIN.matches(typeName)) {
			return null;
		}
		try {
			// Fail early when annotation type is not loadable (before resolving annotation values)
			Class<A> annotationType = (Class<A>) ClassUtils.forName(typeName, classLoader);
			Map<String, Object> attributes = new LinkedHashMap<>(4);
			for (AnnotationElement element : annotation.elements()) {
				Object annotationValue = readAnnotationValue(className, element.value(), classLoader);
				if (annotationValue != null) {
					attributes.put(element.name().stringValue(), annotationValue);
				}
			}
			Map<String, Object> compactedAttributes = (attributes.isEmpty() ? Collections.emptyMap() : attributes);
			return MergedAnnotation.of(classLoader, new Source(annotation), annotationType, compactedAttributes);
		}
		catch (ClassNotFoundException | LinkageError ex) {
			// Non-loadable annotation type -> ignore.
			return null;
		}
	}

	private static @Nullable Object readAnnotationValue(
			String className, AnnotationValue elementValue, @Nullable ClassLoader classLoader) {

		switch (elementValue) {
			case AnnotationValue.OfConstant constantValue -> {
				return constantValue.resolvedValue();
			}
			case AnnotationValue.OfAnnotation annotationValue -> {
				return createMergedAnnotation(className, annotationValue.annotation(), classLoader);
			}
			case AnnotationValue.OfClass classValue -> {
				return ClassFileAnnotationMetadata.resolveTypeName(classValue.classSymbol());
			}
			case AnnotationValue.OfEnum enumValue -> {
				return parseEnum(enumValue, classLoader);
			}
			case AnnotationValue.OfArray arrayValue -> {
				return parseArrayValue(className, classLoader, arrayValue);
			}
		}
	}

	private static Object parseArrayValue(String className, @Nullable ClassLoader classLoader, AnnotationValue.OfArray arrayValue) {
		if (arrayValue.values().isEmpty()) {
			return new Object[0];
		}
		Stream<AnnotationValue> stream = arrayValue.values().stream();
		switch (arrayValue.values().getFirst()) {
			case AnnotationValue.OfInt _ -> {
				return stream.map(AnnotationValue.OfInt.class::cast).mapToInt(AnnotationValue.OfInt::intValue).toArray();
			}
			case AnnotationValue.OfDouble _ -> {
				return stream.map(AnnotationValue.OfDouble.class::cast).mapToDouble(AnnotationValue.OfDouble::doubleValue).toArray();
			}
			case AnnotationValue.OfLong _ -> {
				return stream.map(AnnotationValue.OfLong.class::cast).mapToLong(AnnotationValue.OfLong::longValue).toArray();
			}
			default -> {
				Class<?> arrayElementType = resolveArrayElementType(arrayValue.values(), classLoader);
				return stream
						.map(rawValue -> readAnnotationValue(className, rawValue, classLoader))
						.toArray(length -> (Object[]) Array.newInstance(arrayElementType, length));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <E extends Enum<E>> Enum<E> parseEnum(AnnotationValue.OfEnum enumValue, @Nullable ClassLoader classLoader) {
		Class<E> enumClass = (Class<E>) loadEnumClass(enumValue, classLoader);
		return Enum.valueOf(enumClass, enumValue.constantName().stringValue());
	}

	private static Class<?> loadEnumClass(AnnotationValue.OfEnum enumValue, @Nullable ClassLoader classLoader) {
		String className = ClassFileAnnotationMetadata.resolveTypeName(enumValue.classSymbol());
		try {
			return ClassUtils.forName(className, classLoader);
		}
		catch (ClassNotFoundException | LinkageError ex) {
			throw new TypeNotPresentException(className, ex);
		}
	}

	private static Class<?> resolveArrayElementType(List<AnnotationValue> values, @Nullable ClassLoader classLoader) {
		AnnotationValue firstValue = values.getFirst();
		switch (firstValue) {
			case AnnotationValue.OfConstant constantValue -> {
				return constantValue.resolvedValue().getClass();
			}
			case AnnotationValue.OfAnnotation _ -> {
				return MergedAnnotation.class;
			}
			case AnnotationValue.OfClass _ -> {
				return String.class;
			}
			case AnnotationValue.OfEnum enumValue -> {
				return loadEnumClass(enumValue, classLoader);
			}
			default -> {
				return Object.class;
			}
		}
	}


	record Source(Annotation annotation) {
	}

}
