/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.handler;

import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsProcessor;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.cors.reactive.DefaultCorsProcessor;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Abstract base class for {@link org.springframework.web.reactive.HandlerMapping}
 * implementations.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractHandlerMapping extends ApplicationObjectSupport implements HandlerMapping, Ordered {

	private static final WebHandler REQUEST_HANDLED_HANDLER = exchange -> Mono.empty();


	private int order = Integer.MAX_VALUE;  // default: same as non-Ordered

	private final PathPatternParser patternParser;

	private final UrlBasedCorsConfigurationSource globalCorsConfigSource;

	private CorsProcessor corsProcessor = new DefaultCorsProcessor();


	public AbstractHandlerMapping() {
		  this.patternParser = new PathPatternParser();
		  this.globalCorsConfigSource = new UrlBasedCorsConfigurationSource(this.patternParser);
	}

	/**
	 * Specify the order value for this HandlerMapping bean.
	 * <p>Default value is {@code Integer.MAX_VALUE}, meaning that it's non-ordered.
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public final void setOrder(int order) {
		this.order = order;
	}

	@Override
	public final int getOrder() {
		return this.order;
	}

	/**
	 * Whether to match to URLs irrespective of their case.
	 * If enabled a method mapped to "/users" won't match to "/Users/".
	 * <p>The default value is {@code false}.
	 */
	public void setUseCaseSensitiveMatch(boolean caseSensitiveMatch) {
		this.patternParser.setCaseSensitive(caseSensitiveMatch);
	}

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 * If enabled a method mapped to "/users" also matches to "/users/".
	 * <p>The default value is {@code true}.
	 */
	public void setUseTrailingSlashMatch(boolean trailingSlashMatch) {
		this.patternParser.setMatchOptionalTrailingSeparator(trailingSlashMatch);
	}

	/**
	 * Return the {@link PathPatternParser} instance.
	 */
	public PathPatternParser getPathPatternParser() {
		return this.patternParser;
	}

	/**
	 * Set "global" CORS configuration based on URL patterns. By default the
	 * first matching URL pattern is combined with handler-level CORS
	 * configuration if any.
	 */
	public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
		this.globalCorsConfigSource.setCorsConfigurations(corsConfigurations);
	}

	/**
	 * Configure a custom {@link CorsProcessor} to use to apply the matched
	 * {@link CorsConfiguration} for a request.
	 * <p>By default an instance of {@link DefaultCorsProcessor} is used.
	 */
	public void setCorsProcessor(CorsProcessor corsProcessor) {
		Assert.notNull(corsProcessor, "CorsProcessor must not be null");
		this.corsProcessor = corsProcessor;
	}

	/**
	 * Return the configured {@link CorsProcessor}.
	 */
	public CorsProcessor getCorsProcessor() {
		return this.corsProcessor;
	}


	@Override
	public Mono<Object> getHandler(ServerWebExchange exchange) {
		return getHandlerInternal(exchange).map(handler -> {
			if (CorsUtils.isCorsRequest(exchange.getRequest())) {
				CorsConfiguration configA = this.globalCorsConfigSource.getCorsConfiguration(exchange);
				CorsConfiguration configB = getCorsConfiguration(handler, exchange);
				CorsConfiguration config = (configA != null ? configA.combine(configB) : configB);
				if (!getCorsProcessor().process(config, exchange) ||
						CorsUtils.isPreFlightRequest(exchange.getRequest())) {
					return REQUEST_HANDLED_HANDLER;
				}
			}
			return handler;
		});
	}

	/**
	 * Look up a handler for the given request, returning an empty {@code Mono}
	 * if no specific one is found. This method is called by {@link #getHandler}.
	 * <p>On CORS pre-flight requests this method should return a match not for
	 * the pre-flight request but for the expected actual request based on the URL
	 * path, the HTTP methods from the "Access-Control-Request-Method" header, and
	 * the headers from the "Access-Control-Request-Headers" header.
	 * @param exchange current exchange
	 * @return {@code Mono} for the matching handler, if any
	 */
	protected abstract Mono<?> getHandlerInternal(ServerWebExchange exchange);

	/**
	 * Retrieve the CORS configuration for the given handler.
	 * @param handler the handler to check (never {@code null})
	 * @param exchange the current exchange
	 * @return the CORS configuration for the handler, or {@code null} if none
	 */
	@Nullable
	protected CorsConfiguration getCorsConfiguration(Object handler, ServerWebExchange exchange) {
		if (handler instanceof CorsConfigurationSource) {
			return ((CorsConfigurationSource) handler).getCorsConfiguration(exchange);
		}
		return null;
	}

}
