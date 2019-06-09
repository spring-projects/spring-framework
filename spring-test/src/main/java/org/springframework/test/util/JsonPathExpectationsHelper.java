/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

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
	public <T> void assertValue(String content, Matcher<T> matcher) {
		T value = (T) evaluateJsonPath(content);
		MatcherAssert.assertThat("JSON path \"" + this.expression + "\"", value, matcher);
	}

	/**
	 * An overloaded variant of {@link #assertValue(String, Matcher)} that also
	 * accepts a target type for the resulting value. This can be useful for
	 * matching numbers reliably for example coercing an integer into a double.
	 * @param content the JSON content
	 * @param matcher the matcher with which to assert the result
	 * @param targetType a the expected type of the resulting value
	 * @since 4.3.3
	 */
	@SuppressWarnings("unchecked")
	public <T> void assertValue(String content, Matcher<T> matcher, Class<T> targetType) {
		T value = (T) evaluateJsonPath(content, targetType);
		MatcherAssert.assertThat("JSON path \"" + this.expression + "\"", value, matcher);
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that the result is equal to the expected value.
	 * @param content the JSON content
	 * @param expectedValue the expected value
	 */
	public void assertValue(String content, @Nullable Object expectedValue) {
		Object actualValue = evaluateJsonPath(content);
		if ((actualValue instanceof List) && !(expectedValue instanceof List)) {
			@SuppressWarnings("rawtypes")
			List actualValueList = (List) actualValue;
			if (actualValueList.isEmpty()) {
				AssertionErrors.fail("No matching value at JSON path \"" + this.expression + "\"");
			}
			if (actualValueList.size() != 1) {
				AssertionErrors.fail("Got a list of values " + actualValue +
						" instead of the expected single value " + expectedValue);
			}
			actualValue = actualValueList.get(0);
		}
		else if (actualValue != null && expectedValue != null) {
			if (!actualValue.getClass().equals(expectedValue.getClass())) {
				actualValue = evaluateJsonPath(content, expectedValue.getClass());
			}
		}
		AssertionErrors.assertEquals("JSON path \"" + this.expression + "\"", expectedValue, actualValue);
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that the resulting value is a {@link String}.
	 * @param content the JSON content
	 * @since 4.2.1
	 */
	public void assertValueIsString(String content) {
		Object value = assertExistsAndReturn(content);
		MatcherAssert.assertThat(failureReason("a string", value), value, CoreMatchers.instanceOf(String.class));
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that the resulting value is a {@link Boolean}.
	 * @param content the JSON content
	 * @since 4.2.1
	 */
	public void assertValueIsBoolean(String content) {
		Object value = assertExistsAndReturn(content);
		MatcherAssert.assertThat(failureReason("a boolean", value), value, CoreMatchers.instanceOf(Boolean.class));
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that the resulting value is a {@link Number}.
	 * @param content the JSON content
	 * @since 4.2.1
	 */
	public void assertValueIsNumber(String content) {
		Object value = assertExistsAndReturn(content);
		MatcherAssert.assertThat(failureReason("a number", value), value, CoreMatchers.instanceOf(Number.class));
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that the resulting value is an array.
	 * @param content the JSON content
	 */
	public void assertValueIsArray(String content) {
		Object value = assertExistsAndReturn(content);
		MatcherAssert.assertThat(failureReason("an array", value), value, CoreMatchers.instanceOf(List.class));
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that the resulting value is a {@link Map}.
	 * @param content the JSON content
	 * @since 4.2.1
	 */
	public void assertValueIsMap(String content) {
		Object value = assertExistsAndReturn(content);
		MatcherAssert.assertThat(failureReason("a map", value), value, CoreMatchers.instanceOf(Map.class));
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that a non-null value, possibly an empty array or map, exists
	 * at the given path.
	 * <p>Note that if the JSON path expression is not
	 * {@linkplain JsonPath#isDefinite() definite}, this method asserts
	 * that the list of values at the given path is not <em>empty</em>.
	 * @param content the JSON content
	 */
	public void exists(String content) {
		assertExistsAndReturn(content);
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that a non-null value does not exist at the given path.
	 * <p>Note that if the JSON path expression is not
	 * {@linkplain JsonPath#isDefinite() definite}, this method asserts
	 * that the list of values at the given path is <em>empty</em>.
	 * @param content the JSON content
	 */
	public void doesNotExist(String content) {
		Object value;
		try {
			value = evaluateJsonPath(content);
		}
		catch (AssertionError ex) {
			return;
		}
		String reason = failureReason("no value", value);
		if (pathIsIndefinite() && value instanceof List) {
			AssertionErrors.assertTrue(reason, ((List<?>) value).isEmpty());
		}
		else {
			AssertionErrors.assertTrue(reason, (value == null));
		}
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that an empty value exists at the given path.
	 * <p>For the semantics of <em>empty</em>, consult the Javadoc for
	 * {@link ObjectUtils#isEmpty(Object)}.
	 * @param content the JSON content
	 */
	public void assertValueIsEmpty(String content) {
		Object value = evaluateJsonPath(content);
		AssertionErrors.assertTrue(failureReason("an empty value", value), ObjectUtils.isEmpty(value));
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that a non-empty value exists at the given path.
	 * <p>For the semantics of <em>empty</em>, consult the Javadoc for
	 * {@link ObjectUtils#isEmpty(Object)}.
	 * @param content the JSON content
	 */
	public void assertValueIsNotEmpty(String content) {
		Object value = evaluateJsonPath(content);
		AssertionErrors.assertTrue(failureReason("a non-empty value", value), !ObjectUtils.isEmpty(value));
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that a value, possibly {@code null}, exists.
	 * <p>If the JSON path expression is not
	 * {@linkplain JsonPath#isDefinite() definite}, this method asserts
	 * that the list of values at the given path is not <em>empty</em>.
	 * @param content the JSON content
	 * @since 5.0.3
	 */
	public void hasJsonPath(String content) {
		Object value = evaluateJsonPath(content);
		if (pathIsIndefinite() && value instanceof List) {
			String message = "No values for JSON path \"" + this.expression + "\"";
			AssertionErrors.assertTrue(message, !((List<?>) value).isEmpty());
		}
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that a value, including {@code null} values, does not exist
	 * at the given path.
	 * <p>If the JSON path expression is not
	 * {@linkplain JsonPath#isDefinite() definite}, this method asserts
	 * that the list of values at the given path is <em>empty</em>.
	 * @param content the JSON content
	 * @since 5.0.3
	 */
	public void doesNotHaveJsonPath(String content) {
		Object value;
		try {
			value = evaluateJsonPath(content);
		}
		catch (AssertionError ex) {
			return;
		}
		if (pathIsIndefinite() && value instanceof List) {
			AssertionErrors.assertTrue(failureReason("no values", value), ((List<?>) value).isEmpty());
		}
		else {
			AssertionErrors.fail(failureReason("no value", value));
		}
	}

	private String failureReason(String expectedDescription, @Nullable Object value) {
		return String.format("Expected %s at JSON path \"%s\" but found: %s", expectedDescription, this.expression,
				ObjectUtils.nullSafeToString(StringUtils.quoteIfString(value)));
	}

	/**
	 * Evaluate the JSON path and return the resulting value.
	 * @param content the content to evaluate against
	 * @return the result of the evaluation
	 * @throws AssertionError if the evaluation fails
	 */
	@Nullable
	public Object evaluateJsonPath(String content) {
		try {
			return this.jsonPath.read(content);
		}
		catch (Throwable ex) {
			throw new AssertionError("No value at JSON path \"" + this.expression + "\"", ex);
		}
	}

	/**
	 * Variant of {@link #evaluateJsonPath(String)} with a target type.
	 * This can be useful for matching numbers reliably for example coercing an
	 * integer into a double.
	 * @param content the content to evaluate against
	 * @return the result of the evaluation
	 * @throws AssertionError if the evaluation fails
	 */
	public Object evaluateJsonPath(String content, Class<?> targetType) {
		try {
			return JsonPath.parse(content).read(this.expression, targetType);
		}
		catch (Throwable ex) {
			String message = "No value at JSON path \"" + this.expression + "\"";
			throw new AssertionError(message, ex);
		}
	}

	@Nullable
	private Object assertExistsAndReturn(String content) {
		Object value = evaluateJsonPath(content);
		String reason = "No value at JSON path \"" + this.expression + "\"";
		AssertionErrors.assertTrue(reason, value != null);
		if (pathIsIndefinite() && value instanceof List) {
			AssertionErrors.assertTrue(reason, !((List<?>) value).isEmpty());
		}
		return value;
	}

	private boolean pathIsIndefinite() {
		return !this.jsonPath.isDefinite();
	}

}
