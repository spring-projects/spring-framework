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

package org.springframework.aot.generate;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.ClassName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClassNameGenerator}.
 *
 * @author Phillip Webb
 */
class ClassNameGeneratorTests {

	private static final ClassName TEST_TARGET = ClassName.get("com.example", "Test");

	private final ClassNameGenerator generator = new ClassNameGenerator(TEST_TARGET);

	@Test
	void generateClassNameWhenTargetClassIsNullUsesMainTarget() {
		ClassName generated = this.generator.generateClassName("test", null);
		assertThat(generated).hasToString("com.example.Test__Test");
	}

	@Test
	void generateClassNameUseFeatureNamePrefix() {
		ClassName generated = new ClassNameGenerator(TEST_TARGET, "One")
				.generateClassName("test", ClassName.get(InputStream.class));
		assertThat(generated).hasToString("java.io.InputStream__OneTest");
	}

	@Test
	void generateClassNameWithNoTextFeatureNamePrefix() {
		ClassName generated = new ClassNameGenerator(TEST_TARGET, "  ")
				.generateClassName("test", ClassName.get(InputStream.class));
		assertThat(generated).hasToString("java.io.InputStream__Test");
	}

	@Test
	void generatedClassNameWhenFeatureIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generator.generateClassName("", ClassName.get(InputStream.class)))
				.withMessage("'featureName' must not be empty");
	}

	@Test
	void generatedClassNameWhenFeatureIsNotAllLettersThrowsException() {
		assertThat(this.generator.generateClassName("name!", ClassName.get(InputStream.class)))
				.hasToString("java.io.InputStream__Name");
		assertThat(this.generator.generateClassName("1NameHere", ClassName.get(InputStream.class)))
				.hasToString("java.io.InputStream__NameHere");
		assertThat(this.generator.generateClassName("Y0pe", ClassName.get(InputStream.class)))
				.hasToString("java.io.InputStream__YPe");
	}

	@Test
	void generateClassNameWithClassWhenLowercaseFeatureNameGeneratesName() {
		ClassName generated = this.generator.generateClassName("bytes", ClassName.get(InputStream.class));
		assertThat(generated).hasToString("java.io.InputStream__Bytes");
	}

	@Test
	void generateClassNameWithClassWhenInnerClassGeneratesName() {
		ClassName innerBean = ClassName.get("com.example", "Test", "InnerBean");
		ClassName generated = this.generator.generateClassName("EventListener", innerBean);
		assertThat(generated)
				.hasToString("com.example.Test_InnerBean__EventListener");
	}

	@Test
	void generateClassWithClassWhenMultipleCallsGeneratesSequencedName() {
		ClassName generated1 = this.generator.generateClassName("bytes",ClassName.get(InputStream.class));
		ClassName generated2 = this.generator.generateClassName("bytes", ClassName.get(InputStream.class));
		ClassName generated3 = this.generator.generateClassName("bytes", ClassName.get(InputStream.class));
		assertThat(generated1).hasToString("java.io.InputStream__Bytes");
		assertThat(generated2).hasToString("java.io.InputStream__Bytes1");
		assertThat(generated3).hasToString("java.io.InputStream__Bytes2");
	}

}
