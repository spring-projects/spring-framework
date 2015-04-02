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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link CorsProcessor}, as defined by the
 * <a href="http://www.w3.org/TR/cors/">CORS W3C recommandation</a>.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public class DefaultCorsProcessor implements CorsProcessor {

	protected final Log logger = LogFactory.getLog(getClass());

	@Override
	public boolean processPreFlightRequest(CorsConfiguration config, HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (!CorsUtils.isPreFlightRequest(request)) {
			rejectCorsRequest(response);
			return false;
		}
		if (check(request, response, config)) {
			setOriginHeader(request, response, config.getAllowedOrigins(), config.isAllowCredentials());
			setAllowCredentialsHeader(response, config.isAllowCredentials());
			setAllowMethodsHeader(request, response, config.getAllowedMethods());
			setAllowHeadersHeader(request, response, config.getAllowedHeaders());
			setMaxAgeHeader(response, config.getMaxAge());
		}
		return true;
	}

	@Override
	public boolean processActualRequest(CorsConfiguration config, HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (CorsUtils.isPreFlightRequest(request) || !CorsUtils.isCorsRequest(request)) {
			rejectCorsRequest(response);
			return false;
		}
		if (check(request, response, config)) {
			setOriginHeader(request, response, config.getAllowedOrigins(), config.isAllowCredentials());
			setAllowCredentialsHeader(response, config.isAllowCredentials());
			setExposeHeadersHeader(response, config.getExposedHeaders());
		}
		return true;
	}

	private void rejectCorsRequest(HttpServletResponse response) throws IOException {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CORS request");
	}

	private boolean check(HttpServletRequest request, HttpServletResponse response, CorsConfiguration config) throws IOException {
		if (CorsUtils.isCorsResponse(response)) {
			logger.debug("Skip adding CORS headers, response already contains \"Access-Control-Allow-Origin\"");
			return false;
		}
		if (!(checkOrigin(request, config.getAllowedOrigins()) &&
				checkRequestMethod(request, config.getAllowedMethods()) &&
				checkRequestHeaders(request, config.getAllowedHeaders()))) {
			rejectCorsRequest(response);
			return false;
		}
		return true;
	}

	private boolean checkOrigin(HttpServletRequest request, List<String> allowedOrigins) {
		String origin = request.getHeader(HttpHeaders.ORIGIN);
		if ((origin == null) || (allowedOrigins == null)) {
			return false;
		}
		if (allowedOrigins.contains("*")) {
			return true;
		}
		for (String allowedOrigin : allowedOrigins) {
			if (origin.equalsIgnoreCase(allowedOrigin)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkRequestMethod(HttpServletRequest request, List<String> allowedMethods) {
		String requestMethod = CorsUtils.isPreFlightRequest(request) ?
				request.getHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD) : request.getMethod();
		if (requestMethod == null) {
			return false;
		}
		if (allowedMethods == null) {
			allowedMethods = Arrays.asList(HttpMethod.GET.name());
		}
		if (allowedMethods.contains("*")) {
			return true;
		}
		for (String allowedMethod : allowedMethods) {
			if (requestMethod.equalsIgnoreCase(allowedMethod)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkRequestHeaders(HttpServletRequest request, List<String> allowedHeaders) {
		String[] requestHeaders = CorsUtils.isPreFlightRequest(request) ?
				StringUtils.commaDelimitedListToStringArray(request.getHeader(CorsUtils.ACCESS_CONTROL_REQUEST_HEADERS)) :
				Collections.list(request.getHeaderNames()).toArray(new String [0]);

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

	private void setOriginHeader(HttpServletRequest request, HttpServletResponse response, List<String> allowedOrigins, Boolean allowCredentials) {
		String origin = request.getHeader(HttpHeaders.ORIGIN);
		if (allowedOrigins.contains("*") && (allowCredentials == null || !allowCredentials)) {
			response.addHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			return;
		}
		response.addHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
		response.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
	}

	private void setAllowCredentialsHeader(HttpServletResponse response, Boolean allowCredentials) {
		if ((allowCredentials != null) && allowCredentials) {
			response.addHeader(CorsUtils.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
		}
	}

	private void setAllowMethodsHeader(HttpServletRequest request, HttpServletResponse response, List<String> allowedMethods) {
		if (allowedMethods == null) {
			allowedMethods = Arrays.asList(HttpMethod.GET.name());
		}
		if (allowedMethods.contains("*")) {
			response.addHeader(CorsUtils.ACCESS_CONTROL_ALLOW_METHODS, request.getHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD));
		}
		else {
			response.addHeader(CorsUtils.ACCESS_CONTROL_ALLOW_METHODS, StringUtils.collectionToCommaDelimitedString(allowedMethods));
		}
	}

	private void setAllowHeadersHeader(HttpServletRequest request, HttpServletResponse response, List<String> allowedHeaders) {
		if ((allowedHeaders != null) && !allowedHeaders.isEmpty()) {
			String[] requestHeaders = StringUtils.commaDelimitedListToStringArray(request.getHeader(CorsUtils.ACCESS_CONTROL_REQUEST_HEADERS));
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
				response.addHeader(CorsUtils.ACCESS_CONTROL_ALLOW_HEADERS, StringUtils.collectionToCommaDelimitedString(matchingHeaders));
			}
		}
	}

	private void setExposeHeadersHeader(HttpServletResponse response, List<String> exposedHeaders) {
		if ((exposedHeaders != null) && !exposedHeaders.isEmpty()) {
			response.addHeader(CorsUtils.ACCESS_CONTROL_EXPOSE_HEADERS, StringUtils.collectionToCommaDelimitedString(exposedHeaders));
		}
	}

	private void setMaxAgeHeader(HttpServletResponse response, Long maxAge) {
		if (maxAge != null) {
			response.addHeader(CorsUtils.ACCESS_CONTROL_MAX_AGE, maxAge.toString());
		}
	}

}
