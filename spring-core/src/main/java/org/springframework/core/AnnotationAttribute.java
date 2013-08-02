/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * Simply helper to reference a dedicated attribute of an {@link Annotation}.
 * 
 * @author Oliver Gierke
 */
public class AnnotationAttribute {

	private final Class<? extends Annotation> annotationType;

	private final String attributeName;

	/**
	 * Creates a new {@link AnnotationAttribute} to the {@code value} attribute of the
	 * given {@link Annotation} type.
	 * 
	 * @param annotationType must not be {@literal null}.
	 */
	public AnnotationAttribute(Class<? extends Annotation> annotationType) {
		this(annotationType, null);
	}

	/**
	 * Creates a new {@link AnnotationAttribute} for the given {@link Annotation} type and
	 * annotation attribute name.
	 * 
	 * @param annotationType must not be {@literal null}.
	 * @param attributeName can be {@literal null}, defaults to {@code value}.
	 */
	public AnnotationAttribute(Class<? extends Annotation> annotationType,
			String attributeName) {

		Assert.notNull(annotationType);

		this.annotationType = annotationType;
		this.attributeName = attributeName;
	}

	/**
	 * Returns the annotation type.
	 * 
	 * @return the annotationType
	 */
	public Class<? extends Annotation> getAnnotationType() {
		return annotationType;
	}

	/**
	 * Reads the {@link Annotation} attribute's value from the given
	 * {@link MethodParameter}.
	 * 
	 * @param parameter must not be {@literal null}.
	 * @return
	 */
	public Object getValueFrom(MethodParameter parameter) {

		Assert.notNull(parameter, "MethodParameter must not be null!");
		Annotation annotation = parameter.getParameterAnnotation(annotationType);
		return annotation == null ? null : getValueFrom(annotation);
	}

	/**
	 * Reads the {@link Annotation} attribute's value from the given type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public Object findValueOn(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");
		Annotation annotation = AnnotationUtils.findAnnotation(type, annotationType);
		return annotation == null ? null : getValueFrom(annotation);
	}

	public Object findValueOn(Method method) {

		Assert.notNull(method, "Method must nor be null!");
		Annotation annotation = AnnotationUtils.findAnnotation(method, annotationType);
		return annotation == null ? null : getValueFrom(annotation);
	}

	public Object getValueFrom(AnnotatedElement element) {

		Assert.notNull(element, "Annotated element must not be null!");
		Annotation annotation = AnnotationUtils.getAnnotation(element, annotationType);
		return annotation == null ? null : getValueFrom(annotation);
	}

	/**
	 * Returns the {@link Annotation} attribute's value from the given {@link Annotation}.
	 * 
	 * @param annotation must not be {@literal null}.
	 * @return
	 */
	public Object getValueFrom(Annotation annotation) {

		Assert.notNull(annotation, "Annotation must not be null!");
		return attributeName == null ? AnnotationUtils.getValue(annotation)
				: AnnotationUtils.getValue(annotation, attributeName);
	}
}
