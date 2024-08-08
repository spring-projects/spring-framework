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

package org.springframework.test.util;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link MethodAssert}.
 *
 * @author Stephane Nicoll
 */
class MethodAssertTests {

	@Test
	void isEqualTo() {
		Method method = ReflectionUtils.findMethod(TestData.class, "counter");
		assertThat(method).isEqualTo(method);
	}

	@Test
	void hasName() {
		assertThat(ReflectionUtils.findMethod(TestData.class, "counter")).hasName("counter");
	}

	@Test
	void hasNameWithWrongName() {
		Method method = ReflectionUtils.findMethod(TestData.class, "counter");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(method).hasName("invalid"))
				.withMessageContainingAll("Method name", "counter", "invalid");
	}

	@Test
	void hasNameWithNullMethod() {
		Method method = ReflectionUtils.findMethod(TestData.class, "notAMethod");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(method).hasName("name"))
				.withMessageContaining("Expecting actual not to be null");
	}

	@Test
	void hasDeclaringClass() {
		assertThat(ReflectionUtils.findMethod(TestData.class, "counter")).hasDeclaringClass(TestData.class);
	}

	@Test
	void haDeclaringClassWithWrongClass() {
		Method method = ReflectionUtils.findMethod(TestData.class, "counter");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(method).hasDeclaringClass(Method.class))
				.withMessageContainingAll("Method declaring class",
						TestData.class.getCanonicalName(), Method.class.getCanonicalName());
	}

	@Test
	void hasDeclaringClassWithNullMethod() {
		Method method = ReflectionUtils.findMethod(TestData.class, "notAMethod");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(method).hasDeclaringClass(TestData.class))
				.withMessageContaining("Expecting actual not to be null");
	}


	private MethodAssert assertThat(@Nullable Method method) {
		return new MethodAssert(method);
	}


	record TestData(String name, int counter) {}

}
