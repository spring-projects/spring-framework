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

package org.springframework.beans.factory.aot;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutowiredArgumentsCodeGenerator}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class AutowiredArgumentsCodeGeneratorTests {

	@Test
	void generateCodeWhenNoArguments() {
		Method method = ReflectionUtils.findMethod(UnambiguousMethods.class, "zero");
		AutowiredArgumentsCodeGenerator generator = new AutowiredArgumentsCodeGenerator(
				UnambiguousMethods.class, method);
		assertThat(generator.generateCode(method.getParameterTypes())).hasToString("");
	}

	@Test
	void generatedCodeWhenSingleArgument() {
		Method method = ReflectionUtils.findMethod(UnambiguousMethods.class, "one",
				String.class);
		AutowiredArgumentsCodeGenerator generator = new AutowiredArgumentsCodeGenerator(
				UnambiguousMethods.class, method);
		assertThat(generator.generateCode(method.getParameterTypes()))
				.hasToString("args.get(0)");
	}

	@Test
	void generateCodeWhenMultipleArguments() {
		Method method = ReflectionUtils.findMethod(UnambiguousMethods.class, "three",
				String.class, Integer.class, Boolean.class);
		AutowiredArgumentsCodeGenerator generator = new AutowiredArgumentsCodeGenerator(
				UnambiguousMethods.class, method);
		assertThat(generator.generateCode(method.getParameterTypes()))
				.hasToString("args.get(0), args.get(1), args.get(2)");
	}

	@Test
	void generateCodeWhenMultipleArgumentsWithOffset() {
		Constructor<?> constructor = Outer.Nested.class.getDeclaredConstructors()[0];
		AutowiredArgumentsCodeGenerator generator = new AutowiredArgumentsCodeGenerator(
				Outer.Nested.class, constructor);
		assertThat(generator.generateCode(constructor.getParameterTypes(), 1))
				.hasToString("args.get(0), args.get(1)");
	}

	@Test
	void generateCodeWhenAmbiguousConstructor() throws Exception {
		Constructor<?> constructor = AmbiguousConstructors.class
				.getDeclaredConstructor(String.class, Integer.class);
		AutowiredArgumentsCodeGenerator generator = new AutowiredArgumentsCodeGenerator(
				AmbiguousConstructors.class, constructor);
		assertThat(generator.generateCode(constructor.getParameterTypes())).hasToString(
				"args.get(0, java.lang.String.class), args.get(1, java.lang.Integer.class)");
	}

	@Test
	void generateCodeWhenUnambiguousConstructor() throws Exception {
		Constructor<?> constructor = UnambiguousConstructors.class
				.getDeclaredConstructor(String.class, Integer.class);
		AutowiredArgumentsCodeGenerator generator = new AutowiredArgumentsCodeGenerator(
				UnambiguousConstructors.class, constructor);
		assertThat(generator.generateCode(constructor.getParameterTypes()))
				.hasToString("args.get(0), args.get(1)");
	}

	@Test
	void generateCodeWhenAmbiguousMethod() {
		Method method = ReflectionUtils.findMethod(AmbiguousMethods.class, "two",
				String.class, Integer.class);
		AutowiredArgumentsCodeGenerator generator = new AutowiredArgumentsCodeGenerator(
				AmbiguousMethods.class, method);
		assertThat(generator.generateCode(method.getParameterTypes())).hasToString(
				"args.get(0, java.lang.String.class), args.get(1, java.lang.Integer.class)");
	}

	@Test
	void generateCodeWhenAmbiguousSubclassMethod() {
		Method method = ReflectionUtils.findMethod(UnambiguousMethods.class, "two",
				String.class, Integer.class);
		AutowiredArgumentsCodeGenerator generator = new AutowiredArgumentsCodeGenerator(
				AmbiguousSubclassMethods.class, method);
		assertThat(generator.generateCode(method.getParameterTypes())).hasToString(
				"args.get(0, java.lang.String.class), args.get(1, java.lang.Integer.class)");
	}

	@Test
	void generateCodeWhenUnambiguousMethod() {
		Method method = ReflectionUtils.findMethod(UnambiguousMethods.class, "two",
				String.class, Integer.class);
		AutowiredArgumentsCodeGenerator generator = new AutowiredArgumentsCodeGenerator(
				UnambiguousMethods.class, method);
		assertThat(generator.generateCode(method.getParameterTypes()))
				.hasToString("args.get(0), args.get(1)");
	}

	@Test
	void generateCodeWithCustomArgVariable() {
		Method method = ReflectionUtils.findMethod(UnambiguousMethods.class, "one",
				String.class);
		AutowiredArgumentsCodeGenerator generator = new AutowiredArgumentsCodeGenerator(
				UnambiguousMethods.class, method);
		assertThat(generator.generateCode(method.getParameterTypes(), 0, "objs"))
				.hasToString("objs.get(0)");
	}

	static class Outer {

		class Nested {

			Nested(String a, Integer b) {
			}

		}

	}

	static class UnambiguousMethods {

		void zero() {
		}

		void one(String a) {
		}

		void two(String a, Integer b) {
		}

		void three(String a, Integer b, Boolean c) {
		}

	}

	static class AmbiguousMethods {

		void two(String a, Integer b) {
		}

		void two(Integer b, String a) {
		}

	}

	static class AmbiguousSubclassMethods extends UnambiguousMethods {

		void two(Integer a, String b) {
		}

	}

	static class UnambiguousConstructors {

		UnambiguousConstructors() {
		}

		UnambiguousConstructors(String a) {
		}

		UnambiguousConstructors(String a, Integer b) {
		}

	}

	static class AmbiguousConstructors {

		AmbiguousConstructors(String a, Integer b) {
		}

		AmbiguousConstructors(Integer b, String a) {
		}

	}

}
