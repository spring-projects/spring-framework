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

package org.springframework.test.json;

import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.test.json.JsonComparison.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

/**
 * Tests for the {@link JsonComparator} implementations backed by JsonUnit
 * and JSONassert, verifying that they behave consistently.
 *
 * @author Brian Clozel
 */
class JsonComparatorTests {

	static Stream<Arguments> comparators() {
		return Stream.of(
				argumentSet("JsonUnit", (Function<JsonCompareMode, JsonComparator>) JsonUnitJsonComparator::new),
				argumentSet("JSONassert", (Function<JsonCompareMode, JsonComparator>) JsonAssertJsonComparator::new));
	}


	@ParameterizedTest
	@MethodSource("comparators")
	void strictWhenSameContentWithDifferentFormatting(Function<JsonCompareMode, JsonComparator> factory) {
		assertMatch(factory, JsonCompareMode.STRICT, "{\"a\":1,\"b\":[1,2]}", "{ \"b\" : [1, 2], \"a\" : 1 }");
	}

	@ParameterizedTest
	@MethodSource("comparators")
	void strictWhenExtraField(Function<JsonCompareMode, JsonComparator> factory) {
		assertMismatch(factory, JsonCompareMode.STRICT, "{\"a\":1}", "{\"a\":1,\"b\":2}");
	}

	@ParameterizedTest
	@MethodSource("comparators")
	void strictWhenDifferentArrayOrder(Function<JsonCompareMode, JsonComparator> factory) {
		assertMismatch(factory, JsonCompareMode.STRICT, "{\"a\":[1,2]}", "{\"a\":[2,1]}");
	}

	@ParameterizedTest
	@MethodSource("comparators")
	void strictWhenDifferentValue(Function<JsonCompareMode, JsonComparator> factory) {
		assertMismatch(factory, JsonCompareMode.STRICT, "{\"a\":1}", "{\"a\":2}");
	}

	@ParameterizedTest
	@MethodSource("comparators")
	void lenientWhenExtraField(Function<JsonCompareMode, JsonComparator> factory) {
		assertMatch(factory, JsonCompareMode.LENIENT, "{\"a\":1}", "{\"a\":1,\"b\":2}");
	}

	@ParameterizedTest
	@MethodSource("comparators")
	void lenientWhenDifferentArrayOrder(Function<JsonCompareMode, JsonComparator> factory) {
		assertMatch(factory, JsonCompareMode.LENIENT, "{\"a\":[1,2]}", "{\"a\":[2,1]}");
	}

	@ParameterizedTest
	@MethodSource("comparators")
	void lenientWhenExtraArrayItem(Function<JsonCompareMode, JsonComparator> factory) {
		assertMismatch(factory, JsonCompareMode.LENIENT, "{\"a\":[1,2]}", "{\"a\":[1,2,3]}");
	}

	@ParameterizedTest
	@MethodSource("comparators")
	void lenientWhenMissingField(Function<JsonCompareMode, JsonComparator> factory) {
		assertMismatch(factory, JsonCompareMode.LENIENT, "{\"a\":1,\"b\":2}", "{\"a\":1}");
	}

	@ParameterizedTest
	@MethodSource("comparators")
	void lenientWhenDifferentValue(Function<JsonCompareMode, JsonComparator> factory) {
		assertMismatch(factory, JsonCompareMode.LENIENT, "{\"a\":1}", "{\"a\":2}");
	}

	@ParameterizedTest
	@MethodSource("comparators")
	void whenBothNull(Function<JsonCompareMode, JsonComparator> factory) {
		assertMatch(factory, JsonCompareMode.STRICT, null, null);
	}

	@ParameterizedTest
	@MethodSource("comparators")
	void whenActualNull(Function<JsonCompareMode, JsonComparator> factory) {
		JsonComparison comparison = factory.apply(JsonCompareMode.STRICT).compare("{}", null);
		assertThat(comparison.getResult()).isEqualTo(Result.MISMATCH);
		assertThat(comparison.getMessage()).isEqualTo("Expected null JSON");
	}

	@ParameterizedTest
	@MethodSource("comparators")
	void whenExpectedNull(Function<JsonCompareMode, JsonComparator> factory) {
		JsonComparison comparison = factory.apply(JsonCompareMode.STRICT).compare(null, "{}");
		assertThat(comparison.getResult()).isEqualTo(Result.MISMATCH);
		assertThat(comparison.getMessage()).isEqualTo("Expected non-null JSON");
	}


	private static void assertMatch(Function<JsonCompareMode, JsonComparator> factory,
			JsonCompareMode compareMode, String expected, String actual) {

		JsonComparison comparison = factory.apply(compareMode).compare(expected, actual);
		assertThat(comparison.getResult()).isEqualTo(Result.MATCH);
		assertThat(comparison.getMessage()).isNull();
	}

	private static void assertMismatch(Function<JsonCompareMode, JsonComparator> factory,
			JsonCompareMode compareMode, String expected, String actual) {

		JsonComparison comparison = factory.apply(compareMode).compare(expected, actual);
		assertThat(comparison.getResult()).isEqualTo(Result.MISMATCH);
		assertThat(comparison.getMessage()).isNotEmpty();
	}

}
