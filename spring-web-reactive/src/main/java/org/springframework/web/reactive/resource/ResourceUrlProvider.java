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

package org.springframework.web.reactive.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.HttpRequestPathHelper;

/**
 * A central component to use to obtain the public URL path that clients should
 * use to access a static resource.
 *
 * <p>This class is aware of Spring MVC handler mappings used to serve static
 * resources and uses the {@code ResourceResolver} chains of the configured
 * {@code ResourceHttpRequestHandler}s to make its decisions.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResourceUrlProvider implements ApplicationListener<ContextRefreshedEvent> {

	protected final Log logger = LogFactory.getLog(getClass());

	private HttpRequestPathHelper urlPathHelper = new HttpRequestPathHelper();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private final Map<String, ResourceWebHandler> handlerMap = new LinkedHashMap<>();

	private boolean autodetect = true;


	/**
	 * Configure a {@code UrlPathHelper} to use in
	 * {@link #getForRequestUrl(ServerWebExchange, String)}
	 * in order to derive the lookup path for a target request URL path.
	 */
	public void setUrlPathHelper(HttpRequestPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * Return the configured {@code UrlPathHelper}.
	 */
	public HttpRequestPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * Configure a {@code PathMatcher} to use when comparing target lookup path
	 * against resource mappings.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Return the configured {@code PathMatcher}.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * Manually configure the resource mappings.
	 * <p><strong>Note:</strong> by default resource mappings are auto-detected
	 * from the Spring {@code ApplicationContext}. However if this property is
	 * used, the auto-detection is turned off.
	 */
	public void setHandlerMap(Map<String, ResourceWebHandler> handlerMap) {
		if (handlerMap != null) {
			this.handlerMap.clear();
			this.handlerMap.putAll(handlerMap);
			this.autodetect = false;
		}
	}

	/**
	 * Return the resource mappings, either manually configured or auto-detected
	 * when the Spring {@code ApplicationContext} is refreshed.
	 */
	public Map<String, ResourceWebHandler> getHandlerMap() {
		return this.handlerMap;
	}

	/**
	 * Return {@code false} if resource mappings were manually configured,
	 * {@code true} otherwise.
	 */
	public boolean isAutodetect() {
		return this.autodetect;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (isAutodetect()) {
			this.handlerMap.clear();
			detectResourceHandlers(event.getApplicationContext());
			if (this.handlerMap.isEmpty() && logger.isDebugEnabled()) {
				logger.debug("No resource handling mappings found");
			}
			if (!this.handlerMap.isEmpty()) {
				this.autodetect = false;
			}
		}
	}


	protected void detectResourceHandlers(ApplicationContext appContext) {
		logger.debug("Looking for resource handler mappings");

		Map<String, SimpleUrlHandlerMapping> map = appContext.getBeansOfType(SimpleUrlHandlerMapping.class);
		List<SimpleUrlHandlerMapping> handlerMappings = new ArrayList<>(map.values());
		AnnotationAwareOrderComparator.sort(handlerMappings);

		for (SimpleUrlHandlerMapping hm : handlerMappings) {
			for (String pattern : hm.getHandlerMap().keySet()) {
				Object handler = hm.getHandlerMap().get(pattern);
				if (handler instanceof ResourceWebHandler) {
					ResourceWebHandler resourceHandler = (ResourceWebHandler) handler;
					if (logger.isDebugEnabled()) {
						logger.debug("Found resource handler mapping: URL pattern=\"" + pattern + "\", " +
								"locations=" + resourceHandler.getLocations() + ", " +
								"resolvers=" + resourceHandler.getResourceResolvers());
					}
					this.handlerMap.put(pattern, resourceHandler);
				}
			}
		}
	}

	/**
	 * A variation on {@link #getForLookupPath(String)} that accepts a full request
	 * URL path and returns the full request URL path to expose for public use.
	 * @param exchange the current exchange
	 * @param requestUrl the request URL path to resolve
	 * @return the resolved public URL path, or {@code null} if unresolved
	 */
	public final Mono<String> getForRequestUrl(ServerWebExchange exchange, String requestUrl) {
		if (logger.isTraceEnabled()) {
			logger.trace("Getting resource URL for request URL \"" + requestUrl + "\"");
		}
		int prefixIndex = getLookupPathIndex(exchange);
		int suffixIndex = getQueryParamsIndex(requestUrl);
		String prefix = requestUrl.substring(0, prefixIndex);
		String suffix = requestUrl.substring(suffixIndex);
		String lookupPath = requestUrl.substring(prefixIndex, suffixIndex);
		return getForLookupPath(lookupPath).map(resolvedPath -> prefix + resolvedPath + suffix);
	}

	private int getLookupPathIndex(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		String requestPath = request.getURI().getPath();
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(exchange);
		return requestPath.indexOf(lookupPath);
	}

	private int getQueryParamsIndex(String lookupPath) {
		int index = lookupPath.indexOf("?");
		return index > 0 ? index : lookupPath.length();
	}

	/**
	 * Compare the given path against configured resource handler mappings and
	 * if a match is found use the {@code ResourceResolver} chain of the matched
	 * {@code ResourceHttpRequestHandler} to resolve the URL path to expose for
	 * public use.
	 * <p>It is expected that the given path is what Spring uses for
	 * request mapping purposes.
	 * <p>If several handler mappings match, the handler used will be the one
	 * configured with the most specific pattern.
	 * @param lookupPath the lookup path to check
	 * @return the resolved public URL path, or {@code null} if unresolved
	 */
	public final Mono<String> getForLookupPath(String lookupPath) {
		if (logger.isTraceEnabled()) {
			logger.trace("Getting resource URL for lookup path \"" + lookupPath + "\"");
		}

		List<String> matchingPatterns = new ArrayList<>();
		for (String pattern : this.handlerMap.keySet()) {
			if (getPathMatcher().match(pattern, lookupPath)) {
				matchingPatterns.add(pattern);
			}
		}

		if (matchingPatterns.isEmpty()) {
			return Mono.empty();
		}

		Comparator<String> patternComparator = getPathMatcher().getPatternComparator(lookupPath);
		Collections.sort(matchingPatterns, patternComparator);

		return Flux.fromIterable(matchingPatterns)
				.concatMap(pattern -> {
					String pathWithinMapping = getPathMatcher().extractPathWithinPattern(pattern, lookupPath);
					String pathMapping = lookupPath.substring(0, lookupPath.indexOf(pathWithinMapping));
					if (logger.isTraceEnabled()) {
						logger.trace("Invoking ResourceResolverChain for URL pattern \"" + pattern + "\"");
					}
					ResourceWebHandler handler = this.handlerMap.get(pattern);
					ResourceResolverChain chain = new DefaultResourceResolverChain(handler.getResourceResolvers());
					return chain.resolveUrlPath(pathWithinMapping, handler.getLocations())
							.map(resolvedPath -> {
								if (logger.isTraceEnabled()) {
									logger.trace("Resolved public resource URL path \"" + resolvedPath + "\"");
								}
								return pathMapping + resolvedPath;
							});
				})
				.next();
	}

}
