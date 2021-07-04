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

package org.springframework.web.servlet.resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * A central component to use to obtain the public URL path that clients should
 * use to access a static resource.
 *
 * <p>This class is aware of Spring MVC handler mappings used to serve static
 * resources and uses the {@code ResourceResolver} chains of the configured
 * {@code ResourceHttpRequestHandler}s to make its decisions.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.1
 */
public class ResourceUrlProvider implements ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private ApplicationContext applicationContext;

	private UrlPathHelper urlPathHelper = UrlPathHelper.defaultInstance;

	private PathMatcher pathMatcher = new AntPathMatcher();

	private final Map<String, ResourceHttpRequestHandler> handlerMap = new LinkedHashMap<>();

	private boolean autodetect = true;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * Configure a {@code UrlPathHelper} to use in
	 * {@link #getForRequestUrl(javax.servlet.http.HttpServletRequest, String)}
	 * in order to derive the lookup path for a target request URL path.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * Return the configured {@code UrlPathHelper}.
	 * @since 4.2.8
	 */
	public UrlPathHelper getUrlPathHelper() {
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
	public void setHandlerMap(@Nullable Map<String, ResourceHttpRequestHandler> handlerMap) {
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
	public Map<String, ResourceHttpRequestHandler> getHandlerMap() {
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
		if (event.getApplicationContext() == this.applicationContext && isAutodetect()) {
			this.handlerMap.clear();
			detectResourceHandlers(this.applicationContext);
			if (!this.handlerMap.isEmpty()) {
				this.autodetect = false;
			}
		}
	}

	protected void detectResourceHandlers(ApplicationContext appContext) {
		Map<String, SimpleUrlHandlerMapping> beans = appContext.getBeansOfType(SimpleUrlHandlerMapping.class);
		List<SimpleUrlHandlerMapping> mappings = new ArrayList<>(beans.values());
		AnnotationAwareOrderComparator.sort(mappings);

		for (SimpleUrlHandlerMapping mapping : mappings) {
			for (String pattern : mapping.getHandlerMap().keySet()) {
				Object handler = mapping.getHandlerMap().get(pattern);
				if (handler instanceof ResourceHttpRequestHandler) {
					ResourceHttpRequestHandler resourceHandler = (ResourceHttpRequestHandler) handler;
					this.handlerMap.put(pattern, resourceHandler);
				}
			}
		}

		if (this.handlerMap.isEmpty()) {
			logger.trace("No resource handling mappings found");
		}
	}

	/**
	 * A variation on {@link #getForLookupPath(String)} that accepts a full request
	 * URL path (i.e. including context and servlet path) and returns the full request
	 * URL path to expose for public use.
	 * @param request the current request
	 * @param requestUrl the request URL path to resolve
	 * @return the resolved public URL path, or {@code null} if unresolved
	 */
	@Nullable
	public final String getForRequestUrl(HttpServletRequest request, String requestUrl) {
		int prefixIndex = getLookupPathIndex(request);
		int suffixIndex = getEndPathIndex(requestUrl);
		if (prefixIndex >= suffixIndex) {
			return null;
		}
		String prefix = requestUrl.substring(0, prefixIndex);
		String suffix = requestUrl.substring(suffixIndex);
		String lookupPath = requestUrl.substring(prefixIndex, suffixIndex);
		String resolvedLookupPath = getForLookupPath(lookupPath);
		return (resolvedLookupPath != null ? prefix + resolvedLookupPath + suffix : null);
	}

	private int getLookupPathIndex(HttpServletRequest request) {
		UrlPathHelper pathHelper = getUrlPathHelper();
		String requestUri = pathHelper.getRequestUri(request);
		String lookupPath = this.urlPathHelper.getLookupPathForRequest(request, HandlerMapping.LOOKUP_PATH);
		return requestUri.indexOf(lookupPath);
	}

	private int getEndPathIndex(String lookupPath) {
		int suffixIndex = lookupPath.length();
		int queryIndex = lookupPath.indexOf('?');
		if (queryIndex > 0) {
			suffixIndex = queryIndex;
		}
		int hashIndex = lookupPath.indexOf('#');
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
	 * <p>It is expected that the given path is what Spring MVC would use for
	 * request mapping purposes, i.e. excluding context and servlet path portions.
	 * <p>If several handler mappings match, the handler used will be the one
	 * configured with the most specific pattern.
	 * @param lookupPath the lookup path to check
	 * @return the resolved public URL path, or {@code null} if unresolved
	 */
	@Nullable
	public final String getForLookupPath(String lookupPath) {
		// Clean duplicate slashes or pathWithinPattern won't match lookupPath
		String previous;
		do {
			previous = lookupPath;
			lookupPath = StringUtils.replace(lookupPath, "//", "/");
		} while (!lookupPath.equals(previous));

		List<String> matchingPatterns = new ArrayList<>();
		for (String pattern : this.handlerMap.keySet()) {
			if (getPathMatcher().match(pattern, lookupPath)) {
				matchingPatterns.add(pattern);
			}
		}

		if (!matchingPatterns.isEmpty()) {
			Comparator<String> patternComparator = getPathMatcher().getPatternComparator(lookupPath);
			matchingPatterns.sort(patternComparator);
			for (String pattern : matchingPatterns) {
				String pathWithinMapping = getPathMatcher().extractPathWithinPattern(pattern, lookupPath);
				String pathMapping = lookupPath.substring(0, lookupPath.indexOf(pathWithinMapping));
				ResourceHttpRequestHandler handler = this.handlerMap.get(pattern);
				ResourceResolverChain chain = new DefaultResourceResolverChain(handler.getResourceResolvers());
				String resolved = chain.resolveUrlPath(pathWithinMapping, handler.getLocations());
				if (resolved == null) {
					continue;
				}
				return pathMapping + resolved;
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace("No match for \"" + lookupPath + "\"");
		}

		return null;
	}

}
