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

package org.springframework.core.type.classreading;


import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationFilter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

abstract class ClassFileAnnotationMetadata {

	static MergedAnnotations createMergedAnnotations(String entryName, RuntimeVisibleAnnotationsAttribute annotationAttribute, @Nullable ClassLoader classLoader) {
		Set<MergedAnnotation<?>> annotations = new LinkedHashSet<>(4);
		annotationAttribute.annotations().forEach(ann -> {
			MergedAnnotation<java.lang.annotation.Annotation> mergedAnnotation = createMergedAnnotation(entryName, ann, classLoader);
			if (mergedAnnotation != null) {
				annotations.add(mergedAnnotation);
			}
		});
		return MergedAnnotations.of(annotations);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private static <A extends java.lang.annotation.Annotation> MergedAnnotation<A> createMergedAnnotation(String entryName, Annotation annotation, @Nullable ClassLoader classLoader) {
		String typeName = fromTypeDescriptor(annotation.className().stringValue());
		if (AnnotationFilter.PLAIN.matches(typeName)) {
			return null;
		}
		Map<String, Object> attributes = new LinkedHashMap<>(4);
		try {
			Class<A> annotationType = (Class<A>) ClassUtils.forName(typeName, classLoader);
			for (AnnotationElement element : annotation.elements()) {
				attributes.put(element.name().stringValue(), readAnnotationValue(element.value(), classLoader));
			}
			Map<String, Object> compactedAttributes = (attributes.isEmpty() ? Collections.emptyMap() : attributes);
			return MergedAnnotation.of(classLoader, new Source(entryName), annotationType, compactedAttributes);
		}
		catch (ClassNotFoundException | LinkageError ex) {
			return null;
		}
	}

	private static Object readAnnotationValue(AnnotationValue elementValue, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
		switch (elementValue) {
			case AnnotationValue.OfArray arrayValue -> {
				List<AnnotationValue> rawValues = arrayValue.values();
				List<Object> values = new ArrayList<>(rawValues.size());
				for (AnnotationValue arrayEntry : rawValues) {
					values.add(readAnnotationValue(arrayEntry, classLoader));
				}
				Class<?> elementType = getArrayElementType(values);
				return values.toArray((Object[]) Array.newInstance(elementType, rawValues.size()));
			}
			case AnnotationValue.OfAnnotation annotationValue -> {
				return annotationValue.annotation();
			}
			case AnnotationValue.OfClass classValue -> {
				return fromTypeDescriptor(classValue.className().stringValue());
			}
			case AnnotationValue.OfEnum enumValue -> {
				return parseEnum(enumValue, classLoader);
			}
			case AnnotationValue.OfConstant constantValue -> {
				return constantValue.resolvedValue();
			}
			default -> {
				return elementValue;
			}
		}
	}

	private static Class<?> getArrayElementType(List<Object> values) {
		if (values.isEmpty()) {
			return Object.class;
		}
		Object firstElement = values.getFirst();
		if (firstElement instanceof Enum<?> enumeration) {
			return enumeration.getDeclaringClass();
		}
		return firstElement.getClass();
	}

	private static String fromTypeDescriptor(String descriptor) {
		return descriptor.substring(1, descriptor.length() - 1)
				.replace('/', '.');
	}

	@SuppressWarnings("unchecked")
	private static <E extends Enum<E>> Enum<E> parseEnum(AnnotationValue.OfEnum enumValue, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
		String enumClassName = fromTypeDescriptor(enumValue.className().stringValue());
		Class<E> enumClass = (Class<E>) ClassUtils.forName(enumClassName, classLoader);
		return Enum.valueOf(enumClass, enumValue.constantName().stringValue());
	}

	record Source(String entryName) {

	}

}
