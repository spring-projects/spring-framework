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

import java.util.function.Consumer;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.internal.Failures;

import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * AssertJ {@link org.assertj.core.api.Assert assertions} that can be applied
 * to a {@link CharSequence} representation of a json document using
 * {@linkplain JsonPath JSON path}.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
public class JsonPathAssert extends AbstractAssert<JsonPathAssert, CharSequence> {

	private static final Failures failures = Failures.instance();

	@Nullable
	private final GenericHttpMessageConverter<Object> jsonMessageConverter;

	public JsonPathAssert(CharSequence json,
			@Nullable GenericHttpMessageConverter<Object> jsonMessageConverter) {
		super(json, JsonPathAssert.class);
		this.jsonMessageConverter = jsonMessageConverter;
	}

	/**
	 * Verify that the given JSON {@code path} is present and extract the JSON
	 * value for further {@linkplain JsonPathValueAssert assertions}.
	 * @param path the {@link JsonPath} expression
	 * @see #hasPathSatisfying(String, Consumer)
	 */
	public JsonPathValueAssert extractingPath(String path) {
		Object value = new JsonPathValue(path).getValue();
		return new JsonPathValueAssert(value, path, this.jsonMessageConverter);
	}

	/**
	 * Verify that the given JSON {@code path} is present with a JSON value
	 * satisfying the given {@code valueRequirements}.
	 * @param path the {@link JsonPath} expression
	 * @param valueRequirements a {@link Consumer} of the assertion object
	 */
	public JsonPathAssert hasPathSatisfying(String path, Consumer<AssertProvider<JsonPathValueAssert>> valueRequirements) {
		Object value = new JsonPathValue(path).assertHasPath();
		JsonPathValueAssert valueAssert = new JsonPathValueAssert(value, path, this.jsonMessageConverter);
		valueRequirements.accept(() -> valueAssert);
		return this;
	}

	/**
	 * Verify that the given JSON {@code path} matches. For paths with an
	 * operator, this validates that the path expression is valid, but does not
	 * validate that it yield any results.
	 * @param path the {@link JsonPath} expression
	 */
	public JsonPathAssert hasPath(String path) {
		new JsonPathValue(path).assertHasPath();
		return this;
	}

	/**
	 * Verify that the given JSON {@code path} does not match.
	 * @param path the {@link JsonPath} expression
	 */
	public JsonPathAssert doesNotHavePath(String path) {
		new JsonPathValue(path).assertDoesNotHavePath();
		return this;
	}


	private AssertionError failure(BasicErrorMessageFactory errorMessageFactory) {
		throw failures.failure(this.info, errorMessageFactory);
	}


	/**
	 * A {@link JsonPath} value.
	 */
	private class JsonPathValue {

		private final String path;

		private final JsonPath jsonPath;

		private final String json;

		JsonPathValue(String path) {
			Assert.hasText(path, "'path' must not be null or empty");
			this.path = path;
			this.jsonPath = JsonPath.compile(this.path);
			this.json = JsonPathAssert.this.actual.toString();
		}

		@Nullable
		Object assertHasPath() {
			return getValue();
		}

		void assertDoesNotHavePath() {
			try {
				read();
				throw failure(new JsonPathNotExpected(this.json, this.path));
			}
			catch (PathNotFoundException ignore) {
			}
		}

		@Nullable
		Object getValue() {
			try {
				return read();
			}
			catch (PathNotFoundException ex) {
				throw failure(new JsonPathNotFound(this.json, this.path));
			}
		}

		@Nullable
		private Object read() {
			return this.jsonPath.read(this.json);
		}


		static final class JsonPathNotFound extends BasicErrorMessageFactory {

			private JsonPathNotFound(String actual, String path) {
				super("%nExpecting:%n  %s%nTo match JSON path:%n  %s%n", actual, path);
			}
		}

		static final class JsonPathNotExpected extends BasicErrorMessageFactory {

			private JsonPathNotExpected(String actual, String path) {
				super("%nExpecting:%n  %s%nTo not match JSON path:%n  %s%n", actual, path);
			}
		}
	}
}
