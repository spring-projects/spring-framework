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

import java.util.List;
import java.util.function.Consumer;

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
		assertFieldHint(TypeHint.of(TypeReference.of(String.class))
				.withField("value"), fieldHint -> assertThat(fieldHint.getName()).isEqualTo("value"));
	}

	void assertFieldHint(Builder builder, Consumer<FieldHint> fieldHint) {
		TypeHint hint = builder.build();
		assertThat(hint.fields()).singleElement().satisfies(fieldHint);
		assertThat(hint.constructors()).isEmpty();
		assertThat(hint.methods()).isEmpty();
		assertThat(hint.getMemberCategories()).isEmpty();
	}

	@Test
	void createWithConstructor() {
		List<TypeReference> parameterTypes = TypeReference.listOf(byte[].class, int.class);
		assertConstructorHint(TypeHint.of(TypeReference.of(String.class))
				.withConstructor(parameterTypes, ExecutableMode.INTROSPECT), constructorHint -> {
			assertThat(constructorHint.getParameterTypes()).containsOnlyOnceElementsOf(parameterTypes);
			assertThat(constructorHint.getMode()).isEqualTo(ExecutableMode.INTROSPECT);
		});
	}

	@Test
	void createWithConstructorWithSameConstructorUpdatesEntry() {
		List<TypeReference> parameterTypes = TypeReference.listOf(byte[].class, int.class);
		Builder builder = TypeHint.of(TypeReference.of(String.class))
				.withConstructor(parameterTypes, ExecutableMode.INTROSPECT);
		assertConstructorHint(builder.withConstructor(parameterTypes, ExecutableMode.INVOKE), constructorHint -> {
			assertThat(constructorHint.getParameterTypes()).containsExactlyElementsOf(parameterTypes);
			assertThat(constructorHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
	}

	@Test
	void createWithConstructorAndSameConstructorAppliesExecutableModePrecedence() {
		List<TypeReference> parameterTypes = TypeReference.listOf(byte[].class, int.class);
		Builder builder = TypeHint.of(TypeReference.of(String.class))
				.withConstructor(parameterTypes, ExecutableMode.INVOKE);
		assertConstructorHint(builder.withConstructor(parameterTypes, ExecutableMode.INTROSPECT), constructorHint -> {
			assertThat(constructorHint.getParameterTypes()).containsExactlyElementsOf(parameterTypes);
			assertThat(constructorHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
	}

	void assertConstructorHint(Builder builder, Consumer<ExecutableHint> constructorHint) {
		TypeHint hint = builder.build();
		assertThat(hint.fields()).isEmpty();
		assertThat(hint.constructors()).singleElement().satisfies(constructorHint);
		assertThat(hint.methods()).isEmpty();
		assertThat(hint.getMemberCategories()).isEmpty();
	}

	@Test
	void createWithMethod() {
		List<TypeReference> parameterTypes = List.of(TypeReference.of(char[].class));
		assertMethodHint(TypeHint.of(TypeReference.of(String.class))
				.withMethod("valueOf", parameterTypes, ExecutableMode.INTROSPECT), methodHint -> {
			assertThat(methodHint.getName()).isEqualTo("valueOf");
			assertThat(methodHint.getParameterTypes()).containsExactlyElementsOf(parameterTypes);
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INTROSPECT);
		});
	}

	@Test
	void createWithMethodWithSameMethodUpdatesEntry() {
		List<TypeReference> parameterTypes = TypeReference.listOf(char[].class);
		Builder builder = TypeHint.of(TypeReference.of(String.class))
				.withMethod("valueOf", parameterTypes, ExecutableMode.INTROSPECT);
		assertMethodHint(builder.withMethod("valueOf", parameterTypes, ExecutableMode.INVOKE), methodHint -> {
			assertThat(methodHint.getName()).isEqualTo("valueOf");
			assertThat(methodHint.getParameterTypes()).containsExactlyElementsOf(parameterTypes);
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
	}

	@Test
	void createWithMethodAndSameMethodAppliesExecutableModePrecedence() {
		List<TypeReference> parameterTypes = TypeReference.listOf(char[].class);
		Builder builder = TypeHint.of(TypeReference.of(String.class))
				.withMethod("valueOf", parameterTypes, ExecutableMode.INVOKE);
		assertMethodHint(builder.withMethod("valueOf", parameterTypes, ExecutableMode.INTROSPECT), methodHint -> {
			assertThat(methodHint.getName()).isEqualTo("valueOf");
			assertThat(methodHint.getParameterTypes()).containsExactlyElementsOf(parameterTypes);
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
	}

	void assertMethodHint(Builder builder, Consumer<ExecutableHint> methodHint) {
		TypeHint hint = builder.build();
		assertThat(hint.fields()).isEmpty();
		assertThat(hint.constructors()).isEmpty();
		assertThat(hint.methods()).singleElement().satisfies(methodHint);
		assertThat(hint.getMemberCategories()).isEmpty();
	}


	@Test
	void createWithMemberCategory() {
		TypeHint hint = TypeHint.of(TypeReference.of(String.class))
				.withMembers(MemberCategory.DECLARED_FIELDS).build();
		assertThat(hint.getMemberCategories()).containsOnly(MemberCategory.DECLARED_FIELDS);
	}

	@Test
	void typeHintHasAppropriateToString() {
		TypeHint hint = TypeHint.of(TypeReference.of(String.class)).build();
		assertThat(hint).hasToString("TypeHint[type=java.lang.String]");
	}

	@Test
	void builtWithAppliesMemberCategories() {
		TypeHint.Builder builder = new TypeHint.Builder(TypeReference.of(String.class));
		assertThat(builder.build().getMemberCategories()).isEmpty();
		TypeHint.builtWith(MemberCategory.DECLARED_CLASSES, MemberCategory.DECLARED_FIELDS).accept(builder);
		assertThat(builder.build().getMemberCategories()).containsExactlyInAnyOrder(MemberCategory.DECLARED_CLASSES,
				MemberCategory.DECLARED_FIELDS);
	}

}
