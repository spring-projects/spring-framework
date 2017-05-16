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
 */
public class JsonPathAssertions {

	private final WebTestClient.BodyContentSpec bodySpec;

	private final JsonPathExpectationsHelper pathHelper;


	JsonPathAssertions(WebTestClient.BodyContentSpec spec, String expression, Object... args) {
		this.bodySpec = spec;
		this.pathHelper = new JsonPathExpectationsHelper(expression, args);
	}


	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValue(String, Object)}.
	 */
	public WebTestClient.BodyContentSpec isEqualTo(Object expectedValue) {
		this.bodySpec.consumeAsStringWith(body -> {
			this.pathHelper.assertValue(body, expectedValue);
		});
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#exists(String)}.
	 */
	public WebTestClient.BodyContentSpec exists() {
		this.bodySpec.consumeAsStringWith(this.pathHelper::exists);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#doesNotExist(String)}.
	 */
	public WebTestClient.BodyContentSpec doesNotExist() {
		this.bodySpec.consumeAsStringWith(this.pathHelper::doesNotExist);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsEmpty(String)}.
	 */
	public WebTestClient.BodyContentSpec isEmpty() {
		this.bodySpec.consumeAsStringWith(this.pathHelper::assertValueIsEmpty);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsNotEmpty(String)}.
	 */
	public WebTestClient.BodyContentSpec isNotEmpty() {
		this.bodySpec.consumeAsStringWith(this.pathHelper::assertValueIsNotEmpty);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsBoolean(String)}.
	 */
	public WebTestClient.BodyContentSpec isBoolean() {
		this.bodySpec.consumeAsStringWith(this.pathHelper::assertValueIsBoolean);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsNumber(String)}.
	 */
	public WebTestClient.BodyContentSpec isNumber() {
		this.bodySpec.consumeAsStringWith(this.pathHelper::assertValueIsNumber);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsArray(String)}.
	 */
	public WebTestClient.BodyContentSpec isArray() {
		this.bodySpec.consumeAsStringWith(this.pathHelper::assertValueIsArray);
		return this.bodySpec;
	}

	/**
	 * Applies {@link JsonPathExpectationsHelper#assertValueIsMap(String)}.
	 */
	public WebTestClient.BodyContentSpec isMap() {
		this.bodySpec.consumeAsStringWith(this.pathHelper::assertValueIsMap);
		return this.bodySpec;
	}

}
