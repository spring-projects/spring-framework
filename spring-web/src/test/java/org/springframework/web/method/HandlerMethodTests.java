/*
 * Copyright 2002-2023 the original author or authors.
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
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import org.springframework.util.ClassUtils;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HandlerMethod}.
 * @author Rossen Stoyanchev
 */
public class HandlerMethodTests {

	@Test
	void shouldValidateArgsWithConstraintsDirectlyOnClass() {
		Object target = new MyClass();
		testShouldValidateArguments(target, List.of("addIntValue", "addPersonAndIntValue"), true);
		testShouldValidateArguments(target, List.of("addPerson", "getPerson", "getIntValue", "addPersonNotValidated"), false);
	}

	@Test
	void shouldValidateArgsWithConstraintsOnInterface() {
		Object target = new MyInterfaceImpl();
		testShouldValidateArguments(target, List.of("addIntValue", "addPersonAndIntValue"), true);
		testShouldValidateArguments(target, List.of("addPerson", "addPersonNotValidated", "getPerson", "getIntValue"), false);
	}

	@Test
	void shouldValidateReturnValueWithConstraintsDirectlyOnClass() {
		Object target = new MyClass();
		testShouldValidateReturnValue(target, List.of("getPerson", "getIntValue"), true);
		testShouldValidateReturnValue(target, List.of("addPerson", "addIntValue", "addPersonNotValidated"), false);
	}

	@Test
	void shouldValidateReturnValueWithConstraintsOnInterface() {
		Object target = new MyInterfaceImpl();
		testShouldValidateReturnValue(target, List.of("getPerson", "getIntValue"), true);
		testShouldValidateReturnValue(target, List.of("addPerson", "addIntValue", "addPersonNotValidated"), false);
	}

	@Test
	void classLevelValidatedAnnotation() {
		Object target = new MyValidatedClass();
		testShouldValidateArguments(target, List.of("addPerson"), false);
		testShouldValidateReturnValue(target, List.of("getPerson"), false);
	}

	private static void testShouldValidateArguments(Object target, List<String> methodNames, boolean expected) {
		for (String methodName : methodNames) {
			assertThat(getHandlerMethod(target, methodName).shouldValidateArguments()).isEqualTo(expected);
		}
	}

	private static void testShouldValidateReturnValue(Object target, List<String> methodNames, boolean expected) {
		for (String methodName : methodNames) {
			assertThat(getHandlerMethod(target, methodName).shouldValidateReturnValue()).isEqualTo(expected);
		}
	}

	private static HandlerMethod getHandlerMethod(Object target, String methodName) {
		Method method = ClassUtils.getMethod(target.getClass(), methodName, (Class<?>[]) null);
		return new HandlerMethod(target, method);
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

}
