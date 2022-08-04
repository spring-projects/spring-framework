/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.reactive.handler;

import java.util.Map;

import org.apache.commons.logging.Log;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.Ordered;
import org.springframework.core.log.LogDelegateFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
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
public abstract class AbstractHandlerMapping extends ApplicationObjectSupport
		implements HandlerMapping, Ordered, BeanNameAware {

	private static final WebHandler NO_OP_HANDLER = exchange -> Mono.empty();

	/** Dedicated "hidden" logger for request mappings. */
	protected final Log mappingsLogger =
			LogDelegateFactory.getHiddenLog(HandlerMapping.class.getName() + ".Mappings");


	private final PathPatternParser patternParser;

	@Nullable
	private CorsConfigurationSource corsConfigurationSource;

	private CorsProcessor corsProcessor = new DefaultCorsProcessor();

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

	@Nullable
	private String beanName;


	public AbstractHandlerMapping() {
		this.patternParser = new PathPatternParser();
	}


	/**
	 * Shortcut method for setting the same property on the underlying pattern
	 * parser in use. For more details see:
	 * <ul>
	 * <li>{@link #getPathPatternParser()} -- the underlying pattern parser
	 * <li>{@link PathPatternParser#setCaseSensitive(boolean)} -- the case
	 * sensitive slash option, including its default value.
	 * </ul>
	 * <p><strong>Note:</strong> aside from
	 */
	public void setUseCaseSensitiveMatch(boolean caseSensitiveMatch) {
		this.patternParser.setCaseSensitive(caseSensitiveMatch);
	}

	/**
	 * Shortcut method for setting the same property on the underlying pattern
	 * parser in use. For more details see:
	 * <ul>
	 * <li>{@link #getPathPatternParser()} -- the underlying pattern parser
	 * <li>{@link PathPatternParser#setMatchOptionalTrailingSeparator(boolean)} --
	 * the trailing slash option, including its default value.
	 * </ul>
	 */
	public void setUseTrailingSlashMatch(boolean trailingSlashMatch) {
		this.patternParser.setMatchOptionalTrailingSeparator(trailingSlashMatch);
	}

	/**
	 * Return the {@link PathPatternParser} instance that is used for
	 * {@link #setCorsConfigurations(Map) CORS configuration checks}.
	 * Subclasses can also use this pattern parser for their own request
	 * mapping purposes.
	 */
	public PathPatternParser getPathPatternParser() {
		return this.patternParser;
	}

	/**
	 * Set the "global" CORS configurations based on URL patterns. By default, the
	 * first matching URL pattern is combined with handler-level CORS configuration if any.
	 * @see #setCorsConfigurationSource(CorsConfigurationSource)
	 */
	public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
		Assert.notNull(corsConfigurations, "corsConfigurations must not be null");
		if (!corsConfigurations.isEmpty()) {
			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(this.patternParser);
			source.setCorsConfigurations(corsConfigurations);
			this.corsConfigurationSource = source;
		}
		else {
			this.corsConfigurationSource = null;
		}
	}

	/**
	 * Set the "global" CORS configuration source. By default, the first matching URL
	 * pattern is combined with the CORS configuration for the handler, if any.
	 * @since 5.1
	 * @see #setCorsConfigurations(Map)
	 */
	public void setCorsConfigurationSource(CorsConfigurationSource corsConfigurationSource) {
		Assert.notNull(corsConfigurationSource, "corsConfigurationSource must not be null");
		this.corsConfigurationSource = corsConfigurationSource;
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

	/**
	 * Specify the order value for this HandlerMapping bean.
	 * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	protected String formatMappingName() {
		return this.beanName != null ? "'" + this.beanName + "'" : "<unknown>";
	}


	@Override
	public Mono<Object> getHandler(ServerWebExchange exchange) {
		return getHandlerInternal(exchange).map(handler -> {
			if (logger.isDebugEnabled()) {
				logger.debug(exchange.getLogPrefix() + "Mapped to " + handler);
			}
			ServerHttpRequest request = exchange.getRequest();
			if (hasCorsConfigurationSource(handler) || CorsUtils.isPreFlightRequest(request)) {
				CorsConfiguration config = (this.corsConfigurationSource != null ?
						this.corsConfigurationSource.getCorsConfiguration(exchange) : null);
				CorsConfiguration handlerConfig = getCorsConfiguration(handler, exchange);
				config = (config != null ? config.combine(handlerConfig) : handlerConfig);
				if (config != null) {
					config.validateAllowCredentials();
				}
				if (!this.corsProcessor.process(config, exchange) || CorsUtils.isPreFlightRequest(request)) {
					return NO_OP_HANDLER;
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
	 * Return {@code true} if there is a {@link CorsConfigurationSource} for this handler.
	 * @since 5.2
	 */
	protected boolean hasCorsConfigurationSource(Object handler) {
		return (handler instanceof CorsConfigurationSource || this.corsConfigurationSource != null);
	}

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
