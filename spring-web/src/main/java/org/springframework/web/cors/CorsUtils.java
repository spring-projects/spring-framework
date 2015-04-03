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

import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;

/**
 * Utility class for CORS request handling based on the
 * <a href="http://www.w3.org/TR/cors/">CORS W3C recommandation</a>.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public class CorsUtils {

	/**
	 * The CORS {@code Access-Control-Request-Headers} request header field name.
	 * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommandation</a>
	 */
	public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
	/**
	 * The CORS {@code Access-Control-Request-Method} request header field name.
	 * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommandation</a>
	 */
	public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
	/**
	 * The CORS {@code Access-Control-Allow-Origin} response header field name.
	 * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommandation</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	/**
	 * The CORS {@code Access-Control-Allow-Headers} response header field name.
	 * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommandation</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	/**
	 * The CORS {@code Access-Control-Allow-Methods} response header field name.
	 * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommandation</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	/**
	 * The CORS {@code Access-Control-Max-Age} response header field name.
	 * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommandation</a>
	 */
	public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
	/**
	 * The CORS {@code Access-Control-Allow-Credentials} response header field name.
	 * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommandation</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	/**
	 * The CORS {@code Access-Control-Expose-Headers} response header field name.
	 * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommandation</a>
	 */
	public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";


	/**
	 * Returns {@code true} if the request is a valid CORS one.
	 */
	public static boolean isCorsRequest(HttpServletRequest request) {
		return request.getHeader(HttpHeaders.ORIGIN) != null;
	}

	/**
	 * Returns {@code true} if the request is a valid CORS pre-flight one.
	 */
	public static boolean isPreFlightRequest(HttpServletRequest request) {
		if (!isCorsRequest(request)) {
			return false;
		}
		return request.getMethod().equals(HttpMethod.OPTIONS.name());
	}

}
