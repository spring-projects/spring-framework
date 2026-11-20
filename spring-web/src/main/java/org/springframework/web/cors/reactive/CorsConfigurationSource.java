/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.cors.reactive;

import org.jspecify.annotations.Nullable;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;

/**
 * Interface to be implemented by classes (usually HTTP request handlers) that
 * provides a {@link CorsConfiguration} instance based on the provided reactive request.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface CorsConfigurationSource {

	/**
	 * Return a {@link CorsConfiguration} based on the incoming request.
	 * @return the associated {@link CorsConfiguration}, or {@code null} if none
	 */
	@Nullable CorsConfiguration getCorsConfiguration(ServerWebExchange exchange);

}
