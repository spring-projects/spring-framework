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

package org.springframework.aot.generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.ProtectedAccess.Options;
import org.springframework.core.ResolvableType;
import org.springframework.core.testfixture.aot.generator.visibility.ProtectedGenericParameter;
import org.springframework.core.testfixture.aot.generator.visibility.ProtectedParameter;
import org.springframework.core.testfixture.aot.generator.visibility.PublicFactoryBean;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ProtectedAccess}.
 *
 * @author Stephane Nicoll
 */
class ProtectedAccessTests {

	public static final Options DEFAULT_OPTIONS = Options.defaults().build();

	private final ProtectedAccess protectedAccess = new ProtectedAccess();

	@Test
	void analyzeWithPublicConstructor() throws NoSuchMethodException {
		this.protectedAccess.analyze(PublicClass.class.getConstructor(), DEFAULT_OPTIONS);
		assertThat(this.protectedAccess.isAccessible("com.example")).isTrue();
	}

	@Test
	void analyzeWithPackagePrivateConstructor() {
		this.protectedAccess.analyze(ProtectedAccessor.class.getDeclaredConstructors()[0],
				DEFAULT_OPTIONS);
		assertPrivilegedAccess(ProtectedAccessor.class);
	}

	@Test
	void analyzeWithPackagePrivateConstructorAndReflectionEnabled() {
		Constructor<?> constructor = ProtectedAccessor.class.getDeclaredConstructors()[0];
		this.protectedAccess.analyze(constructor,
				Options.defaults().useReflection(member -> member.equals(constructor)).build());
		assertThat(this.protectedAccess.isAccessible("com.example")).isTrue();
	}

	@Test
	void analyzeWithPackagePrivateClass() {
		this.protectedAccess.analyze(ProtectedClass.class.getDeclaredConstructors()[0], DEFAULT_OPTIONS);
		assertPrivilegedAccess(ProtectedClass.class);
	}

	@Test
	void analyzeWithPackagePrivateDeclaringType() {
		this.protectedAccess.analyze(method(ProtectedClass.class, "stringBean"), DEFAULT_OPTIONS);
		assertPrivilegedAccess(ProtectedClass.class);
	}

	@Test
	void analyzeWithPackagePrivateConstructorParameter() {
		this.protectedAccess.analyze(ProtectedParameter.class.getConstructors()[0], DEFAULT_OPTIONS);
		assertPrivilegedAccess(ProtectedParameter.class);
	}

	@Test
	void analyzeWithPackagePrivateConstructorGenericParameter() {
		this.protectedAccess.analyze(ProtectedGenericParameter.class.getConstructors()[0], DEFAULT_OPTIONS);
		assertPrivilegedAccess(ProtectedParameter.class);
	}

	@Test
	void analyzeWithPackagePrivateMethod() {
		this.protectedAccess.analyze(method(PublicClass.class, "getProtectedMethod"), DEFAULT_OPTIONS);
		assertPrivilegedAccess(PublicClass.class);
	}

	@Test
	void analyzeWithPackagePrivateMethodAndReflectionEnabled() {
		this.protectedAccess.analyze(method(PublicClass.class, "getProtectedMethod"),
				Options.defaults().useReflection(member -> !Modifier.isPublic(member.getModifiers())).build());
		assertThat(this.protectedAccess.isAccessible("com.example")).isTrue();
	}

	@Test
	void analyzeWithPackagePrivateMethodReturnType() {
		this.protectedAccess.analyze(method(ProtectedAccessor.class, "methodWithProtectedReturnType"), DEFAULT_OPTIONS);
		assertThat(this.protectedAccess.isAccessible("com.example")).isTrue();
	}

	@Test
	void analyzeWithPackagePrivateMethodReturnTypeAndAssignReturnTypeFunction() {
		this.protectedAccess.analyze(method(ProtectedAccessor.class, "methodWithProtectedReturnType"),
				Options.defaults().assignReturnType(member -> false).build());
		assertThat(this.protectedAccess.isAccessible("com.example")).isTrue();
	}

	@Test
	void analyzeWithPackagePrivateMethodReturnTypeAndAssignReturnType() {
		this.protectedAccess.analyze(method(ProtectedAccessor.class, "methodWithProtectedReturnType"),
				Options.defaults().assignReturnType(true).build());
		assertPrivilegedAccess(ProtectedAccessor.class);
	}

	@Test
	void analyzeWithPackagePrivateMethodParameter() {
		this.protectedAccess.analyze(method(ProtectedAccessor.class, "methodWithProtectedParameter",
				ProtectedClass.class), DEFAULT_OPTIONS);
		assertPrivilegedAccess(ProtectedAccessor.class);
	}

	@Test
	void analyzeWithPackagePrivateField() {
		this.protectedAccess.analyze(field(PublicClass.class, "protectedField"), DEFAULT_OPTIONS);
		assertPrivilegedAccess(PublicClass.class);
	}

	@Test
	void analyzeWithPackagePrivateFieldAndReflectionEnabled() {
		this.protectedAccess.analyze(field(PublicClass.class, "protectedField"),
				Options.defaults().useReflection(member -> true).build());
		assertThat(this.protectedAccess.isAccessible("com.example")).isTrue();
	}

	@Test
	void analyzeWithPublicFieldAndProtectedType() {
		this.protectedAccess.analyze(field(PublicClass.class, "protectedClassField"), DEFAULT_OPTIONS);
		assertThat(this.protectedAccess.isAccessible("com.example")).isTrue();
	}

	@Test
	void analyzeWithPublicFieldAndProtectedTypeAssigned() {
		this.protectedAccess.analyze(field(PublicClass.class, "protectedClassField"),
				Options.defaults().assignReturnType(true).build());
		assertPrivilegedAccess(ProtectedClass.class);
	}

	@Test
	void analyzeWithPackagePrivateGenericArgument() {
		this.protectedAccess.analyze(method(PublicFactoryBean.class, "protectedTypeFactoryBean"),
				Options.defaults().assignReturnType(true).build());
		assertPrivilegedAccess(PublicFactoryBean.class);
	}

	@Test
	void analyzeTypeWithProtectedGenericArgument() {
		this.protectedAccess.analyze(PublicFactoryBean.resolveToProtectedGenericParameter());
		assertPrivilegedAccess(PublicFactoryBean.class);
	}

	@Test
	void analyzeWithRecursiveType() {
		assertThat(this.protectedAccess.isProtected(ResolvableType.forClassWithGenerics(
				SelfReference.class, SelfReference.class))).isEqualTo(SelfReference.class);
	}

	@Test
	void getProtectedPackageWithPublicAccess() throws NoSuchMethodException {
		this.protectedAccess.analyze(PublicClass.class.getConstructor(), DEFAULT_OPTIONS);
		assertThat(this.protectedAccess.getPrivilegedPackageName("com.example")).isNull();
	}

	@Test
	void getProtectedPackageWithProtectedAccessInOnePackage() {
		this.protectedAccess.analyze(method(PublicFactoryBean.class, "protectedTypeFactoryBean"),
				Options.defaults().assignReturnType(true).build());
		assertThat(this.protectedAccess.getPrivilegedPackageName("com.example"))
				.isEqualTo(PublicFactoryBean.class.getPackageName());
	}

	@Test
	void getProtectedPackageWithProtectedAccessInSeveralPackages() {
		Method protectedMethodFirstPackage = method(PublicFactoryBean.class, "protectedTypeFactoryBean");
		Method protectedMethodSecondPackage = method(ProtectedAccessor.class, "methodWithProtectedParameter",
				ProtectedClass.class);
		this.protectedAccess.analyze(protectedMethodFirstPackage,
				Options.defaults().assignReturnType(true).build());
		this.protectedAccess.analyze(protectedMethodSecondPackage, DEFAULT_OPTIONS);
		assertThatThrownBy(() -> this.protectedAccess.getPrivilegedPackageName("com.example"))
				.isInstanceOfSatisfying(ProtectedAccessException.class, ex ->
						assertThat(ex.getProtectedElements().stream().map(ProtectedElement::getMember))
								.containsOnly(protectedMethodFirstPackage, protectedMethodSecondPackage));
	}

	private void assertPrivilegedAccess(Class<?> target) {
		assertThat(this.protectedAccess.isAccessible("com.example")).isFalse();
		assertThat(this.protectedAccess.getPrivilegedPackageName("com.example")).isEqualTo(target.getPackageName());
		assertThat(this.protectedAccess.isAccessible(target.getPackageName())).isTrue();
	}

	private static Method method(Class<?> type, String name, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(type, name, parameterTypes);
		assertThat(method).isNotNull();
		return method;
	}

	private static Field field(Class<?> type, String name) {
		Field field = ReflectionUtils.findField(type, name);
		assertThat(field).isNotNull();
		return field;
	}


	@SuppressWarnings("unused")
	public static class PublicClass {

		String protectedField;

		public ProtectedClass protectedClassField;

		String getProtectedMethod() {
			return this.protectedField;
		}

	}

	@SuppressWarnings("unused")
	public static class ProtectedAccessor {

		ProtectedAccessor() {
		}

		public String methodWithProtectedParameter(ProtectedClass type) {
			return "test";
		}

		public ProtectedClass methodWithProtectedReturnType() {
			return new ProtectedClass();
		}
	}

	@SuppressWarnings("unused")
	static class ProtectedClass {

		public ProtectedClass() {
		}

		public String stringBean() {
			return "public";
		}

	}

	static class SelfReference<T extends SelfReference<T>> {

		@SuppressWarnings("unchecked")
		T getThis() {
			return (T) this;
		}

	}

}
