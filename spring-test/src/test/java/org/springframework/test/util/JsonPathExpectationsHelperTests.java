/*
 * Copyright 2004-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link JsonPathExpectationsHelper}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
class JsonPathExpectationsHelperTests {

	private static final String CONTENT = "{" + //
			"'str':         'foo',           " + //
			"'num':         5,               " + //
			"'bool':        true,            " + //
			"'arr':         [42],            " + //
			"'colorMap':    {'red': 'rojo'}, " + //
			"'whitespace':  '    ',          " + //
			"'emptyString': '',              " + //
			"'emptyArray':  [],              " + //
			"'emptyMap':    {}               " + //
	"}";

	private static final String SIMPSONS = "{ 'familyMembers': [ " + //
			"{'name': 'Homer' }, " + //
			"{'name': 'Marge' }, " + //
			"{'name': 'Bart'  }, " + //
			"{'name': 'Lisa'  }, " + //
			"{'name': 'Maggie'}  " + //
	" ] }";


	@Test
	void exists() throws Exception {
		new JsonPathExpectationsHelper("$.str").exists(CONTENT);
	}

	@Test
	void existsForAnEmptyArray() throws Exception {
		new JsonPathExpectationsHelper("$.emptyArray").exists(CONTENT);
	}

	@Test
	void existsForAnEmptyMap() throws Exception {
		new JsonPathExpectationsHelper("$.emptyMap").exists(CONTENT);
	}

	@Test
	void existsForIndefinatePathWithResults() throws Exception {
		new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Bart')]").exists(SIMPSONS);
	}

	@Test
	void existsForIndefinatePathWithEmptyResults() throws Exception {
		String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).exists(SIMPSONS))
			.withMessageContaining("No value at JSON path \"" + expression + "\"");
	}

	@Test
	void doesNotExist() throws Exception {
		new JsonPathExpectationsHelper("$.bogus").doesNotExist(CONTENT);
	}

	@Test
	void doesNotExistForAnEmptyArray() throws Exception {
		String expression = "$.emptyArray";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).doesNotExist(CONTENT))
			.withMessageContaining("Expected no value at JSON path \"" + expression + "\" but found: []");
	}

	@Test
	void doesNotExistForAnEmptyMap() throws Exception {
		String expression = "$.emptyMap";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).doesNotExist(CONTENT))
			.withMessageContaining("Expected no value at JSON path \"" + expression + "\" but found: {}");
	}

	@Test
	void doesNotExistForIndefinatePathWithResults() throws Exception {
		String expression = "$.familyMembers[?(@.name == 'Bart')]";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).doesNotExist(SIMPSONS))
			.withMessageContaining("Expected no value at JSON path \"" + expression + "\" but found: [{\"name\":\"Bart\"}]");
	}

	@Test
	void doesNotExistForIndefinatePathWithEmptyResults() throws Exception {
		new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Dilbert')]").doesNotExist(SIMPSONS);
	}

	@Test
	void assertValueIsEmptyForAnEmptyString() throws Exception {
		new JsonPathExpectationsHelper("$.emptyString").assertValueIsEmpty(CONTENT);
	}

	@Test
	void assertValueIsEmptyForAnEmptyArray() throws Exception {
		new JsonPathExpectationsHelper("$.emptyArray").assertValueIsEmpty(CONTENT);
	}

	@Test
	void assertValueIsEmptyForAnEmptyMap() throws Exception {
		new JsonPathExpectationsHelper("$.emptyMap").assertValueIsEmpty(CONTENT);
	}

	@Test
	void assertValueIsEmptyForIndefinatePathWithEmptyResults() throws Exception {
		new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Dilbert')]").assertValueIsEmpty(SIMPSONS);
	}

	@Test
	void assertValueIsEmptyForIndefinatePathWithResults() throws Exception {
		String expression = "$.familyMembers[?(@.name == 'Bart')]";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsEmpty(SIMPSONS))
			.withMessageContaining("Expected an empty value at JSON path \"" + expression + "\" but found: [{\"name\":\"Bart\"}]");
	}

	@Test
	void assertValueIsEmptyForWhitespace() throws Exception {
		String expression = "$.whitespace";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsEmpty(CONTENT))
			.withMessageContaining("Expected an empty value at JSON path \"" + expression + "\" but found: '    '");
	}

	@Test
	void assertValueIsNotEmptyForString() throws Exception {
		new JsonPathExpectationsHelper("$.str").assertValueIsNotEmpty(CONTENT);
	}

	@Test
	void assertValueIsNotEmptyForNumber() throws Exception {
		new JsonPathExpectationsHelper("$.num").assertValueIsNotEmpty(CONTENT);
	}

	@Test
	void assertValueIsNotEmptyForBoolean() throws Exception {
		new JsonPathExpectationsHelper("$.bool").assertValueIsNotEmpty(CONTENT);
	}

	@Test
	void assertValueIsNotEmptyForArray() throws Exception {
		new JsonPathExpectationsHelper("$.arr").assertValueIsNotEmpty(CONTENT);
	}

	@Test
	void assertValueIsNotEmptyForMap() throws Exception {
		new JsonPathExpectationsHelper("$.colorMap").assertValueIsNotEmpty(CONTENT);
	}

	@Test
	void assertValueIsNotEmptyForIndefinatePathWithResults() throws Exception {
		new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Bart')]").assertValueIsNotEmpty(SIMPSONS);
	}

	@Test
	void assertValueIsNotEmptyForIndefinatePathWithEmptyResults() throws Exception {
		String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(SIMPSONS))
			.withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: []");
	}

	@Test
	void assertValueIsNotEmptyForAnEmptyString() throws Exception {
		String expression = "$.emptyString";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(CONTENT))
			.withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: ''");
	}

	@Test
	void assertValueIsNotEmptyForAnEmptyArray() throws Exception {
		String expression = "$.emptyArray";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(CONTENT))
			.withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: []");
	}

	@Test
	void assertValueIsNotEmptyForAnEmptyMap() throws Exception {
		String expression = "$.emptyMap";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(CONTENT))
			.withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: {}");
	}

	@Test
	void hasJsonPath() {
		new JsonPathExpectationsHelper("$.abc").hasJsonPath("{\"abc\": \"123\"}");
	}

	@Test
	void hasJsonPathWithNull() {
		new JsonPathExpectationsHelper("$.abc").hasJsonPath("{\"abc\": null}");
	}

	@Test
	void hasJsonPathForIndefinatePathWithResults() {
		new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Bart')]").hasJsonPath(SIMPSONS);
	}

	@Test
	void hasJsonPathForIndefinatePathWithEmptyResults() {
		String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).hasJsonPath(SIMPSONS))
			.withMessageContaining("No values for JSON path \"" + expression + "\"");
	}

	@Test // SPR-16339
	void doesNotHaveJsonPath() {
		new JsonPathExpectationsHelper("$.abc").doesNotHaveJsonPath("{}");
	}

	@Test // SPR-16339
	void doesNotHaveJsonPathWithNull() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper("$.abc").doesNotHaveJsonPath("{\"abc\": null}"));
	}

	@Test
	void doesNotHaveJsonPathForIndefinatePathWithEmptyResults() {
		new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Dilbert')]").doesNotHaveJsonPath(SIMPSONS);
	}

	@Test
	void doesNotHaveEmptyPathForIndefinatePathWithResults() {
		String expression = "$.familyMembers[?(@.name == 'Bart')]";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).doesNotHaveJsonPath(SIMPSONS))
			.withMessageContaining("Expected no values at JSON path \"" + expression + "\" " + "but found: [{\"name\":\"Bart\"}]");
	}

	@Test
	void assertValue() throws Exception {
		new JsonPathExpectationsHelper("$.num").assertValue(CONTENT, 5);
	}

	@Test // SPR-14498
	void assertValueWithNumberConversion() throws Exception {
		new JsonPathExpectationsHelper("$.num").assertValue(CONTENT, 5.0);
	}

	@Test // SPR-14498
	void assertValueWithNumberConversionAndMatcher() throws Exception {
		new JsonPathExpectationsHelper("$.num").assertValue(CONTENT, is(5.0), Double.class);
	}

	@Test
	void assertValueIsString() throws Exception {
		new JsonPathExpectationsHelper("$.str").assertValueIsString(CONTENT);
	}

	@Test
	void assertValueIsStringForAnEmptyString() throws Exception {
		new JsonPathExpectationsHelper("$.emptyString").assertValueIsString(CONTENT);
	}

	@Test
	void assertValueIsStringForNonString() throws Exception {
		String expression = "$.bool";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsString(CONTENT))
			.withMessageContaining("Expected a string at JSON path \"" + expression + "\" but found: true");
	}

	@Test
	void assertValueIsNumber() throws Exception {
		new JsonPathExpectationsHelper("$.num").assertValueIsNumber(CONTENT);
	}

	@Test
	void assertValueIsNumberForNonNumber() throws Exception {
		String expression = "$.bool";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsNumber(CONTENT))
			.withMessageContaining("Expected a number at JSON path \"" + expression + "\" but found: true");
	}

	@Test
	void assertValueIsBoolean() throws Exception {
		new JsonPathExpectationsHelper("$.bool").assertValueIsBoolean(CONTENT);
	}

	@Test
	void assertValueIsBooleanForNonBoolean() throws Exception {
		String expression = "$.num";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsBoolean(CONTENT))
			.withMessageContaining("Expected a boolean at JSON path \"" + expression + "\" but found: 5");
	}

	@Test
	void assertValueIsArray() throws Exception {
		new JsonPathExpectationsHelper("$.arr").assertValueIsArray(CONTENT);
	}

	@Test
	void assertValueIsArrayForAnEmptyArray() throws Exception {
		new JsonPathExpectationsHelper("$.emptyArray").assertValueIsArray(CONTENT);
	}

	@Test
	void assertValueIsArrayForNonArray() throws Exception {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsArray(CONTENT))
			.withMessageContaining("Expected an array at JSON path \"" + expression + "\" but found: 'foo'");
	}

	@Test
	void assertValueIsMap() throws Exception {
		new JsonPathExpectationsHelper("$.colorMap").assertValueIsMap(CONTENT);
	}

	@Test
	void assertValueIsMapForAnEmptyMap() throws Exception {
		new JsonPathExpectationsHelper("$.emptyMap").assertValueIsMap(CONTENT);
	}

	@Test
	void assertValueIsMapForNonMap() throws Exception {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsMap(CONTENT))
			.withMessageContaining("Expected a map at JSON path \"" + expression + "\" but found: 'foo'");
	}

}
