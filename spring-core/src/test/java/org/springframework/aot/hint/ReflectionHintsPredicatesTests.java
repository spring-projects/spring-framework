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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ReflectionHintsPredicates}
 *
 * @author Brian Clozel
 */
class ReflectionHintsPredicatesTests {

	private static Constructor<?> privateConstructor;

	private static Constructor<?> publicConstructor;

	private final ReflectionHintsPredicates reflection = new ReflectionHintsPredicates();

	private final RuntimeHints runtimeHints = new RuntimeHints();


	@BeforeAll
	static void setupAll() throws Exception {
		privateConstructor = SampleClass.class.getDeclaredConstructor(String.class);
		publicConstructor = SampleClass.class.getConstructor();
	}

	@Nested
	class ReflectionOnType {

		@Test
		void shouldFailForNullType() {
			assertThatIllegalArgumentException().isThrownBy(() -> reflection.onType((TypeReference) null));
		}

		@Test
		void reflectionOnClassShouldMatchIntrospection() {
			runtimeHints.reflection().registerType(SampleClass.class, builder -> {
			});
			assertPredicateMatches(reflection.onType(SampleClass.class));
		}

		@Test
		void reflectionOnTypeReferenceShouldMatchIntrospection() {
			runtimeHints.reflection().registerType(SampleClass.class, builder -> {
			});
			assertPredicateMatches(reflection.onType(TypeReference.of(SampleClass.class)));
		}

		@Test
		void reflectionOnDifferentClassShouldNotMatchIntrospection() {
			runtimeHints.reflection().registerType(Integer.class, builder -> {
			});
			assertPredicateDoesNotMatch(reflection.onType(TypeReference.of(SampleClass.class)));
		}

		@Test
		void typeWithMemberCategoryFailsWithNullCategory() {
			runtimeHints.reflection().registerType(SampleClass.class, builder -> builder.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
			assertThatIllegalArgumentException().isThrownBy(() -> reflection.onType(SampleClass.class).withMemberCategory(null));
		}

		@Test
		void typeWithMemberCategoryMatchesCategory() {
			runtimeHints.reflection().registerType(SampleClass.class, builder -> builder.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
			assertPredicateMatches(reflection.onType(SampleClass.class).withMemberCategory(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		}

		@Test
		void typeWithMemberCategoryDoesNotMatchOtherCategory() {
			runtimeHints.reflection().registerType(SampleClass.class, builder -> builder.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
			assertPredicateDoesNotMatch(reflection.onType(SampleClass.class).withMemberCategory(MemberCategory.INVOKE_PUBLIC_METHODS));
		}

		@Test
		void typeWithMemberCategoriesMatchesCategories() {
			runtimeHints.reflection().registerType(SampleClass.class, builder ->
					builder.withMembers(MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS, MemberCategory.INTROSPECT_PUBLIC_METHODS));
			assertPredicateMatches(reflection.onType(SampleClass.class)
					.withMemberCategories(MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS, MemberCategory.INTROSPECT_PUBLIC_METHODS));
		}

		@Test
		void typeWithMemberCategoriesDoesNotMatchMissingCategory() {
			runtimeHints.reflection().registerType(SampleClass.class, builder -> builder.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
			assertPredicateDoesNotMatch(reflection.onType(SampleClass.class)
					.withMemberCategories(MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS, MemberCategory.INTROSPECT_PUBLIC_METHODS));
		}

		@Test
		void typeWithAnyMemberCategoryFailsWithNullCategories() {
			runtimeHints.reflection().registerType(SampleClass.class, builder -> builder.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
			assertThatIllegalArgumentException().isThrownBy(() -> reflection.onType(SampleClass.class).withAnyMemberCategory(new MemberCategory[0]));
		}

		@Test
		void typeWithAnyMemberCategoryMatchesCategory() {
			runtimeHints.reflection().registerType(SampleClass.class, builder -> builder.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS));
			assertPredicateMatches(reflection.onType(SampleClass.class).withAnyMemberCategory(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		}

		@Test
		void typeWithAnyMemberCategoryDoesNotMatchOtherCategory() {
			runtimeHints.reflection().registerType(SampleClass.class, builder -> builder.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS));
			assertPredicateDoesNotMatch(reflection.onType(SampleClass.class).withAnyMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS));
		}

	}

	@Nested
	class ReflectionOnConstructor {

		@Test
		void constructorIntrospectionMatchesConstructorHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withConstructor(Collections.emptyList(), constructorHint -> {
			}));
			assertPredicateMatches(reflection.onConstructor(publicConstructor).introspect());
		}

		@Test
		void constructorIntrospectionMatchesIntrospectPublicConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS));
			assertPredicateMatches(reflection.onConstructor(publicConstructor).introspect());
		}

		@Test
		void constructorIntrospectionMatchesInvokePublicConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
			assertPredicateMatches(reflection.onConstructor(publicConstructor).introspect());
		}

		@Test
		void constructorIntrospectionMatchesIntrospectDeclaredConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS));
			assertPredicateMatches(reflection.onConstructor(publicConstructor).introspect());
		}

		@Test
		void constructorIntrospectionMatchesInvokeDeclaredConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
			assertPredicateMatches(reflection.onConstructor(publicConstructor).introspect());
		}

		@Test
		void constructorInvocationDoesNotMatchConstructorHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withConstructor(Collections.emptyList(), constructorHint -> {
			}));
			assertPredicateDoesNotMatch(reflection.onConstructor(publicConstructor).invoke());
		}

		@Test
		void constructorInvocationMatchesConstructorInvocationHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withConstructor(Collections.emptyList(), constructorHint -> constructorHint.withMode(ExecutableMode.INVOKE)));
			assertPredicateMatches(reflection.onConstructor(publicConstructor).invoke());
		}

		@Test
		void constructorInvocationDoesNotMatchIntrospectPublicConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS));
			assertPredicateDoesNotMatch(reflection.onConstructor(publicConstructor).invoke());
		}

		@Test
		void constructorInvocationMatchesInvokePublicConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
			assertPredicateMatches(reflection.onConstructor(publicConstructor).invoke());
		}

		@Test
		void constructorInvocationDoesNotMatchIntrospectDeclaredConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS));
			assertPredicateDoesNotMatch(reflection.onConstructor(publicConstructor).invoke());
		}

		@Test
		void constructorInvocationMatchesInvokeDeclaredConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
			assertPredicateMatches(reflection.onConstructor(publicConstructor).invoke());
		}

		@Test
		void privateConstructorIntrospectionMatchesConstructorHint() {
			List<TypeReference> parameterTypes = Collections.singletonList(TypeReference.of(String.class));
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withConstructor(parameterTypes, constructorHint -> {
			}));
			assertPredicateMatches(reflection.onConstructor(privateConstructor).introspect());
		}

		@Test
		void privateConstructorIntrospectionDoesNotMatchIntrospectPublicConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS));
			assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).introspect());
		}

		@Test
		void privateConstructorIntrospectionDoesNotMatchInvokePublicConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
			assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).introspect());
		}

		@Test
		void privateConstructorIntrospectionMatchesIntrospectDeclaredConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS));
			assertPredicateMatches(reflection.onConstructor(privateConstructor).introspect());
		}

		@Test
		void privateConstructorIntrospectionMatchesInvokeDeclaredConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
			assertPredicateMatches(reflection.onConstructor(privateConstructor).introspect());
		}

		@Test
		void privateConstructorInvocationDoesNotMatchConstructorHint() {
			List<TypeReference> parameterTypes = Collections.singletonList(TypeReference.of(String.class));
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withConstructor(parameterTypes, constructorHint -> {
			}));
			assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).invoke());
		}

		@Test
		void privateConstructorInvocationMatchesConstructorInvocationHint() {
			List<TypeReference> parameterTypes = Collections.singletonList(TypeReference.of(String.class));
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withConstructor(parameterTypes, constructorHint -> constructorHint.withMode(ExecutableMode.INVOKE)));
			assertPredicateMatches(reflection.onConstructor(privateConstructor).invoke());
		}

		@Test
		void privateConstructorInvocationDoesNotMatchIntrospectPublicConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS));
			assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).invoke());
		}

		@Test
		void privateConstructorInvocationDoesNotMatchInvokePublicConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
			assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).invoke());
		}

		@Test
		void privateConstructorInvocationDoesNotMatchIntrospectDeclaredConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS));
			assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).invoke());
		}

		@Test
		void privateConstructorInvocationMatchesInvokeDeclaredConstructors() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
			assertPredicateMatches(reflection.onConstructor(privateConstructor).invoke());
		}

	}

	@Nested
	class ReflectionOnMethod {

		@Test
		void methodIntrospectionMatchesMethodHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMethod("publicMethod", Collections.emptyList(), methodHint -> {
			}));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
		}

		@Test
		void methodIntrospectionMatchesIntrospectPublicMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
		}

		@Test
		void methodIntrospectionMatchesInvokePublicMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
		}

		@Test
		void methodIntrospectionMatchesIntrospectDeclaredMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_METHODS));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
		}

		@Test
		void methodIntrospectionMatchesInvokeDeclaredMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
		}

		@Test
		void methodInvocationDoesNotMatchMethodHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMethod("publicMethod", Collections.emptyList(), methodHint -> {
			}));
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void methodInvocationMatchesMethodInvocationHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMethod("publicMethod", Collections.emptyList(), methodHint -> methodHint.withMode(ExecutableMode.INVOKE)));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void methodInvocationDoesNotMatchIntrospectPublicMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void methodInvocationMatchesInvokePublicMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void methodInvocationDoesNotMatchIntrospectDeclaredMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_METHODS));
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void methodInvocationMatchesInvokeDeclaredMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void privateMethodIntrospectionMatchesMethodHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMethod("privateMethod", Collections.emptyList(), methodHint -> {
			}));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
		}

		@Test
		void privateMethodIntrospectionDoesNotMatchIntrospectPublicMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
		}

		@Test
		void privateMethodIntrospectionDoesNotMatchInvokePublicMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
		}

		@Test
		void privateMethodIntrospectionMatchesIntrospectDeclaredMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_METHODS));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
		}

		@Test
		void privateMethodIntrospectionMatchesInvokeDeclaredMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
		}

		@Test
		void privateMethodInvocationDoesNotMatchMethodHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMethod("privateMethod", Collections.emptyList(), methodHint -> {
			}));
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

		@Test
		void privateMethodInvocationMatchesMethodInvocationHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMethod("privateMethod", Collections.emptyList(), methodHint -> methodHint.withMode(ExecutableMode.INVOKE)));
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

		@Test
		void privateMethodInvocationDoesNotMatchIntrospectPublicMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

		@Test
		void privateMethodInvocationDoesNotMatchInvokePublicMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

		@Test
		void privateMethodInvocationDoesNotMatchIntrospectDeclaredMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_METHODS));
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

		@Test
		void privateMethodInvocationMatchesInvokeDeclaredMethods() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));
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
		void fieldReflectionMatchesFieldHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withField("publicField", fieldHint -> {
			}));
			assertPredicateMatches(reflection.onField(SampleClass.class, "publicField"));
		}

		@Test
		void fieldWriteReflectionDoesNotMatchFieldHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withField("publicField", fieldHint -> {
			}));
			assertPredicateDoesNotMatch(reflection.onField(SampleClass.class, "publicField").allowWrite());
		}

		@Test
		void fieldUnsafeReflectionDoesNotMatchFieldHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withField("publicField", fieldHint -> {
			}));
			assertPredicateDoesNotMatch(reflection.onField(SampleClass.class, "publicField").allowUnsafeAccess());
		}

		@Test
		void fieldWriteReflectionMatchesFieldHintWithWrite() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint ->
			typeHint.withField("publicField", fieldHint -> fieldHint.allowWrite(true)));
			assertPredicateMatches(reflection.onField(SampleClass.class, "publicField").allowWrite());
		}

		@Test
		void fieldUnsafeReflectionMatchesFieldHintWithUnsafe() {
			runtimeHints.reflection().registerType(SampleClass.class,
					typeHint -> typeHint.withField("publicField", fieldHint -> fieldHint.allowUnsafeAccess(true)));
			assertPredicateMatches(reflection.onField(SampleClass.class, "publicField").allowUnsafeAccess());
		}

		@Test
		void fieldReflectionMatchesPublicFieldsHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.PUBLIC_FIELDS));
			assertPredicateMatches(reflection.onField(SampleClass.class, "publicField"));
		}

		@Test
		void fieldReflectionMatchesDeclaredFieldsHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.DECLARED_FIELDS));
			assertPredicateMatches(reflection.onField(SampleClass.class, "publicField"));
		}

		@Test
		void privateFieldReflectionMatchesFieldHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withField("privateField", fieldHint -> {
			}));
			assertPredicateMatches(reflection.onField(SampleClass.class, "privateField"));
		}

		@Test
		void privateFieldReflectionDoesNotMatchPublicFieldsHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.PUBLIC_FIELDS));
			assertPredicateDoesNotMatch(reflection.onField(SampleClass.class, "privateField"));
		}

		@Test
		void privateFieldReflectionMatchesDeclaredFieldsHint() {
			runtimeHints.reflection().registerType(SampleClass.class, typeHint -> typeHint.withMembers(MemberCategory.DECLARED_FIELDS));
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
