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

package org.springframework.test.http;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.api.Assertions;

import org.springframework.http.HttpHeaders;

/**
 * AssertJ {@link org.assertj.core.api.Assert assertions} that can be applied to
 * {@link HttpHeaders}.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
public class HttpHeadersAssert extends AbstractMapAssert<HttpHeadersAssert, HttpHeaders, String, List<String>> {

	private static final ZoneId GMT = ZoneId.of("GMT");


	public HttpHeadersAssert(HttpHeaders actual) {
		super(actual, HttpHeadersAssert.class);
		as("HTTP headers");
	}

	/**
	 * Verify that the actual HTTP headers contain a header with the given
	 * {@code name}.
	 * @param name the name of an expected HTTP header
	 * @see #containsKey
	 */
	public HttpHeadersAssert containsHeader(String name) {
		return containsKey(name);
	}

	/**
	 * Verify that the actual HTTP headers contain the headers with the given
	 * {@code names}.
	 * @param names the names of expected HTTP headers
	 * @see #containsKeys
	 */
	public HttpHeadersAssert containsHeaders(String... names) {
		return containsKeys(names);
	}

	/**
	 * Verify that the actual HTTP headers do not contain a header with the
	 * given {@code name}.
	 * @param name the name of an HTTP header that should not be present
	 * @see #doesNotContainKey
	 */
	public HttpHeadersAssert doesNotContainHeader(String name) {
		return doesNotContainKey(name);
	}

	/**
	 * Verify that the actual HTTP headers do not contain any of the headers
	 * with the given {@code names}.
	 * @param names the names of HTTP headers that should not be present
	 * @see #doesNotContainKeys
	 */
	public HttpHeadersAssert doesNotContainsHeaders(String... names) {
		return doesNotContainKeys(names);
	}

	/**
	 * Verify that the actual HTTP headers contain a header with the given
	 * {@code name} and {@link String} {@code value}.
	 * @param name the name of the cookie
	 * @param value the expected value of the header
	 */
	public HttpHeadersAssert hasValue(String name, String value) {
		containsKey(name);
		Assertions.assertThat(this.actual.getFirst(name))
				.as("check primary value for HTTP header '%s'", name)
				.isEqualTo(value);
		return this.myself;
	}

	/**
	 * Verify that the actual HTTP headers contain a header with the given
	 * {@code name} and {@link Long} {@code value}.
	 * @param name the name of the cookie
	 * @param value the expected value of the header
	 */
	public HttpHeadersAssert hasValue(String name, long value) {
		containsKey(name);
		Assertions.assertThat(this.actual.getFirst(name))
				.as("check primary long value for HTTP header '%s'", name)
				.asLong().isEqualTo(value);
		return this.myself;
	}

	/**
	 * Verify that the actual HTTP headers contain a header with the given
	 * {@code name} and {@link Instant} {@code value}.
	 * @param name the name of the cookie
	 * @param value the expected value of the header
	 */
	public HttpHeadersAssert hasValue(String name, Instant value) {
		containsKey(name);
		Assertions.assertThat(this.actual.getFirstZonedDateTime(name))
				.as("check primary date value for HTTP header '%s'", name)
				.isCloseTo(ZonedDateTime.ofInstant(value, GMT), Assertions.within(999, ChronoUnit.MILLIS));
		return this.myself;
	}

}
