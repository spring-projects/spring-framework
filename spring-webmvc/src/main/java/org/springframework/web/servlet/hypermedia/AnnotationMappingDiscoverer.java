/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.web.servlet.hypermedia;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.core.AnnotationAttribute;
import org.springframework.util.Assert;

/**
 * {@link MappingDiscoverer} implementation that inspects mappings from a particular
 * annotation.
 * 
 * @author Oliver Gierke
 */
class AnnotationMappingDiscoverer {

	private final AnnotationAttribute attribute;

	/**
	 * Creates an {@link AnnotationMappingDiscoverer} for the given annotation type. Will
	 * lookup the {@code value} attribute by default.
	 * 
	 * @param annotation must not be {@literal null}.
	 */
	public AnnotationMappingDiscoverer(Class<? extends Annotation> annotation) {
		this(new AnnotationAttribute(annotation));
	}

	/**
	 * Creates an {@link AnnotationMappingDiscoverer} for the given annotation type and
	 * attribute name.
	 * 
	 * @param annotation must not be {@literal null}.
	 * @param mappingAttributeName if {@literal null}, it defaults to {@code value}.
	 */
	public AnnotationMappingDiscoverer(AnnotationAttribute attribute) {

		Assert.notNull(attribute);
		this.attribute = attribute;
	}

	/**
	 * Returns the mapping associated with the given type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return the type-level mapping or {@literal null} in case none is present.
	 */
	public String getMapping(Class<?> type) {

		String[] mapping = getMappingFrom(attribute.findValueOn(type));

		if (mapping.length > 1) {
			throw new IllegalStateException(String.format(
					"Multiple class level mappings defined on class %s!", type.getName()));
		}

		return mapping.length == 0 ? null : mapping[0];
	}

	/**
	 * Returns the mapping associated with the given {@link Method}. This will include the
	 * type-level mapping.
	 * 
	 * @param method must not be {@literal null}.
	 * @return the method mapping including the type-level one or {@literal null} if
	 *         neither of them present.
	 */
	public String getMapping(Method method) {

		String[] mapping = getMappingFrom(attribute.findValueOn(method));

		if (mapping.length > 1) {
			throw new IllegalStateException(String.format(
					"Multiple method level mappings defined on method %s!",
					method.toString()));
		}

		String typeMapping = getMapping(method.getDeclaringClass());

		if (mapping == null || mapping.length == 0) {
			return typeMapping;
		}

		return typeMapping == null || "/".equals(typeMapping) ? mapping[0] : typeMapping
				+ mapping[0];
	}

	private String[] getMappingFrom(Object annotationValue) {

		if (annotationValue instanceof String) {
			return new String[] { (String) annotationValue };
		}
		else if (annotationValue instanceof String[]) {
			return (String[]) annotationValue;
		}
		else if (annotationValue == null) {
			return new String[0];
		}

		throw new IllegalStateException(
				String.format(
						"Unsupported type for the mapping attribute! Support String and String[] but got %s!",
						annotationValue.getClass()));
	}
}
