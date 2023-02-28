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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotatedElementUtilsTests.StandardContainerWithMultipleAttributes;
import org.springframework.core.annotation.AnnotatedElementUtilsTests.StandardRepeatablesWithContainerWithMultipleAttributesTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link RepeatableContainers}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
class RepeatableContainersTests {

	@Nested
	class StandardRepeatableContainersTests {

		@Test
		void standardRepeatablesWhenNonRepeatableReturnsNull() {
			Object[] values = findRepeatedAnnotationValues(RepeatableContainers.standardRepeatables(),
				NonRepeatableTestCase.class, NonRepeatable.class);
			assertThat(values).isNull();
		}

		@Test
		void standardRepeatablesWhenSingleReturnsNull() {
			Object[] values = findRepeatedAnnotationValues(RepeatableContainers.standardRepeatables(),
				SingleStandardRepeatableTestCase.class, StandardRepeatable.class);
			assertThat(values).isNull();
		}

		@Test
		void standardRepeatablesWhenContainerButNotRepeatableReturnsNull() {
			Object[] values = findRepeatedAnnotationValues(RepeatableContainers.standardRepeatables(),
				ExplicitRepeatablesTestCase.class, ExplicitContainer.class);
			assertThat(values).isNull();
		}

		@Test
		void standardRepeatablesWhenContainerReturnsRepeats() {
			Object[] values = findRepeatedAnnotationValues(RepeatableContainers.standardRepeatables(),
				StandardRepeatablesTestCase.class, StandardContainer.class);
			assertThat(values).containsExactly("a", "b");
		}

		@Test
		void standardRepeatablesWithContainerWithMultipleAttributes() {
			Object[] values = findRepeatedAnnotationValues(RepeatableContainers.standardRepeatables(),
				StandardRepeatablesWithContainerWithMultipleAttributesTestCase.class,
				StandardContainerWithMultipleAttributes.class);
			assertThat(values).containsExactly("a", "b");
		}

	}

	@Nested
	class ExplicitRepeatableContainerTests {

		@Test
		void ofExplicitWhenNonRepeatableReturnsNull() {
			Object[] values = findRepeatedAnnotationValues(
				RepeatableContainers.of(ExplicitRepeatable.class, ExplicitContainer.class),
				NonRepeatableTestCase.class, NonRepeatable.class);
			assertThat(values).isNull();
		}

		@Test
		void ofExplicitWhenStandardRepeatableContainerReturnsNull() {
			Object[] values = findRepeatedAnnotationValues(
				RepeatableContainers.of(ExplicitRepeatable.class, ExplicitContainer.class),
				StandardRepeatablesTestCase.class, StandardContainer.class);
			assertThat(values).isNull();
		}

		@Test
		void ofExplicitWhenContainerReturnsRepeats() {
			Object[] values = findRepeatedAnnotationValues(
				RepeatableContainers.of(ExplicitRepeatable.class, ExplicitContainer.class),
				ExplicitRepeatablesTestCase.class, ExplicitContainer.class);
			assertThat(values).containsExactly("a", "b");
		}

		@Test
		void ofExplicitWhenContainerIsNullDeducesContainer() {
			Object[] values = findRepeatedAnnotationValues(RepeatableContainers.of(StandardRepeatable.class, null),
				StandardRepeatablesTestCase.class, StandardContainer.class);
			assertThat(values).containsExactly("a", "b");
		}

		@Test
		void ofExplicitWhenHasNoValueThrowsException() {
			assertThatExceptionOfType(AnnotationConfigurationException.class)
				.isThrownBy(() -> RepeatableContainers.of(ExplicitRepeatable.class, InvalidNoValue.class))
				.withMessageContaining("Invalid declaration of container type [%s] for repeatable annotation [%s]",
					InvalidNoValue.class.getName(), ExplicitRepeatable.class.getName());
		}

		@Test
		void ofExplicitWhenValueIsNotArrayThrowsException() {
			assertThatExceptionOfType(AnnotationConfigurationException.class)
				.isThrownBy(() -> RepeatableContainers.of(ExplicitRepeatable.class, InvalidNotArray.class))
				.withMessage("Container type [%s] must declare a 'value' attribute for an array of type [%s]",
					InvalidNotArray.class.getName(), ExplicitRepeatable.class.getName());
		}

		@Test
		void ofExplicitWhenValueIsArrayOfWrongTypeThrowsException() {
			assertThatExceptionOfType(AnnotationConfigurationException.class)
				.isThrownBy(() -> RepeatableContainers.of(ExplicitRepeatable.class, InvalidWrongArrayType.class))
				.withMessage("Container type [%s] must declare a 'value' attribute for an array of type [%s]",
					InvalidWrongArrayType.class.getName(), ExplicitRepeatable.class.getName());
		}

		@Test
		void ofExplicitWhenAnnotationIsNullThrowsException() {
			assertThatIllegalArgumentException()
				.isThrownBy(() -> RepeatableContainers.of(null, null))
				.withMessage("Repeatable must not be null");
		}

		@Test
		void ofExplicitWhenContainerIsNullAndNotRepeatableThrowsException() {
			assertThatIllegalArgumentException()
				.isThrownBy(() -> RepeatableContainers.of(ExplicitRepeatable.class, null))
				.withMessage("Annotation type must be a repeatable annotation: failed to resolve container type for %s",
					ExplicitRepeatable.class.getName());
		}

	}

	@Test
	void standardAndExplicitReturnsRepeats() {
		RepeatableContainers repeatableContainers = RepeatableContainers.standardRepeatables()
			.and(ExplicitContainer.class, ExplicitRepeatable.class);
		assertThat(findRepeatedAnnotationValues(repeatableContainers, StandardRepeatablesTestCase.class, StandardContainer.class))
			.containsExactly("a", "b");
		assertThat(findRepeatedAnnotationValues(repeatableContainers, ExplicitRepeatablesTestCase.class, ExplicitContainer.class))
			.containsExactly("a", "b");
	}

	@Test
	void noneAlwaysReturnsNull() {
		Object[] values = findRepeatedAnnotationValues(RepeatableContainers.none(), StandardRepeatablesTestCase.class,
			StandardContainer.class);
		assertThat(values).isNull();
	}

	@Test
	void equalsAndHashcode() {
		RepeatableContainers c1 = RepeatableContainers.of(ExplicitRepeatable.class, ExplicitContainer.class);
		RepeatableContainers c2 = RepeatableContainers.of(ExplicitRepeatable.class, ExplicitContainer.class);
		RepeatableContainers c3 = RepeatableContainers.standardRepeatables();
		RepeatableContainers c4 = RepeatableContainers.standardRepeatables().and(ExplicitContainer.class, ExplicitRepeatable.class);
		assertThat(c1).hasSameHashCodeAs(c2);
		assertThat(c1).isEqualTo(c1).isEqualTo(c2);
		assertThat(c1).isNotEqualTo(c3).isNotEqualTo(c4);
	}


	private static Object[] findRepeatedAnnotationValues(RepeatableContainers containers,
			Class<?> element, Class<? extends Annotation> annotationType) {
		Annotation[] annotations = containers.findRepeatedAnnotations(element.getAnnotation(annotationType));
		return extractValues(annotations);
	}

	private static Object[] extractValues(Annotation[] annotations) {
		if (annotations == null) {
			return null;
		}
		return Arrays.stream(annotations).map(AnnotationUtils::getValue).toArray(Object[]::new);
	}


	@Retention(RetentionPolicy.RUNTIME)
	@interface NonRepeatable {

		String value() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface StandardContainer {

		StandardRepeatable[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Repeatable(StandardContainer.class)
	@interface StandardRepeatable {

		String value() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ExplicitContainer {

		ExplicitRepeatable[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ExplicitRepeatable {

		String value() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface InvalidNoValue {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface InvalidNotArray {

		int value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface InvalidWrongArrayType {

		StandardRepeatable[] value();
	}

	@NonRepeatable("a")
	static class NonRepeatableTestCase {
	}

	@StandardRepeatable("a")
	static class SingleStandardRepeatableTestCase {
	}

	@StandardRepeatable("a")
	@StandardRepeatable("b")
	static class StandardRepeatablesTestCase {
	}

	@ExplicitContainer({ @ExplicitRepeatable("a"), @ExplicitRepeatable("b") })
	static class ExplicitRepeatablesTestCase {
	}

}
