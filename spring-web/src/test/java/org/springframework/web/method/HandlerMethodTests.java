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

package org.springframework.web.method;

import java.lang.reflect.Method;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HandlerMethod}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class HandlerMethodTests {

	@Test
	void shouldValidateArgsWithConstraintsDirectlyInClass() {
		Object target = new MyClass();
		testValidateArgs(target, List.of("addIntValue", "addPersonAndIntValue", "addPersons", "addPeople", "addNames"), true);
		testValidateArgs(target, List.of("addPerson", "getPerson", "getIntValue", "addPersonNotValidated"), false);
	}

	@Test
	void shouldValidateArgsWithConstraintsInInterface() {
		Object target = new MyInterfaceImpl();
		testValidateArgs(target, List.of("addIntValue", "addPersonAndIntValue", "addPersons", "addPeople"), true);
		testValidateArgs(target, List.of("addPerson", "addPersonNotValidated", "getPerson", "getIntValue"), false);
	}

	@Test
	void shouldValidateArgsWithConstraintsInGenericAbstractSuperclass() {
		Object target = new GenericInterfaceImpl();
		shouldValidateArguments(getHandlerMethod(target, "processTwo", String.class), true);
	}

	@Test
	void shouldValidateArgsWithConstraintsInGenericInterface() {
		Object target = new GenericInterfaceImpl();
		shouldValidateArguments(getHandlerMethod(target, "processOne", Long.class), false);
		shouldValidateArguments(getHandlerMethod(target, "processOneAndTwo", Long.class, Object.class), true);
	}

	@Test
	void shouldValidateReturnValueWithConstraintsDirectlyInClass() {
		Object target = new MyClass();
		testValidateReturnValue(target, List.of("getPerson", "getIntValue"), true);
		testValidateReturnValue(target, List.of("addPerson", "addIntValue", "addPersonNotValidated"), false);
	}

	@Test
	void shouldValidateReturnValueWithConstraintsInInterface() {
		Object target = new MyInterfaceImpl();
		testValidateReturnValue(target, List.of("getPerson", "getIntValue"), true);
		testValidateReturnValue(target, List.of("addPerson", "addIntValue", "addPersonNotValidated"), false);
	}

	@Test
	void classLevelValidatedAnnotation() {
		Object target = new MyValidatedClass();
		testValidateArgs(target, List.of("addPerson"), false);
		testValidateReturnValue(target, List.of("getPerson"), false);
	}

	@Test // gh-34277
	void createWithResolvedBeanSameInstance() {
		MyClass target = new MyClass();
		HandlerMethod handlerMethod = getHandlerMethod(target, "addPerson");
		assertThat(handlerMethod.createWithResolvedBean()).isSameAs(handlerMethod);
	}

	@Test
	void resolvedFromHandlerMethod() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("myClass", MyClass.class);

		MyClass target = new MyClass();
		Method method = ClassUtils.getMethod(target.getClass(), "addPerson", (Class<?>[]) null);

		HandlerMethod hm1 = new HandlerMethod("myClass", context.getBeanFactory(), method);
		HandlerMethod hm2 = hm1.createWithValidateFlags();
		HandlerMethod hm3 = hm2.createWithResolvedBean();

		assertThat(hm1.getResolvedFromHandlerMethod()).isNull();
		assertThat(hm2.getResolvedFromHandlerMethod()).isSameAs(hm1);
		assertThat(hm3.getResolvedFromHandlerMethod()).isSameAs(hm1);
	}


	private static void shouldValidateArguments(HandlerMethod handlerMethod, boolean expected) {
		if (expected) {
			assertThat(handlerMethod.shouldValidateArguments()).as(handlerMethod.getMethod().getName()).isTrue();
		}
		else {
			assertThat(handlerMethod.shouldValidateArguments()).as(handlerMethod.getMethod().getName()).isFalse();
		}
	}

	private static void testValidateArgs(Object target, List<String> methodNames, boolean expected) {
		for (String methodName : methodNames) {
			shouldValidateArguments(getHandlerMethod(target, methodName), expected);
		}
	}

	private static void testValidateReturnValue(Object target, List<String> methodNames, boolean expected) {
		for (String methodName : methodNames) {
			assertThat(getHandlerMethod(target, methodName).shouldValidateReturnValue()).isEqualTo(expected);
		}
	}

	private static HandlerMethod getHandlerMethod(Object target, String methodName) {
		return getHandlerMethod(target, methodName, (Class<?>[]) null);
	}

	private static HandlerMethod getHandlerMethod(Object target, String methodName, Class<?>... parameterTypes) {
		Method method = ClassUtils.getMethod(target.getClass(), methodName, parameterTypes);
		return new HandlerMethod(target, method).createWithValidateFlags();
	}


	@SuppressWarnings("unused")
	private record Person(@Size(min = 1, max = 10) String name) {

		@Override
		public String name() {
			return this.name;
		}
	}


	@SuppressWarnings("unused")
	private static class MyClass {

		public void addPerson(@Valid Person person) {
		}

		public void addIntValue(@Max(10) int value) {
		}

		public void addPersonAndIntValue(@Valid Person person, @Max(10) int value) {
		}

		public void addPersons(@Valid List<Person> persons) {
		}

		public void addPeople(List<@Valid Person> persons) {
		}

		public void addNames(List<@NotEmpty String> names) {
		}

		public void addPersonNotValidated(Person person) {
		}

		@Valid
		public Person getPerson() {
			throw new UnsupportedOperationException();
		}

		@Max(10)
		public int getIntValue() {
			throw new UnsupportedOperationException();
		}
	}


	@SuppressWarnings("unused")
	private interface MyInterface {

		void addPerson(@Valid Person person);

		void addIntValue(@Max(10) int value);

		void addPersonAndIntValue(@Valid Person person, @Max(10) int value);

		void addPersons(@Valid List<Person> persons);

		void addPeople(List<@Valid Person> persons);

		void addPersonNotValidated(Person person);

		@Valid
		Person getPerson();

		@Max(10)
		int getIntValue();
	}


	@SuppressWarnings("unused")
	private static class MyInterfaceImpl implements MyInterface {

		@Override
		public void addPerson(Person person) {
		}

		@Override
		public void addIntValue(int value) {
		}

		@Override
		public void addPersonAndIntValue(Person person, int value) {
		}

		@Override
		public void addPersons(List<Person> persons) {
		}

		@Override
		public void addPeople(List<@Valid Person> persons) {
		}

		@Override
		public void addPersonNotValidated(Person person) {
		}

		@Override
		public Person getPerson() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getIntValue() {
			throw new UnsupportedOperationException();
		}
	}


	@SuppressWarnings("unused")
	@Validated
	private static class MyValidatedClass {

		public void addPerson(@Valid Person person) {
		}

		@Valid
		public Person getPerson() {
			throw new UnsupportedOperationException();
		}
	}


	interface GenericInterface<A, B> {

		void processOne(@Valid A value1);

		void processOneAndTwo(A value1, @Max(42) B value2);
	}

	abstract static class GenericAbstractSuperclass<C> implements GenericInterface<Long, C> {

		@Override
		public void processOne(Long value1) {
		}

		@Override
		public void processOneAndTwo(Long value1, C value2) {
		}

		public abstract void processTwo(@Max(42) C value);
	}

	static class GenericInterfaceImpl extends GenericAbstractSuperclass<String> {

		@Override
		public void processTwo(String value) {
		}
	}

}
