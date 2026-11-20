/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
