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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;

import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.patterns.PathPattern;
import org.springframework.web.util.patterns.PathPatternParser;

/**
 * Abstract base class for URL-mapped
 * {@link org.springframework.web.reactive.HandlerMapping} implementations.
 *
 * <p>Supports direct matches, e.g. a registered "/test" matches "/test", and
 * various path pattern matches, e.g. a registered "/t*" pattern matches
 * both "/test" and "/team", "/test/*" matches all paths under "/test",
 * "/test/**" matches all paths below "/test". For details, see the
 * {@link PathPatternParser} javadoc.
 *
 * <p>Will search all path patterns to find the most specific match for the
 * current request path. The most specific pattern is defined as the longest
 * path pattern with the fewest captured variables and wildcards.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
public abstract class AbstractUrlHandlerMapping extends AbstractHandlerMapping {

	private boolean lazyInitHandlers = false;

	private final Map<PathPattern, Object> handlerMap = new LinkedHashMap<>();

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 * If enabled a URL pattern such as "/users" also matches to "/users/".
	 * <p>The default value is {@code false}.
	 */
	public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
		this.patternRegistry.setUseTrailingSlashMatch(useTrailingSlashMatch);
	}

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 */
	public boolean useTrailingSlashMatch() {
		return this.patternRegistry.useTrailingSlashMatch();
	}

	/**
	 * Set whether to lazily initialize handlers. Only applicable to
	 * singleton handlers, as prototypes are always lazily initialized.
	 * Default is "false", as eager initialization allows for more efficiency
	 * through referencing the controller objects directly.
	 * <p>If you want to allow your controllers to be lazily initialized,
	 * make them "lazy-init" and set this flag to true. Just making them
	 * "lazy-init" will not work, as they are initialized through the
	 * references from the handler mapping in this case.
	 */
	public void setLazyInitHandlers(boolean lazyInitHandlers) {
		this.lazyInitHandlers = lazyInitHandlers;
	}

	/**
	 * Return the registered handlers as an unmodifiable Map, with the registered path
	 * as key and the handler object (or handler bean name in case of a lazy-init handler)
	 * as value.
	 */
	public final Map<PathPattern, Object> getHandlerMap() {
		return Collections.unmodifiableMap(this.handlerMap);
	}


	@Override
	public Mono<Object> getHandlerInternal(ServerWebExchange exchange) {
		String lookupPath = getPathHelper().getLookupPathForRequest(exchange);
		Object handler;
		try {
			handler = lookupHandler(lookupPath, exchange);
		}
		catch (Exception ex) {
			return Mono.error(ex);
		}

		if (handler != null && logger.isDebugEnabled()) {
			logger.debug("Mapping [" + lookupPath + "] to " + handler);
		}
		else if (handler == null && logger.isTraceEnabled()) {
			logger.trace("No handler mapping found for [" + lookupPath + "]");
		}

		return Mono.justOrEmpty(handler);
	}

	/**
	 * Look up a handler instance for the given URL path.
	 *
	 * <p>Supports direct matches, e.g. a registered "/test" matches "/test",
	 * and various Ant-style pattern matches, e.g. a registered "/t*" matches
	 * both "/test" and "/team". For details, see the PathPattern class.
	 *
	 * <p>Looks for the most exact pattern, where most exact is defined as
	 * the longest path pattern.
	 *
	 * @param urlPath URL the bean is mapped to
	 * @param exchange the current exchange
	 * @return the associated handler instance, or {@code null} if not found
	 * @see org.springframework.util.AntPathMatcher
	 */
	protected Object lookupHandler(String urlPath, ServerWebExchange exchange) throws Exception {
		SortedSet<PathPattern> matches = this.patternRegistry.findMatches(urlPath);
		if (!matches.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Matching patterns for request [" + urlPath + "] are " + matches);
			}
			PathPattern bestMatch = matches.first();
			String pathWithinMapping = bestMatch.extractPathWithinPattern(urlPath);
			return handleMatch(this.handlerMap.get(bestMatch), bestMatch, pathWithinMapping, exchange);
		}

		// No handler found...
		return null;
	}

	private Object handleMatch(Object handler, PathPattern bestMatch, String pathWithinMapping,
			ServerWebExchange exchange) throws Exception {

		// Bean name or resolved handler?
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = getApplicationContext().getBean(handlerName);
		}

		validateHandler(handler, exchange);

		exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathWithinMapping);
		exchange.getAttributes().put(BEST_MATCHING_PATTERN_ATTRIBUTE, bestMatch);

		return handler;
	}

	/**
	 * Validate the given handler against the current request.
	 * <p>The default implementation is empty. Can be overridden in subclasses,
	 * for example to enforce specific preconditions expressed in URL mappings.
	 * @param handler the handler object to validate
	 * @param exchange current exchange
	 * @throws Exception if validation failed
	 */
	@SuppressWarnings("UnusedParameters")
	protected void validateHandler(Object handler, ServerWebExchange exchange) throws Exception {
	}

	/**
	 * Register the specified handler for the given URL paths.
	 * @param urlPaths the URLs that the bean should be mapped to
	 * @param beanName the name of the handler bean
	 * @throws BeansException if the handler couldn't be registered
	 * @throws IllegalStateException if there is a conflicting handler registered
	 */
	protected void registerHandler(String[] urlPaths, String beanName) throws BeansException, IllegalStateException {
		Assert.notNull(urlPaths, "URL path array must not be null");
		for (String urlPath : urlPaths) {
			registerHandler(urlPath, beanName);
		}
	}

	/**
	 * Register the specified handler for the given URL path.
	 * @param urlPath the URL the bean should be mapped to
	 * @param handler the handler instance or handler bean name String
	 * (a bean name will automatically be resolved into the corresponding handler bean)
	 * @throws BeansException if the handler couldn't be registered
	 * @throws IllegalStateException if there is a conflicting handler registered
	 */
	protected void registerHandler(String urlPath, Object handler) throws BeansException, IllegalStateException {
		Assert.notNull(urlPath, "URL path must not be null");
		Assert.notNull(handler, "Handler object must not be null");
		Object resolvedHandler = handler;

		// Eagerly resolve handler if referencing singleton via name.
		if (!this.lazyInitHandlers && handler instanceof String) {
			String handlerName = (String) handler;
			if (getApplicationContext().isSingleton(handlerName)) {
				resolvedHandler = getApplicationContext().getBean(handlerName);
			}
		}
		for (PathPattern newPattern : this.patternRegistry.register(urlPath)) {
			Object mappedHandler = this.handlerMap.get(newPattern);
			if (mappedHandler != null) {
				if (mappedHandler != resolvedHandler) {
					throw new IllegalStateException(
							"Cannot map " + getHandlerDescription(handler) + " to URL path [" + urlPath +
									"]: There is already " + getHandlerDescription(mappedHandler) + " mapped.");
				}
			}
			else {
				this.handlerMap.put(newPattern, resolvedHandler);

			}
		}
		if (logger.isInfoEnabled()) {
			logger.info("Mapped URL path [" + urlPath + "] onto " + getHandlerDescription(handler));
		}
	}

	private String getHandlerDescription(Object handler) {
		return "handler " + (handler instanceof String ? "'" + handler + "'" : "of type [" + handler.getClass() + "]");
	}

}
