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
		FilteringClassLoader classLoader = new FilteringClassLoader(
				getClass().getClassLoader());
		Class<?> withExampleAnnotation = ClassUtils.forName(
				WithExampleAnnotation.class.getName(), classLoader);
		Annotation annotation = withExampleAnnotation.getAnnotations()[0];
		Method method = annotation.annotationType().getMethod("value");
		method.setAccessible(true);
		assertThatExceptionOfType(TypeNotPresentException.class).isThrownBy(() ->
				ReflectionUtils.invokeMethod(method, annotation))
			.withCauseInstanceOf(ClassNotFoundException.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void filteredTypeInMetaAnnotationWhenUsingAnnotatedElementUtilsHandlesException() throws Exception {
		FilteringClassLoader classLoader = new FilteringClassLoader(
				getClass().getClassLoader());
		Class<?> withExampleMetaAnnotation = ClassUtils.forName(
				WithExampleMetaAnnotation.class.getName(), classLoader);
		Class<Annotation> exampleAnnotationClass = (Class<Annotation>) ClassUtils.forName(
				ExampleAnnotation.class.getName(), classLoader);
		Class<Annotation> exampleMetaAnnotationClass = (Class<Annotation>) ClassUtils.forName(
				ExampleMetaAnnotation.class.getName(), classLoader);
		assertThat(AnnotatedElementUtils.getMergedAnnotationAttributes(
				withExampleMetaAnnotation, exampleAnnotationClass)).isNull();
		assertThat(AnnotatedElementUtils.getMergedAnnotationAttributes(
				withExampleMetaAnnotation, exampleMetaAnnotationClass)).isNull();
		assertThat(AnnotatedElementUtils.hasAnnotation(withExampleMetaAnnotation,
				exampleAnnotationClass)).isFalse();
		assertThat(AnnotatedElementUtils.hasAnnotation(withExampleMetaAnnotation,
				exampleMetaAnnotationClass)).isFalse();
	}

	@Test
	@SuppressWarnings("unchecked")
	void filteredTypeInMetaAnnotationWhenUsingMergedAnnotationsHandlesException() throws Exception {
		FilteringClassLoader classLoader = new FilteringClassLoader(
				getClass().getClassLoader());
		Class<?> withExampleMetaAnnotation = ClassUtils.forName(
				WithExampleMetaAnnotation.class.getName(), classLoader);
		Class<Annotation> exampleAnnotationClass = (Class<Annotation>) ClassUtils.forName(
				ExampleAnnotation.class.getName(), classLoader);
		Class<Annotation> exampleMetaAnnotationClass = (Class<Annotation>) ClassUtils.forName(
				ExampleMetaAnnotation.class.getName(), classLoader);
		MergedAnnotations annotations = MergedAnnotations.from(withExampleMetaAnnotation);
		assertThat(annotations.get(exampleAnnotationClass).isPresent()).isFalse();
		assertThat(annotations.get(exampleMetaAnnotationClass).isPresent()).isFalse();
		assertThat(annotations.isPresent(exampleMetaAnnotationClass)).isFalse();
		assertThat(annotations.isPresent(exampleAnnotationClass)).isFalse();
	}


	static class FilteringClassLoader extends OverridingClassLoader {

		FilteringClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected boolean isEligibleForOverriding(String className) {
			return className.startsWith(
					AnnotationIntrospectionFailureTests.class.getName());
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (name.startsWith(AnnotationIntrospectionFailureTests.class.getName()) &&
					name.contains("Filtered")) {
				throw new ClassNotFoundException(name);
			}
			return super.loadClass(name, resolve);
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
