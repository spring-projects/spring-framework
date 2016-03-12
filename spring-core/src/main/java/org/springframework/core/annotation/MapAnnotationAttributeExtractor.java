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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.ClassUtils;

import static org.springframework.core.annotation.AnnotationUtils.*;

/**
 * Implementation of the {@link AnnotationAttributeExtractor} strategy that
 * is backed by a {@link Map}.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see Annotation
 * @see AliasFor
 * @see AbstractAliasAwareAnnotationAttributeExtractor
 * @see DefaultAnnotationAttributeExtractor
 * @see AnnotationUtils#synthesizeAnnotation(Map, Class, AnnotatedElement)
 */
class MapAnnotationAttributeExtractor extends AbstractAliasAwareAnnotationAttributeExtractor<Map<String, Object>> {

	/**
	 * Construct a new {@code MapAnnotationAttributeExtractor}.
	 * <p>The supplied map must contain a key-value pair for every attribute
	 * defined in the supplied {@code annotationType} that is not aliased or
	 * does not have a default value.
	 * @param attributes the map of annotation attributes; never {@code null}
	 * @param annotationType the type of annotation to synthesize; never {@code null}
	 * @param annotatedElement the element that is annotated with the annotation
	 * of the supplied type; may be {@code null} if unknown
	 */
	MapAnnotationAttributeExtractor(Map<String, Object> attributes, Class<? extends Annotation> annotationType,
			AnnotatedElement annotatedElement) {

		super(annotationType, annotatedElement, enrichAndValidateAttributes(attributes, annotationType));
	}


	@Override
	protected Object getRawAttributeValue(Method attributeMethod) {
		return getRawAttributeValue(attributeMethod.getName());
	}

	@Override
	protected Object getRawAttributeValue(String attributeName) {
		return getSource().get(attributeName);
	}


	/**
	 * Enrich and validate the supplied <em>attributes</em> map by ensuring
	 * that it contains a non-null entry for each annotation attribute in
	 * the specified {@code annotationType} and that the type of the entry
	 * matches the return type for the corresponding annotation attribute.
	 * <p>If an entry is a map (presumably of annotation attributes), an
	 * attempt will be made to synthesize an annotation from it. Similarly,
	 * if an entry is an array of maps, an attempt will be made to synthesize
	 * an array of annotations from those maps.
	 * <p>If an attribute is missing in the supplied map, it will be set
	 * either to the value of its alias (if an alias exists) or to the
	 * value of the attribute's default value (if defined), and otherwise
	 * an {@link IllegalArgumentException} will be thrown.
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> enrichAndValidateAttributes(
			Map<String, Object> originalAttributes, Class<? extends Annotation> annotationType) {

		Map<String, Object> attributes = new LinkedHashMap<String, Object>(originalAttributes);
		Map<String, List<String>> attributeAliasMap = getAttributeAliasMap(annotationType);

		for (Method attributeMethod : getAttributeMethods(annotationType)) {
			String attributeName = attributeMethod.getName();
			Object attributeValue = attributes.get(attributeName);

			// if attribute not present, check aliases
			if (attributeValue == null) {
				List<String> aliasNames = attributeAliasMap.get(attributeName);
				if (aliasNames != null) {
					for (String aliasName : aliasNames) {
						Object aliasValue = attributes.get(aliasName);
						if (aliasValue != null) {
							attributeValue = aliasValue;
							attributes.put(attributeName, attributeValue);
							break;
						}
					}
				}
			}

			// if aliases not present, check default
			if (attributeValue == null) {
				Object defaultValue = getDefaultValue(annotationType, attributeName);
				if (defaultValue != null) {
					attributeValue = defaultValue;
					attributes.put(attributeName, attributeValue);
				}
			}

			// if still null
			if (attributeValue == null) {
				throw new IllegalArgumentException(String.format(
						"Attributes map %s returned null for required attribute '%s' defined by annotation type [%s].",
						attributes, attributeName, annotationType.getName()));
			}

			// finally, ensure correct type
			Class<?> requiredReturnType = attributeMethod.getReturnType();
			Class<? extends Object> actualReturnType = attributeValue.getClass();

			if (!ClassUtils.isAssignable(requiredReturnType, actualReturnType)) {
				boolean converted = false;

				// Single element overriding an array of the same type?
				if (requiredReturnType.isArray() && requiredReturnType.getComponentType() == actualReturnType) {
					Object array = Array.newInstance(requiredReturnType.getComponentType(), 1);
					Array.set(array, 0, attributeValue);
					attributes.put(attributeName, array);
					converted = true;
				}

				// Nested map representing a single annotation?
				else if (Annotation.class.isAssignableFrom(requiredReturnType) &&
						Map.class.isAssignableFrom(actualReturnType)) {
					Class<? extends Annotation> nestedAnnotationType =
							(Class<? extends Annotation>) requiredReturnType;
					Map<String, Object> map = (Map<String, Object>) attributeValue;
					attributes.put(attributeName, synthesizeAnnotation(map, nestedAnnotationType, null));
					converted = true;
				}

				// Nested array of maps representing an array of annotations?
				else if (requiredReturnType.isArray() && actualReturnType.isArray() &&
						Annotation.class.isAssignableFrom(requiredReturnType.getComponentType()) &&
						Map.class.isAssignableFrom(actualReturnType.getComponentType())) {
					Class<? extends Annotation> nestedAnnotationType =
							(Class<? extends Annotation>) requiredReturnType.getComponentType();
					Map<String, Object>[] maps = (Map<String, Object>[]) attributeValue;
					attributes.put(attributeName, synthesizeAnnotationArray(maps, nestedAnnotationType));
					converted = true;
				}

				if (!converted) {
					throw new IllegalArgumentException(String.format(
							"Attributes map %s returned a value of type [%s] for attribute '%s', " +
							"but a value of type [%s] is required as defined by annotation type [%s].",
							attributes, actualReturnType.getName(), attributeName, requiredReturnType.getName(),
							annotationType.getName()));
				}
			}
		}

		return attributes;
	}

}
