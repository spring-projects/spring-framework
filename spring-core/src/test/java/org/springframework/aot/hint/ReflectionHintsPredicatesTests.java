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


import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ReflectionHintsPredicates}
 *
 * @author Brian Clozel
 */
class ReflectionHintsPredicatesTests {

	private static Constructor<?> privateConstructor;

	private static Constructor<?> publicConstructor;

	private final ReflectionHintsPredicates reflection = new ReflectionHintsPredicates();

	private RuntimeHints runtimeHints;


	@BeforeAll
	static void setupAll() throws Exception {
		privateConstructor = SampleClass.class.getDeclaredConstructor(String.class);
		publicConstructor = SampleClass.class.getConstructor();
	}

	@BeforeEach
	void setup() {
		this.runtimeHints = new RuntimeHints();
	}

	// Reflection on type

	@Test
	void shouldFailForNullType() {
		assertThatThrownBy(() -> reflection.onType((TypeReference) null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void reflectionOnClassShouldMatchIntrospection() {
		this.runtimeHints.reflection().registerType(SampleClass.class, builder -> {
		});
		assertPredicateMatches(reflection.onType(SampleClass.class));
	}

	@Test
	void reflectionOnTypeReferenceShouldMatchIntrospection() {
		this.runtimeHints.reflection().registerType(SampleClass.class, builder -> {
		});
		assertPredicateMatches(reflection.onType(TypeReference.of(SampleClass.class)));
	}

	@Test
	void reflectionOnDifferentClassShouldNotMatchIntrospection() {
		this.runtimeHints.reflection().registerType(Integer.class, builder -> {
		});
		assertPredicateDoesNotMatch(reflection.onType(TypeReference.of(SampleClass.class)));
	}

	@Test
	void typeWithMemberCategoryFailsWithNullCategory() {
		this.runtimeHints.reflection().registerType(SampleClass.class, builder -> builder.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		assertThatThrownBy(() -> reflection.onType(SampleClass.class).withMemberCategory(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void typeWithMemberCategoryMatchesCategory() {
		this.runtimeHints.reflection().registerType(SampleClass.class, builder -> builder.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		assertPredicateMatches(reflection.onType(SampleClass.class).withMemberCategory(MemberCategory.INTROSPECT_PUBLIC_METHODS));
	}

	@Test
	void typeWithMemberCategoryDoesNotMatchOtherCategory() {
		this.runtimeHints.reflection().registerType(SampleClass.class, builder -> builder.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		assertPredicateDoesNotMatch(reflection.onType(SampleClass.class).withMemberCategory(MemberCategory.INVOKE_PUBLIC_METHODS));
	}

	@Test
	void typeWithAnyMemberCategoryFailsWithNullCategories() {
		this.runtimeHints.reflection().registerType(SampleClass.class, builder -> builder.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		assertThatThrownBy(() -> reflection.onType(SampleClass.class).withAnyMemberCategory(new MemberCategory[]{})).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void typeWithAnyMemberCategoryMatchesCategory() {
		this.runtimeHints.reflection().registerType(SampleClass.class, builder -> builder.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS));
		assertPredicateMatches(reflection.onType(SampleClass.class).withAnyMemberCategory(MemberCategory.INTROSPECT_PUBLIC_METHODS));
	}

	@Test
	void typeWithAnyMemberCategoryDoesNotMatchOtherCategory() {
		this.runtimeHints.reflection().registerType(SampleClass.class, builder -> builder.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS));
		assertPredicateDoesNotMatch(reflection.onType(SampleClass.class).withAnyMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS));
	}

	// Reflection on constructor

	@Test
	void constructorIntrospectionMatchesConstructorHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withConstructor(Collections.emptyList(), constructorHint -> {
		}));
		assertPredicateMatches(reflection.onConstructor(publicConstructor).introspect());
	}

	@Test
	void constructorIntrospectionMatchesIntrospectPublicConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS));
		assertPredicateMatches(reflection.onConstructor(publicConstructor).introspect());
	}

	@Test
	void constructorIntrospectionMatchesInvokePublicConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
		assertPredicateMatches(reflection.onConstructor(publicConstructor).introspect());
	}

	@Test
	void constructorIntrospectionMatchesIntrospectDeclaredConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS));
		assertPredicateMatches(reflection.onConstructor(publicConstructor).introspect());
	}

	@Test
	void constructorIntrospectionMatchesInvokeDeclaredConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
		assertPredicateMatches(reflection.onConstructor(publicConstructor).introspect());
	}

	@Test
	void constructorInvocationDoesNotMatchConstructorHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withConstructor(Collections.emptyList(), constructorHint -> {
		}));
		assertPredicateDoesNotMatch(reflection.onConstructor(publicConstructor).invoke());
	}

	@Test
	void constructorInvocationMatchesConstructorInvocationHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withConstructor(Collections.emptyList(), constructorHint -> constructorHint.withMode(ExecutableMode.INVOKE)));
		assertPredicateMatches(reflection.onConstructor(publicConstructor).invoke());
	}

	@Test
	void constructorInvocationDoesNotMatchIntrospectPublicConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS));
		assertPredicateDoesNotMatch(reflection.onConstructor(publicConstructor).invoke());
	}

	@Test
	void constructorInvocationMatchesInvokePublicConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
		assertPredicateMatches(reflection.onConstructor(publicConstructor).invoke());
	}

	@Test
	void constructorInvocationDoesNotMatchIntrospectDeclaredConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS));
		assertPredicateDoesNotMatch(reflection.onConstructor(publicConstructor).invoke());
	}

	@Test
	void constructorInvocationMatchesInvokeDeclaredConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
		assertPredicateMatches(reflection.onConstructor(publicConstructor).invoke());
	}

	@Test
	void privateConstructorIntrospectionMatchesConstructorHint() {
		List<TypeReference> parameterTypes = Collections.singletonList(TypeReference.of(String.class));
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withConstructor(parameterTypes, constructorHint -> {
		}));
		assertPredicateMatches(reflection.onConstructor(privateConstructor).introspect());
	}

	@Test
	void privateConstructorIntrospectionDoesNotMatchIntrospectPublicConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS));
		assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).introspect());
	}

	@Test
	void privateConstructorIntrospectionDoesNotMatchInvokePublicConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
		assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).introspect());
	}

	@Test
	void privateConstructorIntrospectionMatchesIntrospectDeclaredConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS));
		assertPredicateMatches(reflection.onConstructor(privateConstructor).introspect());
	}

	@Test
	void privateConstructorIntrospectionMatchesInvokeDeclaredConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
		assertPredicateMatches(reflection.onConstructor(privateConstructor).introspect());
	}

	@Test
	void privateConstructorInvocationDoesNotMatchConstructorHint() {
		List<TypeReference> parameterTypes = Collections.singletonList(TypeReference.of(String.class));
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withConstructor(parameterTypes, constructorHint -> {
		}));
		assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).invoke());
	}

	@Test
	void privateConstructorInvocationMatchesConstructorInvocationHint() {
		List<TypeReference> parameterTypes = Collections.singletonList(TypeReference.of(String.class));
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withConstructor(parameterTypes, constructorHint -> constructorHint.withMode(ExecutableMode.INVOKE)));
		assertPredicateMatches(reflection.onConstructor(privateConstructor).invoke());
	}

	@Test
	void privateConstructorInvocationDoesNotMatchIntrospectPublicConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS));
		assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).invoke());
	}

	@Test
	void privateConstructorInvocationDoesNotMatchInvokePublicConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
		assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).invoke());
	}

	@Test
	void privateConstructorInvocationDoesNotMatchIntrospectDeclaredConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS));
		assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).invoke());
	}

	@Test
	void privateConstructorInvocationMatchesInvokeDeclaredConstructors() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
		assertPredicateMatches(reflection.onConstructor(privateConstructor).invoke());
	}

	// Reflection on method

	@Test
	void methodIntrospectionMatchesMethodHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMethod("publicMethod", Collections.emptyList(), methodHint -> {
		}));
		assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
	}

	@Test
	void methodIntrospectionMatchesIntrospectPublicMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
	}

	@Test
	void methodIntrospectionMatchesInvokePublicMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
		assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
	}

	@Test
	void methodIntrospectionMatchesIntrospectDeclaredMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_METHODS));
		assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
	}

	@Test
	void methodIntrospectionMatchesInvokeDeclaredMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));
		assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
	}

	@Test
	void methodInvocationDoesNotMatchMethodHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMethod("publicMethod", Collections.emptyList(), methodHint -> {
		}));
		assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
	}

	@Test
	void methodInvocationMatchesMethodInvocationHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMethod("publicMethod", Collections.emptyList(), methodHint -> methodHint.withMode(ExecutableMode.INVOKE)));
		assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
	}

	@Test
	void methodInvocationDoesNotMatchIntrospectPublicMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
	}

	@Test
	void methodInvocationMatchesInvokePublicMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
		assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
	}

	@Test
	void methodInvocationDoesNotMatchIntrospectDeclaredMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_METHODS));
		assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
	}

	@Test
	void methodInvocationMatchesInvokeDeclaredMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));
		assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
	}

	@Test
	void privateMethodIntrospectionMatchesMethodHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMethod("privateMethod", Collections.emptyList(), methodHint -> {
		}));
		assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
	}

	@Test
	void privateMethodIntrospectionDoesNotMatchIntrospectPublicMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
	}

	@Test
	void privateMethodIntrospectionDoesNotMatchInvokePublicMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
		assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
	}

	@Test
	void privateMethodIntrospectionMatchesIntrospectDeclaredMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_METHODS));
		assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
	}

	@Test
	void privateMethodIntrospectionMatchesInvokeDeclaredMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));
		assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
	}

	@Test
	void privateMethodInvocationDoesNotMatchMethodHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMethod("privateMethod", Collections.emptyList(), methodHint -> {
		}));
		assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
	}

	@Test
	void privateMethodInvocationMatchesMethodInvocationHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMethod("privateMethod", Collections.emptyList(), methodHint -> methodHint.withMode(ExecutableMode.INVOKE)));
		assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
	}

	@Test
	void privateMethodInvocationDoesNotMatchIntrospectPublicMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
	}

	@Test
	void privateMethodInvocationDoesNotMatchInvokePublicMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
		assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
	}

	@Test
	void privateMethodInvocationDoesNotMatchIntrospectDeclaredMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_METHODS));
		assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
	}

	@Test
	void privateMethodInvocationMatchesInvokeDeclaredMethods() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));
		assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
	}

	// Reflection on field

	@Test
	void shouldFailForMissingField() {
		assertThatThrownBy(() -> reflection.onField(SampleClass.class, "missingField")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void fieldReflectionMatchesFieldHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withField("publicField", fieldHint -> {
		}));
		assertPredicateMatches(reflection.onField(SampleClass.class, "publicField"));
	}

	@Test
	void fieldWriteReflectionDoesNotMatchFieldHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withField("publicField", fieldHint -> {
		}));
		assertPredicateDoesNotMatch(reflection.onField(SampleClass.class, "publicField").allowWrite());
	}

	@Test
	void fieldUnsafeReflectionDoesNotMatchFieldHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withField("publicField", fieldHint -> {
		}));
		assertPredicateDoesNotMatch(reflection.onField(SampleClass.class, "publicField").allowUnsafeAccess());
	}

	@Test
	void fieldWriteReflectionMatchesFieldHintWithWrite() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint ->
				typeHint.withField("publicField", fieldHint -> fieldHint.allowWrite(true)));
		assertPredicateMatches(reflection.onField(SampleClass.class, "publicField").allowWrite());
	}

	@Test
	void fieldUnsafeReflectionMatchesFieldHintWithUnsafe() {
		this.runtimeHints.reflection().registerType(SampleClass.class,
				typeHint -> typeHint.withField("publicField", fieldHint -> fieldHint.allowUnsafeAccess(true)));
		assertPredicateMatches(reflection.onField(SampleClass.class, "publicField").allowUnsafeAccess());
	}

	@Test
	void fieldReflectionMatchesPublicFieldsHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.PUBLIC_FIELDS));
		assertPredicateMatches(reflection.onField(SampleClass.class, "publicField"));
	}

	@Test
	void fieldReflectionMatchesDeclaredFieldsHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.DECLARED_FIELDS));
		assertPredicateMatches(reflection.onField(SampleClass.class, "publicField"));
	}

	@Test
	void privateFieldReflectionMatchesFieldHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withField("privateField", fieldHint -> {
		}));
		assertPredicateMatches(reflection.onField(SampleClass.class, "privateField"));
	}

	@Test
	void privateFieldReflectionDoesNotMatchPublicFieldsHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.PUBLIC_FIELDS));
		assertPredicateDoesNotMatch(reflection.onField(SampleClass.class, "privateField"));
	}

	@Test
	void privateFieldReflectionMatchesDeclaredFieldsHint() {
		this.runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.DECLARED_FIELDS));
		assertPredicateMatches(reflection.onField(SampleClass.class, "privateField"));
	}


	private void assertPredicateMatches(Predicate<RuntimeHints> predicate) {
		assertThat(predicate).accepts(this.runtimeHints);
	}

	private void assertPredicateDoesNotMatch(Predicate<RuntimeHints> predicate) {
		assertThat(predicate).rejects(this.runtimeHints);
	}


	static class SampleClass {

		private String privateField;

		public String publicField;

		public SampleClass() {

		}

		private SampleClass(String message) {

		}

		public void publicMethod() {

		}

		private void privateMethod() {

		}

	}

}
