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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.core.OverridingClassLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests that trigger annotation introspection failures and ensure that they are
 * dealt with correctly.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see AnnotationUtils
 * @see AnnotatedElementUtils
 */
class AnnotationIntrospectionFailureTests {

	@Test
	void filteredTypeThrowsTypeNotPresentException() throws Exception {
		FilteringClassLoader classLoader = new FilteringClassLoader(getClass().getClassLoader());
		Class<?> withAnnotation = ClassUtils.forName(WithExampleAnnotation.class.getName(), classLoader);
		Annotation annotation = withAnnotation.getAnnotations()[0];
		Method method = annotation.annotationType().getMethod("value");
		method.setAccessible(true);
		assertThatExceptionOfType(TypeNotPresentException.class)
				.isThrownBy(() -> ReflectionUtils.invokeMethod(method, annotation))
				.withCauseInstanceOf(ClassNotFoundException.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void filteredTypeInMetaAnnotationWhenUsingAnnotatedElementUtilsHandlesException() throws Exception {
		FilteringClassLoader classLoader = new FilteringClassLoader(getClass().getClassLoader());
		Class<?> withAnnotation = ClassUtils.forName(WithExampleMetaAnnotation.class.getName(), classLoader);
		Class<Annotation> annotationClass = (Class<Annotation>)
				ClassUtils.forName(ExampleAnnotation.class.getName(), classLoader);
		Class<Annotation> metaAnnotationClass = (Class<Annotation>)
				ClassUtils.forName(ExampleMetaAnnotation.class.getName(), classLoader);
		assertThat(AnnotatedElementUtils.getMergedAnnotationAttributes(withAnnotation, annotationClass)).isNull();
		assertThat(AnnotatedElementUtils.getMergedAnnotationAttributes(withAnnotation, metaAnnotationClass)).isNull();
		assertThat(AnnotatedElementUtils.hasAnnotation(withAnnotation, annotationClass)).isFalse();
		assertThat(AnnotatedElementUtils.hasAnnotation(withAnnotation, metaAnnotationClass)).isFalse();
	}

	@Test
	@SuppressWarnings("unchecked")
	void filteredTypeInMetaAnnotationWhenUsingMergedAnnotationsHandlesException() throws Exception {
		FilteringClassLoader classLoader = new FilteringClassLoader(getClass().getClassLoader());
		Class<?> withAnnotation = ClassUtils.forName(WithExampleMetaAnnotation.class.getName(), classLoader);
		Class<Annotation> annotationClass = (Class<Annotation>)
				ClassUtils.forName(ExampleAnnotation.class.getName(), classLoader);
		Class<Annotation> metaAnnotationClass = (Class<Annotation>)
				ClassUtils.forName(ExampleMetaAnnotation.class.getName(), classLoader);
		MergedAnnotations annotations = MergedAnnotations.from(withAnnotation);
		assertThat(annotations.get(annotationClass).isPresent()).isFalse();
		assertThat(annotations.get(metaAnnotationClass).isPresent()).isFalse();
		assertThat(annotations.isPresent(metaAnnotationClass)).isFalse();
		assertThat(annotations.isPresent(annotationClass)).isFalse();
	}


	static class FilteringClassLoader extends OverridingClassLoader {

		FilteringClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected boolean isEligibleForOverriding(String className) {
			return className.startsWith(AnnotationIntrospectionFailureTests.class.getName()) ||
					className.startsWith("jdk.internal");
		}

		@Override
		protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
			if (name.contains("Filtered") || name.startsWith("jdk.internal")) {
				throw new ClassNotFoundException(name);
			}
			return super.loadClassForOverriding(name);
		}
	}

	static class FilteredType {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ExampleAnnotation {

		Class<?> value() default Void.class;
	}

	@ExampleAnnotation(FilteredType.class)
	static class WithExampleAnnotation {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@ExampleAnnotation
	@interface ExampleMetaAnnotation {

		@AliasFor(annotation = ExampleAnnotation.class, attribute = "value")
		Class<?> example1() default Void.class;

		@AliasFor(annotation = ExampleAnnotation.class, attribute = "value")
		Class<?> example2() default Void.class;

	}

	@ExampleMetaAnnotation(example1 = FilteredType.class)
	static class WithExampleMetaAnnotation {
	}

}
