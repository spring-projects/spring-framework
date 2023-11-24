/*
 * Copyright 2002-2022 the original author or authors.
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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MergedAnnotationCollectors}.
 *
 * @author Phillip Webb
 */
class MergedAnnotationCollectorsTests {

	@Test
	void toAnnotationSetCollectsLinkedHashSetWithSynthesizedAnnotations() {
		Set<TestAnnotation> set = stream().collect(
				MergedAnnotationCollectors.toAnnotationSet());
		assertThat(set).isInstanceOf(LinkedHashSet.class).flatExtracting(
				TestAnnotation::value).containsExactly("a", "b", "c");
		assertThat(set).allMatch(AnnotationUtils::isSynthesizedAnnotation);
	}

	@Test
	void toAnnotationArrayCollectsAnnotationArrayWithSynthesizedAnnotations() {
		Annotation[] array = stream().collect(
				MergedAnnotationCollectors.toAnnotationArray());
		assertThat(Arrays.stream(array).map(
				annotation -> ((TestAnnotation) annotation).value())).containsExactly("a",
						"b", "c");
		assertThat(array).allMatch(AnnotationUtils::isSynthesizedAnnotation);
	}

	@Test
	void toSuppliedAnnotationArrayCollectsAnnotationArrayWithSynthesizedAnnotations() {
		TestAnnotation[] array = stream().collect(
				MergedAnnotationCollectors.toAnnotationArray(TestAnnotation[]::new));
		assertThat(Arrays.stream(array).map(TestAnnotation::value)).containsExactly("a",
				"b", "c");
		assertThat(array).allMatch(AnnotationUtils::isSynthesizedAnnotation);
	}

	@Test
	void toMultiValueMapCollectsMultiValueMap() {
		MultiValueMap<String, Object> map = stream().map(
				MergedAnnotation::filterDefaultValues).collect(
						MergedAnnotationCollectors.toMultiValueMap(
								Adapt.CLASS_TO_STRING));
		assertThat(map.get("value")).containsExactly("a", "b", "c");
		assertThat(map.get("extra")).containsExactly("java.lang.String",
				"java.lang.Integer");
	}

	@Test
	void toFinishedMultiValueMapCollectsMultiValueMap() {
		MultiValueMap<String, Object> map = stream().collect(
				MergedAnnotationCollectors.toMultiValueMap(result -> {
					result.add("finished", true);
					return result;
				}));
		assertThat(map.get("value")).containsExactly("a", "b", "c");
		assertThat(map.get("extra")).containsExactly(void.class, String.class,
				Integer.class);
		assertThat(map.get("finished")).containsExactly(true);
	}

	private Stream<MergedAnnotation<TestAnnotation>> stream() {
		return MergedAnnotations.from(WithTestAnnotations.class).stream(TestAnnotation.class);
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Repeatable(TestAnnotations.class)
	@interface TestAnnotation {

		@AliasFor("name")
		String value() default "";

		@AliasFor("value")
		String name() default "";

		Class<?> extra() default void.class;

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface TestAnnotations {

		TestAnnotation[] value();
	}

	@TestAnnotation("a")
	@TestAnnotation(name = "b", extra = String.class)
	@TestAnnotation(name = "c", extra = Integer.class)
	static class WithTestAnnotations {
	}

}
