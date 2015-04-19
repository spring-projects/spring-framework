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
import java.util.Arrays;
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

/**
 * Default implementation of {@link CorsProcessor}, as defined by the
 * <a href="http://www.w3.org/TR/cors/">CORS W3C recommandation</a>.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public class DefaultCorsProcessor implements CorsProcessor {

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");


	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public boolean processPreFlightRequest(CorsConfiguration config, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		ServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
		ServerHttpResponse serverResponse = new ServletServerHttpResponse(response);
		boolean isPreFlight = CorsUtils.isPreFlightRequest(request);
		Assert.isTrue(isPreFlight);
		if (skip(serverResponse)) {
			return true;
		}

		if (check(serverRequest, serverResponse, config, isPreFlight)) {
			setAllowOrigin(serverRequest, serverResponse, config.getAllowedOrigins(), config.isAllowCredentials());
			setAllowCredentials(serverResponse, config.isAllowCredentials());
			setAllowMethods(serverRequest, serverResponse, config.getAllowedMethods());
			setAllowHeadersHeader(serverRequest, serverResponse, config.getAllowedHeaders());
			setMaxAgeHeader(serverResponse, config.getMaxAge());
			serverResponse.close();
			return true;
		}
		return false;
	}

	@Override
	public boolean processActualRequest(CorsConfiguration config, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		ServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
		ServerHttpResponse serverResponse = new ServletServerHttpResponse(response);
		boolean isPreFlight = CorsUtils.isPreFlightRequest(request);
		Assert.isTrue(CorsUtils.isCorsRequest(request) && !isPreFlight);
		if (skip(serverResponse)) {
			return true;
		}

		if (check(serverRequest, serverResponse, config, isPreFlight)) {
			setAllowOrigin(serverRequest, serverResponse, config.getAllowedOrigins(), config.isAllowCredentials());
			setAllowCredentials(serverResponse, config.isAllowCredentials());
			setExposeHeadersHeader(serverResponse, config.getExposedHeaders());
			serverResponse.close();
			return true;
		}
		return false;
	}

	private boolean skip(ServerHttpResponse response) {
		if (hasAllowOriginHeader(response)) {
			logger.debug("Skip adding CORS headers, response already contains \"Access-Control-Allow-Origin\"");
			return true;
		}
		return false;
	}

	private boolean check(ServerHttpRequest request, ServerHttpResponse response,
			CorsConfiguration config, boolean isPreFlight) throws IOException {

		if (!checkOrigin(request, config.getAllowedOrigins()) ||
				!checkMethod(request, config.getAllowedMethods(), isPreFlight) ||
				!checkHeaders(request, config.getAllowedHeaders(), isPreFlight)) {
			response.setStatusCode(HttpStatus.FORBIDDEN);
			response.getBody().write("Invalid CORS request".getBytes(UTF8_CHARSET));
			return false;
		}
		return true;
	}

	private boolean hasAllowOriginHeader(ServerHttpResponse response) {
		boolean hasCorsResponseHeaders = false;
		try {
			// Perhaps a CORS Filter has already added this?
			hasCorsResponseHeaders = response.getHeaders().getAccessControlAllowOrigin() != null;
		}
		catch (NullPointerException npe) {
			// See SPR-11919 and https://issues.jboss.org/browse/WFLY-3474
		}
		return hasCorsResponseHeaders;
	}

	private boolean checkOrigin(ServerHttpRequest request, List<String> allowedOrigins) {
		String originHeader = request.getHeaders().getOrigin();
		if (originHeader == null || allowedOrigins == null) {
			return false;
		}
		if (allowedOrigins.contains("*")) {
			return true;
		}
		for (String allowedOrigin : allowedOrigins) {
			if (originHeader.equalsIgnoreCase(allowedOrigin)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkMethod(ServerHttpRequest request, List<String> allowedMethods, boolean isPreFlight) {
		HttpMethod requestMethod = isPreFlight ?
				request.getHeaders().getAccessControlRequestMethod() :
				request.getMethod();
		if (allowedMethods == null) {
			allowedMethods = Arrays.asList(HttpMethod.GET.name());
		}
		if (allowedMethods.contains("*")) {
			return true;
		}
		for (String allowedMethod : allowedMethods) {
			if (allowedMethod.equalsIgnoreCase(requestMethod.name())) {
				return true;
			}
		}
		return false;
	}

	private boolean checkHeaders(ServerHttpRequest request, List<String> allowedHeaders, boolean isPreFlight) {
		List<String> requestHeaders = isPreFlight ? request.getHeaders().getAccessControlRequestHeaders() :
				new ArrayList<String>(request.getHeaders().keySet());
		if ((allowedHeaders != null) && allowedHeaders.contains("*")) {
			return true;
		}
		for (String requestHeader : requestHeaders) {
			if (!HttpHeaders.ORIGIN.equals(requestHeader)) {
				requestHeader = requestHeader.trim();
				boolean found = false;
				if (allowedHeaders != null) {
					for (String header : allowedHeaders) {
						if (requestHeader.equalsIgnoreCase(header)) {
							found = true;
							break;
						}
					}
				}
				if (!found) {
					return false;
				}
			}
		}
		return true;
	}

	private void setAllowOrigin(ServerHttpRequest request, ServerHttpResponse response,
			List<String> allowedOrigins, Boolean allowCredentials) {

		String origin = request.getHeaders().getOrigin();
		if (allowedOrigins.contains("*") && (allowCredentials == null || !allowCredentials)) {
			response.getHeaders().setAccessControlAllowOrigin("*");
			return;
		}
		response.getHeaders().setAccessControlAllowOrigin(origin);
		response.getHeaders().add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
	}

	private void setAllowMethods(ServerHttpRequest request, ServerHttpResponse response,
			List<String> allowedMethods) {

		if (allowedMethods == null) {
			allowedMethods = Arrays.asList(HttpMethod.GET.name());
		}
		if (allowedMethods.contains("*")) {
			HttpMethod method = request.getHeaders().getAccessControlRequestMethod();
			response.getHeaders().setAccessControlAllowMethods(Arrays.asList(method));
		}
		else {
			List<HttpMethod> methods = new ArrayList<HttpMethod>();
			for (String method : allowedMethods) {
				methods.add(HttpMethod.valueOf(method));
			}
			response.getHeaders().setAccessControlAllowMethods(methods);
		}
	}

	private void setAllowHeadersHeader(ServerHttpRequest request, ServerHttpResponse response,
			List<String> allowedHeaders) {
		if ((allowedHeaders != null) && !allowedHeaders.isEmpty()) {
			List<String> requestHeaders = request.getHeaders().getAccessControlRequestHeaders();
			boolean matchAll = allowedHeaders.contains("*");
			List<String> matchingHeaders = new ArrayList<String>();
			for (String requestHeader : requestHeaders) {
				for (String header : allowedHeaders) {
					requestHeader = requestHeader.trim();
					if (matchAll || requestHeader.equalsIgnoreCase(header)) {
						matchingHeaders.add(requestHeader);
						break;
					}
				}
			}
			if (!matchingHeaders.isEmpty()) {
				response.getHeaders().setAccessControlAllowHeaders(matchingHeaders);
			}
		}
	}

	private void setExposeHeadersHeader(ServerHttpResponse response, List<String> exposedHeaders) {
		if ((exposedHeaders != null) && !exposedHeaders.isEmpty()) {
			response.getHeaders().setAccessControlExposeHeaders(exposedHeaders);
		}
	}

	private void setAllowCredentials(ServerHttpResponse response, Boolean allowCredentials) {
		if ((allowCredentials != null) && allowCredentials) {
			response.getHeaders().setAccessControlAllowCredentials(allowCredentials);
		}
	}

	private void setMaxAgeHeader(ServerHttpResponse response, Long maxAge) {
		if (maxAge != null) {
			response.getHeaders().setAccessControlMaxAge(maxAge);
		}
	}

}
