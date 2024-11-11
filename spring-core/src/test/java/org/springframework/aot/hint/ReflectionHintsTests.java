/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link ReflectionHints}.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 */
class ReflectionHintsTests {

	private final ReflectionHints reflectionHints = new ReflectionHints();

	@Test
	void registerType() {
		this.reflectionHints.registerType(TypeReference.of(String.class),
				hint -> hint.withMembers(MemberCategory.DECLARED_FIELDS));
		assertThat(this.reflectionHints.typeHints()).singleElement().satisfies(
				typeWithMemberCategories(String.class, MemberCategory.DECLARED_FIELDS));
	}

	@Test
	void registerTypeIfPresentRegistersExistingClass() {
		this.reflectionHints.registerTypeIfPresent(null, String.class.getName(), MemberCategory.DECLARED_FIELDS);
		assertThat(this.reflectionHints.typeHints()).singleElement().satisfies(
				typeWithMemberCategories(String.class, MemberCategory.DECLARED_FIELDS));
	}

	@Test
	void registerTypeIfPresentIgnoresMissingClass() {
		Consumer<TypeHint.Builder> hintBuilder = mock();
		this.reflectionHints.registerTypeIfPresent(null, "com.example.DoesNotExist", hintBuilder);
		assertThat(this.reflectionHints.typeHints()).isEmpty();
		verifyNoInteractions(hintBuilder);
	}

	@Test
	void getTypeUsingType() {
		this.reflectionHints.registerType(TypeReference.of(String.class),
				hint -> hint.withMembers(MemberCategory.DECLARED_FIELDS));
		assertThat(this.reflectionHints.getTypeHint(String.class)).satisfies(
				typeWithMemberCategories(String.class, MemberCategory.DECLARED_FIELDS));
	}

	@Test
	void getTypeUsingTypeReference() {
		this.reflectionHints.registerType(String.class, MemberCategory.DECLARED_FIELDS);
		assertThat(this.reflectionHints.getTypeHint(TypeReference.of(String.class))).satisfies(
				typeWithMemberCategories(String.class, MemberCategory.DECLARED_FIELDS));
	}

	@Test
	void getTypeForNonExistingType() {
		assertThat(this.reflectionHints.getTypeHint(String.class)).isNull();
	}

	@Test
	void registerTypeReusesBuilder() {
		this.reflectionHints.registerType(TypeReference.of(String.class),
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		Field field = ReflectionUtils.findField(String.class, "value");
		assertThat(field).isNotNull();
		this.reflectionHints.registerField(field);
		assertThat(this.reflectionHints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(String.class.getCanonicalName());
			assertThat(typeHint.fields()).singleElement().satisfies(fieldHint -> assertThat(fieldHint.getName()).isEqualTo("value"));
			assertThat(typeHint.getMemberCategories()).containsOnly(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		});
	}

	@Test
	void registerClass() {
		this.reflectionHints.registerType(Integer.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
		assertThat(this.reflectionHints.typeHints()).singleElement().satisfies(
				typeWithMemberCategories(Integer.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
	}

	@Test
	void registerClassWithCustomizer() {
		this.reflectionHints.registerType(Integer.class,
				typeHint -> typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
		assertThat(this.reflectionHints.typeHints()).singleElement().satisfies(
				typeWithMemberCategories(Integer.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
	}

	@Test
	void registerTypesAppliesTheSameHints() {
		this.reflectionHints.registerTypes(TypeReference.listOf(Integer.class, String.class, Double.class),
				TypeHint.builtWith(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
		assertThat(this.reflectionHints.typeHints())
				.anySatisfy(typeWithMemberCategories(Integer.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS))
				.anySatisfy(typeWithMemberCategories(String.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS))
				.anySatisfy(typeWithMemberCategories(Double.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS))
				.hasSize(3);
	}

	@Test
	void registerField() {
		Field field = ReflectionUtils.findField(TestType.class, "field");
		assertThat(field).isNotNull();
		this.reflectionHints.registerField(field);
		assertTestTypeFieldHint(fieldHint ->
				assertThat(fieldHint.getName()).isEqualTo("field"));
	}

	@Test
	void registerTypeIgnoresLambda() {
		Runnable lambda = () -> { };
		Consumer<TypeHint.Builder> hintBuilder = mock();
		this.reflectionHints.registerType(lambda.getClass());
		this.reflectionHints.registerType(lambda.getClass(), hintBuilder);
		assertThat(this.reflectionHints.typeHints()).isEmpty();
		verifyNoInteractions(hintBuilder);
	}

	private void assertTestTypeFieldHint(Consumer<FieldHint> fieldHint) {
		assertThat(this.reflectionHints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(TestType.class.getCanonicalName());
			assertThat(typeHint.fields()).singleElement().satisfies(fieldHint);
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).isEmpty();
			assertThat(typeHint.getMemberCategories()).isEmpty();
		});
	}

	@Test
	void registerConstructor() {
		this.reflectionHints.registerConstructor(
				TestType.class.getDeclaredConstructors()[0], ExecutableMode.INTROSPECT);
		assertTestTypeConstructorHint(constructorHint -> {
			assertThat(constructorHint.getParameterTypes()).isEmpty();
			assertThat(constructorHint.getMode()).isEqualTo(ExecutableMode.INTROSPECT);
		});
	}

	@Test
	void registerConstructorTwiceUpdatesExistingEntry() {
		Constructor<?> constructor = TestType.class.getDeclaredConstructors()[0];
		this.reflectionHints.registerConstructor(constructor, ExecutableMode.INTROSPECT);
		this.reflectionHints.registerConstructor(constructor, ExecutableMode.INVOKE);
		assertTestTypeConstructorHint(constructorHint -> {
			assertThat(constructorHint.getParameterTypes()).isEmpty();
			assertThat(constructorHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
	}

	private void assertTestTypeConstructorHint(Consumer<ExecutableHint> constructorHint) {
		assertThat(this.reflectionHints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getMemberCategories()).isEmpty();
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(TestType.class.getCanonicalName());
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.constructors()).singleElement().satisfies(constructorHint);
			assertThat(typeHint.methods()).isEmpty();
			assertThat(typeHint.getMemberCategories()).isEmpty();
		});
	}

	@Test
	void registerMethod() {
		Method method = ReflectionUtils.findMethod(TestType.class, "setName", String.class);
		assertThat(method).isNotNull();
		this.reflectionHints.registerMethod(method, ExecutableMode.INTROSPECT);
		assertTestTypeMethodHints(methodHint -> {
			assertThat(methodHint.getName()).isEqualTo("setName");
			assertThat(methodHint.getParameterTypes()).containsOnly(TypeReference.of(String.class));
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INTROSPECT);
		});
	}

	@Test
	void registerMethodTwiceUpdatesExistingEntry() {
		Method method = ReflectionUtils.findMethod(TestType.class, "setName", String.class);
		assertThat(method).isNotNull();
		this.reflectionHints.registerMethod(method, ExecutableMode.INTROSPECT);
		this.reflectionHints.registerMethod(method, ExecutableMode.INVOKE);
		assertTestTypeMethodHints(methodHint -> {
			assertThat(methodHint.getName()).isEqualTo("setName");
			assertThat(methodHint.getParameterTypes()).containsOnly(TypeReference.of(String.class));
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
	}

	@Test
	void registerOnInterfaces() {
		this.reflectionHints.registerForInterfaces(ChildType.class,
				typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		assertThat(this.reflectionHints.typeHints()).hasSize(2)
				.noneMatch(typeHint -> typeHint.getType().getCanonicalName().equals(Serializable.class.getCanonicalName()))
				.anyMatch(typeHint -> typeHint.getType().getCanonicalName().equals(SecondInterface.class.getCanonicalName())
						&& typeHint.getMemberCategories().contains(MemberCategory.INTROSPECT_PUBLIC_METHODS))
				.anyMatch(typeHint -> typeHint.getType().getCanonicalName().equals(FirstInterface.class.getCanonicalName())
						&& typeHint.getMemberCategories().contains(MemberCategory.INTROSPECT_PUBLIC_METHODS));
	}

	private void assertTestTypeMethodHints(Consumer<ExecutableHint> methodHint) {
		assertThat(this.reflectionHints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(TestType.class.getCanonicalName());
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).singleElement().satisfies(methodHint);
		});
	}

	private Consumer<TypeHint> typeWithMemberCategories(Class<?> type, MemberCategory... memberCategories) {
		return typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(type.getCanonicalName());
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).isEmpty();
			assertThat(typeHint.getMemberCategories()).containsExactly(memberCategories);
		};
	}


	@SuppressWarnings("unused")
	static class TestType {

		@Nullable
		private String field;

		void setName(String name) {

		}

	}

	interface FirstInterface {
		void first();
	}

	interface SecondInterface {
		void second();
	}

	@SuppressWarnings("serial")
	static class ParentType implements Serializable, FirstInterface {
		@Override
		public void first() {

		}
	}

	@SuppressWarnings("serial")
	static class ChildType extends ParentType implements SecondInterface {
		@Override
		public void second() {

		}
	}

}
