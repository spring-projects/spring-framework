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

package org.springframework.web.reactive.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * A central component to use to obtain the public URL path that clients should
 * use to access a static resource.
 *
 * <p>This class is aware of Spring WebFlux handler mappings used to serve static
 * resources and uses the {@code ResourceResolver} chains of the configured
 * {@code ResourceHttpRequestHandler}s to make its decisions.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResourceUrlProvider implements ApplicationListener<ContextRefreshedEvent> {

	private static final Log logger = LogFactory.getLog(ResourceUrlProvider.class);


	private final PathPatternParser patternParser = new PathPatternParser();

	private final Map<PathPattern, ResourceWebHandler> handlerMap = new LinkedHashMap<>();


	/**
	 * Return a read-only view of the resource handler mappings either manually
	 * configured or auto-detected from Spring configuration.
	 */
	public Map<PathPattern, ResourceWebHandler> getHandlerMap() {
		return Collections.unmodifiableMap(this.handlerMap);
	}


	/**
	 * Manually configure resource handler mappings.
	 * <p><strong>Note:</strong> by default resource mappings are auto-detected
	 * from the Spring {@code ApplicationContext}. If this property is used,
	 * auto-detection is turned off.
	 */
	public void registerHandlers(Map<String, ResourceWebHandler> handlerMap) {
		this.handlerMap.clear();
		handlerMap.forEach((rawPattern, resourceWebHandler) -> {
			rawPattern = prependLeadingSlash(rawPattern);
			PathPattern pattern = this.patternParser.parse(rawPattern);
			this.handlerMap.put(pattern, resourceWebHandler);
		});
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (this.handlerMap.isEmpty()) {
			detectResourceHandlers(event.getApplicationContext());
			if (logger.isDebugEnabled()) {
				logger.debug("No resource handling mappings found");
			}
		}
	}

	private void detectResourceHandlers(ApplicationContext context) {
		logger.debug("Looking for resource handler mappings");

		Map<String, SimpleUrlHandlerMapping> beans = context.getBeansOfType(SimpleUrlHandlerMapping.class);
		List<SimpleUrlHandlerMapping> mappings = new ArrayList<>(beans.values());
		AnnotationAwareOrderComparator.sort(mappings);

		mappings.forEach(mapping ->
			mapping.getHandlerMap().forEach((pattern, handler) -> {
				if (handler instanceof ResourceWebHandler) {
					ResourceWebHandler resourceHandler = (ResourceWebHandler) handler;
					if (logger.isDebugEnabled()) {
						logger.debug("Found resource handler mapping: URL pattern=\"" + pattern + "\", " +
								"locations=" + resourceHandler.getLocations() + ", " +
								"resolvers=" + resourceHandler.getResourceResolvers());
					}
					this.handlerMap.put(pattern, resourceHandler);
				}
			}));
	}


	/**
	 * Get the public resource URL for the given URI string.
	 * <p>The URI string is expected to be a path and if it contains a query or
	 * fragment those will be preserved in the resulting public resource URL.
	 * @param uriString the URI string to transform
	 * @param exchange the current exchange
	 * @return the resolved public resource URL path, or empty if unresolved
	 */
	public final Mono<String> getForUriString(String uriString, ServerWebExchange exchange) {
		if (logger.isTraceEnabled()) {
			logger.trace("Getting resource URL for request URL \"" + uriString + "\"");
		}
		ServerHttpRequest request = exchange.getRequest();
		int queryIndex = getQueryIndex(uriString);
		String lookupPath = uriString.substring(0, queryIndex);
		String query = uriString.substring(queryIndex);
		PathContainer parsedLookupPath = PathContainer.parsePath(lookupPath);

		if (logger.isTraceEnabled()) {
			logger.trace("Getting resource URL for lookup path \"" + lookupPath + "\"");
		}
		return resolveResourceUrl(parsedLookupPath).map(resolvedPath ->
				request.getPath().contextPath().value() + resolvedPath + query);
	}

	private int getQueryIndex(String path) {
		int suffixIndex = path.length();
		int queryIndex = path.indexOf('?');
		if (queryIndex > 0) {
			suffixIndex = queryIndex;
		}
		int hashIndex = path.indexOf('#');
		if (hashIndex > 0) {
			suffixIndex = Math.min(suffixIndex, hashIndex);
		}
		return suffixIndex;
	}

	private Mono<String> resolveResourceUrl(PathContainer lookupPath) {
		return this.handlerMap.entrySet().stream()
				.filter(entry -> entry.getKey().matches(lookupPath))
				.sorted((entry1, entry2) ->
						PathPattern.SPECIFICITY_COMPARATOR.compare(entry1.getKey(), entry2.getKey()))
				.findFirst()
				.map(entry -> {
					PathContainer path = entry.getKey().extractPathWithinPattern(lookupPath);
					int endIndex = lookupPath.elements().size() - path.elements().size();
					PathContainer mapping = lookupPath.subPath(0, endIndex);
					if (logger.isTraceEnabled()) {
						logger.trace("Invoking ResourceResolverChain for URL pattern \"" + entry.getKey() + "\"");
					}
					ResourceWebHandler handler = entry.getValue();
					List<ResourceResolver> resolvers = handler.getResourceResolvers();
					ResourceResolverChain chain = new DefaultResourceResolverChain(resolvers);
					return chain.resolveUrlPath(path.value(), handler.getLocations())
							.map(resolvedPath -> {
								if (logger.isTraceEnabled()) {
									logger.trace("Resolved public resource URL path \"" + resolvedPath + "\"");
								}
								return mapping.value() + resolvedPath;
							});
				})
				.orElse(Mono.empty());
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
