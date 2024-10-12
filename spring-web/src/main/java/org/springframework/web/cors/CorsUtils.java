/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.cors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.InvalidUrlException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Utility class for CORS request handling based on the
 * <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>.
 *
 * @author Sebastien Deleuze
 * @author Igor Durbek
 * @since 4.2
 */
public abstract class CorsUtils {

	/**
	 * Returns {@code IsCorsRequestResult.IS_CORS_REQUEST} if the request is a valid CORS one by checking {@code Origin}
	 * header presence and ensuring that origins are different. Returns {@code IsCorsRequestResult.IS_NOT_CORS_REQUEST}
	 * otherwise, or {@code IsCorsRequestResult.MALFORMED_ORIGIN} if the origin url is malformed.
	 */
	public static IsCorsRequestResult isCorsRequest(HttpServletRequest request) {
		String origin = request.getHeader(HttpHeaders.ORIGIN);
		if (origin == null) {
			return IsCorsRequestResult.IS_NOT_CORS_REQUEST;
		}
		try {
			UriComponentsBuilder.fromUriString(origin);
		}
		catch (InvalidUrlException ex) {
			return IsCorsRequestResult.MALFORMED_ORIGIN;
		}
		UriComponents originUrl = UriComponentsBuilder.fromUriString(origin).build();
		String scheme = request.getScheme();
		String host = request.getServerName();
		int port = request.getServerPort();
		boolean isCorsRequest = !(ObjectUtils.nullSafeEquals(scheme, originUrl.getScheme()) &&
				ObjectUtils.nullSafeEquals(host, originUrl.getHost()) &&
				getPort(scheme, port) == getPort(originUrl.getScheme(), originUrl.getPort()));
		return isCorsRequest ? IsCorsRequestResult.IS_CORS_REQUEST : IsCorsRequestResult.IS_NOT_CORS_REQUEST;
	}

	private static int getPort(@Nullable String scheme, int port) {
		if (port == -1) {
			if ("http".equals(scheme) || "ws".equals(scheme)) {
				port = 80;
			}
			else if ("https".equals(scheme) || "wss".equals(scheme)) {
				port = 443;
			}
		}
		return port;
	}

	/**
	 * Returns {@code true} if the request is a valid CORS pre-flight one by checking {@code OPTIONS} method with
	 * {@code Origin} and {@code Access-Control-Request-Method} headers presence.
	 */
	public static boolean isPreFlightRequest(HttpServletRequest request) {
		return (HttpMethod.OPTIONS.matches(request.getMethod()) &&
				request.getHeader(HttpHeaders.ORIGIN) != null &&
				request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD) != null);
	}

}
