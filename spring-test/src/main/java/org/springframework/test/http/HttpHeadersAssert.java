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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.presentation.Representation;
import org.assertj.core.presentation.StandardRepresentation;

import org.springframework.http.HttpHeaders;

/**
 * AssertJ {@link org.assertj.core.api.Assert assertions} that can be applied to
 * {@link HttpHeaders}.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
public class HttpHeadersAssert extends AbstractObjectAssert<HttpHeadersAssert, HttpHeaders> {

	private static final ZoneId GMT = ZoneId.of("GMT");

	private final AbstractCollectionAssert<?, Collection<? extends String>, String, ObjectAssert<String>> namesAssert;


	public HttpHeadersAssert(HttpHeaders actual) {
		super(actual, HttpHeadersAssert.class);
		as("HTTP headers");
		withRepresentation(new Representation() {
			@Override
			public String toStringOf(Object object) {
				if (object instanceof HttpHeaders headers) {
					return headers.toString();
				}
				return StandardRepresentation.STANDARD_REPRESENTATION.toStringOf(object);
			}
		});
		this.namesAssert = Assertions.assertThat(actual.headerNames())
				.as("HTTP header names");
	}

	/**
	 * Verify that the actual HTTP headers contain a header with the given
	 * {@code name}.
	 * @param name the name of an expected HTTP header
	 */
	public HttpHeadersAssert containsHeader(String name) {
		this.namesAssert
				.as("check headers contain HTTP header '%s'", name)
				.contains(name);
		return this.myself;
	}

	/**
	 * Verify that the actual HTTP headers contain the headers with the given
	 * {@code names}.
	 * @param names the names of expected HTTP headers
	 */
	public HttpHeadersAssert containsHeaders(String... names) {
		this.namesAssert
				.as("check headers contain HTTP headers '%s'", Arrays.toString(names))
				.contains(names);
		return this.myself;
	}

	/**
	 * Verify that the actual HTTP headers contain only the headers with the
	 * given {@code names}, in any order and in a case-insensitive manner.
	 * @param names the names of expected HTTP headers
	 * @since 7.0
	 */
	public HttpHeadersAssert containsOnlyHeaders(String... names) {
		this.namesAssert
				.as("check headers contains only HTTP headers '%s'", Arrays.toString(names))
				.containsOnly(names);
		return this.myself;
	}

	/**
	 * Verify that the actual HTTP headers do not contain a header with the
	 * given {@code name}.
	 * @param name the name of an HTTP header that should not be present
	 */
	public HttpHeadersAssert doesNotContainHeader(String name) {
		this.namesAssert
				.as("check headers does not contain HTTP header '%s'", name)
				.doesNotContain(name);
		return this.myself;
	}

	/**
	 * Verify that the actual HTTP headers do not contain any of the headers
	 * with the given {@code names}.
	 * @param names the names of HTTP headers that should not be present
	 */
	public HttpHeadersAssert doesNotContainsHeaders(String... names) {
		this.namesAssert
				.as("check headers does not contain HTTP headers '%s'", Arrays.toString(names))
				.doesNotContain(names);
		return this.myself;
	}

	/**
	 * Verify that the actual HTTP headers contain a header with the given
	 * {@code name} and {@link String} {@code value}.
	 * @param name the name of the cookie
	 * @param value the expected value of the header
	 */
	public HttpHeadersAssert hasValue(String name, String value) {
		containsHeader(name);
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
		containsHeader(name);
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
		containsHeader(name);
		Assertions.assertThat(this.actual.getFirstZonedDateTime(name))
				.as("check primary date value for HTTP header '%s'", name)
				.isCloseTo(ZonedDateTime.ofInstant(value, GMT), Assertions.within(999, ChronoUnit.MILLIS));
		return this.myself;
	}

	/**
	 * Verify that the given header has a full list of values exactly equal to
	 * the given list of values, and in the same order.
	 * @param name the considered header name (case-insensitive)
	 * @param values the exhaustive list of expected values
	 * @since 7.0
	 */
	public HttpHeadersAssert hasExactlyValues(String name, List<String> values) {
		containsHeader(name);
		Assertions.assertThat(this.actual.get(name))
				.as("check all values of HTTP header '%s'", name)
				.containsExactlyElementsOf(values);
		return this.myself;
	}

	/**
	 * Verify that the given header has a full list of values exactly equal to
	 * the given list of values, in any order.
	 * @param name the considered header name (case-insensitive)
	 * @param values the exhaustive list of expected values
	 * @since 7.0
	 */
	public HttpHeadersAssert hasExactlyValuesInAnyOrder(String name, List<String> values) {
		containsHeader(name);
		Assertions.assertThat(this.actual.get(name))
				.as("check all values of HTTP header '%s' in any order", name)
				.containsExactlyInAnyOrderElementsOf(values);
		return this.myself;
	}

	/**
	 * Verify that headers are empty and no headers are present.
	 */
	public HttpHeadersAssert isEmpty() {
		this.namesAssert
				.as("check headers are empty")
				.isEmpty();
		return this.myself;
	}

	/**
	 * Verify that headers are not empty and at least one header is present.
	 */
	public HttpHeadersAssert isNotEmpty() {
		this.namesAssert
				.as("check headers are not empty")
				.isNotEmpty();
		return this.myself;
	}

	/**
	 * Verify that there is exactly {@code expected} headers present, when
	 * considering header names in a case-insensitive manner.
	 * @param expected the expected number of headers
	 */
	public HttpHeadersAssert hasSize(int expected) {
		this.namesAssert
				.as("check headers have size '%i'", expected)
				.hasSize(expected);
		return this.myself;
	}

	/**
	 * Verify that the number of headers present is strictly greater than the
	 * given boundary, when considering header names in a case-insensitive
	 * manner.
	 * @param boundary the given value to compare actual header size to
	 */
	public HttpHeadersAssert hasSizeGreaterThan(int boundary) {
		this.namesAssert
				.as("check headers have size > '%i'", boundary)
				.hasSizeGreaterThan(boundary);
		return this.myself;
	}

	/**
	 * Verify that the number of headers present is greater or equal to the
	 * given boundary, when considering header names in a case-insensitive
	 * manner.
	 * @param boundary the given value to compare actual header size to
	 */
	public HttpHeadersAssert hasSizeGreaterThanOrEqualTo(int boundary) {
		this.namesAssert
				.as("check headers have size >= '%i'", boundary)
				.hasSizeGreaterThanOrEqualTo(boundary);
		return this.myself;
	}

	/**
	 * Verify that the number of headers present is strictly less than the
	 * given boundary, when considering header names in a case-insensitive
	 * manner.
	 * @param boundary the given value to compare actual header size to
	 */
	public HttpHeadersAssert hasSizeLessThan(int boundary) {
		this.namesAssert
				.as("check headers have size < '%i'", boundary)
				.hasSizeLessThan(boundary);
		return this.myself;
	}

	/**
	 * Verify that the number of headers present is less than or equal to the
	 * given boundary, when considering header names in a case-insensitive
	 * manner.
	 * @param boundary the given value to compare actual header size to
	 */
	public HttpHeadersAssert hasSizeLessThanOrEqualTo(int boundary) {
		this.namesAssert
				.as("check headers have size <= '%i'", boundary)
				.hasSizeLessThanOrEqualTo(boundary);
		return this.myself;
	}

	/**
	 * Verify that the number of headers present is between the given boundaries
	 * (inclusive), when considering header names in a case-insensitive manner.
	 * @param lowerBoundary the lower boundary compared to which actual size
	 * should be greater than or equal to
	 * @param higherBoundary the higher boundary compared to which actual size
	 * should be less than or equal to
	 */
	public HttpHeadersAssert hasSizeBetween(int lowerBoundary, int higherBoundary) {
		this.namesAssert
				.as("check headers have size between '%i' and '%i'", lowerBoundary, higherBoundary)
				.hasSizeBetween(lowerBoundary, higherBoundary);
		return this.myself;
	}

	/**
	 * Verify that the number actual headers is the same as in the given
	 * {@code HttpHeaders}.
	 * @param other the {@code HttpHeaders} to compare size with
	 * @since 7.0
	 */
	public HttpHeadersAssert hasSameSizeAs(HttpHeaders other) {
		this.namesAssert
				.as("check headers have same size as '%s'", other)
				.hasSize(other.size());
		return this.myself;
	}
}
