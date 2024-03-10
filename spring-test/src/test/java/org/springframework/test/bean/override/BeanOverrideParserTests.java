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

package org.springframework.test.bean.override;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import org.springframework.test.bean.override.example.ExampleBeanOverrideAnnotation;
import org.springframework.test.bean.override.example.TestBeanOverrideMetaAnnotation;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.springframework.test.bean.override.example.ExampleBeanOverrideProcessor.DUPLICATE_TRIGGER;

/**
 * Unit tests for {@link BeanOverrideParser}.
 *
 * @since 6.2
 */
class BeanOverrideParserTests {

	private final BeanOverrideParser parser = new BeanOverrideParser();


	@Test
	void findsOnField() {
		parser.parse(SingleAnnotationOnField.class);

		assertThat(parser.getOverrideMetadata())
				.map(om -> ((ExampleBeanOverrideAnnotation) om.overrideAnnotation()).value())
				.containsExactly("onField");
	}

	@Test
	void allowsMultipleProcessorsOnDifferentElements() {
		parser.parse(AnnotationsOnMultipleFields.class);

		assertThat(parser.getOverrideMetadata())
				.map(om -> ((ExampleBeanOverrideAnnotation) om.overrideAnnotation()).value())
				.containsExactlyInAnyOrder("onField1", "onField2");
	}

	@Test
	void rejectsMultipleAnnotationsOnSameElement() {
		Field field = ReflectionUtils.findField(MultipleAnnotationsOnField.class, "message");
		assertThatRuntimeException()
				.isThrownBy(() -> parser.parse(MultipleAnnotationsOnField.class))
				.withMessage("Multiple @BeanOverride annotations found on field: " + field);
	}

	@Test
	void detectsDuplicateMetadata() {
		assertThatRuntimeException()
				.isThrownBy(() -> parser.parse(DuplicateConf.class))
				.withMessage("Duplicate test OverrideMetadata: {DUPLICATE_TRIGGER}");
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

		@ExampleBeanOverrideAnnotation(DUPLICATE_TRIGGER)
		String message1;

		@ExampleBeanOverrideAnnotation(DUPLICATE_TRIGGER)
		String message2;
	}

}
