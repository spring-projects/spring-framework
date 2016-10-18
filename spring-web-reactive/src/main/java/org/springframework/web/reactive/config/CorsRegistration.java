/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.config;

import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.web.cors.CorsConfiguration;

/**
 * Assists with the creation of a {@link CorsConfiguration} instance mapped to
 * a path pattern.
 *
 * <p>By default, all origins, headers, credentials and {@code GET}, {@code HEAD}, and
 * {@code POST} methods are allowed, while the max age is set to 30 minutes.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see CorsRegistry
 */
public class CorsRegistration {

	private final String pathPattern;

	private final CorsConfiguration config;


	/**
	 * Create a new {@link CorsRegistration} instance with all origins, headers, credentials
	 * and {@code GET}, {@code HEAD}, and {@code POST} methods allowed, and the max age
	 * set to 30 minutes on the specified path.
	 * @param pathPattern the path where the CORS configuration should apply. Exact path
	 * mapping URIs (such as {@code "/admin"}) are supported as well as Ant-style path
	 * patterns (such as {@code "/admin/**"}).
	 */
	public CorsRegistration(String pathPattern) {
		this.pathPattern = pathPattern;
		// Same implicit default values as the @CrossOrigin annotation + allows simple methods
		this.config = new CorsConfiguration().applyDefaultPermitConfiguration();
	}


	/**
	 * Set the origins to allow, e.g. {@code "http://domain1.com"}.
	 * <p>The special value {@code "*"} allows all domains.
	 * <p>By default, all origins are allowed.
	 */
	public CorsRegistration allowedOrigins(String... origins) {
		this.config.setAllowedOrigins(new ArrayList<>(Arrays.asList(origins)));
		return this;
	}

	/**
	 * Set the HTTP methods to allow, e.g. {@code "GET"}, {@code "POST"},
	 * {@code "PUT"}, etc.
	 * <p>The special value {@code "*"} allows all methods.
	 * <p>By default, simple methods ({@code GET}, {@code HEAD}, and {@code POST}) are allowed.
	 */
	public CorsRegistration allowedMethods(String... methods) {
		this.config.setAllowedMethods(new ArrayList<>(Arrays.asList(methods)));
		return this;
	}

	/**
	 * Set the list of headers that a pre-flight request can list as allowed
	 * for use during an actual request.
	 * <p>The special value {@code "*"} allows actual requests to send any
	 * header.
	 * <p>A header name is not required to be listed if it is one of:
	 * {@code Cache-Control}, {@code Content-Language}, {@code Expires},
	 * {@code Last-Modified}, or {@code Pragma}.
	 * <p>By default, all headers are allowed.
	 */
	public CorsRegistration allowedHeaders(String... headers) {
		this.config.setAllowedHeaders(new ArrayList<>(Arrays.asList(headers)));
		return this;
	}

	/**
	 * Set the list of response headers other than simple headers (i.e.
	 * {@code Cache-Control}, {@code Content-Language}, {@code Content-Type},
	 * {@code Expires}, {@code Last-Modified}, or {@code Pragma}) that an
	 * actual response might have and can be exposed.
	 * <p>Note that {@code "*"} is not a valid exposed header value.
	 * <p>By default this is not set.
	 */
	public CorsRegistration exposedHeaders(String... headers) {
		this.config.setExposedHeaders(new ArrayList<>(Arrays.asList(headers)));
		return this;
	}

	/**
	 * Configure how long, in seconds, the response from a pre-flight request
	 * can be cached by clients.
	 * <p>By default, this is set to 1800 seconds (30 minutes).
	 */
	public CorsRegistration maxAge(long maxAge) {
		this.config.setMaxAge(maxAge);
		return this;
	}

	/**
	 * Whether user credentials are supported.
	 * <p>By default, this is set to {@code true} (i.e. user credentials are supported).
	 */
	public CorsRegistration allowCredentials(boolean allowCredentials) {
		this.config.setAllowCredentials(allowCredentials);
		return this;
	}

	protected String getPathPattern() {
		return this.pathPattern;
	}

	protected CorsConfiguration getCorsConfiguration() {
		return this.config;
	}

}
