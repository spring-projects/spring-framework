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

	private static final String CONTENT = "{"         + //
			"\"str\":           \"foo\","             + //
			"\"nr\":            5,"                   + //
			"\"bool\":          true,"                + //
			"\"arr\":           [\"bar\"],"           + //
			"\"emptyArray\":    [],"                  + //
			"\"colorMap\":      {\"red\": \"rojo\"}," + //
			"\"emptyMap\":      {},"                  + //
	"}";


	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void exists() throws Exception {
		new JsonPathExpectationsHelper("$.str").exists(CONTENT);
	}

	@Test
	public void doesNotExist() throws Exception {
		new JsonPathExpectationsHelper("$.bogus").doesNotExist(CONTENT);
	}

	@Test
	public void assertValue() throws Exception {
		new JsonPathExpectationsHelper("$.nr").assertValue(CONTENT, 5);
	}

	@Test
	public void assertValueWithDifferentExpectedType() throws Exception {
		exception.expect(AssertionError.class);
		exception.expectMessage(equalTo("For JSON path \"$.nr\", type of value expected:<java.lang.String> but was:<java.lang.Integer>"));
		new JsonPathExpectationsHelper("$.nr").assertValue(CONTENT, "5");
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
		new JsonPathExpectationsHelper("$.nr").assertValueIsNumber(CONTENT);
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
		new JsonPathExpectationsHelper("$.nr").assertValueIsBoolean(CONTENT);
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
