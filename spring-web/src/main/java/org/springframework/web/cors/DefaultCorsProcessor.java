/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.cors;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Default implementation of {@link CorsProcessor}, as defined by the
 * <a href="http://www.w3.org/TR/cors/">CORS W3C recommandation</a>.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanhcev
 * @since 4.2
 */
public class DefaultCorsProcessor implements CorsProcessor {

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");


	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public boolean processPreFlightRequest(CorsConfiguration config, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		Assert.isTrue(CorsUtils.isPreFlightRequest(request));

		ServerHttpResponse serverResponse = new ServletServerHttpResponse(response);
		if (responseHasCors(serverResponse)) {
			return true;
		}

		ServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
		if (handleInternal(serverRequest, serverResponse, config, true)) {
			serverResponse.flush();
			return true;
		}

		return false;
	}

	@Override
	public boolean processActualRequest(CorsConfiguration config, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		Assert.isTrue(CorsUtils.isCorsRequest(request) && !CorsUtils.isPreFlightRequest(request));

		ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(response);
		if (responseHasCors(serverResponse)) {
			return true;
		}

		ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
		if (handleInternal(serverRequest, serverResponse, config, false)) {
			serverResponse.flush();
			return true;
		}

		return false;
	}

	private boolean responseHasCors(ServerHttpResponse response) {
		boolean hasAllowOrigin = false;
		try {
			hasAllowOrigin = (response.getHeaders().getAccessControlAllowOrigin() != null);
		}
		catch (NullPointerException npe) {
			// SPR-11919 and https://issues.jboss.org/browse/WFLY-3474
		}
		if (hasAllowOrigin) {
			logger.debug("Skip adding CORS headers, response already contains \"Access-Control-Allow-Origin\"");
		}
		return hasAllowOrigin;
	}

	protected boolean handleInternal(ServerHttpRequest request, ServerHttpResponse response,
			CorsConfiguration config, boolean isPreFlight) throws IOException {

		String requestOrigin = request.getHeaders().getOrigin();
		String allowOrigin = checkOrigin(config, requestOrigin);

		HttpMethod requestMethod = getMethodToUse(request, isPreFlight);
		List<HttpMethod> allowMethods = checkMethods(config, requestMethod);

		List<String> requestHeaders = getHeadersToUse(request, isPreFlight);
		List<String> allowHeaders = checkHeaders(config, requestHeaders);

		if (allowOrigin == null || allowMethods == null || (isPreFlight && allowHeaders == null)) {
			handleInvalidCorsRequest(response);
			return false;
		}

		HttpHeaders responseHeaders = response.getHeaders();
		responseHeaders.setAccessControlAllowOrigin(allowOrigin);
		responseHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);

		if (isPreFlight) {
			responseHeaders.setAccessControlAllowMethods(allowMethods);
		}

		if (isPreFlight && !allowHeaders.isEmpty()) {
			responseHeaders.setAccessControlAllowHeaders(allowHeaders);
		}

		if (!CollectionUtils.isEmpty(config.getExposedHeaders())) {
			responseHeaders.setAccessControlExposeHeaders(config.getExposedHeaders());
		}

		if (Boolean.TRUE.equals(config.getAllowCredentials())) {
			responseHeaders.setAccessControlAllowCredentials(true);
		}

		if (isPreFlight && config.getMaxAge() != null) {
			responseHeaders.setAccessControlMaxAge(config.getMaxAge());
		}

		return true;
	}

	/**
	 * Check the origin and determine the origin for the response. The default
	 * implementation simply delegates to
	 * {@link org.springframework.web.cors.CorsConfiguration#checkOrigin(String)}
	 */
	protected String checkOrigin(CorsConfiguration config, String requestOrigin) {
		return config.checkOrigin(requestOrigin);
	}

	/**
	 * Check the HTTP method and determine the methods for the response of a
	 * pre-flight request. The default implementation simply delegates to
	 * {@link org.springframework.web.cors.CorsConfiguration#checkOrigin(String)}
	 */
	protected List<HttpMethod> checkMethods(CorsConfiguration config, HttpMethod requestMethod) {
		return config.checkHttpMethod(requestMethod);
	}

	private HttpMethod getMethodToUse(ServerHttpRequest request, boolean isPreFlight) {
		return (isPreFlight ? request.getHeaders().getAccessControlRequestMethod() : request.getMethod());
	}

	/**
	 * Check the headers and determine the headers for the response of a
	 * pre-flight request. The default implementation simply delegates to
	 * {@link org.springframework.web.cors.CorsConfiguration#checkOrigin(String)}
	 */
	protected List<String> checkHeaders(CorsConfiguration config, List<String> requestHeaders) {
		return config.checkHeaders(requestHeaders);
	}

	private List<String> getHeadersToUse(ServerHttpRequest request, boolean isPreFlight) {
		HttpHeaders headers = request.getHeaders();
		return (isPreFlight ? headers.getAccessControlRequestHeaders() : new ArrayList<String>(headers.keySet()));
	}

	/**
	 * Invoked when one of the CORS checks failed.
	 * The default implementation sets the response status to 403 and writes
	 * "Invalid CORS request" to the response.
	 */
	protected void handleInvalidCorsRequest(ServerHttpResponse response) throws IOException {
		response.setStatusCode(HttpStatus.FORBIDDEN);
		response.getBody().write("Invalid CORS request".getBytes(UTF8_CHARSET));
	}

}
