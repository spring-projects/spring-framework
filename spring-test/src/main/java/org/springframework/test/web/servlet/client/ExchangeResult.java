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
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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


	private static final Log logger = LogFactory.getLog(ExchangeResult.class);


	private final ConvertibleClientHttpResponse clientResponse;

	/** Ensure single logging; for example, for expectAll. */
	private boolean diagnosticsLogged;


	ExchangeResult(@Nullable ConvertibleClientHttpResponse response) {
		Assert.notNull(response, "Response must not be null");
		this.clientResponse = response;
	}

	ExchangeResult(ExchangeResult result) {
		this(result.clientResponse);
		this.diagnosticsLogged = result.diagnosticsLogged;
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

	@Nullable
	public <T> T getBody(Class<T> bodyType) {
		return this.clientResponse.bodyTo(bodyType);
	}

	@Nullable
	public <T> T getBody(ParameterizedTypeReference<T> bodyType) {
		return this.clientResponse.bodyTo(bodyType);
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

}
