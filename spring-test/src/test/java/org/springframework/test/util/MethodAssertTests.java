/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.util;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

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
	void hasDeclaringClassWithWrongClass() {
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
