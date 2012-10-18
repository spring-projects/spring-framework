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

package org.springframework.test.web.servlet.result;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.util.List;

import org.hamcrest.Matcher;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Factory for assertions on the response content using <a
 * href="http://goessner.net/articles/JsonPath/">JSONPath</a> expressions.
 * An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#jsonPpath}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class JsonPathResultMatchers {

	private JsonPathExpectationsHelper jsonPathHelper;

	/**
	 * Protected constructor. Use
	 * {@link MockMvcResultMatchers#jsonPath(String, Object...)} or
	 * {@link MockMvcResultMatchers#jsonPath(String, Matcher)}.
	 */
	protected JsonPathResultMatchers(String expression, Object ... args) {
		this.jsonPathHelper = new JsonPathExpectationsHelper(expression, args);
	}

	/**
	 * Evaluate the JSONPath and assert the value of the content found with the
	 * given Hamcrest {@code Matcher}.
	 */
	public <T> ResultMatcher value(final Matcher<T> matcher) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				jsonPathHelper.assertValue(content, matcher);
			}
		};
	}

	/**
	 * Evaluate the JSONPath and assert the value of the content found.
	 */
	public ResultMatcher value(Object value) {
		return value(equalTo(value));
	}

	/**
	 * Evaluate the JSONPath and assert that content exists.
	 */
	public ResultMatcher exists() {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				jsonPathHelper.exists(content);
			}
		};
	}

	/**
	 * Evaluate the JSON path and assert not content was found.
	 */
	public ResultMatcher doesNotExist() {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				jsonPathHelper.doesNotExist(content);
			}
		};
	}

	/**
	 * Evluate the JSON path and assert the content found is an array.
	 */
	public ResultMatcher isArray() {
		return value(instanceOf(List.class));
	}
}
