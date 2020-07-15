/*
 * Copyright 2002-2020 the original author or authors.
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
package org.springframework.web.util;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.util.Assert;

/**
 * Utility class to parse the path of an {@link HttpServletRequest} to a
 * {@link RequestPath} and cache it in a request attribute for further access.
 * This can then be used for URL path matching with
 * {@link org.springframework.web.util.pattern.PathPattern PathPattern}s.
 *
 * <p>Also includes helper methods to return either a previously
 * {@link UrlPathHelper#resolveAndCacheLookupPath resolved} String lookupPath
 * or a previously {@link #parseAndCache parsed} {@code RequestPath} depending
 * on which is cached in request attributes.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public abstract class ServletRequestPathUtils {

	/** Name of Servlet request attribute that holds the parsed {@link RequestPath}. */
	public static final String PATH_ATTRIBUTE = ServletRequestPathUtils.class.getName() + ".path";


	/**
	 * Parse the {@link HttpServletRequest#getRequestURI() requestURI} of the
	 * request and its {@code contextPath} to create a {@link RequestPath} and
	 * cache it in the request attribute {@link #PATH_ATTRIBUTE}.
	 *
	 * <p>This method ignores the {@link HttpServletRequest#getServletPath()
	 * servletPath} and the {@link HttpServletRequest#getPathInfo() pathInfo}.
	 * Therefore in case of a Servlet mapping by prefix, the
	 * {@link RequestPath#pathWithinApplication()} will always include the
	 * Servlet prefix.
	 */
	public static RequestPath parseAndCache(HttpServletRequest request) {
		String requestUri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
		requestUri = (requestUri != null ? requestUri : request.getRequestURI());
		RequestPath requestPath = RequestPath.parse(requestUri, request.getContextPath());
		request.setAttribute(PATH_ATTRIBUTE, requestPath);
		return requestPath;
	}

	/**
	 * Return a {@link #parseAndCache  previously} parsed and cached {@code RequestPath}.
	 * @throws IllegalArgumentException if not found
	 */
	public static RequestPath getParsedRequestPath(ServletRequest request) {
		RequestPath path = (RequestPath) request.getAttribute(PATH_ATTRIBUTE);
		Assert.notNull(path, "Expected parsed RequestPath in request attribute \"" + PATH_ATTRIBUTE + "\".");
		return path;
	}

	/**
	 * Check for a {@link #parseAndCache  previously} parsed and cached {@code RequestPath}.
	 */
	public static boolean hasParsedRequestPath(ServletRequest request) {
		return (request.getAttribute(PATH_ATTRIBUTE) != null);
	}

	/**
	 * Remove the request attribute {@link #PATH_ATTRIBUTE} that holds a
	 * {@link #parseAndCache  previously} parsed and cached {@code RequestPath}.
	 */
	public static void clearParsedRequestPath(ServletRequest request) {
		request.removeAttribute(PATH_ATTRIBUTE);
	}


	// Methods to select either parsed RequestPath or resolved String lookupPath

	/**
	 * Return the {@link UrlPathHelper#resolveAndCacheLookupPath pre-resolved}
	 * String lookupPath or the {@link #parseAndCache(HttpServletRequest)
	 * pre-parsed} {@code RequestPath}.
	 * <p>In Spring MVC, when at least one {@code HandlerMapping} has parsed
	 * {@code PathPatterns} enabled, the {@code DispatcherServlet} eagerly parses
	 * and caches the {@code RequestPath} and the same can be also done earlier with
	 * {@link org.springframework.web.filter.ServletRequestPathFilter
	 * ServletRequestPathFilter}. In other cases where {@code HandlerMapping}s
	 * use String pattern matching with {@code PathMatcher}, the String
	 * lookupPath is resolved separately by each {@code HandlerMapping}.
	 * @param request the current request
	 * @return a String lookupPath or a {@code RequestPath}
	 * @throws IllegalArgumentException if neither is available
	 */
	public static Object getCachedPath(ServletRequest request) {

		// The RequestPath is pre-parsed if any HandlerMapping uses PathPatterns.
		// The lookupPath is re-resolved or cleared per HandlerMapping.
		// So check for lookupPath first.

		String lookupPath = (String) request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE);
		if (lookupPath != null) {
			return lookupPath;
		}
		RequestPath requestPath = (RequestPath) request.getAttribute(PATH_ATTRIBUTE);
		if (requestPath != null) {
			return requestPath.pathWithinApplication();
		}
		throw new IllegalArgumentException(
				"Neither a pre-parsed RequestPath nor a pre-resolved String lookupPath is available.");
	}

	/**
	 * Variant of {@link #getCachedPath(ServletRequest)} that returns the path
	 * for request mapping as a String.
	 * <p>If the cached path is a {@link #parseAndCache(HttpServletRequest)
	 * pre-parsed} {@code RequestPath} then the returned String path value is
	 * encoded and with path parameters removed.
	 * <p>If the cached path is a {@link UrlPathHelper#resolveAndCacheLookupPath
	 * pre-resolved} String lookupPath, then the returned String path value
	 * depends on how {@link UrlPathHelper} that resolved is configured.
	 * @param request the current request
	 * @return the full request mapping path as a String
	 */
	public static String getCachedPathValue(ServletRequest request) {
		Object path = getCachedPath(request);
		if (path instanceof PathContainer) {
			String value = ((PathContainer) path).value();
			path = UrlPathHelper.defaultInstance.removeSemicolonContent(value);
		}
		return (String) path;
	}

	/**
	 * Check for a previously {@link UrlPathHelper#resolveAndCacheLookupPath
	 * resolved} String lookupPath or a previously {@link #parseAndCache parsed}
	 * {@code RequestPath}.
	 * @param request the current request
	 * @return whether a pre-resolved or pre-parsed path is available
	 */
	public static boolean hasCachedPath(ServletRequest request) {
		return (request.getAttribute(PATH_ATTRIBUTE) != null ||
				request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE) != null);
	}

}
