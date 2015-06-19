/*
 * Copyright 2002-2015 the original author or authors.
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
import java.lang.reflect.Method;
import java.util.HashMap;
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
class MapAnnotationAttributeExtractor extends AbstractAliasAwareAnnotationAttributeExtractor {

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
		super(annotationType, annotatedElement, enrichAndValidateAttributes(new HashMap<String, Object>(attributes), annotationType));
	}

	@Override
	protected Object getRawAttributeValue(Method attributeMethod) {
		return getMap().get(attributeMethod.getName());
	}

	@Override
	protected Object getRawAttributeValue(String attributeName) {
		return getMap().get(attributeName);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getMap() {
		return (Map<String, Object>) getSource();
	}

	/**
	 * Enrich and validate the supplied {@code attributes} map by ensuring
	 * that it contains a non-null entry for each annotation attribute in
	 * the specified {@code annotationType} and that the type of the entry
	 * matches the return type for the corresponding annotation attribute.
	 * <p>If an attribute is missing in the supplied map, it will be set
	 * either to value of its alias (if an alias value exists) or to the
	 * value of the attribute's default value (if defined), and otherwise
	 * an {@link IllegalArgumentException} will be thrown.
	 * @see AliasFor
	 */
	private static Map<String, Object> enrichAndValidateAttributes(Map<String, Object> attributes,
			Class<? extends Annotation> annotationType) {

		Map<String, String> attributeAliasMap = getAttributeAliasMap(annotationType);

		for (Method attributeMethod : getAttributeMethods(annotationType)) {
			String attributeName = attributeMethod.getName();
			Object attributeValue = attributes.get(attributeName);


			// if attribute not present, check alias
			if (attributeValue == null) {
				String aliasName = attributeAliasMap.get(attributeName);
				if (aliasName != null) {
					Object aliasValue = attributes.get(aliasName);
					if (aliasValue != null) {
						attributeValue = aliasValue;
						attributes.put(attributeName, attributeValue);
					}
				}
			}

			// if alias not present, check default
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
					"Attributes map [%s] returned null for required attribute [%s] defined by annotation type [%s].",
					attributes, attributeName, annotationType.getName()));
			}

			// else, ensure correct type
			Class<?> returnType = attributeMethod.getReturnType();
			if (!ClassUtils.isAssignable(returnType, attributeValue.getClass())) {
				throw new IllegalArgumentException(String.format(
					"Attributes map [%s] returned a value of type [%s] for attribute [%s], "
							+ "but a value of type [%s] is required as defined by annotation type [%s].", attributes,
					attributeValue.getClass().getName(), attributeName, returnType.getName(), annotationType.getName()));
			}
		}

		return attributes;
	}

}
