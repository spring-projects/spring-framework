/*
 * Copyright 2002-2016 the original author or authors.
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

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matcher;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.core.IsInstanceOf.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * A helper class for applying assertions via JSON path expressions.
 *
 * <p>Based on the <a href="https://github.com/jayway/JsonPath">JsonPath</a>
 * project: requiring version 0.9+, with 1.1+ strongly recommended.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Craig Andrews
 * @author Sam Brannen
 * @since 3.2
 */
public class JsonPathExpectationsHelper {

	private final String expression;

	private final JsonPath jsonPath;


	/**
	 * Construct a new {@code JsonPathExpectationsHelper}.
	 * @param expression the {@link JsonPath} expression; never {@code null} or empty
	 * @param args arguments to parameterize the {@code JsonPath} expression with,
	 * using formatting specifiers defined in {@link String#format(String, Object...)}
	 */
	public JsonPathExpectationsHelper(String expression, Object... args) {
		Assert.hasText(expression, "expression must not be null or empty");
		this.expression = String.format(expression, args);
		this.jsonPath = JsonPath.compile(this.expression);
	}


	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert the resulting value with the given {@code Matcher}.
	 * @param content the JSON content
	 * @param matcher the matcher with which to assert the result
	 */
	@SuppressWarnings("unchecked")
	public <T> void assertValue(String content, Matcher<T> matcher) throws ParseException {
		T value = (T) evaluateJsonPath(content);
		assertThat("JSON path \"" + this.expression + "\"", value, matcher);
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that the result is equal to the expected value.
	 * @param content the JSON content
	 * @param expectedValue the expected value
	 */
	public void assertValue(String content, Object expectedValue) throws ParseException {
		Object actualValue = evaluateJsonPath(content);
		if ((actualValue instanceof List) && !(expectedValue instanceof List)) {
			@SuppressWarnings("rawtypes")
			List actualValueList = (List) actualValue;
			if (actualValueList.isEmpty()) {
				fail("No matching value at JSON path \"" + this.expression + "\"");
			}
			if (actualValueList.size() != 1) {
				fail("Got a list of values " + actualValue + " instead of the expected single value " + expectedValue);
			}
			actualValue = actualValueList.get(0);
		}
		else if (actualValue != null && expectedValue != null) {
			assertEquals("At JSON path \"" + this.expression + "\", type of value",
					expectedValue.getClass().getName(), actualValue.getClass().getName());
		}
		assertEquals("JSON path \"" + this.expression + "\"", expectedValue, actualValue);
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that the resulting value is a {@link String}.
	 * @param content the JSON content
	 * @since 4.2.1
	 */
	public void assertValueIsString(String content) throws ParseException {
		Object value = assertExistsAndReturn(content);
		assertThat(failureReason("a string", value), value, instanceOf(String.class));
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that the resulting value is a {@link Boolean}.
	 * @param content the JSON content
	 * @since 4.2.1
	 */
	public void assertValueIsBoolean(String content) throws ParseException {
		Object value = assertExistsAndReturn(content);
		assertThat(failureReason("a boolean", value), value, instanceOf(Boolean.class));
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that the resulting value is a {@link Number}.
	 * @param content the JSON content
	 * @since 4.2.1
	 */
	public void assertValueIsNumber(String content) throws ParseException {
		Object value = assertExistsAndReturn(content);
		assertThat(failureReason("a number", value), value, instanceOf(Number.class));
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that the resulting value is an array.
	 * @param content the JSON content
	 */
	public void assertValueIsArray(String content) throws ParseException {
		Object value = assertExistsAndReturn(content);
		assertThat(failureReason("an array", value), value, instanceOf(List.class));
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that the resulting value is a {@link Map}.
	 * @param content the JSON content
	 * @since 4.2.1
	 */
	public void assertValueIsMap(String content) throws ParseException {
		Object value = assertExistsAndReturn(content);
		assertThat(failureReason("a map", value), value, instanceOf(Map.class));
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that a non-null value exists at the given path.
	 * <p>If the JSON path expression is not
	 * {@linkplain JsonPath#isDefinite() definite}, this method asserts
	 * that the value at the given path is not <em>empty</em>.
	 * @param content the JSON content
	 */
	public void exists(String content) throws ParseException {
		assertExistsAndReturn(content);
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that a value does not exist at the given path.
	 * <p>If the JSON path expression is not
	 * {@linkplain JsonPath#isDefinite() definite}, this method asserts
	 * that the value at the given path is <em>empty</em>.
	 * @param content the JSON content
	 */
	public void doesNotExist(String content) throws ParseException {
		Object value;
		try {
			value = evaluateJsonPath(content);
		}
		catch (AssertionError ex) {
			return;
		}
		String reason = failureReason("no value", value);
		if (pathIsIndefinite() && value instanceof List) {
			assertTrue(reason, ((List<?>) value).isEmpty());
		}
		else {
			assertTrue(reason, value == null);
		}
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that an empty value exists at the given path.
	 * <p>For the semantics of <em>empty</em>, consult the Javadoc for
	 * {@link ObjectUtils#isEmpty(Object)}.
	 * @param content the JSON content
	 */
	public void assertValueIsEmpty(String content) throws ParseException {
		Object value = evaluateJsonPath(content);
		assertTrue(failureReason("an empty value", value), ObjectUtils.isEmpty(value));
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that a non-empty value exists at the given path.
	 * <p>For the semantics of <em>empty</em>, consult the Javadoc for
	 * {@link ObjectUtils#isEmpty(Object)}.
	 * @param content the JSON content
	 */
	public void assertValueIsNotEmpty(String content) throws ParseException {
		Object value = evaluateJsonPath(content);
		assertTrue(failureReason("a non-empty value", value), !ObjectUtils.isEmpty(value));
	}

	private String failureReason(String expectedDescription, Object value) {
		return String.format("Expected %s at JSON path \"%s\" but found: %s", expectedDescription, this.expression,
			ObjectUtils.nullSafeToString(StringUtils.quoteIfString(value)));
	}

	private Object evaluateJsonPath(String content) throws ParseException {
		String message = "No value at JSON path \"" + this.expression + "\", exception: ";
		try {
			return this.jsonPath.read(content);
		}
		catch (InvalidPathException | IndexOutOfBoundsException ex) {
			throw new AssertionError(message + ex.getMessage());
		}
	}

	private Object assertExistsAndReturn(String content) throws ParseException {
		Object value = evaluateJsonPath(content);
		String reason = "No value at JSON path \"" + this.expression + "\"";
		assertTrue(reason, value != null);
		if (pathIsIndefinite() && value instanceof List) {
			assertTrue(reason, !((List<?>) value).isEmpty());
		}
		return value;
	}

	private boolean pathIsIndefinite() {
		return !this.jsonPath.isDefinite();
	}

}
