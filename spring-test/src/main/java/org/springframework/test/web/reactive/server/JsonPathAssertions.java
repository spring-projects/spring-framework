/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.test.web.reactive.server;

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

}
