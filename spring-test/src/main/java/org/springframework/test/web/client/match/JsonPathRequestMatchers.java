/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.web.client.match;

import java.io.IOException;
import java.text.ParseException;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matcher;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.client.RequestMatcher;

/**
 * Factory for assertions on the request content using
 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expressions.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockRestRequestMatchers#jsonPath(String, Matcher)} or
 * {@link MockRestRequestMatchers#jsonPath(String, Object...)}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class JsonPathRequestMatchers {

	private final JsonPathExpectationsHelper jsonPathHelper;


	/**
	 * Protected constructor.
	 * <p>Use {@link MockRestRequestMatchers#jsonPath(String, Matcher)} or
	 * {@link MockRestRequestMatchers#jsonPath(String, Object...)}.
	 * @param expression the {@link JsonPath} expression; never {@code null} or empty
	 * @param args arguments to parameterize the {@code JsonPath} expression with,
	 * using formatting specifiers defined in {@link String#format(String, Object...)}
	 */
	protected JsonPathRequestMatchers(String expression, Object... args) {
		this.jsonPathHelper = new JsonPathExpectationsHelper(expression, args);
	}


	/**
	 * Evaluate the JSON path expression against the request content and
	 * assert the resulting value with the given Hamcrest {@link Matcher}.
	 */
	public <T> RequestMatcher value(Matcher<? super T> matcher) {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValue(request.getBodyAsString(), matcher);
			}
		};
	}

	/**
	 * An overloaded variant of {@link #value(Matcher)} that also accepts a
	 * target type for the resulting value that the matcher can work reliably
	 * against.
	 * <p>This can be useful for matching numbers reliably &mdash; for example,
	 * to coerce an integer into a double.
	 * @since 4.3.3
	 */
	public <T> RequestMatcher value(Matcher<? super T> matcher, Class<T> targetType) {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				String body = request.getBodyAsString();
				JsonPathRequestMatchers.this.jsonPathHelper.assertValue(body, matcher, targetType);
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the request content and
	 * assert that the result is equal to the supplied value.
	 */
	public RequestMatcher value(Object expectedValue) {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValue(request.getBodyAsString(), expectedValue);
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the request content and
	 * assert that a non-null value exists at the given path.
	 * <p>If the JSON path expression is not {@linkplain JsonPath#isDefinite
	 * definite}, this method asserts that the value at the given path is not
	 * <em>empty</em>.
	 */
	public RequestMatcher exists() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.exists(request.getBodyAsString());
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the request content and
	 * assert that a value does not exist at the given path.
	 * <p>If the JSON path expression is not {@linkplain JsonPath#isDefinite
	 * definite}, this method asserts that the value at the given path is
	 * <em>empty</em>.
	 */
	public RequestMatcher doesNotExist() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.doesNotExist(request.getBodyAsString());
			}
		};
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
	public RequestMatcher hasJsonPath() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) {
				JsonPathRequestMatchers.this.jsonPathHelper.hasJsonPath(request.getBodyAsString());
			}
		};
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
	public RequestMatcher doesNotHaveJsonPath() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) {
				JsonPathRequestMatchers.this.jsonPathHelper.doesNotHaveJsonPath(request.getBodyAsString());
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the request content and
	 * assert that an empty value exists at the given path.
	 * <p>For the semantics of <em>empty</em>, consult the Javadoc for
	 * {@link org.springframework.util.ObjectUtils#isEmpty(Object)}.
	 * @since 4.2.1
	 * @see #isNotEmpty()
	 * @see #exists()
	 * @see #doesNotExist()
	 */
	public RequestMatcher isEmpty() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsEmpty(request.getBodyAsString());
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the request content and
	 * assert that a non-empty value exists at the given path.
	 * <p>For the semantics of <em>empty</em>, consult the Javadoc for
	 * {@link org.springframework.util.ObjectUtils#isEmpty(Object)}.
	 * @since 4.2.1
	 * @see #isEmpty()
	 * @see #exists()
	 * @see #doesNotExist()
	 */
	public RequestMatcher isNotEmpty() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsNotEmpty(request.getBodyAsString());
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the request content and
	 * assert that the result is a {@link String}.
	 * @since 4.2.1
	 */
	public RequestMatcher isString() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsString(request.getBodyAsString());
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the request content and
	 * assert that the result is a {@link Boolean}.
	 * @since 4.2.1
	 */
	public RequestMatcher isBoolean() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsBoolean(request.getBodyAsString());
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the request content and
	 * assert that the result is a {@link Number}.
	 * @since 4.2.1
	 */
	public RequestMatcher isNumber() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsNumber(request.getBodyAsString());
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the request content and
	 * assert that the result is an array.
	 */
	public RequestMatcher isArray() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsArray(request.getBodyAsString());
			}
		};
	}

	/**
	 * Evaluate the JSON path expression against the request content and
	 * assert that the result is a {@link java.util.Map}.
	 * @since 4.2.1
	 */
	public RequestMatcher isMap() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsMap(request.getBodyAsString());
			}
		};
	}


	/**
	 * Abstract base class for {@code JsonPath}-based {@link RequestMatcher RequestMatchers}.
	 * @see #matchInternal
	 */
	private abstract static class AbstractJsonPathRequestMatcher implements RequestMatcher {

		@Override
		public final void match(ClientHttpRequest request) throws IOException, AssertionError {
			try {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				matchInternal(mockRequest);
			}
			catch (ParseException ex) {
				throw new AssertionError("Failed to parse JSON request content", ex);
			}
		}

		abstract void matchInternal(MockClientHttpRequest request) throws IOException, ParseException;
	}

}
