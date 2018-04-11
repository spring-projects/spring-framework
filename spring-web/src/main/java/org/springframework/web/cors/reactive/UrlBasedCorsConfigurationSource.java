/*
 * Copyright 2002-2018 the original author or authors.
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
 * Provide a per reactive request {@link CorsConfiguration} instance based on a
 * collection of {@link CorsConfiguration} mapped on path patterns.
 *
 * <p>Exact path mapping URIs (such as {@code "/admin"}) are supported
 * as well as Ant-style path patterns (such as {@code "/admin/**"}).
 *
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 5.0
 */
public class UrlBasedCorsConfigurationSource implements CorsConfigurationSource {

	private final Map<PathPattern, CorsConfiguration> corsConfigurations;

	private final PathPatternParser patternParser;


	/**
	 * Construct a new {@code UrlBasedCorsConfigurationSource} instance with default
	 * {@code PathPatternParser}.
	 * @since 5.0.6
	 */
	public UrlBasedCorsConfigurationSource() {
		this(new PathPatternParser());
	}

	/**
	 * Construct a new {@code UrlBasedCorsConfigurationSource} instance from the supplied
	 * {@code PathPatternParser}.
	 */
	public UrlBasedCorsConfigurationSource(PathPatternParser patternParser) {
		this.corsConfigurations = new LinkedHashMap<>();
		this.patternParser = patternParser;
	}


	/**
	 * Set CORS configuration based on URL patterns.
	 */
	public void setCorsConfigurations(@Nullable Map<String, CorsConfiguration> corsConfigurations) {
		this.corsConfigurations.clear();
		if (corsConfigurations != null) {
			corsConfigurations.forEach(this::registerCorsConfiguration);
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
		PathContainer lookupPath = exchange.getRequest().getPath().pathWithinApplication();
		return this.corsConfigurations.entrySet().stream()
				.filter(entry -> entry.getKey().matches(lookupPath))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElse(null);
	}

}
