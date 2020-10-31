/*
 * Copyright 2002-2020 the original author or authors.
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotationFilter}.
 *
 * @author Phillip Webb
 */
class AnnotationFilterTests {

	private static final AnnotationFilter FILTER = annotationType ->
			ObjectUtils.nullSafeEquals(annotationType, TestAnnotation.class.getName());


	@Test
	void matchesAnnotationWhenMatchReturnsTrue() {
		TestAnnotation annotation = WithTestAnnotation.class.getDeclaredAnnotation(TestAnnotation.class);
		assertThat(FILTER.matches(annotation)).isTrue();
	}

	@Test
	void matchesAnnotationWhenNoMatchReturnsFalse() {
		OtherAnnotation annotation = WithOtherAnnotation.class.getDeclaredAnnotation(OtherAnnotation.class);
		assertThat(FILTER.matches(annotation)).isFalse();
	}

	@Test
	void matchesAnnotationClassWhenMatchReturnsTrue() {
		Class<TestAnnotation> annotationType = TestAnnotation.class;
		assertThat(FILTER.matches(annotationType)).isTrue();
	}

	@Test
	void matchesAnnotationClassWhenNoMatchReturnsFalse() {
		Class<OtherAnnotation> annotationType = OtherAnnotation.class;
		assertThat(FILTER.matches(annotationType)).isFalse();
	}

	@Test
	void plainWhenJavaLangAnnotationReturnsTrue() {
		assertThat(AnnotationFilter.PLAIN.matches(Retention.class)).isTrue();
	}

	@Test
	void plainWhenSpringLangAnnotationReturnsTrue() {
		assertThat(AnnotationFilter.PLAIN.matches(Nullable.class)).isTrue();
	}

	@Test
	void plainWhenOtherAnnotationReturnsFalse() {
		assertThat(AnnotationFilter.PLAIN.matches(TestAnnotation.class)).isFalse();
	}

	@Test
	void javaWhenJavaLangAnnotationReturnsTrue() {
		assertThat(AnnotationFilter.JAVA.matches(Retention.class)).isTrue();
	}

	@Test
	void javaWhenJavaxAnnotationReturnsTrue() {
		assertThat(AnnotationFilter.JAVA.matches(Nonnull.class)).isTrue();
	}

	@Test
	void javaWhenSpringLangAnnotationReturnsFalse() {
		assertThat(AnnotationFilter.JAVA.matches(Nullable.class)).isFalse();
	}

	@Test
	void javaWhenOtherAnnotationReturnsFalse() {
		assertThat(AnnotationFilter.JAVA.matches(TestAnnotation.class)).isFalse();
	}

	@Test
	@SuppressWarnings("deprecation")
	void noneReturnsFalse() {
		assertThat(AnnotationFilter.NONE.matches(Retention.class)).isFalse();
		assertThat(AnnotationFilter.NONE.matches(Nullable.class)).isFalse();
		assertThat(AnnotationFilter.NONE.matches(TestAnnotation.class)).isFalse();
	}


	@Retention(RetentionPolicy.RUNTIME)
	@interface TestAnnotation {
	}

	@TestAnnotation
	static class WithTestAnnotation {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface OtherAnnotation {
	}

	@OtherAnnotation
	static class WithOtherAnnotation {
	}

}
