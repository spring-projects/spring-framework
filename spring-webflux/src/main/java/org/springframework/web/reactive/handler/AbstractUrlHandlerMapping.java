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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;

/**
 * Abstract base class for URL-mapped
 * {@link org.springframework.web.reactive.HandlerMapping} implementations.
 *
 * <p>Supports direct matches, e.g. a registered "/test" matches "/test", and
 * various path pattern matches, e.g. a registered "/t*" pattern matches
 * both "/test" and "/team", "/test/*" matches all paths under "/test",
 * "/test/**" matches all paths below "/test". For details, see the
 * {@link org.springframework.web.util.pattern.PathPattern} javadoc.
 *
 * <p>Will search all path patterns to find the most specific match for the
 * current request path. The most specific pattern is defined as the longest
 * path pattern with the fewest captured variables and wildcards.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractUrlHandlerMapping extends AbstractHandlerMapping {

	private boolean lazyInitHandlers = false;

	private final Map<PathPattern, Object> handlerMap = new LinkedHashMap<>();

	@Nullable
	private BiPredicate<Object, ServerWebExchange> handlerPredicate;


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
	 * Return a read-only view of registered path patterns and handlers which may
	 * may be an actual handler instance or the bean name of lazily initialized
	 * handler.
	 */
	public final Map<PathPattern, Object> getHandlerMap() {
		return Collections.unmodifiableMap(this.handlerMap);
	}

	/**
	 * Configure a predicate for extended matching of the handler that was
	 * matched by URL path. This allows for further narrowing of the mapping by
	 * checking additional properties of the request. If the predicate returns
	 * "false", it result in a no-match, which allows another
	 * {@link org.springframework.web.reactive.HandlerMapping} to match or
	 * result in a 404 (NOT_FOUND) response.
	 * @param handlerPredicate a bi-predicate to match the candidate handler
	 * against the current exchange.
	 * @since 5.3.5
	 * @see org.springframework.web.reactive.socket.server.support.WebSocketUpgradeHandlerPredicate
	 */
	public void setHandlerPredicate(BiPredicate<Object, ServerWebExchange> handlerPredicate) {
		this.handlerPredicate = (this.handlerPredicate != null ?
				this.handlerPredicate.and(handlerPredicate) : handlerPredicate);
	}


	@Override
	public Mono<Object> getHandlerInternal(ServerWebExchange exchange) {
		PathContainer lookupPath = exchange.getRequest().getPath().pathWithinApplication();
		Object handler;
		try {
			handler = lookupHandler(lookupPath, exchange);
		}
		catch (Exception ex) {
			return Mono.error(ex);
		}
		return Mono.justOrEmpty(handler);
	}

	/**
	 * Look up a handler instance for the given URL lookup path.
	 * <p>Supports direct matches, e.g. a registered "/test" matches "/test",
	 * and various path pattern matches, e.g. a registered "/t*" matches
	 * both "/test" and "/team". For details, see the PathPattern class.
	 * @param lookupPath the URL the handler is mapped to
	 * @param exchange the current exchange
	 * @return the associated handler instance, or {@code null} if not found
	 * @see org.springframework.web.util.pattern.PathPattern
	 */
	@Nullable
	protected Object lookupHandler(PathContainer lookupPath, ServerWebExchange exchange) throws Exception {
		List<PathPattern> matches = null;
		for (PathPattern pattern : this.handlerMap.keySet()) {
			if (pattern.matches(lookupPath)) {
				matches = (matches != null ? matches : new ArrayList<>());
				matches.add(pattern);
			}
		}
		if (matches == null) {
			return null;
		}
		if (matches.size() > 1) {
			matches.sort(PathPattern.SPECIFICITY_COMPARATOR);
			if (logger.isTraceEnabled()) {
				logger.debug(exchange.getLogPrefix() + "Matching patterns " + matches);
			}
		}

		PathPattern pattern = matches.get(0);
		PathContainer pathWithinMapping = pattern.extractPathWithinPattern(lookupPath);
		PathPattern.PathMatchInfo matchInfo = pattern.matchAndExtract(lookupPath);
		Assert.notNull(matchInfo, "Expected a match");

		Object handler = this.handlerMap.get(pattern);

		// Bean name or resolved handler?
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = obtainApplicationContext().getBean(handlerName);
		}

		if (this.handlerPredicate != null && !this.handlerPredicate.test(handler, exchange)) {
			return null;
		}

		validateHandler(handler, exchange);

		exchange.getAttributes().put(BEST_MATCHING_HANDLER_ATTRIBUTE, handler);
		exchange.getAttributes().put(BEST_MATCHING_PATTERN_ATTRIBUTE, pattern);
		exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathWithinMapping);
		exchange.getAttributes().put(URI_TEMPLATE_VARIABLES_ATTRIBUTE, matchInfo.getUriVariables());

		return handler;
	}

	/**
	 * Validate the given handler against the current request.
	 * <p>The default implementation is empty. Can be overridden in subclasses,
	 * for example to enforce specific preconditions expressed in URL mappings.
	 * @param handler the handler object to validate
	 * @param exchange current exchange
	 */
	@SuppressWarnings("UnusedParameters")
	protected void validateHandler(Object handler, ServerWebExchange exchange) {
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

		// Parse path pattern
		urlPath = prependLeadingSlash(urlPath);
		PathPattern pattern = getPathPatternParser().parse(urlPath);
		if (this.handlerMap.containsKey(pattern)) {
			Object existingHandler = this.handlerMap.get(pattern);
			if (existingHandler != null && existingHandler != resolvedHandler) {
				throw new IllegalStateException(
						"Cannot map " + getHandlerDescription(handler) + " to [" + urlPath + "]: " +
						"there is already " + getHandlerDescription(existingHandler) + " mapped.");
			}
		}

		// Eagerly resolve handler if referencing singleton via name.
		if (!this.lazyInitHandlers && handler instanceof String) {
			String handlerName = (String) handler;
			if (obtainApplicationContext().isSingleton(handlerName)) {
				resolvedHandler = obtainApplicationContext().getBean(handlerName);
			}
		}

		// Register resolved handler
		this.handlerMap.put(pattern, resolvedHandler);
		if (logger.isTraceEnabled()) {
			logger.trace("Mapped [" + urlPath + "] onto " + getHandlerDescription(handler));
		}
	}

	private String getHandlerDescription(Object handler) {
		return (handler instanceof String ? "'" + handler + "'" : handler.toString());
	}


	private static String prependLeadingSlash(String pattern) {
		if (StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
			return "/" + pattern;
		}
		else {
			return pattern;
		}
	}

}
