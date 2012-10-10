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
package org.springframework.test.web.mock.client.match;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.hamcrest.Matcher;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.mock.client.RequestMatcher;
import org.springframework.test.web.mock.support.JsonPathExpectationsHelper;

/**
 * Factory methods for request content {@code RequestMatcher}'s using a <a
 * href="http://goessner.net/articles/JsonPath/">JSONPath</a> expression.
 * An instance of this class is typically accessed via
 * {@code RequestMatchers.jsonPath(..)}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class JsonPathRequestMatchers {

	private JsonPathExpectationsHelper jsonPathHelper;


	/**
	 * Class constructor, not for direct instantiation. Use
	 * {@link MockRestRequestMatchers#jsonPath(String, Matcher)} or
	 * {@link MockRestRequestMatchers#jsonPath(String, Object...)}.
	 *
	 * @param expression the JSONPath expression
	 * @param args arguments to parameterize the JSONPath expression with using
	 * the formatting specifiers defined in
	 * {@link String#format(String, Object...)}
	 */
	protected JsonPathRequestMatchers(String expression, Object ... args) {
		this.jsonPathHelper = new JsonPathExpectationsHelper(expression, args);
	}

	/**
	 * Evaluate the JSONPath and assert the resulting value with the given {@code Matcher}.
	 */
	public <T> RequestMatcher value(final Matcher<T> matcher) {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				jsonPathHelper.assertValue(request.getBodyAsString(), matcher);
			}
		};
	}

	/**
	 * Apply the JSONPath and assert the resulting value.
	 */
	public RequestMatcher value(Object expectedValue) {
		return value(equalTo(expectedValue));
	}

	/**
	 * Apply the JSONPath and assert the resulting value.
	 */
	public RequestMatcher exists() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				jsonPathHelper.exists(request.getBodyAsString());
			}
		};
	}

	/**
	 * Evaluate the JSON path and assert the resulting content exists.
	 */
	public RequestMatcher doesNotExist() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				jsonPathHelper.doesNotExist(request.getBodyAsString());
			}
		};
	}

	/**
	 * Assert the content at the given JSONPath is an array.
	 */
	public RequestMatcher isArray() {
		return value(instanceOf(List.class));
	}


	/**
	 * Abstract base class for JSONPath {@link RequestMatcher}'s.
	 */
	private abstract static class AbstractJsonPathRequestMatcher implements RequestMatcher {

		public final void match(ClientHttpRequest request) throws IOException, AssertionError {
			try {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				matchInternal(mockRequest);
			}
			catch (ParseException e) {
				throw new AssertionError("Failed to parse JSON request content: " + e.getMessage());
			}
		}

		protected abstract void matchInternal(MockClientHttpRequest request) throws IOException, ParseException;

	}
}
