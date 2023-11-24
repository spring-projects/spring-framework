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

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.aot.hint.TypeReference;
import org.springframework.javapoet.ClassName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GeneratedTypeReference}.
 *
 * @author Stephane Nicoll
 */
class GeneratedTypeReferenceTests {


	@ParameterizedTest
	@MethodSource("reflectionTargetNames")
	void hasSuitableReflectionTargetName(TypeReference typeReference, String binaryName) {
		assertThat(typeReference.getName()).isEqualTo(binaryName);
	}

	static Stream<Arguments> reflectionTargetNames() {
		return Stream.of(
				Arguments.of(GeneratedTypeReference.of(ClassName.get("com.example", "Test")), "com.example.Test"),
				Arguments.of(GeneratedTypeReference.of(ClassName.get("com.example", "Test", "Inner")), "com.example.Test$Inner"));
	}

	@Test
	void createWithClassName() {
		GeneratedTypeReference typeReference = GeneratedTypeReference.of(
				ClassName.get("com.example", "Test"));
		assertThat(typeReference.getPackageName()).isEqualTo("com.example");
		assertThat(typeReference.getSimpleName()).isEqualTo("Test");
		assertThat(typeReference.getCanonicalName()).isEqualTo("com.example.Test");
		assertThat(typeReference.getEnclosingType()).isNull();
	}

	@Test
	void createWithClassNameAndParent() {
		GeneratedTypeReference typeReference = GeneratedTypeReference.of(
				ClassName.get("com.example", "Test").nestedClass("Nested"));
		assertThat(typeReference.getPackageName()).isEqualTo("com.example");
		assertThat(typeReference.getSimpleName()).isEqualTo("Nested");
		assertThat(typeReference.getCanonicalName()).isEqualTo("com.example.Test.Nested");
		assertThat(typeReference.getEnclosingType()).satisfies(parentTypeReference -> {
			assertThat(parentTypeReference.getPackageName()).isEqualTo("com.example");
			assertThat(parentTypeReference.getSimpleName()).isEqualTo("Test");
			assertThat(parentTypeReference.getCanonicalName()).isEqualTo("com.example.Test");
			assertThat(parentTypeReference.getEnclosingType()).isNull();
		});
	}

	@Test
	void nameOfCglibProxy() {
		TypeReference reference = GeneratedTypeReference.of(
				ClassName.get("com.example", "Test$$SpringCGLIB$$0"));
		assertThat(reference.getSimpleName()).isEqualTo("Test$$SpringCGLIB$$0");
		assertThat(reference.getEnclosingType()).isNull();
	}

	@Test
	void nameOfNestedCglibProxy() {
		TypeReference reference = GeneratedTypeReference.of(
				ClassName.get("com.example", "Test").nestedClass("Another$$SpringCGLIB$$0"));
		assertThat(reference.getSimpleName()).isEqualTo("Another$$SpringCGLIB$$0");
		assertThat(reference.getEnclosingType()).isNotNull();
		assertThat(reference.getEnclosingType().getSimpleName()).isEqualTo("Test");
	}

	@Test
	void equalsWithIdenticalCanonicalNameIsTrue() {
		assertThat(GeneratedTypeReference.of(ClassName.get("java.lang", "String")))
				.isEqualTo(TypeReference.of(String.class));
	}

}
