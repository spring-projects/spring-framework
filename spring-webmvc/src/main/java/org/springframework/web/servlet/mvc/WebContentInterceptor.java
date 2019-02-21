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

package org.springframework.web.servlet.mvc;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.CacheControl;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.UrlPathHelper;

/**
 * Handler interceptor that checks the request and prepares the response.
 * Checks for supported methods and a required session, and applies the
 * specified {@link org.springframework.http.CacheControl} builder.
 * See superclass bean properties for configuration options.
 *
 * <p>All the settings supported by this interceptor can also be set on
 * {@link AbstractController}. This interceptor is mainly intended for applying
 * checks and preparations to a set of controllers mapped by a HandlerMapping.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 27.11.2003
 * @see AbstractController
 */
public class WebContentInterceptor extends WebContentGenerator implements HandlerInterceptor {

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private Map<String, Integer> cacheMappings = new HashMap<>();

	private Map<String, CacheControl> cacheControlMappings = new HashMap<>();


	public WebContentInterceptor() {
		// No restriction of HTTP methods by default,
		// in particular for use with annotated controllers...
		super(false);
	}


	/**
	 * Shortcut to same property on underlying {@link #setUrlPathHelper UrlPathHelper}.
	 * <p>Only relevant for the "cacheMappings" setting.
	 * @see #setCacheMappings
	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * Shortcut to same property on underlying {@link #setUrlPathHelper UrlPathHelper}.
	 * <p>Only relevant for the "cacheMappings" setting.
	 * @see #setCacheMappings
	 * @see org.springframework.web.util.UrlPathHelper#setUrlDecode
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * Set the UrlPathHelper to use for resolution of lookup paths.
	 * <p>Use this to override the default UrlPathHelper with a custom subclass,
	 * or to share common UrlPathHelper settings across multiple HandlerMappings
	 * and MethodNameResolvers.
	 * <p>Only relevant for the "cacheMappings" setting.
	 * @see #setCacheMappings
	 * @see org.springframework.web.servlet.handler.AbstractUrlHandlerMapping#setUrlPathHelper
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * Map specific URL paths to specific cache seconds.
	 * <p>Overrides the default cache seconds setting of this interceptor.
	 * Can specify "-1" to exclude a URL path from default caching.
	 * <p>Supports direct matches, e.g. a registered "/test" matches "/test",
	 * and a various Ant-style pattern matches, e.g. a registered "/t*" matches
	 * both "/test" and "/team". For details, see the AntPathMatcher javadoc.
	 * <p><b>NOTE:</b> Path patterns are not supposed to overlap. If a request
	 * matches several mappings, it is effectively undefined which one will apply
	 * (due to the lack of key ordering in {@code java.util.Properties}).
	 * @param cacheMappings a mapping between URL paths (as keys) and
	 * cache seconds (as values, need to be integer-parsable)
	 * @see #setCacheSeconds
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setCacheMappings(Properties cacheMappings) {
		this.cacheMappings.clear();
		Enumeration<?> propNames = cacheMappings.propertyNames();
		while (propNames.hasMoreElements()) {
			String path = (String) propNames.nextElement();
			int cacheSeconds = Integer.parseInt(cacheMappings.getProperty(path));
			this.cacheMappings.put(path, cacheSeconds);
		}
	}

	/**
	 * Map specific URL paths to a specific {@link org.springframework.http.CacheControl}.
	 * <p>Overrides the default cache seconds setting of this interceptor.
	 * Can specify a empty {@link org.springframework.http.CacheControl} instance
	 * to exclude a URL path from default caching.
	 * <p>Supports direct matches, e.g. a registered "/test" matches "/test",
	 * and a various Ant-style pattern matches, e.g. a registered "/t*" matches
	 * both "/test" and "/team". For details, see the AntPathMatcher javadoc.
	 * <p><b>NOTE:</b> Path patterns are not supposed to overlap. If a request
	 * matches several mappings, it is effectively undefined which one will apply
	 * (due to the lack of key ordering in the underlying {@code java.util.HashMap}).
	 * @param cacheControl the {@code CacheControl} to use
	 * @param paths the URL paths that will map to the given {@code CacheControl}
	 * @since 4.2
	 * @see #setCacheSeconds
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void addCacheMapping(CacheControl cacheControl, String... paths) {
		for (String path : paths) {
			this.cacheControlMappings.put(path, cacheControl);
		}
	}

	/**
	 * Set the PathMatcher implementation to use for matching URL paths
	 * against registered URL patterns, for determining cache mappings.
	 * Default is AntPathMatcher.
	 * @see #addCacheMapping
	 * @see #setCacheMappings
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException {

		checkRequest(request);

		String lookupPath = this.urlPathHelper.getLookupPathForRequest(request);

		CacheControl cacheControl = lookupCacheControl(lookupPath);
		Integer cacheSeconds = lookupCacheSeconds(lookupPath);
		if (cacheControl != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Applying " + cacheControl);
			}
			applyCacheControl(response, cacheControl);
		}
		else if (cacheSeconds != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Applying cacheSeconds " + cacheSeconds);
			}
			applyCacheSeconds(response, cacheSeconds);
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Applying default cacheSeconds");
			}
			prepareResponse(response);
		}

		return true;
	}

	/**
	 * Look up a {@link org.springframework.http.CacheControl} instance for the given URL path.
	 * <p>Supports direct matches, e.g. a registered "/test" matches "/test",
	 * and various Ant-style pattern matches, e.g. a registered "/t*" matches
	 * both "/test" and "/team". For details, see the AntPathMatcher class.
	 * @param urlPath the URL the bean is mapped to
	 * @return the associated {@code CacheControl}, or {@code null} if not found
	 * @see org.springframework.util.AntPathMatcher
	 */
	@Nullable
	protected CacheControl lookupCacheControl(String urlPath) {
		// Direct match?
		CacheControl cacheControl = this.cacheControlMappings.get(urlPath);
		if (cacheControl != null) {
			return cacheControl;
		}
		// Pattern match?
		for (String registeredPath : this.cacheControlMappings.keySet()) {
			if (this.pathMatcher.match(registeredPath, urlPath)) {
				return this.cacheControlMappings.get(registeredPath);
			}
		}
		return null;
	}

	/**
	 * Look up a cacheSeconds integer value for the given URL path.
	 * <p>Supports direct matches, e.g. a registered "/test" matches "/test",
	 * and various Ant-style pattern matches, e.g. a registered "/t*" matches
	 * both "/test" and "/team". For details, see the AntPathMatcher class.
	 * @param urlPath the URL the bean is mapped to
	 * @return the cacheSeconds integer value, or {@code null} if not found
	 * @see org.springframework.util.AntPathMatcher
	 */
	@Nullable
	protected Integer lookupCacheSeconds(String urlPath) {
		// Direct match?
		Integer cacheSeconds = this.cacheMappings.get(urlPath);
		if (cacheSeconds != null) {
			return cacheSeconds;
		}
		// Pattern match?
		for (String registeredPath : this.cacheMappings.keySet()) {
			if (this.pathMatcher.match(registeredPath, urlPath)) {
				return this.cacheMappings.get(registeredPath);
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
