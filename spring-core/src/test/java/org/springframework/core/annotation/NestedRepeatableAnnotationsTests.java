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

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for various ways to search for repeatable annotations that are
 * nested (i.e., repeatable annotations used as meta-annotations on other
 * repeatable annotations).
 *
 * @author Sam Brannen
 * @since 5.3.24
 */
@SuppressWarnings("unused")
class NestedRepeatableAnnotationsTests {

	@Nested
	class SingleRepeatableAnnotationTests {

		private final Method method = ReflectionUtils.findMethod(getClass(), "annotatedMethod");

		@Test
		void streamRepeatableAnnotations_MergedAnnotationsApi() {
			Set<A> annotations = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY)
					.stream(A.class).collect(MergedAnnotationCollectors.toAnnotationSet());
			// Merged, so we expect to find @A once with its value coming from @B(5).
			assertThat(annotations).extracting(A::value).containsExactly(5);
		}

		@Test
		void findMergedRepeatableAnnotations_AnnotatedElementUtils() {
			Set<A> annotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(method, A.class);
			// Merged, so we expect to find @A once with its value coming from @B(5).
			assertThat(annotations).extracting(A::value).containsExactly(5);
		}

		@Test
		void getMergedRepeatableAnnotationsWithStandardRepeatables_AnnotatedElementUtils() {
			Set<A> annotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(method, A.class);
			// Merged, so we expect to find @A once with its value coming from @B(5).
			assertThat(annotations).extracting(A::value).containsExactly(5);
		}

		@Test
		void getMergedRepeatableAnnotationsWithExplicitContainer_AnnotatedElementUtils() {
			Set<A> annotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(method, A.class, A.Container.class);
			// Merged, so we expect to find @A once with its value coming from @B(5).
			assertThat(annotations).extracting(A::value).containsExactly(5);
		}

		@Test
		@SuppressWarnings("deprecation")
		void getRepeatableAnnotations_AnnotationUtils() {
			Set<A> annotations = AnnotationUtils.getRepeatableAnnotations(method, A.class);
			// Not merged, so we expect to find @A once with the default value of 0.
			// @A will actually be found twice, but we have Set semantics here.
			assertThat(annotations).extracting(A::value).containsExactly(0);
		}

		@B(5)
		void annotatedMethod() {
		}

	}

	@Nested
	class MultipleRepeatableAnnotationsTests {

		private final Method method = ReflectionUtils.findMethod(getClass(), "annotatedMethod");

		@Test
		void streamRepeatableAnnotationsWithStandardRepeatables_MergedAnnotationsApi() {
			RepeatableContainers repeatableContainers = RepeatableContainers.standardRepeatables();
			Set<A> annotations = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY, repeatableContainers)
					.stream(A.class).collect(MergedAnnotationCollectors.toAnnotationSet());
			// Merged, so we expect to find @A twice with values coming from @B(5) and @B(10).
			assertThat(annotations).extracting(A::value).containsExactly(5, 10);
		}

		@Test
		void streamRepeatableAnnotationsWithExplicitRepeatables_MergedAnnotationsApi() {
			RepeatableContainers repeatableContainers =
					RepeatableContainers.of(A.class, A.Container.class).and(B.Container.class, B.class);
			Set<A> annotations = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY, repeatableContainers)
					.stream(A.class).collect(MergedAnnotationCollectors.toAnnotationSet());
			// Merged, so we expect to find @A twice with values coming from @B(5) and @B(10).
			assertThat(annotations).extracting(A::value).containsExactly(5, 10);
		}

		@Test
		void findMergedRepeatableAnnotationsWithStandardRepeatables_AnnotatedElementUtils() {
			Set<A> annotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(method, A.class);
			// Merged, so we expect to find @A twice with values coming from @B(5) and @B(10).
			assertThat(annotations).extracting(A::value).containsExactly(5, 10);
		}

		@Test
		void findMergedRepeatableAnnotationsWithExplicitContainer_AnnotatedElementUtils() {
			Set<A> annotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(method, A.class, A.Container.class);
			// When findMergedRepeatableAnnotations(...) is invoked with an explicit container
			// type, it uses RepeatableContainers.of(...) which limits the repeatable annotation
			// support to a single container type.
			//
			// In this test case, we are therefore limiting the support to @A.Container, which
			// means that @B.Container is unsupported and effectively ignored as a repeatable
			// container type.
			//
			// Long story, short: the search doesn't find anything.
			assertThat(annotations).isEmpty();
		}

		@Test
		void getMergedRepeatableAnnotationsWithStandardRepeatables_AnnotatedElementUtils() {
			Set<A> annotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(method, A.class);
			// Merged, so we expect to find @A twice with values coming from @B(5) and @B(10).
			assertThat(annotations).extracting(A::value).containsExactly(5, 10);
		}

		@Test
		void getMergedRepeatableAnnotationsWithExplicitContainer_AnnotatedElementUtils() {
			Set<A> annotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(method, A.class, A.Container.class);
			// When getMergedRepeatableAnnotations(...) is invoked with an explicit container
			// type, it uses RepeatableContainers.of(...) which limits the repeatable annotation
			// support to a single container type.
			//
			// In this test case, we are therefore limiting the support to @A.Container, which
			// means that @B.Container is unsupported and effectively ignored as a repeatable
			// container type.
			//
			// Long story, short: the search doesn't find anything.
			assertThat(annotations).isEmpty();
		}

		@Test
		@SuppressWarnings("deprecation")
		void getRepeatableAnnotations_AnnotationUtils() {
			Set<A> annotations = AnnotationUtils.getRepeatableAnnotations(method, A.class);
			// Not merged, so we expect to find a single @A with default value of 0.
			// @A will actually be found twice, but we have Set semantics here.
			assertThat(annotations).extracting(A::value).containsExactly(0);
		}

		@B(5)
		@B(10)
		void annotatedMethod() {
		}

	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Repeatable(A.Container.class)
	@interface A {

		int value() default 0;

		@Retention(RetentionPolicy.RUNTIME)
		@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
		@interface Container {
			A[] value();
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Repeatable(B.Container.class)
	@A
	@A
	@interface B {

		@AliasFor(annotation = A.class)
		int value();

		@Retention(RetentionPolicy.RUNTIME)
		@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
		@interface Container {
			B[] value();
		}
	}

}
