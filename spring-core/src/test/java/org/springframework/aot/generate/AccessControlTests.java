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

package org.springframework.aot.generate;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.AccessControl.Visibility;
import org.springframework.core.ResolvableType;
import org.springframework.core.testfixture.aot.generator.visibility.ProtectedGenericParameter;
import org.springframework.core.testfixture.aot.generator.visibility.ProtectedParameter;
import org.springframework.core.testfixture.aot.generator.visibility.PublicFactoryBean;
import org.springframework.javapoet.ClassName;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AccessControl}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class AccessControlTests {

	@Test
	void isAccessibleWhenPublicVisibilityInSamePackage() {
		AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PUBLIC);
		assertThat(accessControl.isAccessibleFrom(ClassName.get(PublicClass.class))).isTrue();
	}

	@Test
	void isAccessibleWhenPublicVisibilityInDifferentPackage() {
		AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PUBLIC);
		assertThat(accessControl.isAccessibleFrom(ClassName.get(String.class))).isTrue();
	}

	@Test
	void isAccessibleWhenProtectedVisibilityInSamePackage() {
		AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PROTECTED);
		assertThat(accessControl.isAccessibleFrom(ClassName.get(PublicClass.class))).isTrue();
	}

	@Test
	void isAccessibleWhenProtectedVisibilityInDifferentPackage() {
		AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PROTECTED);
		assertThat(accessControl.isAccessibleFrom(ClassName.get(String.class))).isFalse();
	}

	@Test
	void isAccessibleWhenPackagePrivateVisibilityInSamePackage() {
		AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PACKAGE_PRIVATE);
		assertThat(accessControl.isAccessibleFrom(ClassName.get(PublicClass.class))).isTrue();
	}

	@Test
	void isAccessibleWhenPackagePrivateVisibilityInDifferentPackage() {
		AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PACKAGE_PRIVATE);
		assertThat(accessControl.isAccessibleFrom(ClassName.get(String.class))).isFalse();
	}

	@Test
	void isAccessibleWhenPrivateVisibilityInSamePackage() {
		AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PRIVATE);
		assertThat(accessControl.isAccessibleFrom(ClassName.get(PublicClass.class))).isFalse();
	}

	@Test
	void isAccessibleWhenPrivateVisibilityInDifferentPackage() {
		AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PRIVATE);
		assertThat(accessControl.isAccessibleFrom(ClassName.get(String.class))).isFalse();
	}

	@Test
	void forMemberWhenPublicConstructor() throws NoSuchMethodException {
		Member member = PublicClass.class.getConstructor();
		AccessControl accessControl = AccessControl.forMember(member);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PUBLIC);
	}

	@Test
	void forMemberWhenPackagePrivateConstructor() {
		Member member = ProtectedAccessor.class.getDeclaredConstructors()[0];
		AccessControl accessControl = AccessControl.forMember(member);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
	}

	@Test
	void forMemberWhenPackagePrivateClassWithPublicConstructor() {
		Member member = PackagePrivateClass.class.getDeclaredConstructors()[0];
		AccessControl accessControl = AccessControl.forMember(member);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
	}

	@Test
	void forMemberWhenPackagePrivateClassWithPublicMethod() {
		Member member = method(PackagePrivateClass.class, "stringBean");
		AccessControl accessControl = AccessControl.forMember(member);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
	}

	@Test
	void forMemberWhenPublicClassWithPackagePrivateConstructorParameter() {
		Member member = ProtectedParameter.class.getConstructors()[0];
		AccessControl accessControl = AccessControl.forMember(member);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
	}

	@Test
	void forMemberWhenPublicClassWithPackagePrivateGenericOnConstructorParameter() {
		Member member = ProtectedGenericParameter.class.getConstructors()[0];
		AccessControl accessControl = AccessControl.forMember(member);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
	}

	@Test
	void forMemberWhenPublicClassWithPackagePrivateMethod() {
		Member member = method(PublicClass.class, "getProtectedMethod");
		AccessControl accessControl = AccessControl.forMember(member);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
	}

	@Test
	void forMemberWhenPublicClassWithPackagePrivateMethodReturnType() {
		Member member = method(ProtectedAccessor.class, "methodWithProtectedReturnType");
		AccessControl accessControl = AccessControl.forMember(member);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
	}

	@Test
	void forMemberWhenPublicClassWithPackagePrivateMethodParameter() {
		Member member = method(ProtectedAccessor.class, "methodWithProtectedParameter",
				PackagePrivateClass.class);
		AccessControl accessControl = AccessControl.forMember(member);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
	}

	@Test
	void forMemberWhenPublicClassWithPackagePrivateField() {
		Field member = field(PublicClass.class, "protectedField");
		AccessControl accessControl = AccessControl.forMember(member);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
	}

	@Test
	void forMemberWhenPublicClassWithPublicFieldAndPackagePrivateFieldType() {
		Member member = field(PublicClass.class, "protectedClassField");
		AccessControl accessControl = AccessControl.forMember(member);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
	}

	@Test
	void forMemberWhenPublicClassWithPrivateField() {
		Member member = field(PublicClass.class, "privateField");
		AccessControl accessControl = AccessControl.forMember(member);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PRIVATE);
	}

	@Test
	void forMemberWhenPublicClassWithPublicMethodAndPackagePrivateGenericOnReturnType() {
		Member member = method(PublicFactoryBean.class, "protectedTypeFactoryBean");
		AccessControl accessControl = AccessControl.forMember(member);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
	}

	@Test
	void forMemberWhenPublicClassWithPackagePrivateArrayComponent() {
		Member member = field(PublicClass.class, "packagePrivateClasses");
		AccessControl accessControl = AccessControl.forMember(member);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
	}

	@Test
	void forResolvableTypeWhenPackagePrivateGeneric() {
		ResolvableType resolvableType = PublicFactoryBean
				.resolveToProtectedGenericParameter();
		AccessControl accessControl = AccessControl.forResolvableType(resolvableType);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
	}

	@Test
	void forResolvableTypeWhenRecursiveType() {
		ResolvableType resolvableType = ResolvableType
				.forClassWithGenerics(SelfReference.class, SelfReference.class);
		AccessControl accessControl = AccessControl.forResolvableType(resolvableType);
		assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
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

	static class SelfReference<T extends SelfReference<T>> {

		@SuppressWarnings({ "unchecked", "unused" })
		T getThis() {
			return (T) this;
		}

	}
}
