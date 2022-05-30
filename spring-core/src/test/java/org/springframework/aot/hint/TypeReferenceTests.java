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

package org.springframework.aot.hint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TypeReference}.
 *
 * @author Stephane Nicoll
 */
class TypeReferenceTests {

	@Test
	void typeReferenceWithClassName() {
		TypeReference type = TypeReference.of("java.lang.String");
		assertThat(type.getName()).isEqualTo("java.lang.String");
		assertThat(type.getCanonicalName()).isEqualTo("java.lang.String");
		assertThat(type.getPackageName()).isEqualTo("java.lang");
		assertThat(type.getSimpleName()).isEqualTo("String");
		assertThat(type.getEnclosingType()).isNull();
	}

	@Test
	void typeReferenceWithInnerClassName() {
		TypeReference type = TypeReference.of("com.example.Example$Inner");
		assertThat(type.getName()).isEqualTo("com.example.Example$Inner");
		assertThat(type.getCanonicalName()).isEqualTo("com.example.Example.Inner");
		assertThat(type.getPackageName()).isEqualTo("com.example");
		assertThat(type.getSimpleName()).isEqualTo("Inner");
		assertThat(type.getEnclosingType()).satisfies(enclosingType -> {
			assertThat(enclosingType.getCanonicalName()).isEqualTo("com.example.Example");
			assertThat(enclosingType.getPackageName()).isEqualTo("com.example");
			assertThat(enclosingType.getSimpleName()).isEqualTo("Example");
			assertThat(enclosingType.getEnclosingType()).isNull();
		});
	}

	@Test
	void typeReferenceWithNestedInnerClassName() {
		TypeReference type = TypeReference.of("com.example.Example$Inner$Nested");
		assertThat(type.getName()).isEqualTo("com.example.Example$Inner$Nested");
		assertThat(type.getCanonicalName()).isEqualTo("com.example.Example.Inner.Nested");
		assertThat(type.getPackageName()).isEqualTo("com.example");
		assertThat(type.getSimpleName()).isEqualTo("Nested");
		assertThat(type.getEnclosingType()).satisfies(enclosingType -> {
			assertThat(enclosingType.getCanonicalName()).isEqualTo("com.example.Example.Inner");
			assertThat(enclosingType.getPackageName()).isEqualTo("com.example");
			assertThat(enclosingType.getSimpleName()).isEqualTo("Inner");
			assertThat(enclosingType.getEnclosingType()).satisfies(parentEnclosingType -> {
				assertThat(parentEnclosingType.getCanonicalName()).isEqualTo("com.example.Example");
				assertThat(parentEnclosingType.getPackageName()).isEqualTo("com.example");
				assertThat(parentEnclosingType.getSimpleName()).isEqualTo("Example");
				assertThat(parentEnclosingType.getEnclosingType()).isNull();
			});
		});
	}

	@Test
	void equalsWithIdenticalNameIsTrue() {
		assertThat(TypeReference.of(String.class)).isEqualTo(
				TypeReference.of("java.lang.String"));
	}

	@Test
	void equalsWithNonTypeReferenceIsFalse() {
		assertThat(TypeReference.of(String.class)).isNotEqualTo("java.lang.String");
	}

	@Test
	void toStringUsesCanonicalName() {
		assertThat(TypeReference.of(String.class)).hasToString("java.lang.String");
	}

}
