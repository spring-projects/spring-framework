/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.web.servlet.result;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matcher;

import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Factory for assertions on the response content using
 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expressions.
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#jsonPath(String, Matcher)} or
 * {@link MockMvcResultMatchers#jsonPath(String, Object...)}.
 *
 * @author Rossen Stoyanchev
 * @author Craig Andrews
 * @author Sam Brannen
 * @since 3.2
 */
public class JsonPathResultMatchers {

	private final JsonPathExpectationsHelper jsonPathHelper;


	/**
	 * Protected constructor.
	 * <p>Use {@link MockMvcResultMatchers#jsonPath(String, Object...)} or
	 * {@link MockMvcResultMatchers#jsonPath(String, Matcher)}.
	 * @param expression the {@link JsonPath} expression; never {@code null} or empty
	 * @param args arguments to parameterize the {@code JsonPath} expression with,
	 * using formatting specifiers defined in {@link String#format(String, Object...)}
	 */
	protected JsonPathResultMatchers(String expression, Object ... args) {
		this.jsonPathHelper = new JsonPathExpectationsHelper(expression, args);
	}


	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert the resulting value with the given Hamcrest {@link Matcher}.
	 */
	public <T> ResultMatcher value(final Matcher<T> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				jsonPathHelper.assertValue(content, matcher);
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that the result is equal to the supplied value.
	 */
	public ResultMatcher value(final Object expectedValue) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				jsonPathHelper.assertValue(result.getResponse().getContentAsString(), expectedValue);
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that a non-null value exists at the given path.
	 * <p>If the JSON path expression is not {@linkplain JsonPath#isDefinite
	 * definite}, this method asserts that the value at the given path is not
	 * <em>empty</em>.
	 */
	public ResultMatcher exists() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				jsonPathHelper.exists(content);
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that a value does not exist at the given path.
	 * <p>If the JSON path expression is not {@linkplain JsonPath#isDefinite
	 * definite}, this method asserts that the value at the given path is
	 * <em>empty</em>.
	 */
	public ResultMatcher doesNotExist() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				jsonPathHelper.doesNotExist(content);
			}
		};
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
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				jsonPathHelper.assertValueIsEmpty(content);
			}
		};
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
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				jsonPathHelper.assertValueIsNotEmpty(content);
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that the result is a {@link String}.
	 * @since 4.2.1
	 */
	public ResultMatcher isString() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				jsonPathHelper.assertValueIsString(content);
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that the result is a {@link Boolean}.
	 * @since 4.2.1
	 */
	public ResultMatcher isBoolean() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				jsonPathHelper.assertValueIsBoolean(content);
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that the result is a {@link Number}.
	 * @since 4.2.1
	 */
	public ResultMatcher isNumber() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				jsonPathHelper.assertValueIsNumber(content);
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that the result is an array.
	 */
	public ResultMatcher isArray() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				jsonPathHelper.assertValueIsArray(content);
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the response content and
	 * assert that the result is a {@link java.util.Map}.
	 * @since 4.2.1
	 */
	public ResultMatcher isMap() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				jsonPathHelper.assertValueIsMap(content);
			}
		};
	}

}
