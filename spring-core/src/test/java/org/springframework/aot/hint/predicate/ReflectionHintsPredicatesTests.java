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

package org.springframework.aot.hint.predicate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ReflectionHintsPredicates}
 *
 * @author Brian Clozel
 */
@SuppressWarnings("removal")
class ReflectionHintsPredicatesTests {

	private static Constructor<?> privateConstructor;

	private static Constructor<?> publicConstructor;

	@SuppressWarnings("unused")
	private static Method publicMethod;

	@SuppressWarnings("unused")
	private static Field publicField;

	private final ReflectionHintsPredicates reflection = new ReflectionHintsPredicates();

	private final RuntimeHints runtimeHints = new RuntimeHints();


	@BeforeAll
	static void setupAll() throws Exception {
		privateConstructor = SampleClass.class.getDeclaredConstructor(String.class);
		publicConstructor = SampleClass.class.getConstructor();
		publicMethod = SampleClass.class.getMethod("publicMethod");
		publicField = SampleClass.class.getField("publicField");
	}

	@Nested
	class ReflectionOnType {

		@Test
		void shouldFailForNullType() {
			assertThatIllegalArgumentException().isThrownBy(() -> reflection.onType((TypeReference) null));
		}

		@Test
		void reflectionOnClassShouldMatchIntrospection() {
			runtimeHints.reflection().registerType(SampleClass.class);
			assertPredicateMatches(reflection.onType(SampleClass.class));
		}

		@Test
		void reflectionOnTypeReferenceShouldMatchIntrospection() {
			runtimeHints.reflection().registerType(SampleClass.class);
			assertPredicateMatches(reflection.onType(TypeReference.of(SampleClass.class)));
		}

		@Test
		void reflectionOnDifferentClassShouldNotMatchIntrospection() {
			runtimeHints.reflection().registerType(Integer.class);
			assertPredicateDoesNotMatch(reflection.onType(TypeReference.of(SampleClass.class)));
		}

		@Test
		void typeWithMemberCategoryFailsWithNullCategory() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
			assertThatIllegalArgumentException().isThrownBy(() ->
					reflection.onType(SampleClass.class).withMemberCategory(null));
		}

		@Test
		void typeWithMemberCategoryMatchesCategory() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
			assertPredicateMatches(reflection.onType(SampleClass.class)
					.withMemberCategory(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		}

		@Test
		void typeWithMemberCategoryDoesNotMatchOtherCategory() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
			assertPredicateDoesNotMatch(reflection.onType(SampleClass.class)
					.withMemberCategory(MemberCategory.INVOKE_PUBLIC_METHODS));
		}

		@Test
		void typeWithMemberCategoriesMatchesCategories() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS,
					MemberCategory.INTROSPECT_PUBLIC_METHODS);
			assertPredicateMatches(reflection.onType(SampleClass.class)
					.withMemberCategories(MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS, MemberCategory.INTROSPECT_PUBLIC_METHODS));
		}

		@Test
		void typeWithMemberCategoriesDoesNotMatchMissingCategory() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
			assertPredicateDoesNotMatch(reflection.onType(SampleClass.class)
					.withMemberCategories(MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS, MemberCategory.INTROSPECT_PUBLIC_METHODS));
		}

		@Test
		void typeWithAnyMemberCategoryFailsWithNullCategories() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
			assertThatIllegalArgumentException().isThrownBy(() ->
					reflection.onType(SampleClass.class).withAnyMemberCategory(new MemberCategory[0]));
		}

		@Test
		void typeWithAnyMemberCategoryMatchesCategory() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_PUBLIC_METHODS,
					MemberCategory.INVOKE_PUBLIC_METHODS);
			assertPredicateMatches(reflection.onType(SampleClass.class)
					.withAnyMemberCategory(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		}

		@Test
		void typeWithAnyMemberCategoryDoesNotMatchOtherCategory() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_PUBLIC_METHODS,
					MemberCategory.INVOKE_PUBLIC_METHODS);
			assertPredicateDoesNotMatch(reflection.onType(SampleClass.class)
					.withAnyMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS));
		}

	}

	@Nested
	class ReflectionOnConstructor {

		@Test
		void constructorInvocationDoesNotMatchConstructorHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.
					withConstructor(Collections.emptyList(), ExecutableMode.INTROSPECT));
			assertPredicateDoesNotMatch(reflection.onConstructorInvocation(publicConstructor));
		}

		@Test
		void constructorInvocationMatchesConstructorInvocationHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.
					withConstructor(Collections.emptyList(), ExecutableMode.INVOKE));
			assertPredicateMatches(reflection.onConstructorInvocation(publicConstructor));
		}

		@Test
		void constructorInvocationDoesNotMatchIntrospectPublicConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS);
			assertPredicateDoesNotMatch(reflection.onConstructorInvocation(publicConstructor));
		}

		@Test
		void constructorInvocationMatchesInvokePublicConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
			assertPredicateMatches(reflection.onConstructorInvocation(publicConstructor));
		}

		@Test
		void constructorInvocationDoesNotMatchIntrospectDeclaredConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS);
			assertPredicateDoesNotMatch(reflection.onConstructorInvocation(publicConstructor));
		}

		@Test
		void constructorInvocationMatchesInvokeDeclaredConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			assertPredicateMatches(reflection.onConstructorInvocation(publicConstructor));
		}

		@Test
		void privateConstructorInvocationDoesNotMatchConstructorHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint ->
					typeHint.withConstructor(TypeReference.listOf(String.class), ExecutableMode.INTROSPECT));
			assertPredicateDoesNotMatch(reflection.onConstructorInvocation(privateConstructor));
		}

		@Test
		void privateConstructorInvocationMatchesConstructorInvocationHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint ->
					typeHint.withConstructor(TypeReference.listOf(String.class), ExecutableMode.INVOKE));
			assertPredicateMatches(reflection.onConstructorInvocation(privateConstructor));
		}

		@Test
		void privateConstructorInvocationDoesNotMatchIntrospectPublicConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS);
			assertPredicateDoesNotMatch(reflection.onConstructorInvocation(privateConstructor));
		}

		@Test
		void privateConstructorInvocationDoesNotMatchInvokePublicConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
			assertPredicateDoesNotMatch(reflection.onConstructorInvocation(privateConstructor));
		}

		@Test
		void privateConstructorInvocationDoesNotMatchIntrospectDeclaredConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS);
			assertPredicateDoesNotMatch(reflection.onConstructorInvocation(privateConstructor));
		}

		@Test
		void privateConstructorInvocationMatchesInvokeDeclaredConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			assertPredicateMatches(reflection.onConstructorInvocation(privateConstructor));
		}

	}

	@Nested
	class ReflectionOnMethod {

		@Test
		void methodIntrospectionMatchesTypeHint() {
			runtimeHints.reflection().registerType(SampleClass.class);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
		}

		@Test
		void methodIntrospectionMatchesMethodHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint ->
					typeHint.withMethod("publicMethod", Collections.emptyList(), ExecutableMode.INTROSPECT));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
		}

		@Test
		void methodIntrospectionFailsForUnknownType() {
			assertThatThrownBy(() -> reflection.onMethod("com.example.DoesNotExist", "publicMethod").introspect())
					.isInstanceOf(ClassNotFoundException.class);
		}

		@Test
		void methodIntrospectionMatchesIntrospectPublicMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
		}

		@Test
		void methodIntrospectionMatchesInvokePublicMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INVOKE_PUBLIC_METHODS);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
		}

		@Test
		void methodInvocationDoesNotMatchMethodHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint ->
					typeHint.withMethod("publicMethod", Collections.emptyList(), ExecutableMode.INTROSPECT));
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void methodInvocationMatchesMethodInvocationHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint ->
					typeHint.withMethod("publicMethod", Collections.emptyList(), ExecutableMode.INVOKE));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void methodInvocationDoesNotMatchIntrospectPublicMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void methodInvocationMatchesInvokePublicMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INVOKE_PUBLIC_METHODS);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void methodInvocationDoesNotMatchIntrospectDeclaredMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_DECLARED_METHODS);
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void methodInvocationDoesNotMatchInvokeDeclaredMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INVOKE_DECLARED_METHODS);
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void privateMethodIntrospectionMatchesTypeHint() {
			runtimeHints.reflection().registerType(SampleClass.class);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
		}

		@Test
		void privateMethodIntrospectionMatchesMethodHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint ->
					typeHint.withMethod("privateMethod", Collections.emptyList(), ExecutableMode.INTROSPECT));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
		}

		@Test
		void privateMethodIntrospectionMatchesIntrospectDeclaredMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_DECLARED_METHODS);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
		}

		@Test
		void privateMethodIntrospectionMatchesInvokeDeclaredMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INVOKE_DECLARED_METHODS);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
		}

		@Test
		void privateMethodInvocationDoesNotMatchMethodHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint ->
					typeHint.withMethod("privateMethod", Collections.emptyList(), ExecutableMode.INTROSPECT));
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

		@Test
		void privateMethodInvocationMatchesMethodInvocationHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint ->
					typeHint.withMethod("privateMethod", Collections.emptyList(), ExecutableMode.INVOKE));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

		@Test
		void privateMethodInvocationDoesNotMatchIntrospectPublicMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

		@Test
		void privateMethodInvocationDoesNotMatchInvokePublicMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INVOKE_PUBLIC_METHODS);
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

		@Test
		void privateMethodInvocationDoesNotMatchIntrospectDeclaredMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INTROSPECT_DECLARED_METHODS);
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

		@Test
		void privateMethodInvocationMatchesInvokeDeclaredMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.INVOKE_DECLARED_METHODS);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

	}

	@Nested
	class ReflectionOnField {

		@Test
		void shouldFailForMissingField() {
			assertThatIllegalArgumentException().isThrownBy(() -> reflection.onField(SampleClass.class, "missingField"));
		}

		@Test
		void shouldFailForUnknownClass() {
			assertThatThrownBy(() -> reflection.onFieldAccess("com.example.DoesNotExist", "missingField"))
					.isInstanceOf(ClassNotFoundException.class);
		}

		@Test
		void publicFieldAccessMatchesFieldHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withField("publicField"));
			assertPredicateMatches(reflection.onField(SampleClass.class, "publicField"));
		}

		@Test
		void publicFieldAccessMatchesPublicFieldsHint() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.PUBLIC_FIELDS);
			assertPredicateMatches(reflection.onField(SampleClass.class, "publicField"));
		}

		@Test
		void publicFieldAccessMatchesAccessPublicFieldsHint() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.ACCESS_PUBLIC_FIELDS);
			assertPredicateMatches(reflection.onField(SampleClass.class, "publicField"));
		}

		@Test
		void fieldAccessDoesNotMatchTypeHint() {
			runtimeHints.reflection().registerType(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onField(SampleClass.class, "publicField"));
		}

		@Test
		void privateFieldAccessDoesNotMatchTypeHint() {
			runtimeHints.reflection().registerType(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onField(SampleClass.class, "privateField"));
		}

		@Test
		void privateFieldAccessMatchesFieldHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withField("privateField"));
			assertPredicateMatches(reflection.onField(SampleClass.class, "privateField"));
		}

		@Test
		void privateFieldAccessMatchesDeclaredFieldsHint() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.DECLARED_FIELDS);
			assertPredicateMatches(reflection.onField(SampleClass.class, "privateField"));
		}

		@Test
		void privateFieldAccessMatchesAccessDeclaredFieldsHint() {
			runtimeHints.reflection().registerType(SampleClass.class, MemberCategory.ACCESS_DECLARED_FIELDS);
			assertPredicateMatches(reflection.onField(SampleClass.class, "privateField"));
		}

	}

	private void assertPredicateMatches(Predicate<RuntimeHints> predicate) {
		assertThat(predicate).accepts(this.runtimeHints);
	}

	private void assertPredicateDoesNotMatch(Predicate<RuntimeHints> predicate) {
		assertThat(predicate).rejects(this.runtimeHints);
	}


	@SuppressWarnings("unused")
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
