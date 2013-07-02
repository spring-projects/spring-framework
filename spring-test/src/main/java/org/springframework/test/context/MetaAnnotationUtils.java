/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context;

import java.lang.annotation.Annotation;

import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

import static org.springframework.core.annotation.AnnotationUtils.*;

/**
 * TODO Document MetaAnnotationUtils.
 * 
 * @author Sam Brannen
 * @since 4.0
 */
abstract class MetaAnnotationUtils {

	private MetaAnnotationUtils() {
		/* no-op */
	}

	// TODO Rename and document findAnnotationDescriptor().
	// Note: does not traverse interface hierarchies.
	public static <T extends Annotation> AnnotationDescriptor<T> findAnnotationDescriptor(Class<T> annotationType,
			Class<?> clazz) {

		Assert.notNull(annotationType, "Annotation type must not be null");
		if (clazz == null || clazz.equals(Object.class)) {
			return null;
		}

		// Declared locally?
		if (isAnnotationDeclaredLocally(annotationType, clazz)) {
			return new AnnotationDescriptor<T>(clazz.getAnnotation(annotationType), clazz);
		}

		// Declared on an annotation (i.e., as a meta-annotation)?
		if (!Annotation.class.isAssignableFrom(clazz)) {
			for (Annotation ann : clazz.getAnnotations()) {
				T annotation = ann.annotationType().getAnnotation(annotationType);
				if (annotation != null) {
					return new AnnotationDescriptor<T>(annotation, clazz, ann.annotationType());
				}
			}
		}

		// Declared on a superclass?
		return findAnnotationDescriptor(annotationType, clazz.getSuperclass());
	}


	/**
	 * Descriptor for an {@link Annotation}, including the
	 * {@linkplain #getAnnotatedClass() class} on which the annotation is declared as well
	 * as the {@linkplain #getAnnotation() annotation} itself.
	 * 
	 * <p>
	 * If the annotation is used as a meta-annotation, the descriptor also includes the
	 * {@linkplain #getMetaAnnotatedClass() meta-annotated class} (i.e., an annotation
	 * that is present on the {@linkplain #getAnnotatedClass() annotated class} and is
	 * itself annotated with the {@linkplain #getAnnotation() annotation} that this object
	 * describes).
	 * 
	 * @author Sam Brannen
	 * @since 4.0
	 */
	public static class AnnotationDescriptor<T extends Annotation> {

		private final T annotation;

		private final Class<?> annotatedClass;

		private final Class<?> metaAnnotatedClass;


		public AnnotationDescriptor(T annotation, Class<?> annotatedClass) {
			this(annotation, annotatedClass, null);
		}

		public AnnotationDescriptor(T annotation, Class<?> annotatedClass, Class<?> metaAnnotatedClass) {
			Assert.notNull(annotation, "annotation must not be null");
			Assert.notNull(annotatedClass, "annotatedClass must not be null");
			this.annotation = annotation;
			this.annotatedClass = annotatedClass;
			this.metaAnnotatedClass = metaAnnotatedClass;
		}

		public T getAnnotation() {
			return this.annotation;
		}

		public Class<? extends Annotation> getAnnotationType() {
			return this.annotation.annotationType();
		}

		public Class<?> getAnnotatedClass() {
			return this.annotatedClass;
		}

		public Class<?> getMetaAnnotatedClass() {
			return this.metaAnnotatedClass;
		}

		/**
		 * Provide a String representation of this {@code AnnotationDescriptor}.
		 */
		@Override
		public String toString() {
			return new ToStringCreator(this)//
			.append("annotation", annotation)//
			.append("annotatedClass", annotatedClass)//
			.append("metaAnnotatedClass", metaAnnotatedClass)//
			.toString();
		}
	}

}
