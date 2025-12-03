/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.aot.generate;

import java.io.InputStream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.javapoet.ClassName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link NameGenerator}.
 *
 * @author Phillip Webb
 */
class NameGeneratorTests {

	private static final ClassName TEST_TARGET = ClassName.get("com.example", "Test");

	private final NameGenerator generator = new NameGenerator(TEST_TARGET);

	@Nested
	class ClassNameTests {

		@Test
		void generateClassNameWhenTargetClassIsNullUsesMainTarget() {
			ClassName generated = generator.generateClassName("test", null);
			assertThat(generated).hasToString("com.example.Test__Test");
		}

		@Test
		void generateClassNameUseFeatureNamePrefix() {
			ClassName generated = new NameGenerator(TEST_TARGET, "One")
					.generateClassName("test", ClassName.get(InputStream.class));
			assertThat(generated).hasToString("java.io.InputStream__OneTest");
		}

		@Test
		void generateClassNameWithNoTextFeatureNamePrefix() {
			ClassName generated = new NameGenerator(TEST_TARGET, "  ")
					.generateClassName("test", ClassName.get(InputStream.class));
			assertThat(generated).hasToString("java.io.InputStream__Test");
		}

		@Test
		void generatedClassNameWhenFeatureIsEmptyThrowsException() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> generator.generateClassName("", ClassName.get(InputStream.class)))
					.withMessage("'featureName' must not be empty");
		}

		@Test
		void generatedClassNameWhenFeatureIsNotAllLettersThrowsException() {
			assertThat(generator.generateClassName("name!", ClassName.get(InputStream.class)))
					.hasToString("java.io.InputStream__Name");
			assertThat(generator.generateClassName("1NameHere", ClassName.get(InputStream.class)))
					.hasToString("java.io.InputStream__NameHere");
			assertThat(generator.generateClassName("Y0pe", ClassName.get(InputStream.class)))
					.hasToString("java.io.InputStream__YPe");
		}

		@Test
		void generateClassNameWithClassWhenLowercaseFeatureNameGeneratesName() {
			ClassName generated = generator.generateClassName("bytes", ClassName.get(InputStream.class));
			assertThat(generated).hasToString("java.io.InputStream__Bytes");
		}

		@Test
		void generateClassNameWithClassWhenInnerClassGeneratesName() {
			ClassName innerBean = ClassName.get("com.example", "Test", "InnerBean");
			ClassName generated = generator.generateClassName("EventListener", innerBean);
			assertThat(generated)
					.hasToString("com.example.Test_InnerBean__EventListener");
		}

		@Test
		void generateClassWithClassWhenMultipleCallsGeneratesSequencedName() {
			ClassName generated1 = generator.generateClassName("bytes", ClassName.get(InputStream.class));
			ClassName generated2 = generator.generateClassName("bytes", ClassName.get(InputStream.class));
			ClassName generated3 = generator.generateClassName("bytes", ClassName.get(InputStream.class));
			assertThat(generated1).hasToString("java.io.InputStream__Bytes");
			assertThat(generated2).hasToString("java.io.InputStream__Bytes1");
			assertThat(generated3).hasToString("java.io.InputStream__Bytes2");
		}

	}

	@Nested
	class ResourcePathTests {

		@Test
		void generateResourcePathWhenTargetClassIsNullUsesMainTarget() {
			String generated = generator.generateResourcePath("txt", "test", null);
			assertThat(generated).isEqualTo("com/example/Test-test.txt");
		}

		@Test
		void generateResourcePathUseFeatureNamePrefix() {
			String generated = new NameGenerator(TEST_TARGET, "one")
					.generateResourcePath("txt", "test", ClassName.get(InputStream.class));
			assertThat(generated).hasToString("java/io/InputStream-one-test.txt");
		}

		@Test
		void generateResourcePathWithEmptyFeatureNamePrefix() {
			String generated = new NameGenerator(TEST_TARGET, "")
					.generateResourcePath("txt", "test", ClassName.get(InputStream.class));
			assertThat(generated).hasToString("java/io/InputStream-test.txt");
		}

		@Test
		void generateResourcePathWithNoTextFeatureNamePrefix() {
			String generated = new NameGenerator(TEST_TARGET, "  ")
					.generateResourcePath("txt", "test", ClassName.get(InputStream.class));
			assertThat(generated).hasToString("java/io/InputStream-test.txt");
		}

		@Test
		void generatedResourcePathWhenExtensionIsEmptyThrowsException() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> generator.generateResourcePath("", "test", ClassName.get(InputStream.class)))
					.withMessage("'extension' must not be empty");
		}

		@Test
		void generatedResourcePathWhenFeatureIsEmptyThrowsException() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> generator.generateResourcePath("txt", "", ClassName.get(InputStream.class)))
					.withMessage("'featureName' must not be empty");
		}

		@Test
		void generateResourcePathWhenCaseFeatureNameGeneratesName() {
			String generated = generator.generateResourcePath("txt", "Bytes", ClassName.get(InputStream.class));
			assertThat(generated).hasToString("java/io/InputStream-Bytes.txt");
		}

		@Test
		void generateResourcePathWhenMultipleCallsGeneratesSequencedName() {
			String generated1 = generator.generateResourcePath("txt","bytes", ClassName.get(InputStream.class));
			String generated2 = generator.generateResourcePath("txt","bytes", ClassName.get(InputStream.class));
			String generated3 = generator.generateResourcePath("txt", "bytes", ClassName.get(InputStream.class));
			assertThat(generated1).hasToString("java/io/InputStream-bytes.txt");
			assertThat(generated2).hasToString("java/io/InputStream-bytes1.txt");
			assertThat(generated3).hasToString("java/io/InputStream-bytes2.txt");
		}

	}

}
