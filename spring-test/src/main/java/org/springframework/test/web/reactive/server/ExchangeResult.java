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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import reactor.core.publisher.MonoProcessor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;

/**
 * Simple container for request and response details from an exchange performed
 * through the {@link WebTestClient}.
 *
 * <p>When an {@code ExchangeResult} is first created it has only the status and
 * headers of the response available. When the response body is extracted, the
 * {@code ExchangeResult} is re-created as either {@link EntityExchangeResult}
 * or {@link FluxExchangeResult} that further expose extracted entities.
 *
 * <p>Raw request and response content may also be accessed once complete via
 * {@link #getRequestContent()} or {@link #getResponseContent()}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 *
 * @see EntityExchangeResult
 * @see FluxExchangeResult
 */
public class ExchangeResult {

	private static final List<MediaType> PRINTABLE_MEDIA_TYPES = Arrays.asList(
			MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.parseMediaType("text/*"),
			MediaType.APPLICATION_FORM_URLENCODED);


	private final WiretapClientHttpRequest request;

	private final WiretapClientHttpResponse response;


	/**
	 * Constructor used when the {@code ClientHttpResponse} becomes available.
	 */
	protected ExchangeResult(WiretapClientHttpRequest request, WiretapClientHttpResponse response) {
		this.request = request;
		this.response = response;
	}

	/**
	 * Copy constructor used when the body is decoded or consumed.
	 */
	protected ExchangeResult(ExchangeResult other) {
		this.request = other.request;
		this.response = other.response;
	}


	/**
	 * Return the method of the request.
	 */
	public HttpMethod getMethod() {
		return this.request.getMethod();
	}

	/**
	 * Return the request headers that were sent to the server.
	 */
	public URI getUrl() {
		return this.request.getURI();
	}

	/**
	 * Return the request headers sent to the server.
	 */
	public HttpHeaders getRequestHeaders() {
		return this.request.getHeaders();
	}

	/**
	 * Return a "promise" for the raw request body content once completed.
	 */
	public MonoProcessor<byte[]> getRequestContent() {
		return this.request.getBodyContent();
	}

	/**
	 * Return the status of the executed request.
	 */
	public HttpStatus getStatus() {
		return this.response.getStatusCode();
	}

	/**
	 * Return the response headers received from the server.
	 */
	public HttpHeaders getResponseHeaders() {
		return this.response.getHeaders();
	}

	/**
	 * Return response cookies received from the server.
	 */
	public MultiValueMap<String, ResponseCookie> getResponseCookies() {
		return this.getResponseCookies();
	}

	/**
	 * Return a "promise" for the raw response body content once completed.
	 */
	public MonoProcessor<byte[]> getResponseContent() {
		return this.response.getBodyContent();
	}


	/**
	 * Execute the given Runnable in the context of "this" instance and decorate
	 * any {@link AssertionError}s raised with request and response details.
	 */
	public void assertWithDiagnostics(Runnable assertion) {
		try {
			assertion.run();
		}
		catch (AssertionError ex) {
			throw new AssertionError("Assertion failed on the following exchange:" + this, ex);
		}
	}

	/**
	 * Variant of {@link #assertWithDiagnostics(Runnable)} that passes through
	 * a return value from the assertion code.
	 */
	public <T> T assertWithDiagnosticsAndReturn(Supplier<T> assertion) {
		try {
			return assertion.get();
		}
		catch (AssertionError ex) {
			throw new AssertionError("Assertion failed on the following exchange:" + this, ex);
		}
	}


	@Override
	public String toString() {
		return "\n" +
				"> " + getMethod() + " " + getUrl() + "\n" +
				"> " + formatHeaders("> ", getRequestHeaders()) + "\n" +
				"\n" +
				formatContent(getRequestHeaders().getContentType(), getRequestContent()) + "\n" +
				"\n" +
				"< " + getStatus() + " " + getStatusReason() + "\n" +
				"< " + formatHeaders("< ", getResponseHeaders()) + "\n" +
				"\n" +
				formatContent(getResponseHeaders().getContentType(), getResponseContent()) + "\n\n";
	}

	private String getStatusReason() {
		String reason = "";
		if (getStatus() != null && getStatus().getReasonPhrase() != null) {
			reason = getStatus().getReasonPhrase();
		}
		return reason;
	}

	private String formatHeaders(String linePrefix, HttpHeaders headers) {
		return headers.entrySet().stream()
				.map(entry -> entry.getKey() + ": " + entry.getValue())
				.collect(Collectors.joining("\n" + linePrefix));
	}

	private String formatContent(MediaType contentType, MonoProcessor<byte[]> body) {
		if (body.isSuccess()) {
			byte[] bytes = body.blockMillis(0);
			if (bytes.length == 0) {
				return "No content";
			}

			if (contentType == null) {
				return "Unknown content type (" + bytes.length + " bytes)";
			}

			Charset charset = contentType.getCharset();
			if (charset != null) {
				return new String(bytes, charset);
			}

			if (PRINTABLE_MEDIA_TYPES.stream().anyMatch(contentType::isCompatibleWith)) {
				return new String(bytes, StandardCharsets.UTF_8);
			}

			return "Unknown charset (" + bytes.length + " bytes)";
		}
		else if (body.isError()) {
			return "I/O failure: " + body.getError().getMessage();
		}
		else {
			return "Content not available yet";
		}
	}

}
