/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aot.hint;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SimpleTypeReference}.
 *
 * @author Stephane Nicoll
 */
class SimpleTypeReferenceTests {


	@ParameterizedTest
	@MethodSource("primitivesAndPrimitivesArray")
	void primitivesAreHandledProperly(TypeReference typeReference, String expectedName) {
		assertThat(typeReference.getName()).isEqualTo(expectedName);
		assertThat(typeReference.getCanonicalName()).isEqualTo(expectedName);
		assertThat(typeReference.getPackageName()).isEqualTo("java.lang");
	}

	static Stream<Arguments> primitivesAndPrimitivesArray() {
		return Stream.of(
				Arguments.of(SimpleTypeReference.of("boolean"), "boolean"),
				Arguments.of(SimpleTypeReference.of("byte"), "byte"),
				Arguments.of(SimpleTypeReference.of("short"), "short"),
				Arguments.of(SimpleTypeReference.of("int"), "int"),
				Arguments.of(SimpleTypeReference.of("long"), "long"),
				Arguments.of(SimpleTypeReference.of("char"), "char"),
				Arguments.of(SimpleTypeReference.of("float"), "float"),
				Arguments.of(SimpleTypeReference.of("double"), "double"),
				Arguments.of(SimpleTypeReference.of("boolean[]"), "boolean[]"),
				Arguments.of(SimpleTypeReference.of("byte[]"), "byte[]"),
				Arguments.of(SimpleTypeReference.of("short[]"), "short[]"),
				Arguments.of(SimpleTypeReference.of("int[]"), "int[]"),
				Arguments.of(SimpleTypeReference.of("long[]"), "long[]"),
				Arguments.of(SimpleTypeReference.of("char[]"), "char[]"),
				Arguments.of(SimpleTypeReference.of("float[]"), "float[]"),
				Arguments.of(SimpleTypeReference.of("double[]"), "double[]"));
	}

	@ParameterizedTest
	@MethodSource("arrays")
	void arraysHaveSuitableReflectionTargetName(TypeReference typeReference, String expectedName) {
		assertThat(typeReference.getName()).isEqualTo(expectedName);
	}

	static Stream<Arguments> arrays() {
		return Stream.of(
				Arguments.of(SimpleTypeReference.of("java.lang.Object[]"), "java.lang.Object[]"),
				Arguments.of(SimpleTypeReference.of("java.lang.Integer[]"), "java.lang.Integer[]"),
				Arguments.of(SimpleTypeReference.of("com.example.Test[]"), "com.example.Test[]"));
	}

	@Test
	void nameOfCglibProxy() {
		TypeReference reference = TypeReference.of("com.example.Test$$SpringCGLIB$$0");
		assertThat(reference.getSimpleName()).isEqualTo("Test$$SpringCGLIB$$0");
		assertThat(reference.getEnclosingType()).isNull();
	}

	@Test
	void nameOfNestedCglibProxy() {
		TypeReference reference = TypeReference.of("com.example.Test$Another$$SpringCGLIB$$0");
		assertThat(reference.getSimpleName()).isEqualTo("Another$$SpringCGLIB$$0");
		assertThat(reference.getEnclosingType()).isNotNull();
		assertThat(reference.getEnclosingType().getSimpleName()).isEqualTo("Test");
	}

	@Test
	void typeReferenceInRootPackage() {
		TypeReference type = SimpleTypeReference.of("MyRootClass");
		assertThat(type.getCanonicalName()).isEqualTo("MyRootClass");
		assertThat(type.getPackageName()).isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "com.example.Tes(t", "com.example..Test" })
	void typeReferenceWithInvalidClassName(String invalidClassName) {
		assertThatIllegalStateException().isThrownBy(() -> SimpleTypeReference.of(invalidClassName))
				.withMessageContaining("Invalid class name");
	}

}
