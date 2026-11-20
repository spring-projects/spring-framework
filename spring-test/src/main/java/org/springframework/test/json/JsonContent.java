/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.json;

import org.assertj.core.api.AssertProvider;
import org.jspecify.annotations.Nullable;

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

	@Override
	public String toString() {
		return "JsonContent " + this.json;
	}

}
