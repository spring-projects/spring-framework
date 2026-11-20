/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.cors.reactive;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.http.server.PathContainer;
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
	public @Nullable CorsConfiguration getCorsConfiguration(ServerWebExchange exchange) {
		PathContainer path = exchange.getRequest().getPath().pathWithinApplication();
		for (Map.Entry<PathPattern, CorsConfiguration> entry : this.corsConfigurations.entrySet()) {
			if (entry.getKey().matches(path)) {
				return entry.getValue();
			}
		}
		return null;
	}

}
