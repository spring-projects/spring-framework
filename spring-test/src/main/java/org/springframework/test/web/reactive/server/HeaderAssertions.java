/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.web.reactive.server;

import java.util.Arrays;
import java.util.function.Consumer;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.test.util.AssertionErrors;

/**
 * Assertions on headers of the response.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 5.0
 * @see WebTestClient.ResponseSpec#expectHeader()
 */
public class HeaderAssertions {

	private final ExchangeResult exchangeResult;

	private final WebTestClient.ResponseSpec responseSpec;


	HeaderAssertions(ExchangeResult result, WebTestClient.ResponseSpec spec) {
		this.exchangeResult = result;
		this.responseSpec = spec;
	}


	/**
	 * Expect a header with the given name to match the specified values.
	 */
	public WebTestClient.ResponseSpec valueEquals(String headerName, String... values) {
		return assertHeader(headerName, Arrays.asList(values), getHeaders().get(headerName));
	}

	/**
	 * Match the first value of the response header with a regex.
	 * @param name the header name
	 * @param pattern the regex pattern
	 */
	public WebTestClient.ResponseSpec valueMatches(String name, String pattern) {
		String value = getRequiredValue(name);
		String message = getMessage(name) + "=[" + value + "] does not match [" + pattern + "]";
		this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.assertTrue(message, value.matches(pattern)));
		return this.responseSpec;
	}

	/**
	 * Assert the first value of the response header with a Hamcrest {@link Matcher}.
	 * @param name the header name
	 * @param matcher the matcher to use
	 * @since 5.1
	 */
	public WebTestClient.ResponseSpec value(String name, Matcher<? super String> matcher) {
		String value = getRequiredValue(name);
		this.exchangeResult.assertWithDiagnostics(() -> MatcherAssert.assertThat(value, matcher));
		return this.responseSpec;
	}

	/**
	 * Consume the first value of the response header.
	 * @param name the header name
	 * @param consumer the consumer to use
	 * @since 5.1
	 */
	public WebTestClient.ResponseSpec value(String name, Consumer<String> consumer) {
		String value = getRequiredValue(name);
		this.exchangeResult.assertWithDiagnostics(() -> consumer.accept(value));
		return this.responseSpec;
	}

	private String getRequiredValue(String name) {
		String value = getHeaders().getFirst(name);
		if (value == null) {
			AssertionErrors.fail(getMessage(name) + " not found");
		}
		return value;
	}

	/**
	 * Expect that the header with the given name is present.
	 * @since 5.0.3
	 */
	public WebTestClient.ResponseSpec exists(String name) {
		if (!getHeaders().containsKey(name)) {
			String message = getMessage(name) + " does not exist";
			this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.fail(message));
		}
		return this.responseSpec;
	}

	/**
	 * Expect that the header with the given name is not present.
	 */
	public WebTestClient.ResponseSpec doesNotExist(String name) {
		if (getHeaders().containsKey(name)) {
			String message = getMessage(name) + " exists with value=[" + getHeaders().getFirst(name) + "]";
			this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.fail(message));
		}
		return this.responseSpec;
	}

	/**
	 * Expect a "Cache-Control" header with the given value.
	 */
	public WebTestClient.ResponseSpec cacheControl(CacheControl cacheControl) {
		return assertHeader("Cache-Control", cacheControl.getHeaderValue(), getHeaders().getCacheControl());
	}

	/**
	 * Expect a "Content-Disposition" header with the given value.
	 */
	public WebTestClient.ResponseSpec contentDisposition(ContentDisposition contentDisposition) {
		return assertHeader("Content-Disposition", contentDisposition, getHeaders().getContentDisposition());
	}

	/**
	 * Expect a "Content-Length" header with the given value.
	 */
	public WebTestClient.ResponseSpec contentLength(long contentLength) {
		return assertHeader("Content-Length", contentLength, getHeaders().getContentLength());
	}

	/**
	 * Expect a "Content-Type" header with the given value.
	 */
	public WebTestClient.ResponseSpec contentType(MediaType mediaType) {
		return assertHeader("Content-Type", mediaType, getHeaders().getContentType());
	}

	/**
	 * Expect a "Content-Type" header with the given value.
	 */
	public WebTestClient.ResponseSpec contentType(String mediaType) {
		return contentType(MediaType.parseMediaType(mediaType));
	}

	/**
	 * Expect a "Content-Type" header compatible with the given value.
	 */
	public WebTestClient.ResponseSpec contentTypeCompatibleWith(MediaType mediaType) {
		MediaType actual = getHeaders().getContentType();
		String message = getMessage("Content-Type") + "=[" + actual + "] is not compatible with [" + mediaType + "]";
		this.exchangeResult.assertWithDiagnostics(() ->
				AssertionErrors.assertTrue(message, (actual != null && actual.isCompatibleWith(mediaType))));
		return this.responseSpec;
	}

	/**
	 * Expect a "Content-Type" header compatible with the given value.
	 */
	public WebTestClient.ResponseSpec contentTypeCompatibleWith(String mediaType) {
		return contentTypeCompatibleWith(MediaType.parseMediaType(mediaType));
	}

	/**
	 * Expect an "Expires" header with the given value.
	 */
	public WebTestClient.ResponseSpec expires(long expires) {
		return assertHeader("Expires", expires, getHeaders().getExpires());
	}

	/**
	 * Expect a "Last-Modified" header with the given value.
	 */
	public WebTestClient.ResponseSpec lastModified(long lastModified) {
		return assertHeader("Last-Modified", lastModified, getHeaders().getLastModified());
	}


	private HttpHeaders getHeaders() {
		return this.exchangeResult.getResponseHeaders();
	}

	private String getMessage(String headerName) {
		return "Response header '" + headerName + "'";
	}

	private WebTestClient.ResponseSpec assertHeader(String name, @Nullable Object expected, @Nullable Object actual) {
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name);
			AssertionErrors.assertEquals(message, expected, actual);
		});
		return this.responseSpec;
	}

}
