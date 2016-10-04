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
package org.springframework.web.reactive.handler;

import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsProcessor;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.cors.reactive.DefaultCorsProcessor;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.util.HttpRequestPathHelper;

/**
 * Abstract base class for {@link org.springframework.web.reactive.HandlerMapping}
 * implementations.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractHandlerMapping extends ApplicationObjectSupport
		implements HandlerMapping, Ordered {

	private int order = Integer.MAX_VALUE;  // default: same as non-Ordered

	private HttpRequestPathHelper pathHelper = new HttpRequestPathHelper();

	private PathMatcher pathMatcher = new AntPathMatcher();

	protected CorsProcessor corsProcessor = new DefaultCorsProcessor();

	protected final UrlBasedCorsConfigurationSource corsConfigSource = new UrlBasedCorsConfigurationSource();

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
	 * Set if the path should be URL-decoded. This sets the same property on the
	 * underlying path helper.
	 * @see HttpRequestPathHelper#setUrlDecode(boolean)
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.pathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * Set the {@link HttpRequestPathHelper} to use for resolution of lookup
	 * paths. Use this to override the default implementation with a custom
	 * subclass or to share common path helper settings across multiple
	 * HandlerMappings.
	 */
	public void setPathHelper(HttpRequestPathHelper pathHelper) {
		this.pathHelper = pathHelper;
	}

	/**
	 * Return the {@link HttpRequestPathHelper} implementation to use for
	 * resolution of lookup paths.
	 */
	public HttpRequestPathHelper getPathHelper() {
		return this.pathHelper;
	}

	/**
	 * Set the PathMatcher implementation to use for matching URL paths
	 * against registered URL patterns. Default is AntPathMatcher.
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
		this.corsConfigSource.setPathMatcher(pathMatcher);
	}

	/**
	 * Return the PathMatcher implementation to use for matching URL paths
	 * against registered URL patterns.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * Configure a custom {@link CorsProcessor} to use to apply the matched
	 * {@link CorsConfiguration} for a request. By default {@link DefaultCorsProcessor} is used.
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

	/**
	 * Set "global" CORS configuration based on URL patterns. By default the first
	 * matching URL pattern is combined with the CORS configuration for the
	 * handler, if any.
	 */
	public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
		this.corsConfigSource.setCorsConfigurations(corsConfigurations);
	}

	/**
	 * Get the CORS configuration.
	 */
	public Map<String, CorsConfiguration> getCorsConfigurations() {
		return this.corsConfigSource.getCorsConfigurations();
	}

	protected CorsConfiguration getCorsConfiguration(Object handler, ServerWebExchange exchange) {
		if (handler != null && handler instanceof CorsConfigurationSource) {
			return ((CorsConfigurationSource) handler).getCorsConfiguration(exchange);
		}
		return null;
	}

	protected Object processCorsRequest(ServerWebExchange exchange, Object handler) {
		if (CorsUtils.isCorsRequest(exchange.getRequest())) {
			CorsConfiguration globalConfig = this.corsConfigSource.getCorsConfiguration(exchange);
			CorsConfiguration handlerConfig = getCorsConfiguration(handler, exchange);
			CorsConfiguration config = (globalConfig != null ? globalConfig.combine(handlerConfig) : handlerConfig);
			if (!corsProcessor.processRequest(config, exchange) || CorsUtils.isPreFlightRequest(exchange.getRequest())) {
				return new NoOpHandler();
			}
		}
		return handler;
	}

	private class NoOpHandler implements WebHandler {
		@Override
		public Mono<Void> handle(ServerWebExchange exchange) {
			return Mono.empty();
		}
	}

}
