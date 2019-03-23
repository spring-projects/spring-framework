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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

import org.junit.Test;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link AnnotationFilter}.
 *
 * @author Phillip Webb
 */
public class AnnotationFilterTests {

	private static final AnnotationFilter FILTER = annotationType ->
			ObjectUtils.nullSafeEquals(annotationType, TestAnnotation.class.getName());


	@Test
	public void matchesAnnotationWhenMatchReturnsTrue() {
		TestAnnotation annotation = WithTestAnnotation.class.getDeclaredAnnotation(TestAnnotation.class);
		assertThat(FILTER.matches(annotation)).isTrue();
	}

	@Test
	public void matchesAnnotationWhenNoMatchReturnsFalse() {
		OtherAnnotation annotation = WithOtherAnnotation.class.getDeclaredAnnotation(OtherAnnotation.class);
		assertThat(FILTER.matches(annotation)).isFalse();
	}

	@Test
	public void matchesAnnotationClassWhenMatchReturnsTrue() {
		Class<TestAnnotation> annotationType = TestAnnotation.class;
		assertThat(FILTER.matches(annotationType)).isTrue();
	}

	@Test
	public void matchesAnnotationClassWhenNoMatchReturnsFalse() {
		Class<OtherAnnotation> annotationType = OtherAnnotation.class;
		assertThat(FILTER.matches(annotationType)).isFalse();
	}

	@Test
	public void plainWhenJavaLangAnnotationReturnsTrue() {
		assertThat(AnnotationFilter.PLAIN.matches(Retention.class)).isTrue();
	}

	@Test
	public void plainWhenSpringLangAnnotationReturnsTrue() {
		assertThat(AnnotationFilter.PLAIN.matches(Nullable.class)).isTrue();
	}

	@Test
	public void plainWhenOtherAnnotationReturnsFalse() {
		assertThat(AnnotationFilter.PLAIN.matches(TestAnnotation.class)).isFalse();
	}

	@Test
	public void javaWhenJavaLangAnnotationReturnsTrue() {
		assertThat(AnnotationFilter.JAVA.matches(Retention.class)).isTrue();
	}

	@Test
	public void javaWhenSpringLangAnnotationReturnsFalse() {
		assertThat(AnnotationFilter.JAVA.matches(Nullable.class)).isFalse();
	}

	@Test
	public void javaWhenOtherAnnotationReturnsFalse() {
		assertThat(AnnotationFilter.JAVA.matches(TestAnnotation.class)).isFalse();
	}

	@Test
	public void noneReturnsFalse() {
		assertThat(AnnotationFilter.NONE.matches(Retention.class)).isFalse();
		assertThat(AnnotationFilter.NONE.matches(Nullable.class)).isFalse();
		assertThat(AnnotationFilter.NONE.matches(TestAnnotation.class)).isFalse();
	}

	@Test
	public void pacakgesReturnsPackagesAnnotationFilter() {
		assertThat(AnnotationFilter.packages("com.example")).isInstanceOf(PackagesAnnotationFilter.class);
	}

	@Test
	public void mostAppropriateForCollectionReturnsPlainWhenPossible() {
		AnnotationFilter filter = AnnotationFilter.mostAppropriateFor(
				Arrays.asList(TestAnnotation.class, OtherAnnotation.class));
		assertThat(filter).isSameAs(AnnotationFilter.PLAIN);
	}

	@Test
	public void mostAppropriateForCollectionWhenCantUsePlainReturnsNone() {
		AnnotationFilter filter = AnnotationFilter.mostAppropriateFor(Arrays.asList(
				TestAnnotation.class, OtherAnnotation.class, Nullable.class));
		assertThat(filter).isSameAs(AnnotationFilter.NONE);
	}

	@Test
	public void mostAppropriateForArrayReturnsPlainWhenPossible() {
		AnnotationFilter filter = AnnotationFilter.mostAppropriateFor(
				TestAnnotation.class, OtherAnnotation.class);
		assertThat(filter).isSameAs(AnnotationFilter.PLAIN);
	}

	@Test
	public void mostAppropriateForArrayWhenCantUsePlainReturnsNone() {
		AnnotationFilter filter = AnnotationFilter.mostAppropriateFor(
				TestAnnotation.class, OtherAnnotation.class, Nullable.class);
		assertThat(filter).isSameAs(AnnotationFilter.NONE);
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
