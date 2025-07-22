/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.servlet.client;

import java.util.function.Consumer;

import com.jayway.jsonpath.Configuration;
import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.util.Assert;

/**
 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> assertions.
 *
 * @author Rob Worsnop
 *
 * @see <a href="https://github.com/jayway/JsonPath">https://github.com/jayway/JsonPath</a>
 * @see JsonPathExpectationsHelper
 */
public class JsonPathAssertions {

	private final RestTestClient.BodyContentSpec bodySpec;

	private final String content;

	private final JsonPathExpectationsHelper pathHelper;


	JsonPathAssertions(RestTestClient.BodyContentSpec spec, String content, String expression, @Nullable Configuration configuration) {
		Assert.hasText(expression, "expression must not be null or empty");
		this.bodySpec = spec;
		this.content = content;
		this.pathHelper = new JsonPathExpectationsHelper(expression, configuration);
	}


	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValue(String, Object)}.
	 */
	public RestTestClient.BodyContentSpec isEqualTo(Object expectedValue) {
		this.pathHelper.assertValue(this.content, expectedValue);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#exists(String)}.
	 */
	public RestTestClient.BodyContentSpec exists() {
		this.pathHelper.exists(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#doesNotExist(String)}.
	 */
	public RestTestClient.BodyContentSpec doesNotExist() {
		this.pathHelper.doesNotExist(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsEmpty(String)}.
	 */
	public RestTestClient.BodyContentSpec isEmpty() {
		this.pathHelper.assertValueIsEmpty(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsNotEmpty(String)}.
	 */
	public RestTestClient.BodyContentSpec isNotEmpty() {
		this.pathHelper.assertValueIsNotEmpty(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#hasJsonPath}.
	 */
	public RestTestClient.BodyContentSpec hasJsonPath() {
		this.pathHelper.hasJsonPath(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#doesNotHaveJsonPath}.
	 */
	public RestTestClient.BodyContentSpec doesNotHaveJsonPath() {
		this.pathHelper.doesNotHaveJsonPath(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsBoolean(String)}.
	 */
	public RestTestClient.BodyContentSpec isBoolean() {
		this.pathHelper.assertValueIsBoolean(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsNumber(String)}.
	 */
	public RestTestClient.BodyContentSpec isNumber() {
		this.pathHelper.assertValueIsNumber(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsArray(String)}.
	 */
	public RestTestClient.BodyContentSpec isArray() {
		this.pathHelper.assertValueIsArray(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsMap(String)}.
	 */
	public RestTestClient.BodyContentSpec isMap() {
		this.pathHelper.assertValueIsMap(this.content);
		return this.bodySpec;
	}

	/**
	 * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher)}.
	 */
	public <T> RestTestClient.BodyContentSpec value(Matcher<? super T> matcher) {
		this.pathHelper.assertValue(this.content, matcher);
		return this.bodySpec;
	}

	/**
	 * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher, Class)}.
	 */
	public <T> RestTestClient.BodyContentSpec value(Class<T> targetType, Matcher<? super T> matcher) {
		this.pathHelper.assertValue(this.content, matcher, targetType);
		return this.bodySpec;
	}

	/**
	 * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher, ParameterizedTypeReference)}.
	 */
	public <T> RestTestClient.BodyContentSpec value(ParameterizedTypeReference<T> targetType, Matcher<? super T> matcher) {
		this.pathHelper.assertValue(this.content, matcher, targetType);
		return this.bodySpec;
	}

	/**
	 * Consume the result of the JSONPath evaluation.
	 */
	@SuppressWarnings("unchecked")
	public <T> RestTestClient.BodyContentSpec value(Consumer<T> consumer) {
		Object value = this.pathHelper.evaluateJsonPath(this.content);
		consumer.accept((T) value);
		return this.bodySpec;
	}

	/**
	 * Consume the result of the JSONPath evaluation and provide a target class.
	 */
	public <T> RestTestClient.BodyContentSpec value(Class<T> targetType, Consumer<T> consumer) {
		T value = this.pathHelper.evaluateJsonPath(this.content, targetType);
		consumer.accept(value);
		return this.bodySpec;
	}

	/**
	 * Consume the result of the JSONPath evaluation and provide a parameterized type.
	 */
	public <T> RestTestClient.BodyContentSpec value(ParameterizedTypeReference<T> targetType, Consumer<T> consumer) {
		T value = this.pathHelper.evaluateJsonPath(this.content, targetType);
		consumer.accept(value);
		return this.bodySpec;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		throw new AssertionError("Object#equals is disabled " +
				"to avoid being used in error instead of JsonPathAssertions#isEqualTo(String).");
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
