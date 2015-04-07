/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.UrlPathHelper;

/**
 * Interceptor that checks and prepares request and response. Checks for supported
 * methods and a required session, and applies the specified number of cache seconds.
 * See superclass bean properties for configuration options.
 *
 * <p>All the settings supported by this interceptor can also be set on AbstractController.
 * This interceptor is mainly intended for applying checks and preparations to a set of
 * controllers mapped by a HandlerMapping.
 *
 * @author Juergen Hoeller
 * @since 27.11.2003
 * @see AbstractController
 */
public class WebContentInterceptor extends WebContentGenerator implements HandlerInterceptor {

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private Map<String, Integer> cacheMappings = new HashMap<String, Integer>();

	private PathMatcher pathMatcher = new AntPathMatcher();


	public WebContentInterceptor() {
		// no restriction of HTTP methods by default,
		// in particular for use with annotated controllers
		super(false);
	}


	/**
	 * Set if URL lookup should always use full path within current servlet
	 * context. Else, the path within the current servlet mapping is used
	 * if applicable (i.e. in the case of a ".../*" servlet mapping in web.xml).
	 * Default is "false".
	 * <p>Only relevant for the "cacheMappings" setting.
	 * @see #setCacheMappings
	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * Set if context path and request URI should be URL-decoded.
	 * Both are returned <i>undecoded</i> by the Servlet API,
	 * in contrast to the servlet path.
	 * <p>Uses either the request encoding or the default encoding according
	 * to the Servlet spec (ISO-8859-1).
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
	 * @see org.springframework.web.servlet.mvc.multiaction.AbstractUrlMethodNameResolver#setUrlPathHelper
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
			this.cacheMappings.put(path, Integer.valueOf(cacheMappings.getProperty(path)));
		}
	}

	/**
	 * Set the PathMatcher implementation to use for matching URL paths
	 * against registered URL patterns, for determining cache mappings.
	 * Default is AntPathMatcher.
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

		String lookupPath = this.urlPathHelper.getLookupPathForRequest(request);
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up cache seconds for [" + lookupPath + "]");
		}

		Integer cacheSeconds = lookupCacheSeconds(lookupPath);
		if (cacheSeconds != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Applying " + cacheSeconds + " cache seconds to [" + lookupPath + "]");
			}
			checkAndPrepare(request, response, cacheSeconds, handler instanceof LastModified);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Applying default cache seconds to [" + lookupPath + "]");
			}
			checkAndPrepare(request, response, handler instanceof LastModified);
		}

		return true;
	}

	/**
	 * Look up a cache seconds value for the given URL path.
	 * <p>Supports direct matches, e.g. a registered "/test" matches "/test",
	 * and various Ant-style pattern matches, e.g. a registered "/t*" matches
	 * both "/test" and "/team". For details, see the AntPathMatcher class.
	 * @param urlPath URL the bean is mapped to
	 * @return the associated cache seconds, or {@code null} if not found
	 * @see org.springframework.util.AntPathMatcher
	 */
	protected Integer lookupCacheSeconds(String urlPath) {
		// direct match?
		Integer cacheSeconds = this.cacheMappings.get(urlPath);
		if (cacheSeconds == null) {
			// pattern match?
			for (String registeredPath : this.cacheMappings.keySet()) {
				if (this.pathMatcher.match(registeredPath, urlPath)) {
					cacheSeconds = this.cacheMappings.get(registeredPath);
				}
			}
		}
		return cacheSeconds;
	}


	/**
	 * This implementation is empty.
	 */
	@Override
	public void postHandle(
			HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
			throws Exception {
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public void afterCompletion(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
	}

}
