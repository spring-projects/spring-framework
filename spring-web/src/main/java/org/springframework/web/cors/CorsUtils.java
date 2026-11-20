/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.cors;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Utility class for CORS request handling based on the
 * <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public abstract class CorsUtils {

	/**
	 * Returns {@code true} if the request is a valid CORS one by checking {@code Origin}
	 * header presence and ensuring that origins are different.
	 */
	public static boolean isCorsRequest(HttpServletRequest request) {
		String origin = request.getHeader(HttpHeaders.ORIGIN);
		if (origin == null) {
			return false;
		}
		UriComponents originUrl = UriComponentsBuilder.fromUriString(origin).build();
		String scheme = request.getScheme();
		String host = request.getServerName();
		int port = request.getServerPort();
		return !(ObjectUtils.nullSafeEquals(scheme, originUrl.getScheme()) &&
				ObjectUtils.nullSafeEquals(host, originUrl.getHost()) &&
				getPort(scheme, port) == getPort(originUrl.getScheme(), originUrl.getPort()));

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
