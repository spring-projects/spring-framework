/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.cors.reactive;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;

/**
 * The default implementation of {@link CorsProcessor},
 * as defined by the <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>.
 *
 * <p>Note that when the supplied {@link CorsConfiguration} is {@code null}, this
 * implementation does not reject CORS requests outright but simply avoids adding
 * CORS headers to the response. CORS processing is also skipped if the response
 * already contains CORS headers.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultCorsProcessor implements CorsProcessor {

	private static final Log logger = LogFactory.getLog(DefaultCorsProcessor.class);

	private static final List<String> VARY_HEADERS = List.of(
			HttpHeaders.ORIGIN, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);

	/**
	 * The {@code Access-Control-Request-Private-Network} request header field name.
	 * @see <a href="https://wicg.github.io/private-network-access/">Private Network Access specification</a>
	 */
	static final String ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK = "Access-Control-Request-Private-Network";

	/**
	 * The {@code Access-Control-Allow-Private-Network} response header field name.
	 * @see <a href="https://wicg.github.io/private-network-access/">Private Network Access specification</a>
	 */
	static final String ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK = "Access-Control-Allow-Private-Network";


	@Override
	public boolean process(@Nullable CorsConfiguration config, ServerWebExchange exchange) {
		if (config == null) {
			return true;
		}

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		HttpHeaders responseHeaders = response.getHeaders();

		List<String> varyHeaders = responseHeaders.get(HttpHeaders.VARY);
		if (varyHeaders == null) {
			responseHeaders.addAll(HttpHeaders.VARY, VARY_HEADERS);
		}
		else {
			for (String header : VARY_HEADERS) {
				if (!varyHeaders.contains(header)) {
					responseHeaders.add(HttpHeaders.VARY, header);
				}
			}
		}

		try {
			if (!CorsUtils.isCorsRequest(request)) {
				return true;
			}
		}
		catch (IllegalArgumentException ex) {
			logger.debug("Reject: origin is malformed");
			rejectRequest(response);
			return false;
		}

		if (responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) != null) {
			logger.trace("Skip: response already contains \"Access-Control-Allow-Origin\"");
			return true;
		}

		return handleInternal(exchange, config, CorsUtils.isPreFlightRequest(request));
	}

	/**
	 * Invoked when one of the CORS checks failed.
	 */
	protected void rejectRequest(ServerHttpResponse response) {
		response.setStatusCode(HttpStatus.FORBIDDEN);
	}

	/**
	 * Handle the given request.
	 */
	protected boolean handleInternal(ServerWebExchange exchange,
			CorsConfiguration config, boolean preFlightRequest) {

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		HttpHeaders responseHeaders = response.getHeaders();

		String requestOrigin = request.getHeaders().getOrigin();
		String allowOrigin = checkOrigin(config, requestOrigin);
		if (allowOrigin == null) {
			logger.debug("Reject: '" + requestOrigin + "' origin is not allowed");
			rejectRequest(response);
			return false;
		}

		HttpMethod requestMethod = getMethodToUse(request, preFlightRequest);
		List<HttpMethod> allowMethods = checkMethods(config, requestMethod);
		if (allowMethods == null) {
			logger.debug("Reject: HTTP '" + requestMethod + "' is not allowed");
			rejectRequest(response);
			return false;
		}

		List<String> requestHeaders = getHeadersToUse(request, preFlightRequest);
		List<String> allowHeaders = checkHeaders(config, requestHeaders);
		if (preFlightRequest && allowHeaders == null) {
			logger.debug("Reject: headers '" + requestHeaders + "' are not allowed");
			rejectRequest(response);
			return false;
		}

		responseHeaders.setAccessControlAllowOrigin(allowOrigin);

		if (preFlightRequest) {
			responseHeaders.setAccessControlAllowMethods(allowMethods);
		}

		if (preFlightRequest && !CollectionUtils.isEmpty(allowHeaders)) {
			responseHeaders.setAccessControlAllowHeaders(allowHeaders);
		}

		if (!CollectionUtils.isEmpty(config.getExposedHeaders())) {
			responseHeaders.setAccessControlExposeHeaders(config.getExposedHeaders());
		}

		if (Boolean.TRUE.equals(config.getAllowCredentials())) {
			responseHeaders.setAccessControlAllowCredentials(true);
		}

		if (Boolean.TRUE.equals(config.getAllowPrivateNetwork()) &&
				Boolean.parseBoolean(request.getHeaders().getFirst(ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK))) {
			responseHeaders.set(ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK, Boolean.toString(true));
		}

		if (preFlightRequest && config.getMaxAge() != null) {
			responseHeaders.setAccessControlMaxAge(config.getMaxAge());
		}

		return true;
	}

	/**
	 * Check the origin and determine the origin for the response. The default
	 * implementation simply delegates to
	 * {@link CorsConfiguration#checkOrigin(String)}.
	 */
	protected @Nullable String checkOrigin(CorsConfiguration config, @Nullable String requestOrigin) {
		return config.checkOrigin(requestOrigin);
	}

	/**
	 * Check the HTTP method and determine the methods for the response of a
	 * pre-flight request. The default implementation simply delegates to
	 * {@link CorsConfiguration#checkHttpMethod(HttpMethod)}.
	 */
	protected @Nullable List<HttpMethod> checkMethods(CorsConfiguration config, @Nullable HttpMethod requestMethod) {
		return config.checkHttpMethod(requestMethod);
	}

	private @Nullable HttpMethod getMethodToUse(ServerHttpRequest request, boolean isPreFlight) {
		return (isPreFlight ? request.getHeaders().getAccessControlRequestMethod() : request.getMethod());
	}

	/**
	 * Check the headers and determine the headers for the response of a
	 * pre-flight request. The default implementation simply delegates to
	 * {@link CorsConfiguration#checkHeaders(List)}.
	 */
	protected @Nullable List<String> checkHeaders(CorsConfiguration config, List<String> requestHeaders) {
		return config.checkHeaders(requestHeaders);
	}

	private List<String> getHeadersToUse(ServerHttpRequest request, boolean isPreFlight) {
		HttpHeaders headers = request.getHeaders();
		return (isPreFlight ? headers.getAccessControlRequestHeaders() : new ArrayList<>(headers.headerNames()));
	}

}
