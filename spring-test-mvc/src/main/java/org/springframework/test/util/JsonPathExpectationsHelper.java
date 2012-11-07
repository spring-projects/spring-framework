/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.util.MatcherAssertionErrors.assertThat;

import java.text.ParseException;
import java.util.List;

import org.hamcrest.Matcher;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;

/**
 * A helper class for applying assertions via JSONPath expressions.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class JsonPathExpectationsHelper {

	private final String expression;

	private final JsonPath jsonPath;


	/**
	 * Class constructor.
	 *
	 * @param expression the JSONPath expression
	 * @param args arguments to parameterize the JSONPath expression with using the
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 */
	public JsonPathExpectationsHelper(String expression, Object ... args) {
		this.expression = String.format(expression, args);
		this.jsonPath = JsonPath.compile(this.expression);
	}

	/**
	 * Evaluate the JSONPath and assert the resulting value with the given {@code Matcher}.
	 * @param content the response content
	 * @param matcher the matcher to assert on the resulting json path
	 */
	@SuppressWarnings("unchecked")
	public <T> void assertValue(String content, Matcher<T> matcher) throws ParseException {
		T value = (T) evaluateJsonPath(content);
		assertThat("JSON path" + this.expression, value, matcher);
	}

	private Object evaluateJsonPath(String content) throws ParseException  {
		String message = "No value for JSON path: " + this.expression + ", exception: ";
		try {
			return this.jsonPath.read(content);
		}
		catch (InvalidPathException ex) {
			throw new AssertionError(message + ex.getMessage());
		}
		catch (ArrayIndexOutOfBoundsException ex) {
			throw new AssertionError(message + ex.getMessage());
		}
		catch (IndexOutOfBoundsException ex) {
			throw new AssertionError(message + ex.getMessage());
		}
	}

	/**
	 * Apply the JSONPath and assert the resulting value.
	 */
	public void assertValue(String responseContent, Object expectedValue) throws ParseException {
		Object actualValue = evaluateJsonPath(responseContent);
		assertEquals("JSON path" + this.expression, expectedValue, actualValue);
	}

	/**
	 * Apply the JSONPath and assert the resulting value is an array.
	 */
	public void assertValueIsArray(String responseContent) throws ParseException {
		Object actualValue = evaluateJsonPath(responseContent);
		assertTrue("No value for JSON path " + this.expression, actualValue != null);
		String reason = "Expected array at JSON path " + this.expression + " but found " + actualValue;
		assertTrue(reason, actualValue instanceof List);
	}

	/**
	 * Evaluate the JSON path and assert the resulting content exists.
	 */
	public void exists(String content) throws ParseException {
		Object value = evaluateJsonPath(content);
		String reason = "No value for JSON path " + this.expression;
		assertTrue(reason, value != null);
		if (List.class.isInstance(value)) {
			assertTrue(reason, !((List<?>) value).isEmpty());
		}
	}

	/**
	 * Evaluate the JSON path and assert it doesn't point to any content.
	 */
	public void doesNotExist(String content) throws ParseException {
		Object value;
		try {
			value = evaluateJsonPath(content);
		}
		catch (AssertionError ex) {
			return;
		}
		String reason = String.format("Expected no value for JSON path: %s but found: %s", this.expression, value);
		if (List.class.isInstance(value)) {
			assertTrue(reason, ((List<?>) value).isEmpty());
		}
		else {
			assertTrue(reason, value == null);
		}
	}
}
