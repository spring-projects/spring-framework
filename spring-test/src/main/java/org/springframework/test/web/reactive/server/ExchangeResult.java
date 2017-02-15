/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.test.web.reactive.server;

import java.net.URI;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * Container for request and response details including the decoded response
 * body from an exchange performed through {@link WebTestClient}.
 *
 * <p>Use {@link #assertThat()} to access built-in assertions on the response,
 * or apply other assertions directly to the data contained in this class.
 * The built-in assertions provide an option for logging diagnostic information
 * about the exchange. The same can also be obtained using the
 * {@link #toString()} method of this class.
 *
 * @param <T> the type of the decoded response body
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ExchangeResult<T> {

	private final HttpMethod method;

	private final URI url;

	private final HttpHeaders requestHeaders;

	private final HttpStatus status;

	private final HttpHeaders responseHeaders;

	private final T responseBody;


	public ExchangeResult(HttpMethod method, URI url, HttpHeaders requestHeaders,
			HttpStatus status, HttpHeaders responseHeaders, T responseBody) {

		this.method = method;
		this.url = url;
		this.requestHeaders = requestHeaders;
		this.status = status;
		this.responseHeaders = responseHeaders;
		this.responseBody = responseBody;
	}


	/**
	 * Provides access to built-in assertions on the response.
	 */
	public ResponseAssertions<T> assertThat() {
		return new ResponseAssertions<T>(this);
	}


	/**
	 * Return the request method of the exchange.
	 */
	public HttpMethod getRequestMethod() {
		return this.method;
	}

	/**
	 * Return the URL of the exchange.
	 */
	public URI getRequestUrl() {
		return this.url;
	}

	/**
	 * Return the request headers of the exchange.
	 */
	public HttpHeaders getRequestHeaders() {
		return this.requestHeaders;
	}

	/**
	 * Return the response status.
	 */
	public HttpStatus getResponseStatus() {
		return this.status;
	}

	/**
	 * Return the response headers.
	 */
	public HttpHeaders getResponseHeaders() {
		return this.responseHeaders;
	}

	/**
	 * Return the decoded response body.
	 */
	public T getResponseBody() {
		return this.responseBody;
	}


	@Override
	public String toString() {
		HttpStatus status = this.status;
		return "\n\n" +
				formatValue("Request", this.method + " " + getRequestUrl()) +
				formatValue("Status", status + " " + status.getReasonPhrase()) +
				formatHeading("Response Headers") +
				formatHeaders(this.responseHeaders) +
				formatHeading("Request Headers") +
				formatHeaders(this.requestHeaders);
	}

	private String formatHeading(String heading) {
		return "\n" + String.format("%s", heading) + "\n";
	}

	private String formatValue(String label, Object value) {
		return String.format("%18s: %s", label, value) + "\n";
	}

	private String formatHeaders(HttpHeaders headers) {
		return headers.entrySet().stream()
				.map(entry -> formatValue(entry.getKey(), entry.getValue()))
				.collect(Collectors.joining());
	}

}
