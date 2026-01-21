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

package org.springframework.test.json;

import org.assertj.core.api.AssertProvider;
import org.jspecify.annotations.Nullable;

import org.springframework.test.http.HttpMessageContentConverter;
import org.springframework.util.Assert;

/**
 * JSON content which is generally used to {@link AssertProvider provide}
 * {@link JsonContentAssert} to AssertJ {@code assertThat} calls.
 *
 * @author Phillip Webb
 * @author Diego Berrueta
 * @since 6.2
 */
public final class JsonContent implements AssertProvider<JsonContentAssert> {

	private final String json;

	private final @Nullable JsonConverterDelegate converterDelegate;


	/**
	 * Create a new {@code JsonContent} instance with the message converter to
	 * use to deserialize content.
	 * @param json the actual JSON content
	 * @param converterDelegate the content converter to use
	 */
	public JsonContent(String json, @Nullable JsonConverterDelegate converterDelegate) {
		Assert.notNull(json, "JSON must not be null");
		this.json = json;
		this.converterDelegate = converterDelegate;
	}

	/**
	 * Create a new {@code JsonContent} instance with the message converter to
	 * use to deserialize content.
	 * @param json the actual JSON content
	 * @param converter the content converter to use
	 * @deprecated in favour of {@link #JsonContent(String, JsonConverterDelegate)}
	 */
	@SuppressWarnings("removal")
	@Deprecated(since = "7.0", forRemoval = true)
	public JsonContent(String json, @Nullable HttpMessageContentConverter converter) {
		this(json, (JsonConverterDelegate) converter);
	}

	/**
	 * Create a new {@code JsonContent} instance.
	 * @param json the actual JSON content
	 */
	public JsonContent(String json) {
		this(json, (JsonConverterDelegate) null);
	}


	/**
	 * Use AssertJ's {@link org.assertj.core.api.Assertions#assertThat assertThat}
	 * instead.
	 */
	@Override
	public JsonContentAssert assertThat() {
		return new JsonContentAssert(this);
	}

	/**
	 * Return the actual JSON content string.
	 */
	public String getJson() {
		return this.json;
	}

	/**
	 * Return the {@link JsonConverterDelegate} to use to decode JSON content.
	 * @since 7.0
	 */
	public @Nullable JsonConverterDelegate getJsonConverterDelegate() {
		return this.converterDelegate;
	}

	/**
	 * Return the {@link HttpMessageContentConverter} to use to deserialize content.
	 * @deprecated in favour of {@link #getJsonConverterDelegate()}
	 */
	@SuppressWarnings("removal")
	@Deprecated(since = "7.0", forRemoval = true)
	@Nullable HttpMessageContentConverter getContentConverter() {
		return (this.converterDelegate instanceof HttpMessageContentConverter cc ? cc : null);
	}

	@Override
	public String toString() {
		return "JsonContent " + this.json;
	}

}
