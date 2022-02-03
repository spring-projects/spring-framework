/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.TypeHint.Builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link TypeHint}.
 *
 * @author Stephane Nicoll
 */
class TypeHintTests {

	@Test
	void createWithNullTypeReference() {
		assertThatIllegalArgumentException().isThrownBy(() -> TypeHint.of(null));
	}

	@Test
	void createWithType() {
		TypeHint hint = TypeHint.of(TypeReference.of(String.class)).build();
		assertThat(hint).isNotNull();
		assertThat(hint.getType().getCanonicalName()).isEqualTo("java.lang.String");
	}

	@Test
	void createWithTypeAndReachableType() {
		TypeHint hint = TypeHint.of(TypeReference.of(String.class))
				.onReachableType(TypeReference.of("com.example.Test")).build();
		assertThat(hint).isNotNull();
		assertThat(hint.getReachableType()).isNotNull();
		assertThat(hint.getReachableType().getCanonicalName()).isEqualTo("com.example.Test");
	}

	@Test
	void createWithField() {
		TypeHint hint = TypeHint.of(TypeReference.of(String.class))
				.withField("value", fieldHint -> fieldHint.allowWrite(true)).build();
		assertThat(hint.fields()).singleElement().satisfies(fieldHint -> {
			assertThat(fieldHint.getName()).isEqualTo("value");
			assertThat(fieldHint.isAllowWrite()).isTrue();
			assertThat(fieldHint.isAllowUnsafeAccess()).isFalse();
		});
	}

	@Test
	void createWithFieldReuseBuilder() {
		Builder builder = TypeHint.of(TypeReference.of(String.class));
		builder.withField("value", fieldHint -> fieldHint.allowUnsafeAccess(true));
		builder.withField("value", fieldHint -> {
			fieldHint.allowWrite(true);
			fieldHint.allowUnsafeAccess(false);
		});
		TypeHint hint = builder.build();
		assertThat(hint.fields()).singleElement().satisfies(fieldHint -> {
			assertThat(fieldHint.getName()).isEqualTo("value");
			assertThat(fieldHint.isAllowWrite()).isTrue();
			assertThat(fieldHint.isAllowUnsafeAccess()).isFalse();
		});
	}

	@Test
	void createWithConstructor() {
		List<TypeReference> parameterTypes = List.of(TypeReference.of(byte[].class), TypeReference.of(int.class));
		TypeHint hint = TypeHint.of(TypeReference.of(String.class)).withConstructor(parameterTypes,
				constructorHint -> constructorHint.withMode(ExecutableMode.INVOKE)).build();
		assertThat(hint.constructors()).singleElement().satisfies(constructorHint -> {
			assertThat(constructorHint.getParameterTypes()).containsOnlyOnceElementsOf(parameterTypes);
			assertThat(constructorHint.getModes()).containsOnly(ExecutableMode.INVOKE);
		});
	}

	@Test
	void createConstructorReuseBuilder() {
		List<TypeReference> parameterTypes = List.of(TypeReference.of(byte[].class), TypeReference.of(int.class));
		Builder builder = TypeHint.of(TypeReference.of(String.class)).withConstructor(parameterTypes,
				constructorHint -> constructorHint.withMode(ExecutableMode.INVOKE));
		TypeHint hint = builder.withConstructor(parameterTypes, constructorHint ->
				constructorHint.withMode(ExecutableMode.INTROSPECT)).build();
		assertThat(hint.constructors()).singleElement().satisfies(constructorHint -> {
			assertThat(constructorHint.getParameterTypes()).containsExactlyElementsOf(parameterTypes);
			assertThat(constructorHint.getModes()).containsOnly(ExecutableMode.INVOKE, ExecutableMode.INTROSPECT);
		});
	}

	@Test
	void createWithMethod() {
		List<TypeReference> parameterTypes = List.of(TypeReference.of(char[].class));
		TypeHint hint = TypeHint.of(TypeReference.of(String.class)).withMethod("valueOf", parameterTypes,
				methodHint -> methodHint.withMode(ExecutableMode.INVOKE)).build();
		assertThat(hint.methods()).singleElement().satisfies(methodHint -> {
			assertThat(methodHint.getName()).isEqualTo("valueOf");
			assertThat(methodHint.getParameterTypes()).containsExactlyElementsOf(parameterTypes);
			assertThat(methodHint.getModes()).containsOnly(ExecutableMode.INVOKE);
		});
	}

	@Test
	void createWithMethodReuseBuilder() {
		List<TypeReference> parameterTypes = List.of(TypeReference.of(char[].class));
		Builder builder = TypeHint.of(TypeReference.of(String.class)).withMethod("valueOf", parameterTypes,
				methodHint -> methodHint.withMode(ExecutableMode.INVOKE));
		TypeHint hint = builder.withMethod("valueOf", parameterTypes,
				methodHint -> methodHint.setModes(ExecutableMode.INTROSPECT)).build();
		assertThat(hint.methods()).singleElement().satisfies(methodHint -> {
			assertThat(methodHint.getName()).isEqualTo("valueOf");
			assertThat(methodHint.getParameterTypes()).containsExactlyElementsOf(parameterTypes);
			assertThat(methodHint.getModes()).containsOnly(ExecutableMode.INTROSPECT);
		});
	}

	@Test
	void createWithMemberCategory() {
		TypeHint hint = TypeHint.of(TypeReference.of(String.class))
				.withMembers(MemberCategory.DECLARED_FIELDS).build();
		assertThat(hint.getMemberCategories()).containsOnly(MemberCategory.DECLARED_FIELDS);
	}

}
