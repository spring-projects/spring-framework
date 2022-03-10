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

package org.springframework.beans.factory.generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.ProtectedAccess;
import org.springframework.aot.generator.ProtectedAccess.Options;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.generator.InjectionGeneratorTests.SimpleConstructorBean.InnerClass;
import org.springframework.beans.testfixture.beans.factory.generator.factory.SampleFactory;
import org.springframework.javapoet.support.CodeSnippet;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InjectionGenerator}.
 *
 * @author Stephane Nicoll
 */
class InjectionGeneratorTests {

	private final ProtectedAccess protectedAccess = new ProtectedAccess();

	@Test
	void generateInstantiationForConstructorWithNoArgUseShortcut() {
		Constructor<?> constructor = SimpleBean.class.getDeclaredConstructors()[0];
		assertThat(generateInstantiation(constructor).lines())
				.containsExactly("new InjectionGeneratorTests.SimpleBean()");
	}

	@Test
	void generateInstantiationForConstructorWithNonGenericParameter() {
		Constructor<?> constructor = SimpleConstructorBean.class.getDeclaredConstructors()[0];
		assertThat(generateInstantiation(constructor).lines()).containsExactly(
				"instanceContext.create(beanFactory, (attributes) -> new InjectionGeneratorTests.SimpleConstructorBean(attributes.get(0), attributes.get(1)))");
	}

	@Test
	void generateInstantiationForConstructorWithGenericParameter() {
		Constructor<?> constructor = GenericConstructorBean.class.getDeclaredConstructors()[0];
		assertThat(generateInstantiation(constructor).lines()).containsExactly(
				"instanceContext.create(beanFactory, (attributes) -> new InjectionGeneratorTests.GenericConstructorBean(attributes.get(0)))");
	}

	@Test
	void generateInstantiationForAmbiguousConstructor() throws Exception {
		Constructor<?> constructor = AmbiguousConstructorBean.class.getDeclaredConstructor(String.class, Number.class);
		assertThat(generateInstantiation(constructor).lines()).containsExactly(
				"instanceContext.create(beanFactory, (attributes) -> new InjectionGeneratorTests.AmbiguousConstructorBean(attributes.get(0, String.class), attributes.get(1, Number.class)))");
	}

	@Test
	void generateInstantiationForConstructorInInnerClass() {
		Constructor<?> constructor = InnerClass.class.getDeclaredConstructors()[0];
		assertThat(generateInstantiation(constructor).lines()).containsExactly(
				"beanFactory.getBean(InjectionGeneratorTests.SimpleConstructorBean.class).new InnerClass()");
	}

	@Test
	void generateInstantiationForMethodWithNoArgUseShortcut() {
		assertThat(generateInstantiation(method(SimpleBean.class, "name")).lines()).containsExactly(
				"beanFactory.getBean(InjectionGeneratorTests.SimpleBean.class).name()");
	}

	@Test
	void generateInstantiationForStaticMethodWithNoArgUseShortcut() {
		assertThat(generateInstantiation(method(SimpleBean.class, "number")).lines()).containsExactly(
				"InjectionGeneratorTests.SimpleBean.number()");
	}

	@Test
	void generateInstantiationForMethodWithNonGenericParameter() {
		assertThat(generateInstantiation(method(SampleBean.class, "source", Integer.class)).lines()).containsExactly(
				"instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(InjectionGeneratorTests.SampleBean.class).source(attributes.get(0)))");
	}

	@Test
	void generateInstantiationForStaticMethodWithNonGenericParameter() {
		assertThat(generateInstantiation(method(SampleBean.class, "staticSource", Integer.class)).lines()).containsExactly(
				"instanceContext.create(beanFactory, (attributes) -> InjectionGeneratorTests.SampleBean.staticSource(attributes.get(0)))");
	}

	@Test
	void generateInstantiationForMethodWithGenericParameters() {
		assertThat(generateInstantiation(method(SampleBean.class, "sourceWithProvider", ObjectProvider.class)).lines()).containsExactly(
				"instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(InjectionGeneratorTests.SampleBean.class).sourceWithProvider(attributes.get(0)))");
	}

	@Test
	void generateInstantiationForAmbiguousMethod() {
		assertThat(generateInstantiation(method(SampleFactory.class, "create", String.class)).lines()).containsExactly(
				"instanceContext.create(beanFactory, (attributes) -> SampleFactory.create(attributes.get(0, String.class)))");
	}

	@Test
	void generateInjectionForUnsupportedMember() {
		assertThatIllegalArgumentException().isThrownBy(() -> generateInjection(mock(Member.class), false));
	}

	@Test
	void generateInjectionForNonRequiredMethodWithNonGenericParameters() {
		Method method = method(SampleBean.class, "sourceAndCounter", String.class, Integer.class);
		assertThat(generateInjection(method, false)).isEqualTo("""
				instanceContext.method("sourceAndCounter", String.class, Integer.class)
						.resolve(beanFactory, false).ifResolved((attributes) -> bean.sourceAndCounter(attributes.get(0), attributes.get(1)))""");
	}

	@Test
	void generateInjectionForRequiredMethodWithGenericParameter() {
		Method method = method(SampleBean.class, "nameAndCounter", String.class, ObjectProvider.class);
		assertThat(generateInjection(method, true)).isEqualTo("""
				instanceContext.method("nameAndCounter", String.class, ObjectProvider.class)
						.invoke(beanFactory, (attributes) -> bean.nameAndCounter(attributes.get(0), attributes.get(1)))""");
	}

	@Test
	void generateInjectionForNonRequiredMethodWithGenericParameter() {
		Method method = method(SampleBean.class, "nameAndCounter", String.class, ObjectProvider.class);
		assertThat(generateInjection(method, false)).isEqualTo("""
				instanceContext.method("nameAndCounter", String.class, ObjectProvider.class)
						.resolve(beanFactory, false).ifResolved((attributes) -> bean.nameAndCounter(attributes.get(0), attributes.get(1)))""");
	}

	@Test
	void generateInjectionForRequiredField() {
		Field field = field(SampleBean.class, "counter");
		assertThat(generateInjection(field, true)).isEqualTo("""
				instanceContext.field("counter")
						.invoke(beanFactory, (attributes) -> bean.counter = attributes.get(0))""");
	}

	@Test
	void generateInjectionForNonRequiredField() {
		Field field = field(SampleBean.class, "counter");
		assertThat(generateInjection(field, false)).isEqualTo("""
				instanceContext.field("counter")
						.resolve(beanFactory, false).ifResolved((attributes) -> bean.counter = attributes.get(0))""");
	}

	@Test
	void generateInjectionForRequiredPrivateField() {
		Field field = field(SampleBean.class, "source");
		assertThat(generateInjection(field, true)).isEqualTo("""
				instanceContext.field("source")
						.invoke(beanFactory, (attributes) -> {
							Field sourceField = ReflectionUtils.findField(InjectionGeneratorTests.SampleBean.class, "source");
							ReflectionUtils.makeAccessible(sourceField);
							ReflectionUtils.setField(sourceField, bean, attributes.get(0));
						})""");
	}

	@Test
	void getProtectedAccessInjectionOptionsForUnsupportedMember() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				getProtectedAccessInjectionOptions(mock(Member.class)));
	}

	@Test
	void getProtectedAccessInjectionOptionsForPackagePublicField() {
		analyzeProtectedAccess(field(SampleBean.class, "enabled"));
		assertThat(this.protectedAccess.isAccessible("com.example")).isTrue();
	}

	@Test
	void getProtectedAccessInjectionOptionsForPackageProtectedField() {
		analyzeProtectedAccess(field(SampleBean.class, "counter"));
		assertPrivilegedAccess(SampleBean.class);
	}

	@Test
	void getProtectedAccessInjectionOptionsForPrivateField() {
		analyzeProtectedAccess(field(SampleBean.class, "source"));
		assertThat(this.protectedAccess.isAccessible("com.example")).isTrue();
	}

	@Test
	void getProtectedAccessInjectionOptionsForPublicMethod() {
		analyzeProtectedAccess(method(SampleBean.class, "setEnabled", Boolean.class));
		assertThat(this.protectedAccess.isAccessible("com.example")).isTrue();
	}

	@Test
	void getProtectedAccessInjectionOptionsForPackageProtectedMethod() {
		analyzeProtectedAccess(method(SampleBean.class, "sourceAndCounter", String.class, Integer.class));
		assertPrivilegedAccess(SampleBean.class);
	}


	private Method method(Class<?> type, String name, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(type, name, parameterTypes);
		assertThat(method).isNotNull();
		return method;
	}

	private Field field(Class<?> type, String name) {
		Field field = ReflectionUtils.findField(type, name);
		assertThat(field).isNotNull();
		return field;
	}

	private String generateInstantiation(Executable creator) {
		return CodeSnippet.process(code -> code.add(new InjectionGenerator().generateInstantiation(creator)));
	}

	private String generateInjection(Member member, boolean required) {
		return CodeSnippet.process(code -> code.add(new InjectionGenerator().generateInjection(member, required)));
	}

	private void analyzeProtectedAccess(Member member) {
		this.protectedAccess.analyze(member, getProtectedAccessInjectionOptions(member));
	}

	private Options getProtectedAccessInjectionOptions(Member member) {
		return new InjectionGenerator().getProtectedAccessInjectionOptions(member);
	}

	private void assertPrivilegedAccess(Class<?> target) {
		assertThat(this.protectedAccess.isAccessible("com.example")).isFalse();
		assertThat(this.protectedAccess.getPrivilegedPackageName("com.example")).isEqualTo(target.getPackageName());
		assertThat(this.protectedAccess.isAccessible(target.getPackageName())).isTrue();
	}

	@SuppressWarnings("unused")
	public static class SampleBean {

		public Boolean enabled;

		private String source;

		Integer counter;


		public void setEnabled(Boolean enabled) {

		}

		void sourceAndCounter(String source, Integer counter) {

		}

		void nameAndCounter(String name, ObjectProvider<Integer> counter) {

		}

		String source(Integer counter) {
			return "source" + counter;
		}

		String sourceWithProvider(ObjectProvider<Integer> counter) {
			return "source" + counter.getIfAvailable(() -> 0);
		}

		static String staticSource(Integer counter) {
			return counter + "source";
		}

	}

	@SuppressWarnings("unused")
	static class SimpleBean {

		String name() {
			return "test";
		}

		static Integer number() {
			return 42;
		}

	}

	@SuppressWarnings("unused")
	static class SimpleConstructorBean {

		private final String source;

		private final Integer counter;

		public SimpleConstructorBean(String source, Integer counter) {
			this.source = source;
			this.counter = counter;
		}

		class InnerClass {

		}

	}

	@SuppressWarnings("unused")
	static class GenericConstructorBean {

		private final ObjectProvider<Integer> counter;

		GenericConstructorBean(ObjectProvider<Integer> counter) {
			this.counter = counter;
		}

	}

	static class AmbiguousConstructorBean {

		AmbiguousConstructorBean(String first, String second) {

		}

		AmbiguousConstructorBean(String first, Number second) {

		}

	}
}
