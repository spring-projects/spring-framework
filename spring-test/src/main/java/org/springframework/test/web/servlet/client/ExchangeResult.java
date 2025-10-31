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

package org.springframework.test.web.servlet.client;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.json.JsonConverterDelegate;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;

/**
 * Container for request and response details for exchanges performed through
 * {@link RestTestClient}.
 *
 * @author Rob Worsnop
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class ExchangeResult {

	private static final Pattern SAME_SITE_PATTERN = Pattern.compile("(?i).*SameSite=(Strict|Lax|None).*");

	private static final Pattern PARTITIONED_PATTERN = Pattern.compile("(?i).*;\\s*Partitioned(\\s*;.*|\\s*)$");

	private static final List<MediaType> PRINTABLE_MEDIA_TYPES = List.of(
			MediaType.parseMediaType("application/*+json"), MediaType.APPLICATION_XML,
			MediaType.parseMediaType("text/*"), MediaType.APPLICATION_FORM_URLENCODED);


	private static final Log logger = LogFactory.getLog(ExchangeResult.class);


	private final HttpRequest request;

	private final ConvertibleClientHttpResponse clientResponse;

	private final @Nullable String uriTemplate;

	private final byte[] requestBody;

	private final @Nullable JsonConverterDelegate converterDelegate;

	/** Ensure single logging; for example, for expectAll. */
	private boolean diagnosticsLogged;


	ExchangeResult(
			HttpRequest request, ConvertibleClientHttpResponse response, @Nullable String uriTemplate,
			byte[] requestBody, @Nullable JsonConverterDelegate converter) {

		Assert.notNull(request, "HttpRequest must not be null");
		Assert.notNull(response, "ClientHttpResponse must not be null");
		this.request = request;
		this.clientResponse = response;
		this.uriTemplate = uriTemplate;
		this.requestBody = requestBody;
		this.converterDelegate = converter;
	}

	ExchangeResult(ExchangeResult result) {
		this(result.request, result.clientResponse, result.uriTemplate, result.requestBody, result.converterDelegate);
		this.diagnosticsLogged = result.diagnosticsLogged;
	}


	/**
	 * Return the method of the request.
	 */
	public HttpMethod getMethod() {
		return this.request.getMethod();
	}

	/**
	 * Return the URI of the request.
	 */
	public URI getUrl() {
		return this.request.getURI();
	}

	/**
	 * Return the original URI template used to prepare the request, if any.
	 */
	public @Nullable String getUriTemplate() {
		return this.uriTemplate;
	}

	/**
	 * Return the request headers sent to the server.
	 */
	public HttpHeaders getRequestHeaders() {
		return this.request.getHeaders();
	}

	/**
	 * Return the HTTP status code as an {@link HttpStatusCode} value.
	 */
	public HttpStatusCode getStatus() {
		try {
			return this.clientResponse.getStatusCode();
		}
		catch (IOException ex) {
			throw new AssertionError(ex);
		}
	}

	/**
	 * Return the response headers received from the server.
	 */
	public HttpHeaders getResponseHeaders() {
		return this.clientResponse.getHeaders();
	}

	/**
	 * Return response cookies received from the server.
	 */
	public MultiValueMap<String, ResponseCookie> getResponseCookies() {
		return Optional.ofNullable(this.clientResponse.getHeaders().get(HttpHeaders.SET_COOKIE)).orElse(List.of()).stream()
				.flatMap(header -> {
					Matcher matcher = SAME_SITE_PATTERN.matcher(header);
					String sameSite = (matcher.matches() ? matcher.group(1) : null);
					boolean partitioned = PARTITIONED_PATTERN.matcher(header).matches();
					return HttpCookie.parse(header).stream().map(cookie -> toResponseCookie(cookie, sameSite, partitioned));
				})
				.collect(LinkedMultiValueMap::new,
						(cookies, cookie) -> cookies.add(cookie.getName(), cookie),
						LinkedMultiValueMap::addAll);
	}

	private static ResponseCookie toResponseCookie(HttpCookie cookie, @Nullable String sameSite, boolean partitioned) {
		return ResponseCookie.from(cookie.getName(), cookie.getValue())
				.domain(cookie.getDomain())
				.httpOnly(cookie.isHttpOnly())
				.maxAge(cookie.getMaxAge())
				.path(cookie.getPath())
				.secure(cookie.getSecure())
				.sameSite(sameSite)
				.partitioned(partitioned)
				.build();
	}

	/**
	 * Return the raw request body content written through the request.
	 */
	public byte[] getRequestBodyContent() {
		return this.requestBody;
	}

	/**
	 * Provide access to the response. For internal use to decode the body.
	 */
	ConvertibleClientHttpResponse getClientResponse() {
		return this.clientResponse;
	}

	/**
	 * Return the raw response body read through the response.
	 */
	public byte[] getResponseBodyContent() {
		try {
			return StreamUtils.copyToByteArray(this.clientResponse.getBody());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to get response content: " + ex);
		}
	}

	/**
	 * Return a {@link JsonConverterDelegate} based on the configured HTTP message converters.
	 * Mainly for internal use from AssertJ support.
	 */
	public @Nullable JsonConverterDelegate getConverterDelegate() {
		return this.converterDelegate;
	}

	/**
	 * Execute the given Runnable, catch any {@link AssertionError}, log details
	 * about the request and response at ERROR level under the class log
	 * category, and after that re-throw the error.
	 */
	public void assertWithDiagnostics(Runnable assertion) {
		try {
			assertion.run();
		}
		catch (AssertionError ex) {
			if (!this.diagnosticsLogged && logger.isErrorEnabled()) {
				this.diagnosticsLogged = true;
				logger.error("Request details for assertion failure:\n" + this);
			}
			throw ex;
		}
	}

	@Override
	public String toString() {
		return "\n" +
				"> " + getMethod() + " " + getUrl() + "\n" +
				"> " + formatHeaders(getRequestHeaders(), "\n> ") + "\n" +
				"\n" +
				formatBody(getRequestHeaders().getContentType(), this.requestBody) + "\n" +
				"\n" +
				"< " + formatStatus(getStatus()) + "\n" +
				"< " + formatHeaders(getResponseHeaders(), "\n< ") + "\n" +
				"\n" +
				formatBody(getResponseHeaders().getContentType(), getResponseBodyContent()) +"\n";
	}

	private String formatStatus(HttpStatusCode statusCode) {
		String result = statusCode.toString();
		if (statusCode instanceof HttpStatus status) {
			result += " " + status.getReasonPhrase();
		}
		return result;
	}

	private String formatHeaders(HttpHeaders headers, String delimiter) {
		return headers.headerSet().stream()
				.map(entry -> entry.getKey() + ": " + entry.getValue())
				.collect(Collectors.joining(delimiter));
	}

	private String formatBody(@Nullable MediaType contentType, byte[] bytes) {
		if (contentType == null) {
			return bytes.length + " bytes of content (unknown content-type).";
		}
		Charset charset = contentType.getCharset();
		if (charset != null) {
			return new String(bytes, charset);
		}
		if (PRINTABLE_MEDIA_TYPES.stream().anyMatch(contentType::isCompatibleWith)) {
			return new String(bytes, StandardCharsets.UTF_8);
		}
		return bytes.length + " bytes of content.";
	}

}
