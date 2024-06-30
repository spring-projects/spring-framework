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

package org.springframework.test.http;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link HttpHeadersAssert}.
 *
 * @author Stephane Nicoll
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
				.withMessageContainingAll("HTTP headers", "first", "wrong-name");
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
	void doesNotContainHeader() {
		assertThat(Map.of("first", "1")).doesNotContainHeader("second");
	}

	@Test
	void doesNotContainHeaderWithNamePresent() {
		Map<String, String> map = Map.of("first", "1");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(map).doesNotContainKey("first"))
				.withMessageContainingAll("HTTP headers", "first");
	}

	@Test
	void doesNotContainHeaders() {
		assertThat(Map.of("first", "1", "third", "3"))
				.doesNotContainsHeaders("second", "fourth");
	}

	@Test
	void doesNotContainHeadersWithSeveralNamesPresent() {
		Map<String, String> map = Map.of("first", "1", "second", "2", "third", "3");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(map).doesNotContainsHeaders("first", "another-wrong-name", "second"))
				.withMessageContainingAll("HTTP headers", "first", "second");
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
				.withMessageContainingAll("HTTP headers", "header", "wrong-name");
	}

	@Test
	void hasValueWithNonPresentHeader() {
		HttpHeaders map = new HttpHeaders();
		map.add("test-header", "a");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(map).hasValue("wrong-name", "a"))
				.withMessageContainingAll("HTTP headers", "test-header", "wrong-name");
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
				.withMessageContainingAll("HTTP headers", "header", "wrong-name");
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
				.withMessageContainingAll("HTTP headers", "header", "wrong-name");
	}

	@Test
	void hasValueWithNoInstantMatchOneSecOfDifference() {
		Instant instant = Instant.now();
		HttpHeaders map = new HttpHeaders();
		map.setInstant("header", instant);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(map).hasValue("wrong-name", instant.minusSeconds(1)))
				.withMessageContainingAll("HTTP headers", "header", "wrong-name");
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
