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

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;


/**
 * {@link WebFilter} that handles CORS preflight requests and intercepts
 * CORS simple and actual requests thanks to a {@link CorsProcessor} implementation
 * ({@link DefaultCorsProcessor} by default) in order to add the relevant CORS
 * response headers (like {@code Access-Control-Allow-Origin}) using the provided
 * {@link CorsConfigurationSource} (for example an {@link UrlBasedCorsConfigurationSource}
 * instance.
 *
 * <p>This is an alternative to Spring WebFlux Java config CORS configuration,
 * mostly useful for applications using the functional API.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommendation</a>
 */
public class CorsWebFilter implements WebFilter {

	private final CorsConfigurationSource configSource;

	private final CorsProcessor processor;


	/**
	 * Constructor accepting a {@link CorsConfigurationSource} used by the filter
	 * to find the {@link CorsConfiguration} to use for each incoming request.
	 * @see UrlBasedCorsConfigurationSource
	 */
	public CorsWebFilter(CorsConfigurationSource configSource) {
		this(configSource, new DefaultCorsProcessor());
	}

	/**
	 * Constructor accepting a {@link CorsConfigurationSource} used by the filter
	 * to find the {@link CorsConfiguration} to use for each incoming request and a
	 * custom {@link CorsProcessor} to use to apply the matched
	 * {@link CorsConfiguration} for a request.
	 * @see UrlBasedCorsConfigurationSource
	 */
	public CorsWebFilter(CorsConfigurationSource configSource, CorsProcessor processor) {
		Assert.notNull(configSource, "CorsConfigurationSource must not be null");
		Assert.notNull(processor, "CorsProcessor must not be null");
		this.configSource = configSource;
		this.processor = processor;
	}


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		if (CorsUtils.isCorsRequest(request)) {
			CorsConfiguration corsConfiguration = this.configSource.getCorsConfiguration(exchange);
			if (corsConfiguration != null) {
				boolean isValid = this.processor.process(corsConfiguration, exchange);
				if (!isValid || CorsUtils.isPreFlightRequest(request)) {
					return Mono.empty();
				}
			}
		}
		return chain.filter(exchange);
	}

}
