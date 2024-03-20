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

package org.springframework.test.context.bean.override;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.bean.override.example.ExampleBeanOverrideAnnotation;
import org.springframework.test.context.bean.override.example.TestBeanOverrideMetaAnnotation;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * Unit tests for {@link BeanOverrideParsingUtils}.
 *
 * @since 6.2
 */
class BeanOverrideParsingUtilsTests {

	// Metadata built from a String that starts with DUPLICATE_TRIGGER are considered equal
	private static final String DUPLICATE_TRIGGER1 = ExampleBeanOverrideAnnotation.DUPLICATE_TRIGGER + "-v1";
	private static final String DUPLICATE_TRIGGER2 = ExampleBeanOverrideAnnotation.DUPLICATE_TRIGGER + "-v2";

	@Test
	void findsOnField() {
		assertThat(BeanOverrideParsingUtils.parse(SingleAnnotationOnField.class))
				.map(Object::toString)
				.containsExactly("onField");
	}

	@Test
	void allowsMultipleProcessorsOnDifferentElements() {
		assertThat(BeanOverrideParsingUtils.parse(AnnotationsOnMultipleFields.class))
				.map(Object::toString)
				.containsExactlyInAnyOrder("onField1", "onField2");
	}

	@Test
	void rejectsMultipleAnnotationsOnSameElement() {
		Field field = ReflectionUtils.findField(MultipleAnnotationsOnField.class, "message");
		assertThatRuntimeException()
				.isThrownBy(() -> BeanOverrideParsingUtils.parse(MultipleAnnotationsOnField.class))
				.withMessage("Multiple @BeanOverride annotations found on field: " + field);
	}

	@Test
	void keepsFirstOccurrenceOfEqualMetadata() {
		assertThat(BeanOverrideParsingUtils.parse(DuplicateConf.class))
				.map(Object::toString)
				.containsExactly("{DUPLICATE-v1}");
	}


	static class SingleAnnotationOnField {

		@ExampleBeanOverrideAnnotation("onField")
		String message;

		static String onField() {
			return "OK";
		}
	}

	static class MultipleAnnotationsOnField {

		@ExampleBeanOverrideAnnotation("foo")
		@TestBeanOverrideMetaAnnotation
		String message;

		static String foo() {
			return "foo";
		}
	}

	static class AnnotationsOnMultipleFields {

		@ExampleBeanOverrideAnnotation("onField1")
		String message;

		@ExampleBeanOverrideAnnotation("onField2")
		String messageOther;

		static String onField1() {
			return "OK1";
		}

		static String onField2() {
			return "OK2";
		}
	}

	static class DuplicateConf {

		@ExampleBeanOverrideAnnotation(DUPLICATE_TRIGGER1)
		String message1;

		@ExampleBeanOverrideAnnotation(DUPLICATE_TRIGGER2)
		String message2;
	}

}
