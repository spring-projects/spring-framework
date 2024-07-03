/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.json;

import com.jayway.jsonpath.JsonPath;

import org.springframework.lang.Nullable;
import org.springframework.test.http.HttpMessageContentConverter;

/**
 * AssertJ {@linkplain org.assertj.core.api.Assert assertions} that can be applied
 * to a JSON value produced by evaluating a {@linkplain JsonPath JSON path}
 * expression.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
public class JsonPathValueAssert extends AbstractJsonValueAssert<JsonPathValueAssert> {

	private final String expression;


	JsonPathValueAssert(@Nullable Object actual, String expression,
			@Nullable HttpMessageContentConverter contentConverter) {

		super(actual, JsonPathValueAssert.class, contentConverter);
		this.expression = expression;
	}

	@Override
	protected String getExpectedErrorMessagePrefix() {
		return "Expected value at JSON path \"%s\":".formatted(this.expression);
	}

}
