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

package org.springframework.web.reactive.resource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.springframework.http.server.reactive.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
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


	private final PathPatternParser patternParser;

	private final Map<PathPattern, ResourceWebHandler> handlerMap = new LinkedHashMap<>();

	private boolean autodetect = true;


	public ResourceUrlProvider() {
		this(new PathPatternParser());
	}

	public ResourceUrlProvider(PathPatternParser patternParser) {
		Assert.notNull(patternParser, "'patternParser' is required.");
		this.patternParser = patternParser;
	}


	/**
	 * Return a read-only view of the resource handler mappings either manually
	 * configured or auto-detected when the Spring {@code ApplicationContext}
	 * is refreshed.
	 */
	public Map<PathPattern, ResourceWebHandler> getHandlerMap() {
		return Collections.unmodifiableMap(this.handlerMap);
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
			if (!this.handlerMap.isEmpty()) {
				this.autodetect = false;
			}
			else if(logger.isDebugEnabled()) {
				logger.debug("No resource handling mappings found");
			}
		}
	}

	private void detectResourceHandlers(ApplicationContext context) {
		logger.debug("Looking for resource handler mappings");

		Map<String, SimpleUrlHandlerMapping> map = context.getBeansOfType(SimpleUrlHandlerMapping.class);
		List<SimpleUrlHandlerMapping> handlerMappings = new ArrayList<>(map.values());
		AnnotationAwareOrderComparator.sort(handlerMappings);

		handlerMappings.forEach(mapping -> {
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
			});
		});
	}

	/**
	 * Manually configure the resource mappings.
	 * <p><strong>Note:</strong> by default resource mappings are auto-detected
	 * from the Spring {@code ApplicationContext}. However if this property is
	 * used, the auto-detection is turned off.
	 */
	public void registerHandlers(@Nullable Map<String, ResourceWebHandler> handlerMap) {
		if (handlerMap == null) {
			return;
		}
		this.handlerMap.clear();
		handlerMap.forEach((rawPattern, resourceWebHandler) -> {
			rawPattern = prependLeadingSlash(rawPattern);
			PathPattern pattern = this.patternParser.parse(rawPattern);
			this.handlerMap.put(pattern, resourceWebHandler);
		});
		this.autodetect = false;
	}

	private static String prependLeadingSlash(String pattern) {
		if (StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
			return "/" + pattern;
		}
		else {
			return pattern;
		}
	}


	/**
	 * A variation on {@link #getForLookupPath(PathContainer)} that accepts a
	 * full request URL path and returns the full request URL path to expose
	 * for public use.
	 * @param exchange the current exchange
	 * @param requestUrl the request URL path to resolve
	 * @return the resolved public URL path, or empty if unresolved
	 */
	public final Mono<String> getForRequestUrl(ServerWebExchange exchange, String requestUrl) {
		if (logger.isTraceEnabled()) {
			logger.trace("Getting resource URL for request URL \"" + requestUrl + "\"");
		}
		ServerHttpRequest request = exchange.getRequest();
		int queryIndex = getQueryIndex(requestUrl);
		String lookupPath = requestUrl.substring(0, queryIndex);
		String query = requestUrl.substring(queryIndex);
		PathContainer parsedLookupPath = PathContainer.parse(lookupPath, StandardCharsets.UTF_8);
		return getForLookupPath(parsedLookupPath).map(resolvedPath ->
				request.getPath().contextPath().value() + resolvedPath + query);
	}

	private int getQueryIndex(String lookupPath) {
		int suffixIndex = lookupPath.length();
		int queryIndex = lookupPath.indexOf("?");
		if (queryIndex > 0) {
			suffixIndex = queryIndex;
		}
		int hashIndex = lookupPath.indexOf("#");
		if (hashIndex > 0) {
			suffixIndex = Math.min(suffixIndex, hashIndex);
		}
		return suffixIndex;
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
	 * @return the resolved public URL path, or empty if unresolved
	 */
	public final Mono<String> getForLookupPath(PathContainer lookupPath) {
		if (logger.isTraceEnabled()) {
			logger.trace("Getting resource URL for lookup path \"" + lookupPath + "\"");
		}
		return this.handlerMap.entrySet().stream()
				.filter(entry -> entry.getKey().matches(lookupPath))
				.sorted(Comparator.comparing(Map.Entry::getKey))
				.findFirst()
				.map(entry -> {
					PathContainer path = entry.getKey().extractPathWithinPattern(lookupPath);
					int endIndex = lookupPath.elements().size() - path.elements().size();
					PathContainer mapping = lookupPath.subPath(0, endIndex);
					if (logger.isTraceEnabled()) {
						logger.trace("Invoking ResourceResolverChain for URL pattern " +
								"\"" + entry.getKey() + "\"");
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

}
