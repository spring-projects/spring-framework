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

package org.springframework.test.web.support;

import java.util.function.Consumer;

import com.jayway.jsonpath.Configuration;
import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.util.Assert;

/**
 * Base class for applying
 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> assertions
 * in RestTestClient and WebTestClient.
 *
 * @author Rob Worsnop
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <B> the type of body spec (RestTestClient vs WebTestClient specific)
 * @see <a href="https://github.com/jayway/JsonPath">https://github.com/jayway/JsonPath</a>
 * @see JsonPathExpectationsHelper
 */
public abstract class AbstractJsonPathAssertions<B> {

	private final B bodySpec;

	private final String content;

	private final JsonPathExpectationsHelper pathHelper;


	protected AbstractJsonPathAssertions(B spec, String content, String expression, @Nullable Configuration configuration) {
		Assert.hasText(expression, "expression must not be null or empty");
		this.bodySpec = spec;
		this.content = content;
		this.pathHelper = new JsonPathExpectationsHelper(expression, configuration);
	}


	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValue(String, Object)}.
	 */
	public B isEqualTo(Object expectedValue) {
		this.pathHelper.assertValue(this.content, expectedValue);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#exists(String)}.
	 */
	public B exists() {
		this.pathHelper.exists(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#doesNotExist(String)}.
	 */
	public B doesNotExist() {
		this.pathHelper.doesNotExist(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsEmpty(String)}.
	 */
	public B isEmpty() {
		this.pathHelper.assertValueIsEmpty(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsNotEmpty(String)}.
	 */
	public B isNotEmpty() {
		this.pathHelper.assertValueIsNotEmpty(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#hasJsonPath}.
	 */
	public B hasJsonPath() {
		this.pathHelper.hasJsonPath(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#doesNotHaveJsonPath}.
	 */
	public B doesNotHaveJsonPath() {
		this.pathHelper.doesNotHaveJsonPath(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsBoolean(String)}.
	 */
	public B isBoolean() {
		this.pathHelper.assertValueIsBoolean(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsNumber(String)}.
	 */
	public B isNumber() {
		this.pathHelper.assertValueIsNumber(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsArray(String)}.
	 */
	public B isArray() {
		this.pathHelper.assertValueIsArray(this.content);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsMap(String)}.
	 */
	public B isMap() {
		this.pathHelper.assertValueIsMap(this.content);
		return this.bodySpec;
	}

	/**
	 * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher)}.
	 */
	public <T> B value(Matcher<? super T> matcher) {
		this.pathHelper.assertValue(this.content, matcher);
		return this.bodySpec;
	}

	/**
	 * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher, Class)}.
	 */
	public <T> B value(Class<T> targetType, Matcher<? super T> matcher) {
		this.pathHelper.assertValue(this.content, matcher, targetType);
		return this.bodySpec;
	}

	/**
	 * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher, ParameterizedTypeReference)}.
	 */
	public <T> B value(ParameterizedTypeReference<T> targetType, Matcher<? super T> matcher) {
		this.pathHelper.assertValue(this.content, matcher, targetType);
		return this.bodySpec;
	}

	/**
	 * Consume the result of the JSONPath evaluation.
	 */
	@SuppressWarnings("unchecked")
	public <T> B value(Consumer<T> consumer) {
		Object value = this.pathHelper.evaluateJsonPath(this.content);
		consumer.accept((T) value);
		return this.bodySpec;
	}

	/**
	 * Consume the result of the JSONPath evaluation and provide a target class.
	 */
	public <T> B value(Class<T> targetType, Consumer<T> consumer) {
		T value = this.pathHelper.evaluateJsonPath(this.content, targetType);
		consumer.accept(value);
		return this.bodySpec;
	}

	/**
	 * Consume the result of the JSONPath evaluation and provide a parameterized type.
	 */
	public <T> B value(ParameterizedTypeReference<T> targetType, Consumer<T> consumer) {
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

