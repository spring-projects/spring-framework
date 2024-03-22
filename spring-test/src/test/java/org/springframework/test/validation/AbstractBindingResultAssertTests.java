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

package org.springframework.test.validation;

import java.util.Map;

import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AbstractBindingResultAssert}.
 *
 * @author Stephane Nicoll
 */
class AbstractBindingResultAssertTests {

	@Test
	void hasErrorsCountWithNoError() {
		assertThat(bindingResult(new TestBean(), Map.of("name", "John", "age", "42"))).hasErrorsCount(0);
	}

	@Test
	void hasErrorsCountWithInvalidCount() {
		AssertProvider<BindingResultAssert> actual = bindingResult(new TestBean(),
				Map.of("name", "John", "age", "4x", "touchy", "invalid.value"));
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
						() -> assertThat(actual).hasErrorsCount(1))
				.withMessageContainingAll("check errors for attribute 'test'", "1", "2");
	}

	@Test
	void hasFieldErrorsWithMatchingSubset() {
		assertThat(bindingResult(new TestBean(), Map.of("name", "John", "age", "4x", "touchy", "x.y")))
				.hasFieldErrors("touchy");
	}

	@Test
	void hasFieldErrorsWithAllMatching() {
		assertThat(bindingResult(new TestBean(), Map.of("name", "John", "age", "4x", "touchy", "x.y")))
				.hasFieldErrors("touchy", "age");
	}

	@Test
	void hasFieldErrorsWithNotAllMatching() {
		AssertProvider<BindingResultAssert> actual = bindingResult(new TestBean(), Map.of("name", "John", "age", "4x", "touchy", "x.y"));
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
						() -> assertThat(actual).hasFieldErrors("age", "name"))
				.withMessageContainingAll("check field errors", "age", "touchy", "name");
	}

	@Test
	void hasOnlyFieldErrorsWithAllMatching() {
		assertThat(bindingResult(new TestBean(), Map.of("name", "John", "age", "4x", "touchy", "x.y")))
				.hasOnlyFieldErrors("touchy", "age");
	}

	@Test
	void hasOnlyFieldErrorsWithMatchingSubset() {
		AssertProvider<BindingResultAssert> actual = bindingResult(new TestBean(), Map.of("name", "John", "age", "4x", "touchy", "x.y"));
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
						() -> assertThat(actual).hasOnlyFieldErrors("age"))
				.withMessageContainingAll("check field errors", "age", "touchy");
	}

	@Test
	void hasFieldErrorCodeWithMatchingCode() {
		assertThat(bindingResult(new TestBean(), Map.of("name", "John", "age", "4x", "touchy", "x.y")))
				.hasFieldErrorCode("age", "typeMismatch");
	}

	@Test
	void hasFieldErrorCodeWitNonMatchingCode() {
		AssertProvider<BindingResultAssert> actual = bindingResult(new TestBean(), Map.of("name", "John", "age", "4x", "touchy", "x.y"));
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
						() -> assertThat(actual).hasFieldErrorCode("age", "castFailure"))
				.withMessageContainingAll("check error code for field 'age'", "castFailure", "typeMismatch");
	}

	@Test
	void hasFieldErrorCodeWitNonMatchingField() {
		AssertProvider<BindingResultAssert> actual = bindingResult(new TestBean(), Map.of("name", "John", "age", "4x", "touchy", "x.y"));
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
						() -> assertThat(actual).hasFieldErrorCode("unknown", "whatever"))
				.withMessageContainingAll("Expecting binding result", "touchy", "age",
						"to have at least an error for field 'unknown'");
	}


	private AssertProvider<BindingResultAssert> bindingResult(Object instance, Map<String, Object> propertyValues) {
		return () -> new BindingResultAssert("test", createBindingResult(instance, propertyValues));
	}

	private static BindingResult createBindingResult(Object instance, Map<String, Object> propertyValues) {
		DataBinder binder = new DataBinder(instance, "test");
		MutablePropertyValues pvs = new MutablePropertyValues(propertyValues);
		binder.bind(pvs);
		try {
			binder.close();
			return binder.getBindingResult();
		}
		catch (BindException ex) {
			return ex.getBindingResult();
		}
	}


	private static final class BindingResultAssert extends AbstractBindingResultAssert<BindingResultAssert> {
		public BindingResultAssert(String name, BindingResult bindingResult) {
			super(name, bindingResult, BindingResultAssert.class);
		}
	}

}
