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
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * Container for the result of an exchange through the {@link WebTestClient}.
 *
 * <p>This type only exposes the status and response headers that are available
 * when the {@link ClientResponse} is first received and before the response
 * body has been consumed.
 *
 * <p>The sub-classes {@link EntityExchangeResult} and {@link FluxExchangeResult}
 * expose further information about the response body and are returned only
 * after the test client has been used to decode and consume the response.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ExchangeResult {

	private final HttpMethod method;

	private final URI url;

	private final HttpHeaders requestHeaders;

	private final HttpStatus status;

	private final HttpHeaders responseHeaders;


	ExchangeResult(ClientHttpRequest request, ClientResponse response) {
		this.method = request.getMethod();
		this.url = request.getURI();
		this.requestHeaders = request.getHeaders();
		this.status = response.statusCode();
		this.responseHeaders = response.headers().asHttpHeaders();
	}

	ExchangeResult(ExchangeResult exchange) {
		this.method = exchange.getMethod();
		this.url = exchange.getUrl();
		this.requestHeaders = exchange.getRequestHeaders();
		this.status = exchange.getStatus();
		this.responseHeaders = exchange.getResponseHeaders();
	}


	/**
	 * Return the method of the request.
	 */
	public HttpMethod getMethod() {
		return this.method;
	}

	/**
	 * Return the request headers that were sent to the server.
	 */
	public URI getUrl() {
		return this.url;
	}

	/**
	 * Return the request headers sent to the server.
	 */
	public HttpHeaders getRequestHeaders() {
		return this.requestHeaders;
	}

	/**
	 * Return the status of the executed request.
	 */
	public HttpStatus getStatus() {
		return this.status;
	}

	/**
	 * Return the response headers received from the server.
	 */
	public HttpHeaders getResponseHeaders() {
		return this.responseHeaders;
	}


	@Override
	public String toString() {
		HttpStatus status = this.status;
		return "\n\n" +
				formatValue("Request", this.method + " " + getUrl()) +
				formatValue("Status", status + " " + status.getReasonPhrase()) +
				formatHeading("Response Headers") + formatHeaders(this.responseHeaders) +
				formatHeading("Request Headers") + formatHeaders(this.requestHeaders) +
				"\n" +
				formatValue("Response Body", formatResponseBody());
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

	protected String formatResponseBody() {
		return "Not read yet";
	}

}
