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

package org.springframework.test.web.servlet.assertj;

import java.nio.charset.Charset;

import jakarta.servlet.http.HttpServletResponse;
import org.assertj.core.api.AbstractByteArrayAssert;
import org.assertj.core.api.AbstractStringAssert;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.test.json.JsonContentAssert;
import org.springframework.test.json.JsonPathAssert;

/**
 * AssertJ {@link org.assertj.core.api.Assert assertions} that can be applied to
 * the response body.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.2
 */
public class ResponseBodyAssert extends AbstractByteArrayAssert<ResponseBodyAssert> {

	private final Charset characterEncoding;

	@Nullable
	private final GenericHttpMessageConverter<Object> jsonMessageConverter;

	ResponseBodyAssert(byte[] actual, Charset characterEncoding,
			@Nullable GenericHttpMessageConverter<Object> jsonMessageConverter) {

		super(actual, ResponseBodyAssert.class);
		this.characterEncoding = characterEncoding;
		this.jsonMessageConverter = jsonMessageConverter;
		as("Response body");
	}

	/**
	 * Return a new {@linkplain JsonPathAssert assertion} object that provides
	 * {@linkplain com.jayway.jsonpath.JsonPath JSON path} assertions on the
	 * response body.
	 */
	public JsonPathAssert jsonPath() {
		return new JsonPathAssert(getJson(), this.jsonMessageConverter);
	}

	/**
	 * Return a new {@linkplain JsonContentAssert assertion} object that
	 * provides {@linkplain org.skyscreamer.jsonassert.JSONCompareMode JSON
	 * assert} comparison to expected json input that can be loaded from the
	 * classpath. Only absolute locations are supported, consider using
	 * {@link #json(Class)} to load json documents relative to a given class.
	 * Example: <pre><code class='java'>
	 * // Check that the response is strictly equal to the content of
	 * // "/com/acme/web/person/person-created.json":
	 * assertThat(...).body().json()
	 *         .isStrictlyEqualToJson("/com/acme/web/person/person-created.json");
	 * </code></pre>
	 */
	public JsonContentAssert json() {
		return json(null);
	}

	/**
	 * Return a new {@linkplain JsonContentAssert assertion} object that
	 * provides {@linkplain org.skyscreamer.jsonassert.JSONCompareMode JSON
	 * assert} comparison to expected json input that can be loaded from the
	 * classpath. Documents can be absolute using a leading slash, or relative
	 * to the given {@code resourceLoadClass}.
	 * Example: <pre><code class='java'>
	 * // Check that the response is strictly equal to the content of
	 * // the specified file:
	 * assertThat(...).body().json(PersonController.class)
	 *         .isStrictlyEqualToJson("person-created.json");
	 * </code></pre>
	 * @param resourceLoadClass the class used to load relative json documents
	 * @see ClassPathResource#ClassPathResource(String, Class)
	 */
	public JsonContentAssert json(@Nullable Class<?> resourceLoadClass) {
		return new JsonContentAssert(getJson(), resourceLoadClass, this.characterEncoding);
	}

	/**
	 * Verifies that the response body is equal to the given {@link String}.
	 * <p>Convert the actual byte array to a String using the character encoding
	 * of the {@link HttpServletResponse}.
	 * @param expected the expected content of the response body
	 * @see #asString()
	 */
	public ResponseBodyAssert isEqualTo(String expected) {
		asString().isEqualTo(expected);
		return this;
	}

	/**
	 * Override that uses the character encoding of {@link HttpServletResponse} to
	 * convert the byte[] to a String, rather than the platform's default charset.
	 */
	@Override
	public AbstractStringAssert<?> asString() {
		return asString(this.characterEncoding);
	}

	private String getJson() {
		return new String(this.actual, this.characterEncoding);
	}

}
