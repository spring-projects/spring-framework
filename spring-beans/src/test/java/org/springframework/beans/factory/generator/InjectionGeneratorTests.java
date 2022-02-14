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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.generator.InjectionGeneratorTests.SimpleConstructorBean.InnerClass;
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

	@Test
	void writeInstantiationForConstructorWithNoArgUseShortcut() {
		Constructor<?> constructor = SimpleBean.class.getDeclaredConstructors()[0];
		assertThat(writeInstantiation(constructor).lines())
				.containsExactly("new InjectionGeneratorTests.SimpleBean()");
	}

	@Test
	void writeInstantiationForConstructorWithNonGenericParameter() {
		Constructor<?> constructor = SimpleConstructorBean.class.getDeclaredConstructors()[0];
		assertThat(writeInstantiation(constructor).lines()).containsExactly(
				"instanceContext.create(beanFactory, (attributes) -> new InjectionGeneratorTests.SimpleConstructorBean(attributes.get(0), attributes.get(1)))");
	}

	@Test
	void writeInstantiationForConstructorWithGenericParameter() {
		Constructor<?> constructor = GenericConstructorBean.class.getDeclaredConstructors()[0];
		assertThat(writeInstantiation(constructor).lines()).containsExactly(
				"instanceContext.create(beanFactory, (attributes) -> new InjectionGeneratorTests.GenericConstructorBean(attributes.get(0)))");
	}

	@Test
	void writeInstantiationForAmbiguousConstructor() throws Exception {
		Constructor<?> constructor = AmbiguousConstructorBean.class.getDeclaredConstructor(String.class, Number.class);
		assertThat(writeInstantiation(constructor).lines()).containsExactly(
				"instanceContext.create(beanFactory, (attributes) -> new InjectionGeneratorTests.AmbiguousConstructorBean(attributes.get(0, String.class), attributes.get(1, Number.class)))");
	}

	@Test
	void writeInstantiationForConstructorInInnerClass() {
		Constructor<?> constructor = InnerClass.class.getDeclaredConstructors()[0];
		assertThat(writeInstantiation(constructor).lines()).containsExactly(
				"beanFactory.getBean(InjectionGeneratorTests.SimpleConstructorBean.class).new InnerClass()");
	}

	@Test
	void writeInstantiationForMethodWithNoArgUseShortcut() {
		assertThat(writeInstantiation(method(SimpleBean.class, "name")).lines()).containsExactly(
				"beanFactory.getBean(InjectionGeneratorTests.SimpleBean.class).name()");
	}

	@Test
	void writeInstantiationForStaticMethodWithNoArgUseShortcut() {
		assertThat(writeInstantiation(method(SimpleBean.class, "number")).lines()).containsExactly(
				"InjectionGeneratorTests.SimpleBean.number()");
	}

	@Test
	void writeInstantiationForMethodWithNonGenericParameter() {
		assertThat(writeInstantiation(method(SampleBean.class, "source", Integer.class)).lines()).containsExactly(
				"instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(InjectionGeneratorTests.SampleBean.class).source(attributes.get(0)))");
	}

	@Test
	void writeInstantiationForStaticMethodWithNonGenericParameter() {
		assertThat(writeInstantiation(method(SampleBean.class, "staticSource", Integer.class)).lines()).containsExactly(
				"instanceContext.create(beanFactory, (attributes) -> InjectionGeneratorTests.SampleBean.staticSource(attributes.get(0)))");
	}

	@Test
	void writeInstantiationForMethodWithGenericParameters() {
		assertThat(writeInstantiation(method(SampleBean.class, "source", ObjectProvider.class)).lines()).containsExactly(
				"instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(InjectionGeneratorTests.SampleBean.class).source(attributes.get(0)))");
	}

	@Test
	void writeInjectionForUnsupportedMember() {
		assertThatIllegalArgumentException().isThrownBy(() -> writeInjection(mock(Member.class), false));
	}

	@Test
	void writeInjectionForNonRequiredMethodWithNonGenericParameters() {
		Method method = method(SampleBean.class, "sourceAndCounter", String.class, Integer.class);
		assertThat(writeInjection(method, false)).isEqualTo("""
				instanceContext.method("sourceAndCounter", String.class, Integer.class)
						.resolve(beanFactory, false).ifResolved((attributes) -> bean.sourceAndCounter(attributes.get(0), attributes.get(1)))""");
	}

	@Test
	void writeInjectionForRequiredMethodWithGenericParameter() {
		Method method = method(SampleBean.class, "nameAndCounter", String.class, ObjectProvider.class);
		assertThat(writeInjection(method, true)).isEqualTo("""
				instanceContext.method("nameAndCounter", String.class, ObjectProvider.class)
						.invoke(beanFactory, (attributes) -> bean.nameAndCounter(attributes.get(0), attributes.get(1)))""");
	}

	@Test
	void writeInjectionForNonRequiredMethodWithGenericParameter() {
		Method method = method(SampleBean.class, "nameAndCounter", String.class, ObjectProvider.class);
		assertThat(writeInjection(method, false)).isEqualTo("""
				instanceContext.method("nameAndCounter", String.class, ObjectProvider.class)
						.resolve(beanFactory, false).ifResolved((attributes) -> bean.nameAndCounter(attributes.get(0), attributes.get(1)))""");
	}

	@Test
	void writeInjectionForRequiredField() {
		Field field = field(SampleBean.class, "counter");
		assertThat(writeInjection(field, true)).isEqualTo("""
				instanceContext.field("counter", Integer.class)
						.invoke(beanFactory, (attributes) -> bean.counter = attributes.get(0))""");
	}

	@Test
	void writeInjectionForNonRequiredField() {
		Field field = field(SampleBean.class, "counter");
		assertThat(writeInjection(field, false)).isEqualTo("""
				instanceContext.field("counter", Integer.class)
						.resolve(beanFactory, false).ifResolved((attributes) -> bean.counter = attributes.get(0))""");
	}

	@Test
	void writeInjectionForRequiredPrivateField() {
		Field field = field(SampleBean.class, "source");
		assertThat(writeInjection(field, true)).isEqualTo("""
				instanceContext.field("source", String.class)
						.invoke(beanFactory, (attributes) -> {
							Field sourceField = ReflectionUtils.findField(InjectionGeneratorTests.SampleBean.class, "source", String.class);
							ReflectionUtils.makeAccessible(sourceField);
							ReflectionUtils.setField(sourceField, bean, attributes.get(0));
						})""");
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

	private String writeInstantiation(Executable creator) {
		return CodeSnippet.process(code -> code.add(new InjectionGenerator().writeInstantiation(creator)));
	}

	private String writeInjection(Member member, boolean required) {
		return CodeSnippet.process(code -> code.add(new InjectionGenerator().writeInjection(member, required)));
	}

	@SuppressWarnings("unused")
	static class SampleBean {

		private String source;

		Integer counter;


		void sourceAndCounter(String source, Integer counter) {

		}

		void nameAndCounter(String name, ObjectProvider<Integer> counter) {

		}

		String source(Integer counter) {
			return "source" + counter;
		}

		String source(ObjectProvider<Integer> counter) {
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
