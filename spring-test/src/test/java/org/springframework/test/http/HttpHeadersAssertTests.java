/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.http;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link HttpHeadersAssert}.
 *
 * @author Stephane Nicoll
 * @author Simon Baslé
 */
class HttpHeadersAssertTests {

	@Test
	void containsHeader() {
		assertThat(Map.of("first", "1")).containsHeader("first");
	}

	@Test
	void containsHeaderWithNameNotPresent() {
		Map<String, String> map = Map.of("first", "1");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(map).containsHeader("wrong-name"))
				.withMessageContainingAll("HTTP header", "first", "wrong-name");
	}

	@Test
	void containsHeaders() {
		assertThat(Map.of("first", "1", "second", "2", "third", "3"))
				.containsHeaders("first", "third");
	}

	@Test
	void containsHeadersWithSeveralNamesNotPresent() {
		Map<String, String> map = Map.of("first", "1", "second", "2", "third", "3");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(map).containsHeaders("first", "wrong-name", "another-wrong-name", "third"))
				.withMessageContainingAll("HTTP headers", "first", "wrong-name", "another-wrong-name");
	}

	@Test
	void containsOnlyHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("name1", "value1");
		headers.add("name2", "value2");
		assertThat(headers).containsOnlyHeaders("name2", "name1");
	}

	@Test
	void containsOnlyHeadersWithMissingOne() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("name1", "value1");
		headers.add("name2", "value2");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(headers).containsOnlyHeaders("name1", "name2", "name3"))
				.withMessageContainingAll("check headers contains only HTTP headers",
						"could not find the following element(s)", "[\"name3\"]");
	}

	@Test
	void containsOnlyHeadersWithExtraOne() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("name1", "value1");
		headers.add("name2", "value2");
		headers.add("name3", "value3");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(headers).containsOnlyHeaders("name1", "name2"))
				.withMessageContainingAll("check headers contains only HTTP headers",
						"the following element(s) were unexpected", "[\"name3\"]");
	}

	@Test
	void doesNotContainHeader() {
		assertThat(Map.of("first", "1")).doesNotContainHeader("second");
	}

	@Test
	void doesNotContainHeaderWithNamePresent() {
		Map<String, String> map = Map.of("first", "1");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(map).doesNotContainHeader("first"))
				.withMessageContainingAll("HTTP header", "first");
	}

	@Test
	void doesNotContainHeaders() {
		assertThat(Map.of("first", "1", "third", "3"))
				.doesNotContainHeaders("second", "fourth");
	}

	@Test
	void doesNotContainHeadersWithSeveralNamesPresent() {
		Map<String, String> map = Map.of("first", "1", "second", "2", "third", "3");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(map).doesNotContainHeaders("first", "another-wrong-name", "second"))
				.withMessageContainingAll("HTTP headers", "first", "second");
	}

	@Test
	@SuppressWarnings("unchecked")
	void hasHeaderSatisfying() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("header", List.of("first", "second", "third"));
		Consumer<List<String>> mock = mock(Consumer.class);
		assertThatNoException().isThrownBy(() -> assertThat(headers).hasHeaderSatisfying("header", mock));
		verify(mock).accept(List.of("first", "second", "third"));
	}

	@Test
	void hasHeaderSatisfyingWithExceptionInConsumer() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("header", List.of("first", "second", "third"));
		IllegalStateException testException = new IllegalStateException("test");
		assertThatIllegalStateException()
				.isThrownBy(() -> assertThat(headers).hasHeaderSatisfying("header", values -> {
					throw testException;
				})).isEqualTo(testException);
	}

	@Test
	void hasHeaderSatisfyingWithFailingAssertion() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("header", List.of("first", "second", "third"));
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(headers).hasHeaderSatisfying("header", values ->
						Assertions.assertThat(values).hasSize(42)))
				.withMessageContainingAll("HTTP header", "header", "first", "second", "third", "42", "3");
	}

	@Test
	void hasValueWithStringMatch() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("header", List.of("a", "b", "c"));
		assertThat(headers).hasValue("header", "a");
	}

	@Test
	void hasValueWithStringMatchOnSecondaryValue() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("header", List.of("first", "second", "third"));
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(headers).hasValue("header", "second"))
				.withMessageContainingAll("check primary value for HTTP header 'header'", "first", "second");
	}

	@Test
	void hasValueWithNoStringMatch() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("header", List.of("first", "second", "third"));
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(headers).hasValue("wrong-name", "second"))
				.withMessageContainingAll("HTTP header", "header", "wrong-name");
	}

	@Test
	void hasValueWithNonPresentHeader() {
		HttpHeaders map = new HttpHeaders();
		map.add("test-header", "a");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(map).hasValue("wrong-name", "a"))
				.withMessageContainingAll("HTTP header", "test-header", "wrong-name");
	}

	@Test
	void hasValueWithLongMatch() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("header", List.of("123", "456", "789"));
		assertThat(headers).hasValue("header", 123);
	}

	@Test
	void hasValueWithLongMatchOnSecondaryValue() {
		HttpHeaders map = new HttpHeaders();
		map.addAll("header", List.of("123", "456", "789"));
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(map).hasValue("header", 456))
				.withMessageContainingAll("check primary long value for HTTP header 'header'", "123", "456");
	}

	@Test
	void hasValueWithNoLongMatch() {
		HttpHeaders map = new HttpHeaders();
		map.addAll("header", List.of("123", "456", "789"));
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(map).hasValue("wrong-name", 456))
				.withMessageContainingAll("HTTP header", "header", "wrong-name");
	}

	@Test
	void hasValueWithInstantMatch() {
		Instant instant = Instant.now();
		HttpHeaders headers = new HttpHeaders();
		headers.setInstant("header", instant);
		assertThat(headers).hasValue("header", instant);
	}

	@Test
	void hasValueWithNoInstantMatch() {
		Instant instant = Instant.now();
		HttpHeaders map = new HttpHeaders();
		map.setInstant("header", instant);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(map).hasValue("wrong-name", instant.minusSeconds(30)))
				.withMessageContainingAll("HTTP header", "header", "wrong-name");
	}

	@Test
	void hasValueWithNoInstantMatchOneSecOfDifference() {
		Instant instant = Instant.now();
		HttpHeaders map = new HttpHeaders();
		map.setInstant("header", instant);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(map).hasValue("wrong-name", instant.minusSeconds(1)))
				.withMessageContainingAll("HTTP header", "header", "wrong-name");
	}

	@Test
	void hasSingleValueWithStringMatch() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("header", "a");
		assertThat(headers).hasSingleValue("header", "a");
	}

	@Test
	void hasSingleValueWithSecondaryValues() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("header", List.of("first", "second", "third"));
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(headers).hasSingleValue("header", "first"))
				.withMessage("Expected HTTP header 'header' to be present without secondary values, " +
						"but found <2> secondary values");
	}

	@Test
	void hasSingleValueWithLongMatch() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("header", "123");
		assertThat(headers).hasSingleValue("header", 123);
	}

	@Test
	void hasSingleValueWithLongMatchButSecondaryValues() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("header", List.of("123", "456", "789"));
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(headers).hasSingleValue("header", 123))
				.withMessage("Expected HTTP header 'header' to be present without secondary values, " +
						"but found <2> secondary values");
	}

	@Test
	void hasSingleValueWithInstantMatch() {
		Instant instant = Instant.now();
		HttpHeaders headers = new HttpHeaders();
		headers.setInstant("header", instant);
		assertThat(headers).hasSingleValue("header", instant);
	}

	@Test
	void hasSingleValueWithInstantAndSecondaryValues() {
		Instant instant = Instant.now();
		HttpHeaders headers = new HttpHeaders();
		headers.setInstant("header", instant);
		headers.add("header", "second");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(headers).hasSingleValue("header", instant.minusSeconds(30)))
				.withMessage("Expected HTTP header 'header' to be present without secondary values, " +
						"but found <1> secondary values");
	}

	@Test
	void hasExactlyValues() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("name", List.of("value1", "value2"));
		headers.add("otherName", "otherValue");
		assertThat(headers).hasExactlyValues("name", List.of("value1", "value2"));
	}

	@Test
	void hasExactlyValuesWithMissingOne() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("name", List.of("value1", "value2"));
		headers.add("otherName", "otherValue");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(headers).hasExactlyValues("name", List.of("value1", "value2", "value3")))
				.withMessageContainingAll("check all values of HTTP header 'name'",
						"to contain exactly (and in same order)",
						"could not find the following elements", "[\"value3\"]");
	}

	@Test
	void hasExactlyValuesWithExtraOne() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("name", List.of("value1", "value2"));
		headers.add("otherName", "otherValue");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(headers).hasExactlyValues("name", List.of("value1")))
				.withMessageContainingAll("check all values of HTTP header 'name'",
						"to contain exactly (and in same order)",
						"some elements were not expected", "[\"value2\"]");
	}

	@Test
	void hasExactlyValuesWithWrongOrder() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("name", List.of("value1", "value2"));
		headers.add("otherName", "otherValue");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(headers).hasExactlyValues("name", List.of("value2", "value1")))
				.withMessageContainingAll("check all values of HTTP header 'name'",
						"to contain exactly (and in same order)",
						"there were differences at these indexes",
						"element at index 0: expected \"value2\" but was \"value1\"",
						"element at index 1: expected \"value1\" but was \"value2\"");
	}

	@Test
	void hasExactlyValuesInAnyOrder() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("name", List.of("value1", "value2"));
		headers.add("otherName", "otherValue");
		assertThat(headers).hasExactlyValuesInAnyOrder("name", List.of("value1", "value2"));
	}

	@Test
	void hasExactlyValuesInAnyOrderWithDifferentOrder() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("name", List.of("value1", "value2"));
		headers.add("otherName", "otherValue");
		assertThat(headers).hasExactlyValuesInAnyOrder("name", List.of("value2", "value1"));
	}

	@Test
	void hasExactlyValuesInAnyOrderWithMissingOne() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("name", List.of("value1", "value2"));
		headers.add("otherName", "otherValue");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(headers).hasExactlyValuesInAnyOrder("name",
						List.of("value1", "value2", "value3")))
				.withMessageContainingAll("check all values of HTTP header 'name'",
						"to contain exactly in any order",
						"could not find the following elements", "[\"value3\"]");
	}

	@Test
	void hasExactlyValuesInAnyOrderWithExtraOne() {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll("name", List.of("value1", "value2"));
		headers.add("otherName", "otherValue");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(headers).hasExactlyValuesInAnyOrder("name", List.of("value1")))
				.withMessageContainingAll("check all values of HTTP header 'name'",
						"to contain exactly in any order",
						"the following elements were unexpected", "[\"value2\"]");
	}

	@Test
	void isEmpty() {
		assertThat(new HttpHeaders()).isEmpty();
	}

	@Test
	void isEmptyWithHeaders() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(Map.of("first", "1", "second", "2")).isEmpty())
				.withMessageContainingAll("check headers are empty", "Expecting empty", "first", "second");
	}

	@Test
	void isNotEmpty() {
		assertThat(Map.of("header", "value")).isNotEmpty();
	}

	@Test
	void isNotEmptyWithNoHeaders() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(new HttpHeaders()).isNotEmpty())
				.withMessageContainingAll("check headers are not empty", "Expecting actual not to be empty");
	}

	@Test
	void hasSize() {
		assertThat(Map.of("first", "1", "second", "2")).hasSize(2);
	}

	@Test
	void hasSizeWithWrongSize() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(Map.of("first", "1")).hasSize(42))
				.withMessageContainingAll("check headers have size '42'", "1");
	}

	@Test
	void hasSameSizeAs() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("name1", "value1");
		headers.add("name2", "value2");

		HttpHeaders other = new HttpHeaders();
		other.add("name3", "value3");
		other.add("name4", "value4");

		assertThat(headers).hasSameSizeAs(other);
	}

	@Test
	void hasSameSizeAsWithSmallerOther() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("name1", "value1");
		headers.add("name2", "value2");

		HttpHeaders other = new HttpHeaders();
		other.add("name3", "value3");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(headers).hasSameSizeAs(other))
				.withMessageContainingAll("check headers have same size as '", other.toString(),
						"Expected size: 1 but was: 2");
	}

	private static HttpHeadersAssert assertThat(Map<String, String> values) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		values.forEach(map::add);
		return assertThat(new HttpHeaders(map));
	}

	private static HttpHeadersAssert assertThat(HttpHeaders values) {
		return new HttpHeadersAssert(values);
	}

}
