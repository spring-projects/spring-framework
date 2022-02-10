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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.ProtectedAccess.Options;
import org.springframework.core.ResolvableType;
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

	private final ProtectedAccess protectedAccess = new ProtectedAccess();

	@Test
	void analyzeWithPublicConstructor() throws NoSuchMethodException {
		this.protectedAccess.analyze(PublicClass.class.getConstructor());
		assertThat(this.protectedAccess.isAccessible("com.example")).isTrue();
	}

	@Test
	void analyzeWithPackagePrivateConstructorAndDefaultOptions() {
		this.protectedAccess.analyze(ProtectedAccessor.class.getDeclaredConstructors()[0]);
		assertThat(this.protectedAccess.isAccessible("com.example")).isTrue();
		assertThat(this.protectedAccess.getPrivilegedPackageName("com.example")).isNull();
	}

	@Test
	void analyzeWithPackagePrivateConstructorAndReflectionDisabled() {
		this.protectedAccess.analyze(ProtectedAccessor.class.getDeclaredConstructors()[0],
				new Options(false, true));
		assertThat(this.protectedAccess.isAccessible("com.example")).isFalse();
		assertThat(this.protectedAccess.getPrivilegedPackageName("com.example"))
				.isEqualTo(ProtectedAccessor.class.getPackageName());
		assertThat(this.protectedAccess.isAccessible(ProtectedAccessor.class.getPackageName())).isTrue();
	}

	@Test
	void analyzeWithPackagePrivateClass() {
		this.protectedAccess.analyze(ProtectedClass.class.getDeclaredConstructors()[0]);
		assertThat(this.protectedAccess.isAccessible("com.example")).isFalse();
		assertThat(this.protectedAccess.getPrivilegedPackageName("com.example"))
				.isEqualTo(ProtectedClass.class.getPackageName());
		assertThat(this.protectedAccess.isAccessible(ProtectedClass.class.getPackageName())).isTrue();
	}

	@Test
	void analyzeWithPackagePrivateDeclaringType() {
		this.protectedAccess.analyze(method(ProtectedClass.class, "stringBean"));
		assertThat(this.protectedAccess.isAccessible("com.example")).isFalse();
		assertThat(this.protectedAccess.getPrivilegedPackageName("com.example"))
				.isEqualTo(ProtectedClass.class.getPackageName());
		assertThat(this.protectedAccess.isAccessible(ProtectedClass.class.getPackageName())).isTrue();
	}

	@Test
	void analyzeWithPackagePrivateConstructorParameter() {
		this.protectedAccess.analyze(ProtectedParameter.class.getConstructors()[0]);
		assertThat(this.protectedAccess.isAccessible("com.example")).isFalse();
		assertThat(this.protectedAccess.getPrivilegedPackageName("com.example"))
				.isEqualTo(ProtectedParameter.class.getPackageName());
		assertThat(this.protectedAccess.isAccessible(ProtectedParameter.class.getPackageName())).isTrue();
	}

	@Test
	void analyzeWithPackagePrivateMethod() {
		this.protectedAccess.analyze(method(PublicClass.class, "getProtectedMethod"));
		assertThat(this.protectedAccess.isAccessible("com.example")).isTrue();
	}

	@Test
	void analyzeWithPackagePrivateMethodAndReflectionDisabled() {
		this.protectedAccess.analyze(method(PublicClass.class, "getProtectedMethod"),
				new Options(false, false));
		assertThat(this.protectedAccess.isAccessible("com.example")).isFalse();
	}

	@Test
	void analyzeWithPackagePrivateMethodReturnType() {
		this.protectedAccess.analyze(method(ProtectedAccessor.class, "methodWithProtectedReturnType"));
		assertThat(this.protectedAccess.isAccessible("com.example")).isFalse();
		assertThat(this.protectedAccess.getPrivilegedPackageName("com.example"))
				.isEqualTo(ProtectedAccessor.class.getPackageName());
		assertThat(this.protectedAccess.isAccessible(ProtectedAccessor.class.getPackageName())).isTrue();
	}

	@Test
	void analyzeWithPackagePrivateMethodParameter() {
		this.protectedAccess.analyze(method(ProtectedAccessor.class, "methodWithProtectedParameter",
				ProtectedClass.class));
		assertThat(this.protectedAccess.isAccessible("com.example")).isFalse();
		assertThat(this.protectedAccess.getPrivilegedPackageName("com.example"))
				.isEqualTo(ProtectedClass.class.getPackageName());
		assertThat(this.protectedAccess.isAccessible(ProtectedClass.class.getPackageName())).isTrue();
	}

	@Test
	void analyzeWithPackagePrivateField() {
		this.protectedAccess.analyze(field(PublicClass.class, "protectedField"));
		assertThat(this.protectedAccess.isAccessible("com.example")).isTrue();
	}

	@Test
	void analyzeWithPackagePrivateFieldAndReflectionDisabled() {
		this.protectedAccess.analyze(field(PublicClass.class, "protectedField"),
				new Options(false, true));
		assertThat(this.protectedAccess.isAccessible("com.example")).isFalse();
		assertThat(this.protectedAccess.getPrivilegedPackageName("com.example"))
				.isEqualTo(PublicClass.class.getPackageName());
		assertThat(this.protectedAccess.isAccessible(PublicClass.class.getPackageName())).isTrue();
	}

	@Test
	void analyzeWithPublicFieldAndProtectedType() {
		this.protectedAccess.analyze(field(PublicClass.class, "protectedClassField"),
				new Options(false, true));
		assertThat(this.protectedAccess.isAccessible("com.example")).isFalse();
		assertThat(this.protectedAccess.getPrivilegedPackageName("com.example"))
				.isEqualTo(ProtectedClass.class.getPackageName());
		assertThat(this.protectedAccess.isAccessible(ProtectedClass.class.getPackageName())).isTrue();
	}

	@Test
	void analyzeWithPackagePrivateGenericArgument() {
		this.protectedAccess.analyze(method(PublicFactoryBean.class, "protectedTypeFactoryBean"));
		assertThat(this.protectedAccess.isAccessible("com.example")).isFalse();
		assertThat(this.protectedAccess.isAccessible(PublicFactoryBean.class.getPackageName())).isTrue();
	}

	@Test
	void analyzeTypeWithProtectedGenericArgument() {
		this.protectedAccess.analyze(PublicFactoryBean.resolveToProtectedGenericParameter());
		assertThat(this.protectedAccess.isAccessible("com.example")).isFalse();
		assertThat(this.protectedAccess.isAccessible(PublicFactoryBean.class.getPackageName())).isTrue();
	}

	@Test
	void analyzeWithRecursiveType() {
		assertThat(this.protectedAccess.isProtected(ResolvableType.forClassWithGenerics(
				SelfReference.class, SelfReference.class))).isTrue();
	}

	@Test
	void getProtectedPackageWithPublicAccess() throws NoSuchMethodException {
		this.protectedAccess.analyze(PublicClass.class.getConstructor());
		assertThat(this.protectedAccess.getPrivilegedPackageName("com.example")).isNull();
	}

	@Test
	void getProtectedPackageWithProtectedAccessInOnePackage() {
		this.protectedAccess.analyze(method(PublicFactoryBean.class, "protectedTypeFactoryBean"));
		assertThat(this.protectedAccess.getPrivilegedPackageName("com.example"))
				.isEqualTo(PublicFactoryBean.class.getPackageName());
	}

	@Test
	void getProtectedPackageWithProtectedAccessInSeveralPackages() {
		Method protectedMethodFirstPackage = method(PublicFactoryBean.class, "protectedTypeFactoryBean");
		Method protectedMethodSecondPackage = method(ProtectedAccessor.class, "methodWithProtectedParameter",
				ProtectedClass.class);
		this.protectedAccess.analyze(protectedMethodFirstPackage);
		this.protectedAccess.analyze(protectedMethodSecondPackage);
		assertThatThrownBy(() -> this.protectedAccess.getPrivilegedPackageName("com.example"))
				.isInstanceOfSatisfying(ProtectedAccessException.class, ex ->
						assertThat(ex.getProtectedElements().stream().map(ProtectedElement::getMember))
								.containsOnly(protectedMethodFirstPackage, protectedMethodSecondPackage));
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
