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

import org.assertj.core.api.AssertProvider;

import org.springframework.lang.Nullable;
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

	@Nullable
	private final Class<?> resourceLoadClass;


	/**
	 * Create a new {@code JsonContent} instance.
	 * @param json the actual JSON content
	 * @param resourceLoadClass the source class used to load resources
	 */
	JsonContent(String json, @Nullable Class<?> resourceLoadClass) {
		Assert.notNull(json, "JSON must not be null");
		this.json = json;
		this.resourceLoadClass = resourceLoadClass;
	}


	/**
	 * Use AssertJ's {@link org.assertj.core.api.Assertions#assertThat assertThat}
	 * instead.
	 */
	@Override
	public JsonContentAssert assertThat() {
		return new JsonContentAssert(this.json, null).withResourceLoadClass(this.resourceLoadClass);
	}

	/**
	 * Return the actual JSON content string.
	 * @return the JSON content
	 */
	public String getJson() {
		return this.json;
	}

	@Override
	public String toString() {
		return "JsonContent " + this.json;
	}

}
