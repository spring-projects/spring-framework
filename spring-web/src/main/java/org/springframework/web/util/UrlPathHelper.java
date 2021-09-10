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

package org.springframework.web.util;

import java.net.URLDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.MappingMatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Helper class for URL path matching. Provides support for URL paths in
 * {@code RequestDispatcher} includes and support for consistent URL decoding.
 *
 * <p>Used by {@link org.springframework.web.servlet.handler.AbstractUrlHandlerMapping}
 * and {@link org.springframework.web.servlet.support.RequestContext} for path matching
 * and/or URI determination.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rossen Stoyanchev
 * @since 14.01.2004
 * @see #getLookupPathForRequest
 * @see javax.servlet.RequestDispatcher
 */
public class UrlPathHelper {

	/**
	 * Name of Servlet request attribute that holds a
	 * {@link #getLookupPathForRequest resolved} lookupPath.
	 * @since 5.3
	 */
	public static final String PATH_ATTRIBUTE = UrlPathHelper.class.getName() + ".PATH";

	static final boolean servlet4Present =
			ClassUtils.hasMethod(HttpServletRequest.class, "getHttpServletMapping");

	/**
	 * Special WebSphere request attribute, indicating the original request URI.
	 * Preferable over the standard Servlet 2.4 forward attribute on WebSphere,
	 * simply because we need the very first URI in the request forwarding chain.
	 */
	private static final String WEBSPHERE_URI_ATTRIBUTE = "com.ibm.websphere.servlet.uri_non_decoded";

	private static final Log logger = LogFactory.getLog(UrlPathHelper.class);

	@Nullable
	static volatile Boolean websphereComplianceFlag;


	private boolean alwaysUseFullPath = false;

	private boolean urlDecode = true;

	private boolean removeSemicolonContent = true;

	private String defaultEncoding = WebUtils.DEFAULT_CHARACTER_ENCODING;

	private boolean readOnly = false;


	/**
	 * Whether URL lookups should always use the full path within the current
	 * web application context, i.e. within
	 * {@link javax.servlet.ServletContext#getContextPath()}.
	 * <p>If set to {@literal false} the path within the current servlet mapping
	 * is used instead if applicable (i.e. in the case of a prefix based Servlet
	 * mapping such as "/myServlet/*").
	 * <p>By default this is set to "false".
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		checkReadOnly();
		this.alwaysUseFullPath = alwaysUseFullPath;
	}

	/**
	 * Whether the context path and request URI should be decoded -- both of
	 * which are returned <i>undecoded</i> by the Servlet API, in contrast to
	 * the servlet path.
	 * <p>Either the request encoding or the default Servlet spec encoding
	 * (ISO-8859-1) is used when set to "true".
	 * <p>By default this is set to {@literal true}.
	 * <p><strong>Note:</strong> Be aware the servlet path will not match when
	 * compared to encoded paths. Therefore use of {@code urlDecode=false} is
	 * not compatible with a prefix-based Servlet mapping and likewise implies
	 * also setting {@code alwaysUseFullPath=true}.
	 * @see #getServletPath
	 * @see #getContextPath
	 * @see #getRequestUri
	 * @see WebUtils#DEFAULT_CHARACTER_ENCODING
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public void setUrlDecode(boolean urlDecode) {
		checkReadOnly();
		this.urlDecode = urlDecode;
	}

	/**
	 * Whether to decode the request URI when determining the lookup path.
	 * @since 4.3.13
	 */
	public boolean isUrlDecode() {
		return this.urlDecode;
	}

	/**
	 * Set if ";" (semicolon) content should be stripped from the request URI.
	 * <p>Default is "true".
	 */
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		checkReadOnly();
		this.removeSemicolonContent = removeSemicolonContent;
	}

	/**
	 * Whether configured to remove ";" (semicolon) content from the request URI.
	 */
	public boolean shouldRemoveSemicolonContent() {
		return this.removeSemicolonContent;
	}

	/**
	 * Set the default character encoding to use for URL decoding.
	 * Default is ISO-8859-1, according to the Servlet spec.
	 * <p>If the request specifies a character encoding itself, the request
	 * encoding will override this setting. This also allows for generically
	 * overriding the character encoding in a filter that invokes the
	 * {@code ServletRequest.setCharacterEncoding} method.
	 * @param defaultEncoding the character encoding to use
	 * @see #determineEncoding
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 * @see javax.servlet.ServletRequest#setCharacterEncoding(String)
	 * @see WebUtils#DEFAULT_CHARACTER_ENCODING
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		checkReadOnly();
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * Return the default character encoding to use for URL decoding.
	 */
	protected String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * Switch to read-only mode where further configuration changes are not allowed.
	 */
	private void setReadOnly() {
		this.readOnly = true;
	}

	private void checkReadOnly() {
		Assert.isTrue(!this.readOnly, "This instance cannot be modified");
	}


	/**
	 * {@link #getLookupPathForRequest Resolve} the lookupPath and cache it in a
	 * request attribute with the key {@link #PATH_ATTRIBUTE} for subsequent
	 * access via {@link #getResolvedLookupPath(ServletRequest)}.
	 * @param request the current request
	 * @return the resolved path
	 * @since 5.3
	 */
	public String resolveAndCacheLookupPath(HttpServletRequest request) {
		String lookupPath = getLookupPathForRequest(request);
		request.setAttribute(PATH_ATTRIBUTE, lookupPath);
		return lookupPath;
	}

	/**
	 * Return a previously {@link #getLookupPathForRequest resolved} lookupPath.
	 * @param request the current request
	 * @return the previously resolved lookupPath
	 * @throws IllegalArgumentException if the not found
	 * @since 5.3
	 */
	public static String getResolvedLookupPath(ServletRequest request) {
		String lookupPath = (String) request.getAttribute(PATH_ATTRIBUTE);
		Assert.notNull(lookupPath, "Expected lookupPath in request attribute \"" + PATH_ATTRIBUTE + "\".");
		return lookupPath;
	}

	/**
	 * Variant of {@link #getLookupPathForRequest(HttpServletRequest)} that
	 * automates checking for a previously computed lookupPath saved as a
	 * request attribute. The attribute is only used for lookup purposes.
	 * @param request current HTTP request
	 * @param name the request attribute that holds the lookupPath
	 * @return the lookup path
	 * @since 5.2
	 * @deprecated as of 5.3 in favor of using
	 * {@link #resolveAndCacheLookupPath(HttpServletRequest)} and
	 * {@link #getResolvedLookupPath(ServletRequest)}.
	 */
	@Deprecated
	public String getLookupPathForRequest(HttpServletRequest request, @Nullable String name) {
		String result = null;
		if (name != null) {
			result = (String) request.getAttribute(name);
		}
		return (result != null ? result : getLookupPathForRequest(request));
	}

	/**
	 * Return the mapping lookup path for the given request, within the current
	 * servlet mapping if applicable, else within the web application.
	 * <p>Detects include request URL if called within a RequestDispatcher include.
	 * @param request current HTTP request
	 * @return the lookup path
	 * @see #getPathWithinServletMapping
	 * @see #getPathWithinApplication
	 */
	public String getLookupPathForRequest(HttpServletRequest request) {
		String pathWithinApp = getPathWithinApplication(request);
		// Always use full path within current servlet context?
		if (this.alwaysUseFullPath || skipServletPathDetermination(request)) {
			return pathWithinApp;
		}
		// Else, use path within current servlet mapping if applicable
		String rest = getPathWithinServletMapping(request, pathWithinApp);
		if (StringUtils.hasLength(rest)) {
			return rest;
		}
		else {
			return pathWithinApp;
		}
	}

	/**
	 * Check whether servlet path determination can be skipped for the given request.
	 * @param request current HTTP request
	 * @return {@code true} if the request mapping has not been achieved using a path
	 * or if the servlet has been mapped to root; {@code false} otherwise
	 */
	private boolean skipServletPathDetermination(HttpServletRequest request) {
		if (servlet4Present) {
			return Servlet4Delegate.skipServletPathDetermination(request);
		}
		return false;
	}

	/**
	 * Return the path within the servlet mapping for the given request,
	 * i.e. the part of the request's URL beyond the part that called the servlet,
	 * or "" if the whole URL has been used to identify the servlet.
	 * @param request current HTTP request
	 * @return the path within the servlet mapping, or ""
	 * @see #getPathWithinServletMapping(HttpServletRequest, String)
	 */
	public String getPathWithinServletMapping(HttpServletRequest request) {
		return getPathWithinServletMapping(request, getPathWithinApplication(request));
	}

	/**
	 * Return the path within the servlet mapping for the given request,
	 * i.e. the part of the request's URL beyond the part that called the servlet,
	 * or "" if the whole URL has been used to identify the servlet.
	 * <p>Detects include request URL if called within a RequestDispatcher include.
	 * <p>E.g.: servlet mapping = "/*"; request URI = "/test/a" -> "/test/a".
	 * <p>E.g.: servlet mapping = "/"; request URI = "/test/a" -> "/test/a".
	 * <p>E.g.: servlet mapping = "/test/*"; request URI = "/test/a" -> "/a".
	 * <p>E.g.: servlet mapping = "/test"; request URI = "/test" -> "".
	 * <p>E.g.: servlet mapping = "/*.test"; request URI = "/a.test" -> "".
	 * @param request current HTTP request
	 * @param pathWithinApp a precomputed path within the application
	 * @return the path within the servlet mapping, or ""
	 * @since 5.2.9
	 * @see #getLookupPathForRequest
	 */
	protected String getPathWithinServletMapping(HttpServletRequest request, String pathWithinApp) {
		String servletPath = getServletPath(request);
		String sanitizedPathWithinApp = getSanitizedPath(pathWithinApp);
		String path;

		// If the app container sanitized the servletPath, check against the sanitized version
		if (servletPath.contains(sanitizedPathWithinApp)) {
			path = getRemainingPath(sanitizedPathWithinApp, servletPath, false);
		}
		else {
			path = getRemainingPath(pathWithinApp, servletPath, false);
		}

		if (path != null) {
			// Normal case: URI contains servlet path.
			return path;
		}
		else {
			// Special case: URI is different from servlet path.
			String pathInfo = request.getPathInfo();
			if (pathInfo != null) {
				// Use path info if available. Indicates index page within a servlet mapping?
				// e.g. with index page: URI="/", servletPath="/index.html"
				return pathInfo;
			}
			if (!this.urlDecode) {
				// No path info... (not mapped by prefix, nor by extension, nor "/*")
				// For the default servlet mapping (i.e. "/"), urlDecode=false can
				// cause issues since getServletPath() returns a decoded path.
				// If decoding pathWithinApp yields a match just use pathWithinApp.
				path = getRemainingPath(decodeInternal(request, pathWithinApp), servletPath, false);
				if (path != null) {
					return pathWithinApp;
				}
			}
			// Otherwise, use the full servlet path.
			return servletPath;
		}
	}

	/**
	 * Return the path within the web application for the given request.
	 * <p>Detects include request URL if called within a RequestDispatcher include.
	 * @param request current HTTP request
	 * @return the path within the web application
	 * @see #getLookupPathForRequest
	 */
	public String getPathWithinApplication(HttpServletRequest request) {
		String contextPath = getContextPath(request);
		String requestUri = getRequestUri(request);
		String path = getRemainingPath(requestUri, contextPath, true);
		if (path != null) {
			// Normal case: URI contains context path.
			return (StringUtils.hasText(path) ? path : "/");
		}
		else {
			return requestUri;
		}
	}

	/**
	 * Match the given "mapping" to the start of the "requestUri" and if there
	 * is a match return the extra part. This method is needed because the
	 * context path and the servlet path returned by the HttpServletRequest are
	 * stripped of semicolon content unlike the requestUri.
	 */
	@Nullable
	private String getRemainingPath(String requestUri, String mapping, boolean ignoreCase) {
		int index1 = 0;
		int index2 = 0;
		for (; (index1 < requestUri.length()) && (index2 < mapping.length()); index1++, index2++) {
			char c1 = requestUri.charAt(index1);
			char c2 = mapping.charAt(index2);
			if (c1 == ';') {
				index1 = requestUri.indexOf('/', index1);
				if (index1 == -1) {
					return null;
				}
				c1 = requestUri.charAt(index1);
			}
			if (c1 == c2 || (ignoreCase && (Character.toLowerCase(c1) == Character.toLowerCase(c2)))) {
				continue;
			}
			return null;
		}
		if (index2 != mapping.length()) {
			return null;
		}
		else if (index1 == requestUri.length()) {
			return "";
		}
		else if (requestUri.charAt(index1) == ';') {
			index1 = requestUri.indexOf('/', index1);
		}
		return (index1 != -1 ? requestUri.substring(index1) : "");
	}

	/**
	 * Sanitize the given path. Uses the following rules:
	 * <ul>
	 * <li>replace all "//" by "/"</li>
	 * </ul>
	 */
	private static String getSanitizedPath(final String path) {
		int index = path.indexOf("//");
		if (index >= 0) {
			StringBuilder sanitized = new StringBuilder(path);
			while (index != -1) {
				sanitized.deleteCharAt(index);
				index = sanitized.indexOf("//", index);
			}
			return sanitized.toString();
		}
		return path;
	}

	/**
	 * Return the request URI for the given request, detecting an include request
	 * URL if called within a RequestDispatcher include.
	 * <p>As the value returned by {@code request.getRequestURI()} is <i>not</i>
	 * decoded by the servlet container, this method will decode it.
	 * <p>The URI that the web container resolves <i>should</i> be correct, but some
	 * containers like JBoss/Jetty incorrectly include ";" strings like ";jsessionid"
	 * in the URI. This method cuts off such incorrect appendices.
	 * @param request current HTTP request
	 * @return the request URI
	 */
	public String getRequestUri(HttpServletRequest request) {
		String uri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
		if (uri == null) {
			uri = request.getRequestURI();
		}
		return decodeAndCleanUriString(request, uri);
	}

	/**
	 * Return the context path for the given request, detecting an include request
	 * URL if called within a RequestDispatcher include.
	 * <p>As the value returned by {@code request.getContextPath()} is <i>not</i>
	 * decoded by the servlet container, this method will decode it.
	 * @param request current HTTP request
	 * @return the context path
	 */
	public String getContextPath(HttpServletRequest request) {
		String contextPath = (String) request.getAttribute(WebUtils.INCLUDE_CONTEXT_PATH_ATTRIBUTE);
		if (contextPath == null) {
			contextPath = request.getContextPath();
		}
		if (StringUtils.matchesCharacter(contextPath, '/')) {
			// Invalid case, but happens for includes on Jetty: silently adapt it.
			contextPath = "";
		}
		return decodeRequestString(request, contextPath);
	}

	/**
	 * Return the servlet path for the given request, regarding an include request
	 * URL if called within a RequestDispatcher include.
	 * <p>As the value returned by {@code request.getServletPath()} is already
	 * decoded by the servlet container, this method will not attempt to decode it.
	 * @param request current HTTP request
	 * @return the servlet path
	 */
	public String getServletPath(HttpServletRequest request) {
		String servletPath = (String) request.getAttribute(WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE);
		if (servletPath == null) {
			servletPath = request.getServletPath();
		}
		if (servletPath.length() > 1 && servletPath.endsWith("/") && shouldRemoveTrailingServletPathSlash(request)) {
			// On WebSphere, in non-compliant mode, for a "/foo/" case that would be "/foo"
			// on all other servlet containers: removing trailing slash, proceeding with
			// that remaining slash as final lookup path...
			servletPath = servletPath.substring(0, servletPath.length() - 1);
		}
		return servletPath;
	}


	/**
	 * Return the request URI for the given request. If this is a forwarded request,
	 * correctly resolves to the request URI of the original request.
	 */
	public String getOriginatingRequestUri(HttpServletRequest request) {
		String uri = (String) request.getAttribute(WEBSPHERE_URI_ATTRIBUTE);
		if (uri == null) {
			uri = (String) request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE);
			if (uri == null) {
				uri = request.getRequestURI();
			}
		}
		return decodeAndCleanUriString(request, uri);
	}

	/**
	 * Return the context path for the given request, detecting an include request
	 * URL if called within a RequestDispatcher include.
	 * <p>As the value returned by {@code request.getContextPath()} is <i>not</i>
	 * decoded by the servlet container, this method will decode it.
	 * @param request current HTTP request
	 * @return the context path
	 */
	public String getOriginatingContextPath(HttpServletRequest request) {
		String contextPath = (String) request.getAttribute(WebUtils.FORWARD_CONTEXT_PATH_ATTRIBUTE);
		if (contextPath == null) {
			contextPath = request.getContextPath();
		}
		return decodeRequestString(request, contextPath);
	}

	/**
	 * Return the servlet path for the given request, detecting an include request
	 * URL if called within a RequestDispatcher include.
	 * @param request current HTTP request
	 * @return the servlet path
	 */
	public String getOriginatingServletPath(HttpServletRequest request) {
		String servletPath = (String) request.getAttribute(WebUtils.FORWARD_SERVLET_PATH_ATTRIBUTE);
		if (servletPath == null) {
			servletPath = request.getServletPath();
		}
		return servletPath;
	}

	/**
	 * Return the query string part of the given request's URL. If this is a forwarded request,
	 * correctly resolves to the query string of the original request.
	 * @param request current HTTP request
	 * @return the query string
	 */
	public String getOriginatingQueryString(HttpServletRequest request) {
		if ((request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE) != null) ||
			(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE) != null)) {
			return (String) request.getAttribute(WebUtils.FORWARD_QUERY_STRING_ATTRIBUTE);
		}
		else {
			return request.getQueryString();
		}
	}

	/**
	 * Decode the supplied URI string and strips any extraneous portion after a ';'.
	 */
	private String decodeAndCleanUriString(HttpServletRequest request, String uri) {
		uri = removeSemicolonContent(uri);
		uri = decodeRequestString(request, uri);
		uri = getSanitizedPath(uri);
		return uri;
	}

	/**
	 * Decode the given source string with a URLDecoder. The encoding will be taken
	 * from the request, falling back to the default "ISO-8859-1".
	 * <p>The default implementation uses {@code URLDecoder.decode(input, enc)}.
	 * @param request current HTTP request
	 * @param source the String to decode
	 * @return the decoded String
	 * @see WebUtils#DEFAULT_CHARACTER_ENCODING
	 * @see javax.servlet.ServletRequest#getCharacterEncoding
	 * @see java.net.URLDecoder#decode(String, String)
	 * @see java.net.URLDecoder#decode(String)
	 */
	public String decodeRequestString(HttpServletRequest request, String source) {
		if (this.urlDecode) {
			return decodeInternal(request, source);
		}
		return source;
	}

	@SuppressWarnings("deprecation")
	private String decodeInternal(HttpServletRequest request, String source) {
		String enc = determineEncoding(request);
		try {
			return UriUtils.decode(source, enc);
		}
		catch (UnsupportedCharsetException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Could not decode request string [" + source + "] with encoding '" + enc +
						"': falling back to platform default encoding; exception message: " + ex.getMessage());
			}
			return URLDecoder.decode(source);
		}
	}

	/**
	 * Determine the encoding for the given request.
	 * Can be overridden in subclasses.
	 * <p>The default implementation checks the request encoding,
	 * falling back to the default encoding specified for this resolver.
	 * @param request current HTTP request
	 * @return the encoding for the request (never {@code null})
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 * @see #setDefaultEncoding
	 */
	protected String determineEncoding(HttpServletRequest request) {
		String enc = request.getCharacterEncoding();
		if (enc == null) {
			enc = getDefaultEncoding();
		}
		return enc;
	}

	/**
	 * Remove ";" (semicolon) content from the given request URI if the
	 * {@linkplain #setRemoveSemicolonContent removeSemicolonContent}
	 * property is set to "true". Note that "jsessionid" is always removed.
	 * @param requestUri the request URI string to remove ";" content from
	 * @return the updated URI string
	 */
	public String removeSemicolonContent(String requestUri) {
		return (this.removeSemicolonContent ?
				removeSemicolonContentInternal(requestUri) : removeJsessionid(requestUri));
	}

	private static String removeSemicolonContentInternal(String requestUri) {
		int semicolonIndex = requestUri.indexOf(';');
		if (semicolonIndex == -1) {
			return requestUri;
		}
		StringBuilder sb = new StringBuilder(requestUri);
		while (semicolonIndex != -1) {
			int slashIndex = sb.indexOf("/", semicolonIndex + 1);
			if (slashIndex == -1) {
				return sb.substring(0, semicolonIndex);
			}
			sb.delete(semicolonIndex, slashIndex);
			semicolonIndex = sb.indexOf(";", semicolonIndex);
		}
		return sb.toString();
	}

	private String removeJsessionid(String requestUri) {
		String key = ";jsessionid=";
		int index = requestUri.toLowerCase().indexOf(key);
		if (index == -1) {
			return requestUri;
		}
		String start = requestUri.substring(0, index);
		for (int i = index + key.length(); i < requestUri.length(); i++) {
			char c = requestUri.charAt(i);
			if (c == ';' || c == '/') {
				return start + requestUri.substring(i);
			}
		}
		return start;
	}

	/**
	 * Decode the given URI path variables via {@link #decodeRequestString} unless
	 * {@link #setUrlDecode} is set to {@code true} in which case it is assumed
	 * the URL path from which the variables were extracted is already decoded
	 * through a call to {@link #getLookupPathForRequest(HttpServletRequest)}.
	 * @param request current HTTP request
	 * @param vars the URI variables extracted from the URL path
	 * @return the same Map or a new Map instance
	 */
	public Map<String, String> decodePathVariables(HttpServletRequest request, Map<String, String> vars) {
		if (this.urlDecode) {
			return vars;
		}
		else {
			Map<String, String> decodedVars = CollectionUtils.newLinkedHashMap(vars.size());
			vars.forEach((key, value) -> decodedVars.put(key, decodeInternal(request, value)));
			return decodedVars;
		}
	}

	/**
	 * Decode the given matrix variables via {@link #decodeRequestString} unless
	 * {@link #setUrlDecode} is set to {@code true} in which case it is assumed
	 * the URL path from which the variables were extracted is already decoded
	 * through a call to {@link #getLookupPathForRequest(HttpServletRequest)}.
	 * @param request current HTTP request
	 * @param vars the URI variables extracted from the URL path
	 * @return the same Map or a new Map instance
	 */
	public MultiValueMap<String, String> decodeMatrixVariables(
			HttpServletRequest request, MultiValueMap<String, String> vars) {

		if (this.urlDecode) {
			return vars;
		}
		else {
			MultiValueMap<String, String> decodedVars = new LinkedMultiValueMap<>(vars.size());
			vars.forEach((key, values) -> {
				for (String value : values) {
					decodedVars.add(key, decodeInternal(request, value));
				}
			});
			return decodedVars;
		}
	}

	private boolean shouldRemoveTrailingServletPathSlash(HttpServletRequest request) {
		if (request.getAttribute(WEBSPHERE_URI_ATTRIBUTE) == null) {
			// Regular servlet container: behaves as expected in any case,
			// so the trailing slash is the result of a "/" url-pattern mapping.
			// Don't remove that slash.
			return false;
		}
		Boolean flagToUse = websphereComplianceFlag;
		if (flagToUse == null) {
			ClassLoader classLoader = UrlPathHelper.class.getClassLoader();
			String className = "com.ibm.ws.webcontainer.WebContainer";
			String methodName = "getWebContainerProperties";
			String propName = "com.ibm.ws.webcontainer.removetrailingservletpathslash";
			boolean flag = false;
			try {
				Class<?> cl = classLoader.loadClass(className);
				Properties prop = (Properties) cl.getMethod(methodName).invoke(null);
				flag = Boolean.parseBoolean(prop.getProperty(propName));
			}
			catch (Throwable ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not introspect WebSphere web container properties: " + ex);
				}
			}
			flagToUse = flag;
			websphereComplianceFlag = flag;
		}
		// Don't bother if WebSphere is configured to be fully Servlet compliant.
		// However, if it is not compliant, do remove the improper trailing slash!
		return !flagToUse;
	}


	/**
	 * Shared, read-only instance with defaults. The following apply:
	 * <ul>
	 * <li>{@code alwaysUseFullPath=false}
	 * <li>{@code urlDecode=true}
	 * <li>{@code removeSemicolon=true}
	 * <li>{@code defaultEncoding=}{@link WebUtils#DEFAULT_CHARACTER_ENCODING}
	 * </ul>
	 */
	public static final UrlPathHelper defaultInstance = new UrlPathHelper();

	static {
		defaultInstance.setReadOnly();
	}


	/**
	 * Shared, read-only instance for the full, encoded path. The following apply:
	 * <ul>
	 * <li>{@code alwaysUseFullPath=true}
	 * <li>{@code urlDecode=false}
	 * <li>{@code removeSemicolon=false}
	 * <li>{@code defaultEncoding=}{@link WebUtils#DEFAULT_CHARACTER_ENCODING}
	 * </ul>
	 */
	public static final UrlPathHelper rawPathInstance = new UrlPathHelper() {

		@Override
		public String removeSemicolonContent(String requestUri) {
			return requestUri;
		}
	};

	static {
		rawPathInstance.setAlwaysUseFullPath(true);
		rawPathInstance.setUrlDecode(false);
		rawPathInstance.setRemoveSemicolonContent(false);
		rawPathInstance.setReadOnly();
	}


	/**
	 * Inner class to avoid a hard dependency on Servlet 4 {@link HttpServletMapping}
	 * and {@link MappingMatch} at runtime.
	 */
	private static class Servlet4Delegate {

		public static boolean skipServletPathDetermination(HttpServletRequest request) {
			HttpServletMapping mapping = (HttpServletMapping) request.getAttribute(RequestDispatcher.INCLUDE_MAPPING);
			if (mapping == null) {
				mapping = request.getHttpServletMapping();
			}
			MappingMatch match = mapping.getMappingMatch();
			return (match != null && (!match.equals(MappingMatch.PATH) || mapping.getPattern().equals("/*")));
		}
	}

}
