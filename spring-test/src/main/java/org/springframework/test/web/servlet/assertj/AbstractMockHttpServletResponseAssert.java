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

import org.assertj.core.api.AbstractByteArrayAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ByteArrayAssert;

import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.http.HttpMessageContentConverter;
import org.springframework.test.json.AbstractJsonContentAssert;
import org.springframework.test.json.JsonContent;
import org.springframework.test.json.JsonContentAssert;
import org.springframework.test.web.UriAssert;

/**
 * Extension of {@link AbstractHttpServletResponseAssert} for
 * {@link MockHttpServletResponse}.
 *
 * @author Stephane Nicoll
 * @since 6.2
 * @param <SELF> the type of assertions
 * @param <ACTUAL> the type of the object to assert
 */
public abstract class AbstractMockHttpServletResponseAssert<SELF extends AbstractMockHttpServletResponseAssert<SELF, ACTUAL>, ACTUAL>
		extends AbstractHttpServletResponseAssert<MockHttpServletResponse, SELF, ACTUAL> {

	@Nullable
	private final HttpMessageContentConverter contentConverter;

	protected AbstractMockHttpServletResponseAssert(
			@Nullable HttpMessageContentConverter contentConverter, ACTUAL actual, Class<?> selfType) {

		super(actual, selfType);
		this.contentConverter = contentConverter;
	}


	/**
	 * Return a new {@linkplain AbstractStringAssert assertion} object that uses
	 * the response body converted to text as the object to test.
	 * <p>Examples: <pre><code class="java">
	 * // Check that the response body is equal to "Hello World":
	 * assertThat(response).bodyText().isEqualTo("Hello World");
	 * </code></pre>
	 */
	public AbstractStringAssert<?> bodyText() {
		return Assertions.assertThat(readBody());
	}

	/**
	 * Return a new {@linkplain AbstractJsonContentAssert assertion} object that
	 * uses the response body converted to text as the object to test. Compared
	 * to {@link #bodyText()}, the assertion object provides dedicated JSON
	 * support.
	 * <p>Examples: <pre><code class="java">
	 * // Check that the response body is strictly equal to the content of
	 * // "/com/acme/sample/person-created.json":
	 * assertThat(response).bodyJson()
	 *         .isStrictlyEqualToJson("/com/acme/sample/person-created.json");
	 *
	 * // Check that the response is strictly equal to the content of the
	 * // specified file located in the same package as the PersonController:
	 * assertThat(response).bodyJson().withResourceLoadClass(PersonController.class)
	 *         .isStrictlyEqualToJson("person-created.json");
	 * </code></pre>
	 * The returned assert object also supports JSON path expressions.
	 * <p>Examples: <pre><code class="java">
	 * // Check that the JSON document does not have an "error" element
	 * assertThat(response).bodyJson().doesNotHavePath("$.error");
	 *
	 * // Check that the JSON document as a top level "message" element
	 * assertThat(response).bodyJson()
	 *         .extractingPath("$.message").asString().isEqualTo("hello");
	 * </code></pre>
	 */
	public AbstractJsonContentAssert<?> bodyJson() {
		return new JsonContentAssert(new JsonContent(readBody(), this.contentConverter));
	}

	private String readBody() {
		return new String(getResponse().getContentAsByteArray(),
				Charset.forName(getResponse().getCharacterEncoding()));
	}

	/**
	 * Return a new {@linkplain AbstractByteArrayAssert assertion} object that
	 * uses the response body as the object to test.
	 * @see #bodyText()
	 * @see #bodyJson()
	 */
	public AbstractByteArrayAssert<?> body() {
		return new ByteArrayAssert(getResponse().getContentAsByteArray());
	}

	/**
	 * Return a new {@linkplain UriAssert assertion} object that uses the
	 * forwarded URL as the object to test. If a simple equality check is
	 * required, consider using {@link #hasForwardedUrl(String)} instead.
	 * <p>Example: <pre><code class="java">
	 * // Check that the forwarded URL starts with "/orders/":
	 * assertThat(response).forwardedUrl().matchPattern("/orders/*);
	 * </code></pre>
	 */
	public UriAssert forwardedUrl() {
		return new UriAssert(getResponse().getForwardedUrl(), "Forwarded URL");
	}

	/**
	 * Return a new {@linkplain UriAssert assertion} object that uses the
	 * redirected URL as the object to test. If a simple equality check is
	 * required, consider using {@link #hasRedirectedUrl(String)} instead.
	 * <p>Example: <pre><code class="java">
	 * // Check that the redirected URL starts with "/orders/":
	 * assertThat(response).redirectedUrl().matchPattern("/orders/*);
	 * </code></pre>
	 */
	public UriAssert redirectedUrl() {
		return new UriAssert(getResponse().getRedirectedUrl(), "Redirected URL");
	}

	/**
	 * Verify that the response body is equal to the given value.
	 */
	public SELF hasBodyTextEqualTo(String bodyText) {
		bodyText().isEqualTo(bodyText);
		return this.myself;
	}

	/**
	 * Verify that the forwarded URL is equal to the given value.
	 * @param forwardedUrl the expected forwarded URL (can be null)
	 */
	public SELF hasForwardedUrl(@Nullable String forwardedUrl) {
		forwardedUrl().isEqualTo(forwardedUrl);
		return this.myself;
	}

	/**
	 * Verify that the redirected URL is equal to the given value.
	 * @param redirectedUrl the expected redirected URL (can be null)
	 */
	public SELF hasRedirectedUrl(@Nullable String redirectedUrl) {
		redirectedUrl().isEqualTo(redirectedUrl);
		return this.myself;
	}

}
