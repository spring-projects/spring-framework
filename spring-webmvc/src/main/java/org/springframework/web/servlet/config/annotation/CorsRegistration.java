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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;

/**
 * {@code CorsRegistration} assists with the creation of a
 * {@link CorsConfiguration} instance mapped to a path pattern.
 *
 * <p>If no path pattern is specified, cross-origin request handling is
 * mapped to {@code "/**"}.
 *
 * <p>By default, all origins, all headers, credentials and {@code GET},
 * {@code HEAD}, and {@code POST} methods are allowed, and the max age is
 * set to 30 minutes.
 *
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 4.2
 * @see CorsConfiguration
 * @see CorsRegistry
 */
public class CorsRegistration {

	private final String pathPattern;

	private final CorsConfiguration config;

	public CorsRegistration(String pathPattern) {
		this.pathPattern = pathPattern;
		// Same implicit default values as the @CrossOrigin annotation + allows simple methods
		this.config = new CorsConfiguration();
		this.config.addAllowedOrigin(CorsConfiguration.ALL);
		this.config.addAllowedMethod(HttpMethod.GET);
		this.config.addAllowedMethod(HttpMethod.HEAD);
		this.config.addAllowedMethod(HttpMethod.POST);
		this.config.addAllowedHeader(CorsConfiguration.ALL);
		this.config.setAllowCredentials(Boolean.TRUE);
		this.config.setMaxAge(CorsConfiguration.DEFAULT_MAX_AGE);
	}

	public CorsRegistration allowedOrigins(String... origins) {
		this.config.setAllowedOrigins(new ArrayList<String>(Arrays.asList(origins)));
		return this;
	}

	public CorsRegistration allowedMethods(String... methods) {
		this.config.setAllowedMethods(new ArrayList<String>(Arrays.asList(methods)));
		return this;
	}

	public CorsRegistration allowedHeaders(String... headers) {
		this.config.setAllowedHeaders(new ArrayList<String>(Arrays.asList(headers)));
		return this;
	}

	public CorsRegistration exposedHeaders(String... headers) {
		this.config.setExposedHeaders(new ArrayList<String>(Arrays.asList(headers)));
		return this;
	}

	public CorsRegistration maxAge(long maxAge) {
		this.config.setMaxAge(maxAge);
		return this;
	}

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
