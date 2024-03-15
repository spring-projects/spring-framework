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
import org.springframework.test.http.HttpHeadersAssert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.function.SingletonSupplier;

/**
 * Base AssertJ {@link org.assertj.core.api.Assert assertions} that can be
 * applied to any object that provides an {@link HttpServletResponse}. This
 * allows to provide direct access to response assertions while providing
 * access to a different top-level object.
 *
 * @author Stephane Nicoll
 * @since 6.2
 * @param <R> the type of {@link HttpServletResponse}
 * @param <SELF> the type of assertions
 * @param <ACTUAL> the type of the object to assert
 */
public abstract class AbstractHttpServletResponseAssert<R extends HttpServletResponse, SELF extends AbstractHttpServletResponseAssert<R, SELF, ACTUAL>, ACTUAL>
		extends AbstractObjectAssert<SELF, ACTUAL> {

	private final Supplier<AbstractIntegerAssert<?>> statusAssert;

	private final Supplier<HttpHeadersAssert> headersAssertSupplier;


	protected AbstractHttpServletResponseAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
		this.statusAssert = SingletonSupplier.of(() -> Assertions.assertThat(getResponse().getStatus()).as("HTTP status code"));
		this.headersAssertSupplier = SingletonSupplier.of(() -> new HttpHeadersAssert(getHttpHeaders(getResponse())));
	}

	/**
	 * Provide the response to use if it is available. Throw an
	 * {@link AssertionError} if the request has failed to process and the
	 * response is not available.
	 * @return the response to use
	 */
	protected abstract R getResponse();

	/**
	 * Return a new {@linkplain HttpHeadersAssert assertion} object that uses
	 * the {@link HttpHeaders} as the object to test. The return assertion
	 * object provides all the regular {@linkplain AbstractMapAssert map
	 * assertions}, with headers mapped by header name.
	 * Examples: <pre><code class='java'>
	 * // Check for the presence of the Accept header:
	 * assertThat(response).headers().containsHeader(HttpHeaders.ACCEPT);
	 * // Check for the absence of the Content-Length header:
	 * assertThat(response).headers().doesNotContainsHeader(HttpHeaders.CONTENT_LENGTH);
	 * </code></pre>
	 */
	public HttpHeadersAssert headers() {
		return this.headersAssertSupplier.get();
	}

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

	private static HttpHeaders getHttpHeaders(HttpServletResponse response) {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		response.getHeaderNames().forEach(name -> headers.put(name, new ArrayList<>(response.getHeaders(name))));
		return new HttpHeaders(headers);
	}

}
