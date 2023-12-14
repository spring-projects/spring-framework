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

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.test.bean.override.example.ExampleBeanOverrideAnnotation;
import org.springframework.test.bean.override.example.TestBeanOverrideMetaAnnotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.springframework.test.bean.override.example.ExampleBeanOverrideProcessor.DUPLICATE_TRIGGER;

class BeanOverrideParserTests {

	@Test
	void findsOnField() {
		BeanOverrideParser parser = new BeanOverrideParser();
		parser.parse(OnFieldConf.class);

		assertThat(parser.getOverrideMetadata()).hasSize(1)
				.first()
				.extracting(om -> ((ExampleBeanOverrideAnnotation) om.overrideAnnotation()).value())
				.isEqualTo("onField");
	}

	@Test
	void allowMultipleProcessorsOnDifferentElements() {
		BeanOverrideParser parser = new BeanOverrideParser();
		parser.parse(MultipleFieldsWithOnFieldConf.class);

		assertThat(parser.getOverrideMetadata())
				.hasSize(2)
				.map(om -> ((ExampleBeanOverrideAnnotation) om.overrideAnnotation()).value())
				.containsOnly("onField1", "onField2");
	}

	@Test
	void rejectsMultipleAnnotationsOnSameElement() {
		BeanOverrideParser parser = new BeanOverrideParser();
		assertThatRuntimeException().isThrownBy(() -> parser.parse(MultipleOnFieldConf.class))
				.withMessage("Multiple bean override annotations found on annotated field <" +
						String.class.getName() + " " + MultipleOnFieldConf.class.getName() + ".message>");
	}

	@Test
	void detectsDuplicateMetadata() {
		BeanOverrideParser parser = new BeanOverrideParser();
		assertThatRuntimeException().isThrownBy(() -> parser.parse(DuplicateConf.class))
				.withMessage("Duplicate test overrideMetadata {DUPLICATE_TRIGGER}");
	}


	@Configuration
	static class OnFieldConf {

		@ExampleBeanOverrideAnnotation("onField")
		String message;

		static String onField() {
			return "OK";
		}

	}

	@Configuration
	static class MultipleOnFieldConf {

		@ExampleBeanOverrideAnnotation("foo")
		@TestBeanOverrideMetaAnnotation
		String message;

		static String foo() {
			return "foo";
		}

	}

	@Configuration
	static class MultipleFieldsWithOnFieldConf {
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

	@Configuration
	static class DuplicateConf {

		@ExampleBeanOverrideAnnotation(DUPLICATE_TRIGGER)
		String message1;

		@ExampleBeanOverrideAnnotation(DUPLICATE_TRIGGER)
		String message2;

	}

}
