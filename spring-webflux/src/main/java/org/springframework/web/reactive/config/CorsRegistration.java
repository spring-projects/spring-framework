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

package org.springframework.web.reactive.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.cors.CorsConfiguration;

/**
 * Assists with the creation of a {@link CorsConfiguration} instance for a given
 * URL path pattern.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see CorsConfiguration
 * @see CorsRegistry
 */
public class CorsRegistration {

	private final String pathPattern;

	private CorsConfiguration config;


	public CorsRegistration(String pathPattern) {
		this.pathPattern = pathPattern;
		// Same implicit default values as the @CrossOrigin annotation + allows simple methods
		this.config = new CorsConfiguration().applyPermitDefaultValues();
	}


	/**
	 * Set the origins for which cross-origin requests are allowed from a browser.
	 * Please, refer to {@link CorsConfiguration#setAllowedOrigins(List)} for
	 * format details and other considerations.
	 *
	 * <p>By default, all origins are allowed, but if
	 * {@link #allowedOriginPatterns(String...) allowedOriginPatterns} is also
	 * set, then that takes precedence.
	 * @see #allowedOriginPatterns(String...)
	 */
	public CorsRegistration allowedOrigins(String... origins) {
		this.config.setAllowedOrigins(Arrays.asList(origins));
		return this;
	}

	/**
	 * Alternative to {@link #allowedOrigins(String...)} that supports more
	 * flexible patterns for specifying the origins for which cross-origin
	 * requests are allowed from a browser. Please, refer to
	 * {@link CorsConfiguration#setAllowedOriginPatterns(List)} for format
	 * details and other considerations.
	 * <p>By default this is not set.
	 * @since 5.3
	 */
	public CorsRegistration allowedOriginPatterns(String... patterns) {
		this.config.setAllowedOriginPatterns(Arrays.asList(patterns));
		return this;
	}

	/**
	 * Set the HTTP methods to allow, e.g. {@code "GET"}, {@code "POST"}, etc.
	 * The special value {@code "*"} allows all methods. By default,
	 * "simple" methods {@code GET}, {@code HEAD}, and {@code POST}
	 * are allowed.
	 * <p>Please, see {@link CorsConfiguration#setAllowedMethods(List)} for
	 * details.
	 */
	public CorsRegistration allowedMethods(String... methods) {
		this.config.setAllowedMethods(Arrays.asList(methods));
		return this;
	}

	/**
	 * Set the list of headers that a pre-flight request can list as allowed
	 * for use during an actual request. The special value {@code "*"}
	 * may be used to allow all headers.
	 * <p>Please, see {@link CorsConfiguration#setAllowedHeaders(List)} for
	 * details.
	 * <p>By default all headers are allowed.
	 */
	public CorsRegistration allowedHeaders(String... headers) {
		this.config.setAllowedHeaders(Arrays.asList(headers));
		return this;
	}

	/**
	 * Set the list of response headers that an actual response might have and
	 * can be exposed. The special value {@code "*"} allows all headers to be
	 * exposed.
	 * <p>Please, see {@link CorsConfiguration#setExposedHeaders(List)} for
	 * details.
	 * <p>By default this is not set.
	 */
	public CorsRegistration exposedHeaders(String... headers) {
		this.config.setExposedHeaders(Arrays.asList(headers));
		return this;
	}

	/**
	 * Whether the browser should send credentials, such as cookies along with
	 * cross domain requests, to the annotated endpoint. The configured value is
	 * set on the {@code Access-Control-Allow-Credentials} response header of
	 * preflight requests.
	 * <p><strong>NOTE:</strong> Be aware that this option establishes a high
	 * level of trust with the configured domains and also increases the surface
	 * attack of the web application by exposing sensitive user-specific
	 * information such as cookies and CSRF tokens.
	 * <p>By default this is not set in which case the
	 * {@code Access-Control-Allow-Credentials} header is also not set and
	 * credentials are therefore not allowed.
	 */
	public CorsRegistration allowCredentials(boolean allowCredentials) {
		this.config.setAllowCredentials(allowCredentials);
		return this;
	}

	/**
	 * Whether private network access is supported.
	 * <p>Please, see {@link CorsConfiguration#setAllowPrivateNetwork(Boolean)} for details.
	 * <p>By default this is not set (i.e. private network access is not supported).
	 * @since 5.3.32
	 */
	public CorsRegistration allowPrivateNetwork(boolean allowPrivateNetwork) {
		this.config.setAllowPrivateNetwork(allowPrivateNetwork);
		return this;
	}

	/**
	 * Configure how long in seconds the response from a pre-flight request
	 * can be cached by clients.
	 * <p>By default this is set to 1800 seconds (30 minutes).
	 */
	public CorsRegistration maxAge(long maxAge) {
		this.config.setMaxAge(maxAge);
		return this;
	}

	/**
	 * Apply the given {@code CorsConfiguration} to the one being configured via
	 * {@link CorsConfiguration#combine(CorsConfiguration)} which in turn has been
	 * initialized with {@link CorsConfiguration#applyPermitDefaultValues()}.
	 * @param other the configuration to apply
	 * @since 5.3
	 */
	public CorsRegistration combine(CorsConfiguration other) {
		this.config = this.config.combine(other);
		return this;
	}

	protected String getPathPattern() {
		return this.pathPattern;
	}

	protected CorsConfiguration getCorsConfiguration() {
		return this.config;
	}

}
