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

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

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

	/**
	 * TODO Document findAnnotationDescriptor().
	 *
	 * @param clazz the class to look for annotations on
	 * @param annotationType the annotation class to look for, both locally and
	 * as a meta-annotation
	 * @return the corresponding annotation descriptor if the annotation was found;
	 * otherwise {@code null}
	 */
	public static <T extends Annotation> AnnotationDescriptor<T> findAnnotationDescriptor(Class<?> clazz,
			Class<T> annotationType) {

		Assert.notNull(annotationType, "Annotation type must not be null");

		if (clazz == null || clazz.equals(Object.class)) {
			return null;
		}

		// Declared locally?
		if (isAnnotationDeclaredLocally(annotationType, clazz)) {
			return new AnnotationDescriptor<T>(clazz, clazz.getAnnotation(annotationType));
		}

		// Declared on a stereotype annotation (i.e., as a meta-annotation)?
		if (!Annotation.class.isAssignableFrom(clazz)) {
			for (Annotation stereotype : clazz.getAnnotations()) {
				T annotation = stereotype.annotationType().getAnnotation(annotationType);
				if (annotation != null) {
					return new AnnotationDescriptor<T>(clazz, stereotype, annotation);
				}
			}
		}

		// Declared on a superclass?
		return findAnnotationDescriptor(clazz.getSuperclass(), annotationType);
	}

	/**
	 * TODO Document findAnnotationDescriptorForTypes().
	 *
	 * @param clazz the class to look for annotations on
	 * @param annotationTypes the types of annotations to look for, both locally
	 * and as meta-annotations
	 * @return the corresponding annotation descriptor if one of the annotations
	 * was found; otherwise {@code null}
	 */
	@SuppressWarnings("unchecked")
	public static UntypedAnnotationDescriptor findAnnotationDescriptorForTypes(Class<?> clazz,
			Class<? extends Annotation>... annotationTypes) {

		assertNonEmptyAnnotationTypeArray(annotationTypes, "The list of annotation types must not be empty");

		if (clazz == null || clazz.equals(Object.class)) {
			return null;
		}

		// Declared locally?
		for (Class<? extends Annotation> annotationType : annotationTypes) {
			if (isAnnotationDeclaredLocally(annotationType, clazz)) {
				return new UntypedAnnotationDescriptor(clazz, clazz.getAnnotation(annotationType));
			}
		}

		// Declared on a stereotype annotation (i.e., as a meta-annotation)?
		if (!Annotation.class.isAssignableFrom(clazz)) {
			for (Annotation stereotype : clazz.getAnnotations()) {
				for (Class<? extends Annotation> annotationType : annotationTypes) {
					Annotation annotation = stereotype.annotationType().getAnnotation(annotationType);
					if (annotation != null) {
						return new UntypedAnnotationDescriptor(clazz, stereotype, annotation);
					}
				}
			}
		}

		// Declared on a superclass?
		return findAnnotationDescriptorForTypes(clazz.getSuperclass(), annotationTypes);
	}


	/**
	 * Descriptor for an {@link Annotation}, including the {@linkplain
	 * #getDeclaringClass() class} on which the annotation is <em>declared</em>
	 * as well as the actual {@linkplain #getAnnotation() annotation} instance.
	 *
	 * <p>
	 * If the annotation is used as a meta-annotation, the descriptor also includes
	 * the {@linkplain #getStereotype() stereotype} on which the annotation is
	 * present. In such cases, the <em>root declaring class</em> is not directly
	 * annotated with the annotation but rather indirectly via the stereotype.
	 *
	 * <p>
	 * Given the following example, if we are searching for the {@code @Transactional}
	 * annotation <em>on</em> the {@code TransactionalTests} class, then the
	 * properties of the {@code AnnotationDescriptor} would be as follows.
	 *
	 * <ul>
	 * <li>rootDeclaringClass: {@code TransactionalTests} class object</li>
	 * <li>declaringClass: {@code TransactionalTests} class object</li>
	 * <li>stereotype: {@code null}</li>
	 * <li>annotation: instance of the {@code Transactional} annotation</li>
	 * </ul>
	 *
	 * <pre style="code">
	 * &#064;Transactional
	 * &#064;ContextConfiguration({"/test-datasource.xml", "/repository-config.xml"})
	 * public class TransactionalTests { }
	 * </pre>
	 *
	 * <p>
	 * Given the following example, if we are searching for the {@code @Transactional}
	 * annotation <em>on</em> the {@code UserRepositoryTests} class, then the
	 * properties of the {@code AnnotationDescriptor} would be as follows.
	 *
	 * <ul>
	 * <li>rootDeclaringClass: {@code UserRepositoryTests} class object</li>
	 * <li>declaringClass: {@code RepositoryTests} class object</li>
	 * <li>stereotype: instance of the {@code RepositoryTests} annotation</li>
	 * <li>annotation: instance of the {@code Transactional} annotation</li>
	 * </ul>
	 *
	 * <pre style="code">
	 * &#064;Transactional
	 * &#064;ContextConfiguration({"/test-datasource.xml", "/repository-config.xml"})
	 * &#064;Retention(RetentionPolicy.RUNTIME)
	 * public &#064;interface RepositoryTests { }
	 *
	 * &#064;RepositoryTests
	 * public class UserRepositoryTests { }
	 * </pre>
	 *
	 * @author Sam Brannen
	 * @since 4.0
	 */
	public static class AnnotationDescriptor<T extends Annotation> {

		private final Class<?> rootDeclaringClass;
		private final Class<?> declaringClass;
		private final Annotation stereotype;
		private final T annotation;
		private final AnnotationAttributes annotationAttributes;


		public AnnotationDescriptor(Class<?> rootDeclaringClass, T annotation) {
			this(rootDeclaringClass, null, annotation);
		}

		public AnnotationDescriptor(Class<?> rootDeclaringClass, Annotation stereotype, T annotation) {
			Assert.notNull(rootDeclaringClass, "rootDeclaringClass must not be null");
			Assert.notNull(annotation, "annotation must not be null");

			this.rootDeclaringClass = rootDeclaringClass;
			this.declaringClass = (stereotype != null) ? stereotype.annotationType() : rootDeclaringClass;
			this.stereotype = stereotype;
			this.annotation = annotation;
			this.annotationAttributes = AnnotatedElementUtils.getAnnotationAttributes(rootDeclaringClass,
				annotation.annotationType().getName());
		}

		public Class<?> getRootDeclaringClass() {
			return this.rootDeclaringClass;
		}

		public Class<?> getDeclaringClass() {
			return this.declaringClass;
		}

		public T getAnnotation() {
			return this.annotation;
		}

		public Class<? extends Annotation> getAnnotationType() {
			return this.annotation.annotationType();
		}

		public AnnotationAttributes getAnnotationAttributes() {
			return this.annotationAttributes;
		}

		public Annotation getStereotype() {
			return this.stereotype;
		}

		public Class<? extends Annotation> getStereotypeType() {
			return this.stereotype == null ? null : this.stereotype.annotationType();
		}

		/**
		 * Provide a textual representation of this {@code AnnotationDescriptor}.
		 */
		@Override
		public String toString() {
			return new ToStringCreator(this)//
			.append("rootDeclaringClass", rootDeclaringClass)//
			.append("declaringClass", declaringClass)//
			.append("stereotype", stereotype)//
			.append("annotation", annotation)//
			.toString();
		}
	}

	public static class UntypedAnnotationDescriptor extends AnnotationDescriptor<Annotation> {

		public UntypedAnnotationDescriptor(Class<?> declaringClass, Annotation annotation) {
			super(declaringClass, annotation);
		}

		public UntypedAnnotationDescriptor(Class<?> declaringClass, Annotation stereotype, Annotation annotation) {
			super(declaringClass, stereotype, annotation);
		}
	}


	private static void assertNonEmptyAnnotationTypeArray(Class<?>[] annotationTypes, String message) {
		if (ObjectUtils.isEmpty(annotationTypes)) {
			throw new IllegalArgumentException(message);
		}

		for (Class<?> clazz : annotationTypes) {
			if (!Annotation.class.isAssignableFrom(clazz)) {
				throw new IllegalArgumentException("Array elements must be of type Annotation");
			}
		}
	}

}
