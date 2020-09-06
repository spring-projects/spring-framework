/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.cors.reactive;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * {@code CorsConfigurationSource} that uses URL patterns to select the
 * {@code CorsConfiguration} for a request.
 *
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 5.0
 * @see PathPattern
 */
public class UrlBasedCorsConfigurationSource implements CorsConfigurationSource {

	private final PathPatternParser patternParser;

	private final Map<PathPattern, CorsConfiguration> corsConfigurations = new LinkedHashMap<>();


	/**
	 * Construct a new {@code UrlBasedCorsConfigurationSource} instance with default
	 * {@code PathPatternParser}.
	 * @since 5.0.6
	 */
	public UrlBasedCorsConfigurationSource() {
		this(PathPatternParser.defaultInstance);
	}

	/**
	 * Construct a new {@code UrlBasedCorsConfigurationSource} instance from the supplied
	 * {@code PathPatternParser}.
	 */
	public UrlBasedCorsConfigurationSource(PathPatternParser patternParser) {
		this.patternParser = patternParser;
	}


	/**
	 * Set CORS configuration based on URL patterns.
	 */
	public void setCorsConfigurations(@Nullable Map<String, CorsConfiguration> configMap) {
		this.corsConfigurations.clear();
		if (configMap != null) {
			configMap.forEach(this::registerCorsConfiguration);
		}
	}

	/**
	 * Register a {@link CorsConfiguration} for the specified path pattern.
	 */
	public void registerCorsConfiguration(String path, CorsConfiguration config) {
		this.corsConfigurations.put(this.patternParser.parse(path), config);
	}

	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(ServerWebExchange exchange) {
		PathContainer path = exchange.getRequest().getPath().pathWithinApplication();
		for (Map.Entry<PathPattern, CorsConfiguration> entry : this.corsConfigurations.entrySet()) {
			if (entry.getKey().matches(path)) {
				return entry.getValue();
			}
		}
		return null;
	}

}
