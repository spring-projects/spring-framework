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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MergedAnnotation} to ensure the correct class loader is
 * used.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class MergedAnnotationClassLoaderTests {

	private static final String TEST_ANNOTATION = TestAnnotation.class.getName();

	private static final String TEST_META_ANNOTATION = TestMetaAnnotation.class.getName();

	private static final String WITH_TEST_ANNOTATION = WithTestAnnotation.class.getName();

	private static final String TEST_REFERENCE = TestReference.class.getName();

	@Test
	void synthesizedUsesCorrectClassLoader() throws Exception {
		ClassLoader parent = getClass().getClassLoader();
		TestClassLoader child = new TestClassLoader(parent);
		Class<?> source = child.loadClass(WITH_TEST_ANNOTATION);
		Annotation annotation = getDeclaredAnnotation(source, TEST_ANNOTATION);
		Annotation metaAnnotation = getDeclaredAnnotation(annotation.annotationType(),
				TEST_META_ANNOTATION);
		// We should have loaded the source and initial annotation from child
		assertThat(source.getClassLoader()).isEqualTo(child);
		assertThat(annotation.getClass().getClassLoader()).isEqualTo(child);
		assertThat(annotation.annotationType().getClassLoader()).isEqualTo(child);
		// The meta-annotation should have been loaded by the parent
		assertThat(metaAnnotation.getClass().getClassLoader()).isEqualTo(parent);
		assertThat(metaAnnotation.getClass().getClassLoader()).isEqualTo(parent);
		assertThat(
				getEnumAttribute(metaAnnotation).getClass().getClassLoader()).isEqualTo(
						parent);
		assertThat(getClassAttribute(metaAnnotation).getClassLoader()).isEqualTo(child);
		// MergedAnnotation should follow the same class loader logic
		MergedAnnotations mergedAnnotations = MergedAnnotations.from(source);
		Annotation synthesized = mergedAnnotations.get(TEST_ANNOTATION).synthesize();
		Annotation synthesizedMeta = mergedAnnotations.get(
				TEST_META_ANNOTATION).synthesize();
		assertThat(synthesized.getClass().getClassLoader()).isEqualTo(child);
		assertThat(synthesized.annotationType().getClassLoader()).isEqualTo(child);
		assertThat(synthesizedMeta.getClass().getClassLoader()).isEqualTo(parent);
		assertThat(synthesizedMeta.getClass().getClassLoader()).isEqualTo(parent);
		assertThat(getClassAttribute(synthesizedMeta).getClassLoader()).isEqualTo(child);
		assertThat(
				getEnumAttribute(synthesizedMeta).getClass().getClassLoader()).isEqualTo(
						parent);
		assertThat(synthesized).isEqualTo(annotation);
		assertThat(synthesizedMeta).isEqualTo(metaAnnotation);
		// Also check utils version
		Annotation utilsMeta = AnnotatedElementUtils.getMergedAnnotation(source,
				TestMetaAnnotation.class);
		assertThat(utilsMeta.getClass().getClassLoader()).isEqualTo(parent);
		assertThat(getClassAttribute(utilsMeta).getClassLoader()).isEqualTo(child);
		assertThat(getEnumAttribute(utilsMeta).getClass().getClassLoader()).isEqualTo(
				parent);
		assertThat(utilsMeta).isEqualTo(metaAnnotation);
	}

	private Class<?> getClassAttribute(Annotation annotation) throws Exception {
		return (Class<?>) getAttributeValue(annotation, "classValue");
	}

	private Enum<?> getEnumAttribute(Annotation annotation) throws Exception {
		return (Enum<?>) getAttributeValue(annotation, "enumValue");
	}

	private Object getAttributeValue(Annotation annotation, String name)
			throws Exception {
		Method classValueMethod = annotation.annotationType().getDeclaredMethod(name);
		classValueMethod.setAccessible(true);
		return classValueMethod.invoke(annotation);
	}

	private Annotation getDeclaredAnnotation(Class<?> element, String annotationType) {
		for (Annotation annotation : element.getDeclaredAnnotations()) {
			if (annotation.annotationType().getName().equals(annotationType)) {
				return annotation;
			}
		}
		return null;
	}

	private static class TestClassLoader extends OverridingClassLoader {

		public TestClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected boolean isEligibleForOverriding(String className) {
			return WITH_TEST_ANNOTATION.equals(className)
					|| TEST_ANNOTATION.equals(className)
					|| TEST_REFERENCE.equals(className);
		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface TestMetaAnnotation {

		@AliasFor("d")
		String c() default "";

		@AliasFor("c")
		String d() default "";

		Class<?> classValue();

		TestEnum enumValue();

	}

	@TestMetaAnnotation(classValue = TestReference.class, enumValue = TestEnum.TWO)
	@Retention(RetentionPolicy.RUNTIME)
	static @interface TestAnnotation {

		@AliasFor("b")
		String a() default "";

		@AliasFor("a")
		String b() default "";

	}

	@TestAnnotation
	static class WithTestAnnotation {

	}

	static class TestReference {

	}

	static enum TestEnum {

		ONE, TWO, THREE

	}
}
