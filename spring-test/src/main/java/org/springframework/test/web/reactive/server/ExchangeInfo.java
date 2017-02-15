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
import java.time.Duration;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * Contains information about a performed exchange.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ExchangeInfo {

	private final HttpMethod method;

	private final URI url;

	private final HttpHeaders requestHeaders;

	private final ClientResponse response;

	private final Duration responseTimeout;


	public ExchangeInfo(HttpMethod httpMethod, URI uri, HttpHeaders requestHeaders,
			ClientResponse response, Duration responseTimeout) {

		this.method = httpMethod;
		this.url = uri;
		this.requestHeaders = requestHeaders;
		this.response = response;
		this.responseTimeout = responseTimeout;
	}


	/**
	 * Return the HTTP method of the exchange.
	 */
	public HttpMethod getHttpMethod() {
		return this.method;
	}

	/**
	 * Return the URI of the exchange.
	 */
	public URI getUrl() {
		return this.url;
	}

	/**
	 * Return the request headers of the exchange.
	 */
	public HttpHeaders getRequestHeaders() {
		return this.requestHeaders;
	}

	/**
	 * Return the {@link ClientResponse} for the exchange.
	 */
	public ClientResponse getResponse() {
		return this.response;
	}

	/**
	 * Return the configured timeout for blocking on response data.
	 */
	public Duration getResponseTimeout() {
		return this.responseTimeout;
	}


	@Override
	public String toString() {
		HttpStatus status = getResponse().statusCode();
		return "\n\n" +
				formatValue("Request", getHttpMethod() + " " + getUrl()) +
				formatValue("Status", status + " " + status.getReasonPhrase()) +
				formatHeading("Response Headers") +
				formatHeaders(getResponse().headers().asHttpHeaders()) +
				formatHeading("Request Headers") +
				formatHeaders(getRequestHeaders());
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
