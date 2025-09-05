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

package org.springframework.test.web.support;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.util.AssertionErrors.fail;

/**
 * Assertions on headers of the response.
 *
 * @author Rob Worsnop
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <E> the type of the exchange result
 * @param <R> the type of the response spec
 */
public abstract class AbstractHeaderAssertions <E, R> {

	private final E exchangeResult;

	private final R responseSpec;


	protected AbstractHeaderAssertions(E exchangeResult, R responseSpec) {
		this.exchangeResult = exchangeResult;
		this.responseSpec = responseSpec;
	}


	/**
	 * Return the exchange result.
	 */
	protected E getExchangeResult() {
		return this.exchangeResult;
	}

	/**
	 * Subclasses must implement this to provide access to response headers.
	 */
	protected abstract HttpHeaders getResponseHeaders();

	/**
	 * Subclasses must implement this to assert with diagnostics.
	 */
	protected abstract void assertWithDiagnostics(Runnable assertion);


	/**
	 * Expect a header with the given name to match the specified values.
	 */
	public R valueEquals(String headerName, String... values) {
		return assertHeader(headerName, Arrays.asList(values), getResponseHeaders().getOrEmpty(headerName));
	}

	/**
	 * Expect a header with the given name to match the given long value.
	 */
	public R valueEquals(String headerName, long value) {
		String actual = getResponseHeaders().getFirst(headerName);
		assertWithDiagnostics(() ->
				assertNotNull("Response does not contain header '" + headerName + "'", actual));
		return assertHeader(headerName, value, Long.parseLong(actual));
	}

	/**
	 * Expect a header with the given name to match the specified long value
	 * parsed into a date using the preferred date format described in RFC 7231.
	 * <p>An {@link AssertionError} is thrown if the response does not contain
	 * the specified header, or if the supplied {@code value} does not match the
	 * primary header value.
	 */
	public R valueEqualsDate(String headerName, long value) {
		assertWithDiagnostics(() -> {
			String headerValue = getResponseHeaders().getFirst(headerName);
			assertNotNull("Response does not contain header '" + headerName + "'", headerValue);

			HttpHeaders headers = new HttpHeaders();
			headers.setDate("expected", value);
			headers.set("actual", headerValue);

			assertEquals(getMessage(headerName) + "='" + headerValue + "' " +
							"does not match expected value '" + headers.getFirst("expected") + "'",
					headers.getFirstDate("expected"), headers.getFirstDate("actual"));
		});
		return this.responseSpec;
	}

	/**
	 * Match the first value of the response header with a regex.
	 * @param name the header name
	 * @param pattern the regex pattern
	 */
	public R valueMatches(String name, String pattern) {
		String value = getRequiredValue(name);
		String message = getMessage(name) + "=[" + value + "] does not match [" + pattern + "]";
		assertWithDiagnostics(() -> assertTrue(message, value.matches(pattern)));
		return this.responseSpec;
	}

	/**
	 * Match all values of the response header with the given regex
	 * patterns which are applied to the values of the header in the
	 * same order. Note that the number of patterns must match the
	 * number of actual values.
	 * @param name the header name
	 * @param patterns one or more regex patterns, one per expected value
	 */
	public R valuesMatch(String name, String... patterns) {
		List<String> values = getRequiredValues(name);
		assertWithDiagnostics(() -> {
			assertTrue(
					getMessage(name) + " has fewer or more values " + values +
							" than number of patterns to match with " + Arrays.toString(patterns),
					values.size() == patterns.length);
			for (int i = 0; i < values.size(); i++) {
				String value = values.get(i);
				String pattern = patterns[i];
				assertTrue(
						getMessage(name) + "[" + i + "]='" + value + "' does not match '" + pattern + "'",
						value.matches(pattern));
			}
		});
		return this.responseSpec;
	}

	/**
	 * Assert the first value of the response header with a Hamcrest {@link Matcher}.
	 * @param name the header name
	 * @param matcher the matcher to use
	 */
	public R value(String name, Matcher<? super String> matcher) {
		String value = getResponseHeaders().getFirst(name);
		assertWithDiagnostics(() -> {
			String message = getMessage(name);
			assertThat(message, value, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * Assert all values of the response header with a Hamcrest {@link Matcher}.
	 * @param name the header name
	 * @param matcher the matcher to use
	 */
	public R values(String name, Matcher<? super Iterable<String>> matcher) {
		List<String> values = getResponseHeaders().get(name);
		assertWithDiagnostics(() -> {
			String message = getMessage(name);
			assertThat(message, values, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * Consume the first value of the named response header.
	 * @param name the header name
	 * @param consumer the consumer to use
	 */
	public R value(String name, Consumer<String> consumer) {
		String value = getRequiredValue(name);
		assertWithDiagnostics(() -> consumer.accept(value));
		return this.responseSpec;
	}

	/**
	 * Consume all values of the named response header.
	 * @param name the header name
	 * @param consumer the consumer to use
	 */
	public R values(String name, Consumer<List<String>> consumer) {
		List<String> values = getRequiredValues(name);
		assertWithDiagnostics(() -> consumer.accept(values));
		return this.responseSpec;
	}

	/**
	 * Expect that the header with the given name is present.
	 */
	public R exists(String name) {
		if (!getResponseHeaders().containsHeader(name)) {
			String message = getMessage(name) + " does not exist";
			assertWithDiagnostics(() -> fail(message));
		}
		return this.responseSpec;
	}

	/**
	 * Expect that the header with the given name is not present.
	 */
	public R doesNotExist(String name) {
		if (getResponseHeaders().containsHeader(name)) {
			String message = getMessage(name) + " exists with value=[" + getResponseHeaders().getFirst(name) + "]";
			assertWithDiagnostics(() -> fail(message));
		}
		return this.responseSpec;
	}

	/**
	 * Expect a "Cache-Control" header with the given value.
	 */
	public R cacheControl(CacheControl cacheControl) {
		return assertHeader("Cache-Control", cacheControl.getHeaderValue(), getResponseHeaders().getCacheControl());
	}

	/**
	 * Expect a "Content-Disposition" header with the given value.
	 */
	public R contentDisposition(ContentDisposition contentDisposition) {
		return assertHeader("Content-Disposition", contentDisposition, getResponseHeaders().getContentDisposition());
	}

	/**
	 * Expect a "Content-Length" header with the given value.
	 */
	public R contentLength(long contentLength) {
		return assertHeader("Content-Length", contentLength, getResponseHeaders().getContentLength());
	}

	/**
	 * Expect a "Content-Type" header with the given value.
	 */
	public R contentType(MediaType mediaType) {
		return assertHeader("Content-Type", mediaType, getResponseHeaders().getContentType());
	}

	/**
	 * Expect a "Content-Type" header with the given value.
	 */
	public R contentType(String mediaType) {
		return contentType(MediaType.parseMediaType(mediaType));
	}

	/**
	 * Expect a "Content-Type" header compatible with the given value.
	 */
	public R contentTypeCompatibleWith(MediaType mediaType) {
		MediaType actual = getResponseHeaders().getContentType();
		String message = getMessage("Content-Type") + "=[" + actual + "] is not compatible with [" + mediaType + "]";
		assertWithDiagnostics(() ->
				assertTrue(message, (actual != null && actual.isCompatibleWith(mediaType))));
		return this.responseSpec;
	}

	/**
	 * Expect a "Content-Type" header compatible with the given value.
	 */
	public R contentTypeCompatibleWith(String mediaType) {
		return contentTypeCompatibleWith(MediaType.parseMediaType(mediaType));
	}

	/**
	 * Expect an "Expires" header with the given value.
	 */
	public R expires(long expires) {
		return assertHeader("Expires", expires, getResponseHeaders().getExpires());
	}

	/**
	 * Expect a "Last-Modified" header with the given value.
	 */
	public R lastModified(long lastModified) {
		return assertHeader("Last-Modified", lastModified, getResponseHeaders().getLastModified());
	}

	/**
	 * Expect a "Location" header with the given value.
	 */
	public R location(String location) {
		return assertHeader("Location", URI.create(location), getResponseHeaders().getLocation());
	}

	private R assertHeader(String name, @Nullable Object expected, @Nullable Object actual) {
		assertWithDiagnostics(() -> {
			String message = getMessage(name);
			assertEquals(message, expected, actual);
		});
		return this.responseSpec;
	}

	private String getRequiredValue(String name) {
		return getRequiredValues(name).get(0);
	}

	private List<String> getRequiredValues(String name) {
		List<String> values = getResponseHeaders().get(name);
		if (!CollectionUtils.isEmpty(values)) {
			return values;
		}
		else {
			assertWithDiagnostics(() -> fail(getMessage(name) + " not found"));
		}
		throw new IllegalStateException("This code path should not be reachable");
	}

	private static String getMessage(String headerName) {
		return "Response header '" + headerName + "'";
	}
}
