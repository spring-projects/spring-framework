/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.function.server.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.accept.ApiVersionStrategy;
import org.springframework.web.reactive.accept.DefaultApiVersionStrategy;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.handler.AbstractHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;

/**
 * {@code HandlerMapping} implementation that supports {@link RouterFunction RouterFunctions}.
 *
 * <p>If no {@link RouterFunction} is provided at
 * {@linkplain #RouterFunctionMapping(RouterFunction) construction time}, this mapping
 * will detect all router functions in the application context, and consult them in
 * {@linkplain org.springframework.core.annotation.Order order}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class RouterFunctionMapping extends AbstractHandlerMapping implements InitializingBean {

	private @Nullable RouterFunction<?> routerFunction;

	private List<HttpMessageReader<?>> messageReaders = Collections.emptyList();

	private @Nullable ApiVersionStrategy versionStrategy;


	/**
	 * Create an empty {@code RouterFunctionMapping}.
	 * <p>If this constructor is used, this mapping will detect all
	 * {@link RouterFunction} instances available in the application context.
	 */
	public RouterFunctionMapping() {
	}

	/**
	 * Create a {@code RouterFunctionMapping} with the given {@link RouterFunction}.
	 * <p>If this constructor is used, no application context detection will occur.
	 * @param routerFunction the router function to use for mapping
	 */
	public RouterFunctionMapping(RouterFunction<?> routerFunction) {
		this.routerFunction = routerFunction;
	}


	/**
	 * Return the configured {@link RouterFunction}.
	 * <p><strong>Note:</strong> When router functions are detected from the
	 * ApplicationContext, this method may return {@code null} if invoked
	 * prior to {@link #afterPropertiesSet()}.
	 * @return the router function or {@code null}
	 */
	public @Nullable RouterFunction<?> getRouterFunction() {
		return this.routerFunction;
	}

	/**
	 * Configure HTTP message readers to de-serialize the request body with.
	 * <p>By default this is set to the {@link ServerCodecConfigurer}'s defaults.
	 */
	public void setMessageReaders(List<HttpMessageReader<?>> messageReaders) {
		this.messageReaders = messageReaders;
	}

	/**
	 * Configure a strategy to manage API versioning.
	 * @param strategy the strategy to use
	 * @since 7.0
	 */
	public void setApiVersionStrategy(@Nullable ApiVersionStrategy strategy) {
		this.versionStrategy = strategy;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (CollectionUtils.isEmpty(this.messageReaders)) {
			ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();
			this.messageReaders = codecConfigurer.getReaders();
		}

		if (this.routerFunction == null) {
			initRouterFunctions();
		}

		if (this.routerFunction != null) {
			RouterFunctions.changeParser(this.routerFunction, getPathPatternParser());
			if (this.versionStrategy instanceof DefaultApiVersionStrategy davs) {
				if (davs.detectSupportedVersions()) {
					this.routerFunction.accept(new SupportedVersionVisitor(davs));
				}
			}
		}

	}

	/**
	 * Initialized the router functions by detecting them in the application context.
	 */
	protected void initRouterFunctions() {
		List<RouterFunction<?>> routerFunctions = routerFunctions();
		this.routerFunction = routerFunctions.stream().reduce(RouterFunction::andOther).orElse(null);
		logRouterFunctions(routerFunctions);
	}

	private List<RouterFunction<?>> routerFunctions() {
		return obtainApplicationContext()
				.getBeanProvider(RouterFunction.class)
				.orderedStream()
				.map(router -> (RouterFunction<?>) router)
				.collect(Collectors.toList());
	}

	private void logRouterFunctions(List<RouterFunction<?>> routerFunctions) {
		if (mappingsLogger.isDebugEnabled()) {
			routerFunctions.forEach(function -> mappingsLogger.debug("Mapped " + function));
		}
		else if (logger.isDebugEnabled()) {
			int total = routerFunctions.size();
			String message = total + " RouterFunction(s) in " + formatMappingName();
			if (logger.isTraceEnabled()) {
				if (total > 0) {
					routerFunctions.forEach(function -> logger.trace("Mapped " + function));
				}
				else {
					logger.trace(message);
				}
			}
			else if (total > 0) {
				logger.debug(message);
			}
		}
	}


	@Override
	protected Mono<?> getHandlerInternal(ServerWebExchange exchange) {

		if (this.routerFunction == null) {
			return Mono.empty();
		}

		if (this.versionStrategy != null) {
			Comparable<?> version = exchange.getAttribute(API_VERSION_ATTRIBUTE);
			if (version == null) {
				version = this.versionStrategy.resolveParseAndValidateVersion(exchange);
				if (version != null) {
					exchange.getAttributes().put(API_VERSION_ATTRIBUTE, version);
				}
			}
		}

		ServerRequest request = ServerRequest.create(
				exchange, this.messageReaders, this.versionStrategy);

		return this.routerFunction.route(request)
				.doOnNext(handler -> setAttributes(exchange.getAttributes(), request, handler));
	}

	@SuppressWarnings("unchecked")
	private void setAttributes(
			Map<String, Object> attributes, ServerRequest serverRequest, HandlerFunction<?> handlerFunction) {

		attributes.put(RouterFunctions.REQUEST_ATTRIBUTE, serverRequest);
		attributes.put(BEST_MATCHING_HANDLER_ATTRIBUTE, handlerFunction);

		PathPattern matchingPattern = (PathPattern) attributes.get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE);
		if (matchingPattern != null) {
			attributes.put(BEST_MATCHING_PATTERN_ATTRIBUTE, matchingPattern);
			ServerRequestObservationContext.findCurrent(serverRequest.exchange().getAttributes())
					.ifPresent(context -> context.setPathPattern(matchingPattern.toString()));
		}
		Map<String, String> uriVariables =
				(Map<String, String>) attributes.get(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		if (uriVariables != null) {
			attributes.put(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriVariables);
		}
	}

}
