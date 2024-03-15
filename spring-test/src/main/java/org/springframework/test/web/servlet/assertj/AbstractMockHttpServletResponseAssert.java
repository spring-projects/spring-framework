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

import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletResponse;
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
	private final GenericHttpMessageConverter<Object> jsonMessageConverter;

	protected AbstractMockHttpServletResponseAssert(
			@Nullable GenericHttpMessageConverter<Object> jsonMessageConverter, ACTUAL actual, Class<?> selfType) {

		super(actual, selfType);
		this.jsonMessageConverter = jsonMessageConverter;
	}

	/**
	 * Return a new {@linkplain ResponseBodyAssert assertion} object that uses
	 * the response body as the object to test. The return assertion object
	 * provides access to the raw byte array, a String value decoded using the
	 * response's character encoding, and dedicated json testing support.
	 * Examples: <pre><code class='java'>
	 * // Check that the response body is equal to "Hello World":
	 * assertThat(response).body().isEqualTo("Hello World");
	 * // Check that the response body is strictly equal to the content of "test.json":
	 * assertThat(response).body().json().isStrictlyEqualToJson("test.json");
	 * </code></pre>
	 */
	public ResponseBodyAssert body() {
		return new ResponseBodyAssert(getResponse().getContentAsByteArray(),
				Charset.forName(getResponse().getCharacterEncoding()), this.jsonMessageConverter);
	}

	/**
	 * Return a new {@linkplain UriAssert assertion} object that uses the
	 * forwarded URL as the object to test. If a simple equality check is
	 * required consider using {@link #hasForwardedUrl(String)} instead.
	 * Example: <pre><code class='java'>
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
	 * required consider using {@link #hasRedirectedUrl(String)} instead.
	 * Example: <pre><code class='java'>
	 * // Check that the redirected URL starts with "/orders/":
	 * assertThat(response).redirectedUrl().matchPattern("/orders/*);
	 * </code></pre>
	 */
	public UriAssert redirectedUrl() {
		return new UriAssert(getResponse().getRedirectedUrl(), "Redirected URL");
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
