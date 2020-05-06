/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.test.web.reactive.server;

import java.util.function.Consumer;

import org.hamcrest.Matcher;

import org.springframework.lang.Nullable;
import org.springframework.test.util.JsonPathExpectationsHelper;

/**
 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> assertions.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see <a href="https://github.com/jayway/JsonPath">https://github.com/jayway/JsonPath</a>
 * @see JsonPathExpectationsHelper
 */
public class JsonPathAssertions {

	private final WebTestClient.BodyContentSpec bodySpec;

	private final String content;

	private final JsonPathExpectationsHelper pathHelper;


	JsonPathAssertions(WebTestClient.BodyContentSpec spec, String content, String expression, Object... args) {
		this.bodySpec = spec;
		this.content = content;
		this.pathHelper = new JsonPathExpectationsHelper(expression, args);
	}


	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValue(String, Object)}.
	 */
	public WebTestClient.BodyContentSpec isEqualTo(Object expectedValue) {
		this.pathHelper.assertValue(this.content, expectedValue);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#exists(String)}.
	 */
	public WebTestClient.BodyContentSpec exists() {
		this.pathHelper.exists(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#doesNotExist(String)}.
	 */
	public WebTestClient.BodyContentSpec doesNotExist() {
		this.pathHelper.doesNotExist(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsEmpty(String)}.
	 */
	public WebTestClient.BodyContentSpec isEmpty() {
		this.pathHelper.assertValueIsEmpty(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsNotEmpty(String)}.
	 */
	public WebTestClient.BodyContentSpec isNotEmpty() {
		this.pathHelper.assertValueIsNotEmpty(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#hasJsonPath}.
	 * @since 5.0.3
	 */
	public WebTestClient.BodyContentSpec hasJsonPath() {
		this.pathHelper.hasJsonPath(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#doesNotHaveJsonPath}.
	 * @since 5.0.3
	 */
	public WebTestClient.BodyContentSpec doesNotHaveJsonPath() {
		this.pathHelper.doesNotHaveJsonPath(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsBoolean(String)}.
	 */
	public WebTestClient.BodyContentSpec isBoolean() {
		this.pathHelper.assertValueIsBoolean(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsNumber(String)}.
	 */
	public WebTestClient.BodyContentSpec isNumber() {
		this.pathHelper.assertValueIsNumber(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsArray(String)}.
	 */
	public WebTestClient.BodyContentSpec isArray() {
		this.pathHelper.assertValueIsArray(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsMap(String)}.
	 */
	public WebTestClient.BodyContentSpec isMap() {
		this.pathHelper.assertValueIsMap(this.content);
		return this.bodySpec;
	}

	/**
	 * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher)}.
	 * @since 5.1
	 */
	public <T> WebTestClient.BodyContentSpec value(Matcher<T> matcher) {
		this.pathHelper.assertValue(this.content, matcher);
		return this.bodySpec;
	}

	/**
	 * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher, Class)}.
	 * @since 5.1
	 */
	public <T> WebTestClient.BodyContentSpec value(Matcher<T> matcher, Class<T> targetType) {
		this.pathHelper.assertValue(this.content, matcher, targetType);
		return this.bodySpec;
	}

	/**
	 * Consume the result of the JSONPath evaluation.
	 * @since 5.1
	 */
	@SuppressWarnings("unchecked")
	public <T> WebTestClient.BodyContentSpec value(Consumer<T> consumer) {
		Object value = this.pathHelper.evaluateJsonPath(this.content);
		consumer.accept((T) value);
		return this.bodySpec;
	}

	/**
	 * Consume the result of the JSONPath evaluation and provide a target class.
	 * @since 5.1
	 */
	@SuppressWarnings("unchecked")
	public <T> WebTestClient.BodyContentSpec value(Consumer<T> consumer, Class<T> targetType) {
		Object value = this.pathHelper.evaluateJsonPath(this.content, targetType);
		consumer.accept((T) value);
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
