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

import java.util.ArrayList;
import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletResponse;
import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;
import org.springframework.test.http.HttpHeadersAssert;
import org.springframework.test.http.MediaTypeAssert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.function.SingletonSupplier;

/**
 * Base AssertJ {@linkplain org.assertj.core.api.Assert assertions} that can be
 * applied to any object that provides an {@link HttpServletResponse}. This
 * provides direct access to response assertions while also providing access to
 * a different top-level object.
 *
 * @author Stephane Nicoll
 * @since 6.2
 * @param <R> the type of {@link HttpServletResponse}
 * @param <SELF> the type of assertions
 * @param <ACTUAL> the type of the object to assert
 */
public abstract class AbstractHttpServletResponseAssert<R extends HttpServletResponse, SELF extends AbstractHttpServletResponseAssert<R, SELF, ACTUAL>, ACTUAL>
		extends AbstractObjectAssert<SELF, ACTUAL> {

	private final Supplier<MediaTypeAssert> contentTypeAssertSupplier;

	private final Supplier<HttpHeadersAssert> headersAssertSupplier;

	private final Supplier<AbstractIntegerAssert<?>> statusAssert;


	protected AbstractHttpServletResponseAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
		this.contentTypeAssertSupplier = SingletonSupplier.of(() -> new MediaTypeAssert(getResponse().getContentType()));
		this.headersAssertSupplier = SingletonSupplier.of(() -> new HttpHeadersAssert(getHttpHeaders(getResponse())));
		this.statusAssert = SingletonSupplier.of(() -> Assertions.assertThat(getResponse().getStatus()).as("HTTP status code"));
	}

	private static HttpHeaders getHttpHeaders(HttpServletResponse response) {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		response.getHeaderNames().forEach(name -> headers.put(name, new ArrayList<>(response.getHeaders(name))));
		return new HttpHeaders(headers);
	}

	/**
	 * Provide the response to use if it is available.
	 * <p>Throws an {@link AssertionError} if the request has failed to process,
	 * and the response is not available.
	 * @return the response to use
	 */
	protected abstract R getResponse();

	/**
	 * Return a new {@linkplain MediaTypeAssert assertion} object that uses the
	 * response's {@linkplain MediaType content type} as the object to test.
	 */
	public MediaTypeAssert contentType() {
		return this.contentTypeAssertSupplier.get();
	}

	/**
	 * Return a new {@linkplain HttpHeadersAssert assertion} object that uses
	 * {@link HttpHeaders} as the object to test. The returned assertion
	 * object provides all the regular {@linkplain AbstractMapAssert map
	 * assertions}, with headers mapped by header name.
	 * Examples: <pre><code class="java">
	 * // Check for the presence of the Accept header:
	 * assertThat(response).headers().containsHeader(HttpHeaders.ACCEPT);
	 *
	 * // Check for the absence of the Content-Length header:
	 * assertThat(response).headers().doesNotContainsHeader(HttpHeaders.CONTENT_LENGTH);
	 * </code></pre>
	 */
	public HttpHeadersAssert headers() {
		return this.headersAssertSupplier.get();
	}

	// Content-type shortcuts

	/**
	 * Verify that the response's {@code Content-Type} is equal to the given value.
	 * @param contentType the expected content type
	 */
	public SELF hasContentType(MediaType contentType) {
		contentType().isEqualTo(contentType);
		return this.myself;
	}

	/**
	 * Verify that the response's {@code Content-Type} is equal to the given
	 * string representation.
	 * @param contentType the expected content type
	 */
	public SELF hasContentType(String contentType) {
		contentType().isEqualTo(contentType);
		return this.myself;
	}

	/**
	 * Verify that the response's {@code Content-Type} is
	 * {@linkplain MediaType#isCompatibleWith(MediaType) compatible} with the
	 * given value.
	 * @param contentType the expected compatible content type
	 */
	public SELF hasContentTypeCompatibleWith(MediaType contentType) {
		contentType().isCompatibleWith(contentType);
		return this.myself;
	}

	/**
	 * Verify that the response's {@code Content-Type} is
	 * {@linkplain MediaType#isCompatibleWith(MediaType) compatible} with the
	 * given string representation.
	 * @param contentType the expected compatible content type
	 */
	public SELF hasContentTypeCompatibleWith(String contentType) {
		contentType().isCompatibleWith(contentType);
		return this.myself;
	}

	// Headers shortcuts

	/**
	 * Verify that the response contains a header with the given {@code name}.
	 * @param name the name of an expected HTTP header
	 */
	public SELF containsHeader(String name) {
		headers().containsHeader(name);
		return this.myself;
	}

	/**
	 * Verify that the response does not contain a header with the given {@code name}.
	 * @param name the name of an HTTP header that should not be present
	 */
	public SELF doesNotContainHeader(String name) {
		headers().doesNotContainHeader(name);
		return this.myself;
	}

	/**
	 * Verify that the response contains a header with the given {@code name}
	 * and primary {@code value}.
	 * @param name the name of an expected HTTP header
	 * @param value the expected value of the header
	 */
	public SELF hasHeader(String name, String value) {
		headers().hasValue(name, value);
		return this.myself;
	}

	// Status

	/**
	 * Verify that the HTTP status is equal to the specified status code.
	 * @param status the expected HTTP status code
	 */
	public SELF hasStatus(int status) {
		status().isEqualTo(status);
		return this.myself;
	}

	/**
	 * Verify that the HTTP status is equal to the specified
	 * {@linkplain HttpStatus status}.
	 * @param status the expected HTTP status code
	 */
	public SELF hasStatus(HttpStatus status) {
		return hasStatus(status.value());
	}

	/**
	 * Verify that the HTTP status is equal to {@link HttpStatus#OK}.
	 * @see #hasStatus(HttpStatus)
	 */
	public SELF hasStatusOk() {
		return hasStatus(HttpStatus.OK);
	}

	/**
	 * Verify that the HTTP status code is in the 1xx range.
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.1">RFC 2616</a>
	 */
	public SELF hasStatus1xxInformational() {
		return hasStatusSeries(Series.INFORMATIONAL);
	}

	/**
	 * Verify that the HTTP status code is in the 2xx range.
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.2">RFC 2616</a>
	 */
	public SELF hasStatus2xxSuccessful() {
		return hasStatusSeries(Series.SUCCESSFUL);
	}

	/**
	 * Verify that the HTTP status code is in the 3xx range.
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.3">RFC 2616</a>
	 */
	public SELF hasStatus3xxRedirection() {
		return hasStatusSeries(Series.REDIRECTION);
	}

	/**
	 * Verify that the HTTP status code is in the 4xx range.
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.4">RFC 2616</a>
	 */
	public SELF hasStatus4xxClientError() {
		return hasStatusSeries(Series.CLIENT_ERROR);
	}

	/**
	 * Verify that the HTTP status code is in the 5xx range.
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.5">RFC 2616</a>
	 */
	public SELF hasStatus5xxServerError() {
		return hasStatusSeries(Series.SERVER_ERROR);
	}

	private SELF hasStatusSeries(Series series) {
		Assertions.assertThat(Series.resolve(getResponse().getStatus())).as("HTTP status series").isEqualTo(series);
		return this.myself;
	}

	private AbstractIntegerAssert<?> status() {
		return this.statusAssert.get();
	}

}
