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
 * Assists with the creation of a {@link CorsConfiguration} mapped to one or more path patterns.
 * If no path pattern is specified, cross-origin request handling is mapped on "/**" .
 *
 * <p>By default, all origins, all headers, credentials and GET, HEAD, POST methods are allowed.
 * Max age is set to 30 minutes.</p>
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public class CorsRegistration {

	private final String[] pathPatterns;

	private final CorsConfiguration config;

	public CorsRegistration(String... pathPatterns) {
		this.pathPatterns = (pathPatterns.length == 0 ? new String[]{ "/**" } : pathPatterns);
		// Same default values than @CrossOrigin annotation + allows simple methods
		this.config = new CorsConfiguration();
		this.config.addAllowedOrigin("*");
		this.config.addAllowedMethod(HttpMethod.GET.name());
		this.config.addAllowedMethod(HttpMethod.HEAD.name());
		this.config.addAllowedMethod(HttpMethod.POST.name());
		this.config.addAllowedHeader("*");
		this.config.setAllowCredentials(true);
		this.config.setMaxAge(1800L);
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

	protected String[] getPathPatterns() {
		return this.pathPatterns;
	}

	protected CorsConfiguration getCorsConfiguration() {
		return this.config;
	}

}
