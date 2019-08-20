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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link RepeatableContainers}.
 *
 * @author Phillip Webb
 */
class RepeatableContainersTests {

	@Test
	void standardRepeatablesWhenNonRepeatableReturnsNull() {
		Object[] values = findRepeatedAnnotationValues(
				RepeatableContainers.standardRepeatables(), WithNonRepeatable.class,
				NonRepeatable.class);
		assertThat(values).isNull();
	}

	@Test
	void standardRepeatablesWhenSingleReturnsNull() {
		Object[] values = findRepeatedAnnotationValues(
				RepeatableContainers.standardRepeatables(),
				WithSingleStandardRepeatable.class, StandardRepeatable.class);
		assertThat(values).isNull();
	}

	@Test
	void standardRepeatablesWhenContainerReturnsRepeats() {
		Object[] values = findRepeatedAnnotationValues(
				RepeatableContainers.standardRepeatables(), WithStandardRepeatables.class,
				StandardContainer.class);
		assertThat(values).containsExactly("a", "b");
	}

	@Test
	void standardRepeatablesWhenContainerButNotRepeatableReturnsNull() {
		Object[] values = findRepeatedAnnotationValues(
				RepeatableContainers.standardRepeatables(), WithExplicitRepeatables.class,
				ExplicitContainer.class);
		assertThat(values).isNull();
	}

	@Test
	void ofExplicitWhenNonRepeatableReturnsNull() {
		Object[] values = findRepeatedAnnotationValues(
				RepeatableContainers.of(ExplicitRepeatable.class,
						ExplicitContainer.class),
				WithNonRepeatable.class, NonRepeatable.class);
		assertThat(values).isNull();
	}

	@Test
	void ofExplicitWhenStandardRepeatableContainerReturnsNull() {
		Object[] values = findRepeatedAnnotationValues(
				RepeatableContainers.of(ExplicitRepeatable.class,
						ExplicitContainer.class),
				WithStandardRepeatables.class, StandardContainer.class);
		assertThat(values).isNull();
	}

	@Test
	void ofExplicitWhenContainerReturnsRepeats() {
		Object[] values = findRepeatedAnnotationValues(
				RepeatableContainers.of(ExplicitRepeatable.class,
						ExplicitContainer.class),
				WithExplicitRepeatables.class, ExplicitContainer.class);
		assertThat(values).containsExactly("a", "b");
	}

	@Test
	void ofExplicitWhenHasNoValueThrowsException() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
				RepeatableContainers.of(ExplicitRepeatable.class, InvalidNoValue.class))
			.withMessageContaining("Invalid declaration of container type ["
									+ InvalidNoValue.class.getName()
									+ "] for repeatable annotation ["
									+ ExplicitRepeatable.class.getName() + "]");
	}

	@Test
	void ofExplicitWhenValueIsNotArrayThrowsException() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
				RepeatableContainers.of(ExplicitRepeatable.class, InvalidNotArray.class))
			.withMessage("Container type ["
								+ InvalidNotArray.class.getName()
								+ "] must declare a 'value' attribute for an array of type ["
								+ ExplicitRepeatable.class.getName() + "]");
	}

	@Test
	void ofExplicitWhenValueIsArrayOfWrongTypeThrowsException() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
				RepeatableContainers.of(ExplicitRepeatable.class, InvalidWrongArrayType.class))
			.withMessage("Container type ["
								+ InvalidWrongArrayType.class.getName()
								+ "] must declare a 'value' attribute for an array of type ["
								+ ExplicitRepeatable.class.getName() + "]");
	}

	@Test
	void ofExplicitWhenAnnotationIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				RepeatableContainers.of(null, null))
			.withMessage("Repeatable must not be null");
	}

	@Test
	void ofExplicitWhenContainerIsNullDeducesContainer() {
		Object[] values = findRepeatedAnnotationValues(
				RepeatableContainers.of(StandardRepeatable.class, null),
				WithStandardRepeatables.class, StandardContainer.class);
		assertThat(values).containsExactly("a", "b");
	}

	@Test
	void ofExplicitWhenContainerIsNullAndNotRepeatableThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				RepeatableContainers.of(ExplicitRepeatable.class, null))
			.withMessage("Annotation type must be a repeatable annotation: " +
						"failed to resolve container type for " +
						ExplicitRepeatable.class.getName());
	}

	@Test
	void standardAndExplicitReturnsRepeats() {
		RepeatableContainers repeatableContainers = RepeatableContainers.standardRepeatables().and(
				ExplicitContainer.class, ExplicitRepeatable.class);
		assertThat(findRepeatedAnnotationValues(repeatableContainers,
				WithStandardRepeatables.class, StandardContainer.class)).containsExactly(
						"a", "b");
		assertThat(findRepeatedAnnotationValues(repeatableContainers,
				WithExplicitRepeatables.class, ExplicitContainer.class)).containsExactly(
						"a", "b");
	}

	@Test
	void noneAlwaysReturnsNull() {
		Object[] values = findRepeatedAnnotationValues(
				RepeatableContainers.none(), WithStandardRepeatables.class,
				StandardContainer.class);
		assertThat(values).isNull();
	}

	@Test
	void equalsAndHashcode() {
		RepeatableContainers c1 = RepeatableContainers.of(ExplicitRepeatable.class,
				ExplicitContainer.class);
		RepeatableContainers c2 = RepeatableContainers.of(ExplicitRepeatable.class,
				ExplicitContainer.class);
		RepeatableContainers c3 = RepeatableContainers.standardRepeatables();
		RepeatableContainers c4 = RepeatableContainers.standardRepeatables().and(
				ExplicitContainer.class, ExplicitRepeatable.class);
		assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
		assertThat(c1).isEqualTo(c1).isEqualTo(c2);
		assertThat(c1).isNotEqualTo(c3).isNotEqualTo(c4);
	}

	private Object[] findRepeatedAnnotationValues(RepeatableContainers containers,
			Class<?> element, Class<? extends Annotation> annotationType) {
		Annotation[] annotations = containers.findRepeatedAnnotations(
				element.getAnnotation(annotationType));
		return extractValues(annotations);
	}

	private Object[] extractValues(Annotation[] annotations) {
		try {
			if (annotations == null) {
				return null;
			}
			Object[] result = new String[annotations.length];
			for (int i = 0; i < annotations.length; i++) {
				result[i] = annotations[i].annotationType().getMethod("value").invoke(
						annotations[i]);
			}
			return result;
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface NonRepeatable {

		String value() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Repeatable(StandardContainer.class)
	static @interface StandardRepeatable {

		String value() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface StandardContainer {

		StandardRepeatable[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ExplicitRepeatable {

		String value() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ExplicitContainer {

		ExplicitRepeatable[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface InvalidNoValue {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface InvalidNotArray {

		int value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface InvalidWrongArrayType {

		StandardRepeatable[] value();

	}

	@NonRepeatable("a")
	static class WithNonRepeatable {

	}

	@StandardRepeatable("a")
	static class WithSingleStandardRepeatable {

	}

	@StandardRepeatable("a")
	@StandardRepeatable("b")
	static class WithStandardRepeatables {

	}

	@ExplicitContainer({ @ExplicitRepeatable("a"), @ExplicitRepeatable("b") })
	static class WithExplicitRepeatables {

	}

}
