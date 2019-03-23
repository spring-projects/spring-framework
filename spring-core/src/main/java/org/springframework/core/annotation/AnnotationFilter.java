/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.Arrays;
import java.util.Collection;

import org.springframework.util.Assert;

/**
 * Callback interface that can be used to filter specific annotation types.
 *
 * @author Phillip Webb
 * @since 5.2
 */
@FunctionalInterface
public interface AnnotationFilter {

	/**
	 * {@link AnnotationFilter} that matches annotations is in the
	 * {@code java.lang.*} or in the
	 * {@code org.springframework.lang.*} package.
	 */
	AnnotationFilter PLAIN = packages("java.lang", "org.springframework.lang");

	/**
	 * {@link AnnotationFilter} that matches annotations in the
	 * {@code java.lang.*} package.
	 */
	AnnotationFilter JAVA = packages("java.lang");

	/**
	 * {@link AnnotationFilter} that never matches and can be used when no
	 * filtering is needed.
	 */
	AnnotationFilter NONE = new AnnotationFilter() {
		@Override
		public boolean matches(String typeName) {
			return false;
		}
		@Override
		public String toString() {
			return "No annotation filtering";
		}
	};


	/**
	 * Test if the given annotation matches the filter.
	 * @param annotation the annotation to test
	 * @return {@code true} if the annotation matches
	 */
	default boolean matches(Annotation annotation) {
		return matches(annotation.annotationType());
	}

	/**
	 * Test if the given type matches the filter.
	 * @param type the annotation type to test
	 * @return {@code true} if the annotation matches
	 */
	default boolean matches(Class<?> type) {
		return matches(type.getName());
	}

	/**
	 * Test if the given type name matches the filter.
	 * @param typeName the annotation type to test
	 * @return {@code true} if the annotation matches
	 */
	boolean matches(String typeName);


	/**
	 * Return a new {@link AnnotationFilter} that matches annotations in the
	 * specified packages.
	 * @param packages the annotation packages that should match
	 * @return a new {@link AnnotationFilter} instance
	 */
	static AnnotationFilter packages(String... packages) {
		return new PackagesAnnotationFilter(packages);
	}

	/**
	 * Return an {@link AnnotationFilter} that is the most appropriate for, and
	 * will always match the given annotation type. Whenever possible,
	 * {@link AnnotationFilter#PLAIN} will be returned.
	 * @param annotationType the annotation type to check
	 * @return the most appropriate annotation filter
	 */
	static AnnotationFilter mostAppropriateFor(Class<?> annotationType) {
		return (PLAIN.matches(annotationType) ? NONE : PLAIN);
	}

	/**
	 * Return an {@link AnnotationFilter} that is the most appropriate for, and
	 * will always match all the given annotation types. Whenever possible,
	 * {@link AnnotationFilter#PLAIN} will be returned.
	 * @param annotationTypes the annotation types to check
	 * @return the most appropriate annotation filter
	 */
	static AnnotationFilter mostAppropriateFor(Class<?>... annotationTypes) {
		return mostAppropriateFor(Arrays.asList(annotationTypes));
	}

	/**
	 * Return an {@link AnnotationFilter} that is the most appropriate for, and
	 * will always match all the given annotation types. Whenever possible,
	 * {@link AnnotationFilter#PLAIN} will be returned.
	 * @param annotationTypes the annotation types to check (may be class names
	 * or class types)
	 * @return the most appropriate annotation filter
	 */
	@SuppressWarnings("unchecked")
	static AnnotationFilter mostAppropriateFor(Collection<?> annotationTypes) {
		for (Object annotationType : annotationTypes) {
			if (annotationType == null) {
				continue;
			}
			Assert.isTrue(annotationType instanceof Class || annotationType instanceof String,
					"AnnotationType must be a Class or String");
			if (annotationType instanceof Class && PLAIN.matches((Class<Annotation>) annotationType)) {
				return NONE;
			}
			if (annotationType instanceof String && PLAIN.matches((String) annotationType)) {
				return NONE;
			}
		}
		return PLAIN;
	}

}
