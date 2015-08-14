/*
 * Copyright 2004-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.*;

/**
 * Unit tests for {@link JsonPathExpectationsHelper}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class JsonPathExpectationsHelperTests {

	private static final String CONTENT = "{" + //
			"'str':        'foo',           " + //
			"'num':        5,               " + //
			"'bool':       true,            " + //
			"'arr':        [42],            " + //
			"'emptyArray': [],              " + //
			"'colorMap':   {'red': 'rojo'}, " + //
			"'emptyMap':   {}               " + //
	"}";

	private static final String SIMPSONS = "{ 'familyMembers': [ " + //
			"{'name': 'Homer' }, " + //
			"{'name': 'Marge' }, " + //
			"{'name': 'Bart'  }, " + //
			"{'name': 'Lisa'  }, " + //
			"{'name': 'Maggie'}  " + //
	" ] }";

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void exists() throws Exception {
		new JsonPathExpectationsHelper("$.str").exists(CONTENT);
	}

	@Test
	public void existsForAnEmptyArray() throws Exception {
		new JsonPathExpectationsHelper("$.emptyArray").exists(CONTENT);
	}

	@Test
	public void existsForAnEmptyMap() throws Exception {
		new JsonPathExpectationsHelper("$.emptyMap").exists(CONTENT);
	}

	@Test
	public void existsForIndefinatePathWithResults() throws Exception {
		new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Bart')]").exists(SIMPSONS);
	}

	@Test
	public void existsForIndefinatePathWithEmptyResults() throws Exception {
		String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
		exception.expect(AssertionError.class);
		exception.expectMessage("No value at JSON path \"" + expression + "\"");
		new JsonPathExpectationsHelper(expression).exists(SIMPSONS);
	}

	@Test
	public void doesNotExist() throws Exception {
		new JsonPathExpectationsHelper("$.bogus").doesNotExist(CONTENT);
	}

	@Test
	public void doesNotExistForAnEmptyArray() throws Exception {
		String expression = "$.emptyArray";
		exception.expect(AssertionError.class);
		exception.expectMessage("Expected no value at JSON path \"" + expression + "\" but found: []");
		new JsonPathExpectationsHelper(expression).doesNotExist(CONTENT);
	}

	@Test
	public void doesNotExistForAnEmptyMap() throws Exception {
		String expression = "$.emptyMap";
		exception.expect(AssertionError.class);
		exception.expectMessage("Expected no value at JSON path \"" + expression + "\" but found: {}");
		new JsonPathExpectationsHelper(expression).doesNotExist(CONTENT);
	}

	@Test
	public void doesNotExistForIndefinatePathWithResults() throws Exception {
		String expression = "$.familyMembers[?(@.name == 'Bart')]";
		exception.expect(AssertionError.class);
		exception.expectMessage("Expected no value at JSON path \"" + expression
				+ "\" but found: [{\"name\":\"Bart\"}]");
		new JsonPathExpectationsHelper(expression).doesNotExist(SIMPSONS);
	}

	@Test
	public void doesNotExistForIndefinatePathWithEmptyResults() throws Exception {
		String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
		new JsonPathExpectationsHelper(expression).doesNotExist(SIMPSONS);
	}

	@Test
	public void assertValue() throws Exception {
		new JsonPathExpectationsHelper("$.num").assertValue(CONTENT, 5);
	}

	@Test
	public void assertValueWithDifferentExpectedType() throws Exception {
		exception.expect(AssertionError.class);
		exception.expectMessage(equalTo("At JSON path \"$.num\", type of value expected:<java.lang.String> but was:<java.lang.Integer>"));
		new JsonPathExpectationsHelper("$.num").assertValue(CONTENT, "5");
	}

	@Test
	public void assertValueIsString() throws Exception {
		new JsonPathExpectationsHelper("$.str").assertValueIsString(CONTENT);
	}

	@Test
	public void assertValueIsStringForNonString() throws Exception {
		exception.expect(AssertionError.class);
		new JsonPathExpectationsHelper("$.bool").assertValueIsString(CONTENT);
	}

	@Test
	public void assertValueIsNumber() throws Exception {
		new JsonPathExpectationsHelper("$.num").assertValueIsNumber(CONTENT);
	}

	@Test
	public void assertValueIsNumberForNonNumber() throws Exception {
		exception.expect(AssertionError.class);
		new JsonPathExpectationsHelper("$.bool").assertValueIsNumber(CONTENT);
	}

	@Test
	public void assertValueIsBoolean() throws Exception {
		new JsonPathExpectationsHelper("$.bool").assertValueIsBoolean(CONTENT);
	}

	@Test
	public void assertValueIsBooleanForNonBoolean() throws Exception {
		exception.expect(AssertionError.class);
		new JsonPathExpectationsHelper("$.num").assertValueIsBoolean(CONTENT);
	}

	@Test
	public void assertValueIsArray() throws Exception {
		new JsonPathExpectationsHelper("$.arr").assertValueIsArray(CONTENT);
	}

	@Test
	public void assertValueIsArrayForAnEmptyArray() throws Exception {
		new JsonPathExpectationsHelper("$.emptyArray").assertValueIsArray(CONTENT);
	}

	@Test
	public void assertValueIsArrayForNonArray() throws Exception {
		exception.expect(AssertionError.class);
		new JsonPathExpectationsHelper("$.str").assertValueIsArray(CONTENT);
	}

	@Test
	public void assertValueIsMap() throws Exception {
		new JsonPathExpectationsHelper("$.colorMap").assertValueIsMap(CONTENT);
	}

	@Test
	public void assertValueIsMapForAnEmptyMap() throws Exception {
		new JsonPathExpectationsHelper("$.emptyMap").assertValueIsMap(CONTENT);
	}

	@Test
	public void assertValueIsMapForNonMap() throws Exception {
		exception.expect(AssertionError.class);
		new JsonPathExpectationsHelper("$.str").assertValueIsMap(CONTENT);
	}

}
