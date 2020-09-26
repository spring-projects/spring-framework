/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.web.servlet.result;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringStartsWith;

import org.springframework.lang.Nullable;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.StringUtils;

/**
 * Factory for assertions on the response content using
 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expressions.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#jsonPath(String, Object...)}.
 *
 * @author Rossen Stoyanchev
 * @author Craig Andrews
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 3.2
 */
public class JsonPathResultMatchers {

	private final JsonPathExpectationsHelper jsonPathHelper;

	@Nullable
	private String prefix;


	/**
	 * Protected constructor.
	 * <p>Use {@link MockMvcResultMatchers#jsonPath(String, Object...)} or
	 * {@link MockMvcResultMatchers#jsonPath(String, Matcher)}.
	 * @param expression the {@link JsonPath} expression; never {@code null} or empty
	 * @param args arguments to parameterize the {@code JsonPath} expression with,
	 * using formatting specifiers defined in {@link String#format(String, Object...)}
	 */
	protected JsonPathResultMatchers(String expression, Object... args) {
		this.jsonPathHelper = new JsonPathExpectationsHelper(expression, args);
	}

	/**
	 * Configures the current {@code JsonPathResultMatchers} instance
	 * to verify that the JSON payload is prepended with the given prefix.
	 * <p>Use this method if the JSON payloads are prefixed to avoid
	 * Cross Site Script Inclusion (XSSI) attacks.
	 * @param prefix the string prefix prepended to the actual JSON payload
	 * @since 4.3
	 */
	public JsonPathResultMatchers prefix(String prefix) {
		this.prefix = prefix;
		return this;
	}


	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert the resulting value with the given Hamcrest {@link Matcher}.
	 * @see #value(Matcher, Class)
	 * @see #value(Object)
	 */
	public <T> ResultMatcher value(Matcher<? super T> matcher) {
		return result -> this.jsonPathHelper.assertValue(getContent(result), matcher);
	}

	/**
	 * An overloaded variant of {@link #value(Matcher)} that also accepts a
	 * target type for the resulting value that the matcher can work reliably
	 * against.
	 * <p>This can be useful for matching numbers reliably &mdash; for example,
	 * to coerce an integer into a double.
	 * @since 4.3.15
	 * @see #value(Matcher)
	 * @see #value(Object)
	 */
	public <T> ResultMatcher value(Matcher<? super T> matcher, Class<T> targetType) {
		return result -> this.jsonPathHelper.assertValue(getContent(result), matcher, targetType);
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that the result is equal to the supplied value.
	 * @see #value(Matcher)
	 * @see #value(Matcher, Class)
	 */
	public ResultMatcher value(Object expectedValue) {
		return result -> this.jsonPathHelper.assertValue(getContent(result), expectedValue);
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that a non-null value, possibly an empty array or map, exists at
	 * the given path.
	 * <p>If the JSON path expression is not {@linkplain JsonPath#isDefinite
	 * definite}, this method asserts that the value at the given path is not
	 * <em>empty</em>.
	 */
	public ResultMatcher exists() {
		return result -> this.jsonPathHelper.exists(getContent(result));
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that a non-null value does not exist at the given path.
	 * <p>If the JSON path expression is not {@linkplain JsonPath#isDefinite
	 * definite}, this method asserts that the value at the given path is
	 * <em>empty</em>.
	 */
	public ResultMatcher doesNotExist() {
		return result -> this.jsonPathHelper.doesNotExist(getContent(result));
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that an empty value exists at the given path.
	 * <p>For the semantics of <em>empty</em>, consult the Javadoc for
	 * {@link org.springframework.util.ObjectUtils#isEmpty(Object)}.
	 * @since 4.2.1
	 * @see #isNotEmpty()
	 * @see #exists()
	 * @see #doesNotExist()
	 */
	public ResultMatcher isEmpty() {
		return result -> this.jsonPathHelper.assertValueIsEmpty(getContent(result));
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that a non-empty value exists at the given path.
	 * <p>For the semantics of <em>empty</em>, consult the Javadoc for
	 * {@link org.springframework.util.ObjectUtils#isEmpty(Object)}.
	 * @since 4.2.1
	 * @see #isEmpty()
	 * @see #exists()
	 * @see #doesNotExist()
	 */
	public ResultMatcher isNotEmpty() {
		return result -> this.jsonPathHelper.assertValueIsNotEmpty(getContent(result));
	}

	/**
	 * Evaluate the JSON path expression against the response content
	 * and assert that a value, possibly {@code null}, exists.
	 * <p>If the JSON path expression is not
	 * {@linkplain JsonPath#isDefinite() definite}, this method asserts
	 * that the list of values at the given path is not <em>empty</em>.
	 * @since 5.0.3
	 * @see #exists()
	 * @see #isNotEmpty()
	 */
	public ResultMatcher hasJsonPath() {
		return result -> this.jsonPathHelper.hasJsonPath(getContent(result));
	}

	/**
	 * Evaluate the JSON path expression against the supplied {@code content}
	 * and assert that a value, including {@code null} values, does not exist
	 * at the given path.
	 * <p>If the JSON path expression is not
	 * {@linkplain JsonPath#isDefinite() definite}, this method asserts
	 * that the list of values at the given path is <em>empty</em>.
	 * @since 5.0.3
	 * @see #doesNotExist()
	 * @see #isEmpty()
	 */
	public ResultMatcher doesNotHaveJsonPath() {
		return result -> this.jsonPathHelper.doesNotHaveJsonPath(getContent(result));
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that the result is a {@link String}.
	 * @since 4.2.1
	 */
	public ResultMatcher isString() {
		return result -> this.jsonPathHelper.assertValueIsString(getContent(result));
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that the result is a {@link Boolean}.
	 * @since 4.2.1
	 */
	public ResultMatcher isBoolean() {
		return result -> this.jsonPathHelper.assertValueIsBoolean(getContent(result));
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that the result is a {@link Number}.
	 * @since 4.2.1
	 */
	public ResultMatcher isNumber() {
		return result -> this.jsonPathHelper.assertValueIsNumber(getContent(result));
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that the result is an array.
	 */
	public ResultMatcher isArray() {
		return result -> this.jsonPathHelper.assertValueIsArray(getContent(result));
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that the result is a {@link java.util.Map}.
	 * @since 4.2.1
	 */
	public ResultMatcher isMap() {
		return result -> this.jsonPathHelper.assertValueIsMap(getContent(result));
	}

	private String getContent(MvcResult result) throws UnsupportedEncodingException {
		String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		if (StringUtils.hasLength(this.prefix)) {
			try {
				String reason = String.format("Expected a JSON payload prefixed with \"%s\" but found: %s",
						this.prefix, StringUtils.quote(content.substring(0, this.prefix.length())));
				MatcherAssert.assertThat(reason, content, StringStartsWith.startsWith(this.prefix));
				return content.substring(this.prefix.length());
			}
			catch (StringIndexOutOfBoundsException ex) {
				throw new AssertionError("JSON prefix \"" + this.prefix + "\" not found", ex);
			}
		}
		else {
			return content;
		}
	}

}
