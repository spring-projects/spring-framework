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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
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
	public boolean processPreFlightRequest(CorsConfiguration config, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		Assert.isTrue(CorsUtils.isPreFlightRequest(request));

		if (check(request, response, config)) {
			setAllowOrigin(request, response, config.getAllowedOrigins(), config.isAllowCredentials());
			setAllowCredentials(response, config.isAllowCredentials());
			setAllowMethods(request, response, config.getAllowedMethods());
			setAllowHeadersHeader(request, response, config.getAllowedHeaders());
			setMaxAgeHeader(response, config.getMaxAge());
		}
		return true;
	}

	@Override
	public boolean processActualRequest(CorsConfiguration config, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		Assert.isTrue(CorsUtils.isCorsRequest(request) && !CorsUtils.isPreFlightRequest(request));

		if (check(request, response, config)) {
			setAllowOrigin(request, response, config.getAllowedOrigins(), config.isAllowCredentials());
			setAllowCredentials(response, config.isAllowCredentials());
			setExposeHeadersHeader(response, config.getExposedHeaders());
		}
		return true;
	}

	private boolean check(HttpServletRequest request, HttpServletResponse response,
			CorsConfiguration config) throws IOException {

		if (hasAllowOriginHeader(response)) {
			logger.debug("Skip adding CORS headers, response already contains \"Access-Control-Allow-Origin\"");
			return false;
		}
		if (!checkOrigin(request, config.getAllowedOrigins()) || !checkMethod(request, config.getAllowedMethods()) ||
				!checkHeaders(request, config.getAllowedHeaders())) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CORS request");
			return false;
		}
		return true;
	}

	private boolean hasAllowOriginHeader(HttpServletResponse response) {
		boolean hasCorsResponseHeaders = false;
		try {
			// Perhaps a CORS Filter has already added this?
			Collection<String> headers = response.getHeaders(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN);
			hasCorsResponseHeaders = !CollectionUtils.isEmpty(headers);
		}
		catch (NullPointerException npe) {
			// See SPR-11919 and https://issues.jboss.org/browse/WFLY-3474
		}
		return hasCorsResponseHeaders;
	}

	private boolean checkOrigin(HttpServletRequest request, List<String> allowedOrigins) {
		String originHeader = request.getHeader(HttpHeaders.ORIGIN);
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

	private boolean checkMethod(HttpServletRequest request, List<String> allowedMethods) {
		String requestMethod = CorsUtils.isPreFlightRequest(request) ?
				request.getHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD) : request.getMethod();
		if (allowedMethods == null) {
			allowedMethods = Arrays.asList(HttpMethod.GET.name());
		}
		if (allowedMethods.contains("*")) {
			return true;
		}
		for (String allowedMethod : allowedMethods) {
			if (allowedMethod.equalsIgnoreCase(requestMethod)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkHeaders(HttpServletRequest request, List<String> allowedHeaders) {
		String headerValue = request.getHeader(CorsUtils.ACCESS_CONTROL_REQUEST_HEADERS);
		String[] requestHeaders = CorsUtils.isPreFlightRequest(request) ?
				StringUtils.commaDelimitedListToStringArray(headerValue) :
				Collections.list(request.getHeaderNames()).toArray(new String[0]);
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

	private void setAllowOrigin(HttpServletRequest request, HttpServletResponse response,
			List<String> allowedOrigins, Boolean allowCredentials) {

		String origin = request.getHeader(HttpHeaders.ORIGIN);
		if (allowedOrigins.contains("*") && (allowCredentials == null || !allowCredentials)) {
			response.addHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			return;
		}
		response.addHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
		response.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
	}

	private void setAllowMethods(HttpServletRequest request, HttpServletResponse response,
			List<String> allowedMethods) {

		if (allowedMethods == null) {
			allowedMethods = Arrays.asList(HttpMethod.GET.name());
		}
		if (allowedMethods.contains("*")) {
			String headerValue = request.getHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD);
			response.addHeader(CorsUtils.ACCESS_CONTROL_ALLOW_METHODS, headerValue);
		}
		else {
			String headerValue = StringUtils.collectionToCommaDelimitedString(allowedMethods);
			response.addHeader(CorsUtils.ACCESS_CONTROL_ALLOW_METHODS, headerValue);
		}
	}

	private void setAllowHeadersHeader(HttpServletRequest request, HttpServletResponse response, List<String> allowedHeaders) {
		if ((allowedHeaders != null) && !allowedHeaders.isEmpty()) {
			String headerValue = request.getHeader(CorsUtils.ACCESS_CONTROL_REQUEST_HEADERS);
			String[] requestHeaders = StringUtils.commaDelimitedListToStringArray(headerValue);
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
				response.addHeader(CorsUtils.ACCESS_CONTROL_ALLOW_HEADERS,
						StringUtils.collectionToCommaDelimitedString(matchingHeaders));
			}
		}
	}

	private void setExposeHeadersHeader(HttpServletResponse response, List<String> exposedHeaders) {
		if ((exposedHeaders != null) && !exposedHeaders.isEmpty()) {
			response.addHeader(CorsUtils.ACCESS_CONTROL_EXPOSE_HEADERS,
					StringUtils.collectionToCommaDelimitedString(exposedHeaders));
		}
	}

	private void setAllowCredentials(HttpServletResponse response, Boolean allowCredentials) {
		if ((allowCredentials != null) && allowCredentials) {
			response.addHeader(CorsUtils.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
		}
	}

	private void setMaxAgeHeader(HttpServletResponse response, Long maxAge) {
		if (maxAge != null) {
			response.addHeader(CorsUtils.ACCESS_CONTROL_MAX_AGE, maxAge.toString());
		}
	}

}
