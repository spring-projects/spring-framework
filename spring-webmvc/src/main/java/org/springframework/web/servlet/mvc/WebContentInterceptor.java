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

package org.springframework.web.servlet.mvc;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.CacheControl;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Handler interceptor that checks the request for supported methods and a
 * required session and prepares the response by applying the configured
 * cache settings.
 *
 * <p>Cache settings may be configured for specific URLs via path pattern with
 * {@link #addCacheMapping(CacheControl, String...)} and
 * {@link #setCacheMappings(Properties)}, along with a fallback on default
 * settings for all URLs via {@link #setCacheControl(CacheControl)}.
 *
 * <p>Pattern matching can be done with {@link PathMatcher} or with parsed
 * {@link PathPattern}s. The syntax is largely the same with the latter being
 * more tailored for web usage and more efficient. The choice depends on the
 * presence of a {@link UrlPathHelper#resolveAndCacheLookupPath resolved}
 * {@code String} lookupPath or a {@link ServletRequestPathUtils#parseAndCache
 * parsed} {@code RequestPath} which in turn depends on the
 * {@link HandlerMapping} that matched the current request.
 *
 * <p>All the settings supported by this interceptor can also be set on
 * {@link AbstractController}. This interceptor is mainly intended for applying
 * checks and preparations to a set of controllers mapped by a HandlerMapping.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 27.11.2003
 * @see PathMatcher
 * @see AntPathMatcher
 */
public class WebContentInterceptor extends WebContentGenerator implements HandlerInterceptor {

	private static PathMatcher defaultPathMatcher = new AntPathMatcher();


	private final PathPatternParser patternParser;

	private PathMatcher pathMatcher = defaultPathMatcher;

	private final Map<PathPattern, Integer> cacheMappings = new HashMap<>();

	private final Map<PathPattern, CacheControl> cacheControlMappings = new HashMap<>();


	/**
	 * Default constructor with {@link PathPatternParser#defaultInstance}.
	 */
	public WebContentInterceptor() {
		this(PathPatternParser.defaultInstance);
	}

	/**
	 * Constructor with a {@link PathPatternParser} to parse patterns with.
	 * @since 5.3
	 */
	public WebContentInterceptor(PathPatternParser parser) {
		// No restriction of HTTP methods by default,
		// in particular for use with annotated controllers...
		super(false);
		this.patternParser = parser;
	}


	/**
	 * Shortcut to the
	 * {@link org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath
	 * same property} on the configured {@code UrlPathHelper}.
	 * @deprecated as of 5.3, the path is resolved externally and obtained with
	 * {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)}
	 */
	@Deprecated
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
	}

	/**
	 * Shortcut to the
	 * {@link org.springframework.web.util.UrlPathHelper#setUrlDecode
	 * same property} on the configured {@code UrlPathHelper}.
	 * @deprecated as of 5.3, the path is resolved externally and obtained with
	 * {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)}
	 */
	@Deprecated
	public void setUrlDecode(boolean urlDecode) {
	}

	/**
	 * Set the UrlPathHelper to use for resolution of lookup paths.
	 * @deprecated as of 5.3, the path is resolved externally and obtained with
	 * {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)}
	 */
	@Deprecated
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
	}

	/**
	 * Configure the PathMatcher to use to match URL paths against registered
	 * URL patterns to select the cache settings for a request.
	 * <p>This is an advanced property that should be used only when a
	 * customized {@link AntPathMatcher} or a custom PathMatcher is required.
	 * <p>By default this is {@link AntPathMatcher}.
	 * <p><strong>Note:</strong> Setting {@code PathMatcher} enforces use of
	 * String pattern matching even when a
	 * {@link ServletRequestPathUtils#parseAndCache parsed} {@code RequestPath}
	 * is available.
	 * @see #addCacheMapping
	 * @see #setCacheMappings
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Map settings for  cache seconds to specific URL paths via patterns.
	 * <p>Overrides the default cache seconds setting of this interceptor.
	 * Can specify "-1" to exclude a URL path from default caching.
	 * <p>For pattern syntax see {@link AntPathMatcher} and {@link PathPattern}
	 * as well as the class-level Javadoc for details for when each is used.
	 * The syntax is largely the same with {@link PathPattern} more tailored for
	 * web usage.
	 * <p><b>NOTE:</b> Path patterns are not supposed to overlap. If a request
	 * matches several mappings, it is effectively undefined which one will apply
	 * (due to the lack of key ordering in {@code java.util.Properties}).
	 * @param cacheMappings a mapping between URL paths (as keys) and
	 * cache seconds (as values, need to be integer-parsable)
	 * @see #setCacheSeconds
	 */
	public void setCacheMappings(Properties cacheMappings) {
		this.cacheMappings.clear();
		Enumeration<?> propNames = cacheMappings.propertyNames();
		while (propNames.hasMoreElements()) {
			String path = (String) propNames.nextElement();
			int cacheSeconds = Integer.parseInt(cacheMappings.getProperty(path));
			this.cacheMappings.put(this.patternParser.parse(path), cacheSeconds);
		}
	}

	/**
	 * Map specific URL paths to a specific {@link org.springframework.http.CacheControl}.
	 * <p>Overrides the default cache seconds setting of this interceptor.
	 * Can specify an empty {@link org.springframework.http.CacheControl} instance
	 * to exclude a URL path from default caching.
	 * <p>For pattern syntax see {@link AntPathMatcher} and {@link PathPattern}
	 * as well as the class-level Javadoc for details for when each is used.
	 * The syntax is largely the same with {@link PathPattern} more tailored for
	 * web usage.
	 * <p><b>NOTE:</b> Path patterns are not supposed to overlap. If a request
	 * matches several mappings, it is effectively undefined which one will apply
	 * (due to the lack of key ordering in the underlying {@code java.util.HashMap}).
	 * @param cacheControl the {@code CacheControl} to use
	 * @param paths the URL paths that will map to the given {@code CacheControl}
	 * @since 4.2
	 * @see #setCacheSeconds
	 */
	public void addCacheMapping(CacheControl cacheControl, String... paths) {
		for (String path : paths) {
			this.cacheControlMappings.put(this.patternParser.parse(path), cacheControl);
		}
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException {

		checkRequest(request);

		Object path = ServletRequestPathUtils.getCachedPath(request);
		if (this.pathMatcher != defaultPathMatcher) {
			path = path.toString();
		}

		if (!ObjectUtils.isEmpty(this.cacheControlMappings)) {
			CacheControl control = (path instanceof PathContainer pathContainer ?
					lookupCacheControl(pathContainer) : lookupCacheControl((String) path));
			if (control != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Applying " + control);
				}
				applyCacheControl(response, control);
				return true;
			}
		}

		if (!ObjectUtils.isEmpty(this.cacheMappings)) {
			Integer cacheSeconds = (path instanceof PathContainer pathContainer ?
					lookupCacheSeconds(pathContainer) : lookupCacheSeconds((String) path));
			if (cacheSeconds != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Applying cacheSeconds " + cacheSeconds);
				}
				applyCacheSeconds(response, cacheSeconds);
				return true;
			}
		}

		prepareResponse(response);
		return true;
	}

	/**
	 * Find a {@link org.springframework.http.CacheControl} instance for the
	 * given parsed {@link PathContainer path}. This is used when the
	 * {@code HandlerMapping} uses parsed {@code PathPatterns}.
	 * @param path the path to match to
	 * @return the matched {@code CacheControl}, or {@code null} if no match
	 * @since 5.3
	 */
	@Nullable
	protected CacheControl lookupCacheControl(PathContainer path) {
		for (Map.Entry<PathPattern, CacheControl> entry : this.cacheControlMappings.entrySet()) {
			if (entry.getKey().matches(path)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * Find a {@link org.springframework.http.CacheControl} instance for the
	 * given String lookupPath. This is used when the {@code HandlerMapping}
	 * relies on String pattern matching with {@link PathMatcher}.
	 * @param lookupPath the path to match to
	 * @return the matched {@code CacheControl}, or {@code null} if no match
	 */
	@Nullable
	protected CacheControl lookupCacheControl(String lookupPath) {
		for (Map.Entry<PathPattern, CacheControl> entry : this.cacheControlMappings.entrySet()) {
			if (this.pathMatcher.match(entry.getKey().getPatternString(), lookupPath)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * Find a cacheSeconds value for the given parsed {@link PathContainer path}.
	 * This is used when the {@code HandlerMapping} uses parsed {@code PathPatterns}.
	 * @param path the path to match to
	 * @return the matched cacheSeconds, or {@code null} if there is no match
	 * @since 5.3
	 */
	@Nullable
	protected Integer lookupCacheSeconds(PathContainer path) {
		for (Map.Entry<PathPattern, Integer> entry : this.cacheMappings.entrySet()) {
			if (entry.getKey().matches(path)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * Find a cacheSeconds instance for the given String lookupPath.
	 * This is used when the {@code HandlerMapping} relies on String pattern
	 * matching with {@link PathMatcher}.
	 * @param lookupPath the path to match to
	 * @return the matched cacheSeconds, or {@code null} if there is no match
	 */
	@Nullable
	protected Integer lookupCacheSeconds(String lookupPath) {
		for (Map.Entry<PathPattern, Integer> entry : this.cacheMappings.entrySet()) {
			if (this.pathMatcher.match(entry.getKey().getPatternString(), lookupPath)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) throws Exception {
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) throws Exception {
	}

}
