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
	 * <p>The supplied map must contain key-value pairs for every attribute
	 * defined in the supplied {@code annotationType}.
	 * @param attributes the map of annotation attributes; never {@code null}
	 * @param annotationType the type of annotation to synthesize; never {@code null}
	 * @param annotatedElement the element that is annotated with the annotation
	 * of the supplied type; may be {@code null} if unknown
	 */
	MapAnnotationAttributeExtractor(Map<String, Object> attributes, Class<? extends Annotation> annotationType,
			AnnotatedElement annotatedElement) {
		super(annotationType, annotatedElement, new HashMap<String, Object>(attributes));
		validateAttributes(attributes, annotationType);
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
	 * Validate the supplied {@code attributes} map by verifying that it
	 * contains a non-null entry for each annotation attribute in the specified
	 * {@code annotationType} and that the type of the entry matches the
	 * return type for the corresponding annotation attribute.
	 */
	private static void validateAttributes(Map<String, Object> attributes, Class<? extends Annotation> annotationType) {
		for (Method attributeMethod : getAttributeMethods(annotationType)) {
			String attributeName = attributeMethod.getName();

			Object attributeValue = attributes.get(attributeName);
			if (attributeValue == null) {
				throw new IllegalArgumentException(String.format(
					"Attributes map [%s] returned null for required attribute [%s] defined by annotation type [%s].",
					attributes, attributeName, annotationType.getName()));
			}

			Class<?> returnType = attributeMethod.getReturnType();
			if (!ClassUtils.isAssignable(returnType, attributeValue.getClass())) {
				throw new IllegalArgumentException(String.format(
					"Attributes map [%s] returned a value of type [%s] for attribute [%s], "
							+ "but a value of type [%s] is required as defined by annotation type [%s].", attributes,
					attributeValue.getClass().getName(), attributeName, returnType.getName(), annotationType.getName()));
			}
		}
	}

}
