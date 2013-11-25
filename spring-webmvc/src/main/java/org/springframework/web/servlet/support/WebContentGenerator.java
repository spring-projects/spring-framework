/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.servlet.support;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.mvc.LastModified;

/**
 * Convenient superclass for any kind of web content generator,
 * like {@link org.springframework.web.servlet.mvc.AbstractController}
 * and {@link org.springframework.web.servlet.mvc.WebContentInterceptor}.
 * Can also be used for custom handlers that have their own
 * {@link org.springframework.web.servlet.HandlerAdapter}.
 *
 * <p>Supports HTTP cache control options. The usage of corresponding
 * HTTP headers can be controlled via the "useExpiresHeader",
 * "useCacheControlHeader" and "useCacheControlNoStore" properties.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setCacheSeconds
 * @see #setRequireSession
 */
public abstract class WebContentGenerator extends WebApplicationObjectSupport {

	/** HTTP method "GET" */
	public static final String METHOD_GET = "GET";

	/** HTTP method "HEAD" */
	public static final String METHOD_HEAD = "HEAD";

	/** HTTP method "POST" */
	public static final String METHOD_POST = "POST";


	private static final String HEADER_PRAGMA = "Pragma";

	private static final String HEADER_EXPIRES = "Expires";

	private static final String HEADER_CACHE_CONTROL = "Cache-Control";


	/** Set of supported HTTP methods */
	private Set<String>	supportedMethods;

	private boolean requireSession = false;

	/** Use HTTP 1.0 expires header? */
	private boolean useExpiresHeader = true;

	/** Use HTTP 1.1 cache-control header? */
	private boolean useCacheControlHeader = true;

	/** Use HTTP 1.1 cache-control header value "no-store"? */
	private boolean useCacheControlNoStore = true;

	private int cacheSeconds = -1;

	private boolean alwaysMustRevalidate = false;


	/**
	 * Create a new WebContentGenerator which supports
	 * HTTP methods GET, HEAD and POST by default.
	 */
	public WebContentGenerator() {
		this(true);
	}

	/**
	 * Create a new WebContentGenerator.
	 * @param restrictDefaultSupportedMethods {@code true} if this
	 * generator should support HTTP methods GET, HEAD and POST by default,
	 * or {@code false} if it should be unrestricted
	 */
	public WebContentGenerator(boolean restrictDefaultSupportedMethods) {
		if (restrictDefaultSupportedMethods) {
			this.supportedMethods = new HashSet<String>(4);
			this.supportedMethods.add(METHOD_GET);
			this.supportedMethods.add(METHOD_HEAD);
			this.supportedMethods.add(METHOD_POST);
		}
	}

	/**
	 * Create a new WebContentGenerator.
	 * @param supportedMethods the supported HTTP methods for this content generator
	 */
	public WebContentGenerator(String... supportedMethods) {
		this.supportedMethods = new HashSet<String>(Arrays.asList(supportedMethods));
	}


	/**
	 * Set the HTTP methods that this content generator should support.
	 * <p>Default is GET, HEAD and POST for simple form controller types;
	 * unrestricted for general controllers and interceptors.
	 */
	public final void setSupportedMethods(String[] methods) {
		if (methods != null) {
			this.supportedMethods = new HashSet<String>(Arrays.asList(methods));
		}
		else {
			this.supportedMethods = null;
		}
	}

	/**
	 * Return the HTTP methods that this content generator supports.
	 */
	public final String[] getSupportedMethods() {
		return StringUtils.toStringArray(this.supportedMethods);
	}

	/**
	 * Set whether a session should be required to handle requests.
	 */
	public final void setRequireSession(boolean requireSession) {
		this.requireSession = requireSession;
	}

	/**
	 * Return whether a session is required to handle requests.
	 */
	public final boolean isRequireSession() {
		return this.requireSession;
	}

	/**
	 * Set whether to use the HTTP 1.0 expires header. Default is "true".
	 * <p>Note: Cache headers will only get applied if caching is enabled
	 * (or explicitly prevented) for the current request.
	 */
	public final void setUseExpiresHeader(boolean useExpiresHeader) {
		this.useExpiresHeader = useExpiresHeader;
	}

	/**
	 * Return whether the HTTP 1.0 expires header is used.
	 */
	public final boolean isUseExpiresHeader() {
		return this.useExpiresHeader;
	}

	/**
	 * Set whether to use the HTTP 1.1 cache-control header. Default is "true".
	 * <p>Note: Cache headers will only get applied if caching is enabled
	 * (or explicitly prevented) for the current request.
	 */
	public final void setUseCacheControlHeader(boolean useCacheControlHeader) {
		this.useCacheControlHeader = useCacheControlHeader;
	}

	/**
	 * Return whether the HTTP 1.1 cache-control header is used.
	 */
	public final boolean isUseCacheControlHeader() {
		return this.useCacheControlHeader;
	}

	/**
	 * Set whether to use the HTTP 1.1 cache-control header value "no-store"
	 * when preventing caching. Default is "true".
	 */
	public final void setUseCacheControlNoStore(boolean useCacheControlNoStore) {
		this.useCacheControlNoStore = useCacheControlNoStore;
	}

	/**
	 * Return whether the HTTP 1.1 cache-control header value "no-store" is used.
	 */
	public final boolean isUseCacheControlNoStore() {
		return this.useCacheControlNoStore;
	}

	/**
	 * An option to add 'must-revalidate' to every Cache-Control header. This
	 * may be useful with annotated controller methods, which can
	 * programmatically do a lastModified calculation as described in
	 * {@link WebRequest#checkNotModified(long)}. Default is "false",
	 * effectively relying on whether the handler implements
	 * {@link LastModified} or not.
	 */
	public void setAlwaysMustRevalidate(boolean mustRevalidate) {
		this.alwaysMustRevalidate = mustRevalidate;
	}

	/**
	 * Return whether 'must-revalidate' is added to every Cache-Control header.
	 */
	public boolean isAlwaysMustRevalidate() {
		return alwaysMustRevalidate;
	}

	/**
	 * Cache content for the given number of seconds. Default is -1,
	 * indicating no generation of cache-related headers.
	 * <p>Only if this is set to 0 (no cache) or a positive value (cache for
	 * this many seconds) will this class generate cache headers.
	 * <p>The headers can be overwritten by subclasses, before content is generated.
	 */
	public final void setCacheSeconds(int seconds) {
		this.cacheSeconds = seconds;
	}

	/**
	 * Return the number of seconds that content is cached.
	 */
	public final int getCacheSeconds() {
		return this.cacheSeconds;
	}


	/**
	 * Check and prepare the given request and response according to the settings
	 * of this generator. Checks for supported methods and a required session,
	 * and applies the number of cache seconds specified for this generator.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param lastModified if the mapped handler provides Last-Modified support
	 * @throws ServletException if the request cannot be handled because a check failed
	 */
	protected final void checkAndPrepare(
			HttpServletRequest request, HttpServletResponse response, boolean lastModified)
			throws ServletException {

		checkAndPrepare(request, response, this.cacheSeconds, lastModified);
	}

	/**
	 * Check and prepare the given request and response according to the settings
	 * of this generator. Checks for supported methods and a required session,
	 * and applies the given number of cache seconds.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param cacheSeconds positive number of seconds into the future that the
	 * response should be cacheable for, 0 to prevent caching
	 * @param lastModified if the mapped handler provides Last-Modified support
	 * @throws ServletException if the request cannot be handled because a check failed
	 */
	protected final void checkAndPrepare(
			HttpServletRequest request, HttpServletResponse response, int cacheSeconds, boolean lastModified)
			throws ServletException {

		// Check whether we should support the request method.
		String method = request.getMethod();
		if (this.supportedMethods != null && !this.supportedMethods.contains(method)) {
			throw new HttpRequestMethodNotSupportedException(
					method, StringUtils.toStringArray(this.supportedMethods));
		}

		// Check whether a session is required.
		if (this.requireSession) {
			if (request.getSession(false) == null) {
				throw new HttpSessionRequiredException("Pre-existing session required but none found");
			}
		}

		// Do declarative cache control.
		// Revalidate if the controller supports last-modified.
		applyCacheSeconds(response, cacheSeconds, lastModified);
	}

	/**
	 * Prevent the response from being cached.
	 * See {@code http://www.mnot.net/cache_docs}.
	 */
	protected final void preventCaching(HttpServletResponse response) {
		response.setHeader(HEADER_PRAGMA, "no-cache");
		if (this.useExpiresHeader) {
			// HTTP 1.0 header
			response.setDateHeader(HEADER_EXPIRES, 1L);
		}
		if (this.useCacheControlHeader) {
			// HTTP 1.1 header: "no-cache" is the standard value,
			// "no-store" is necessary to prevent caching on FireFox.
			response.setHeader(HEADER_CACHE_CONTROL, "no-cache");
			if (this.useCacheControlNoStore) {
				response.addHeader(HEADER_CACHE_CONTROL, "no-store");
			}
		}
	}

	/**
	 * Set HTTP headers to allow caching for the given number of seconds.
	 * Does not tell the browser to revalidate the resource.
	 * @param response current HTTP response
	 * @param seconds number of seconds into the future that the response
	 * should be cacheable for
	 * @see #cacheForSeconds(javax.servlet.http.HttpServletResponse, int, boolean)
	 */
	protected final void cacheForSeconds(HttpServletResponse response, int seconds) {
		cacheForSeconds(response, seconds, false);
	}

	/**
	 * Set HTTP headers to allow caching for the given number of seconds.
	 * Tells the browser to revalidate the resource if mustRevalidate is
	 * {@code true}.
	 * @param response the current HTTP response
	 * @param seconds number of seconds into the future that the response
	 * should be cacheable for
	 * @param mustRevalidate whether the client should revalidate the resource
	 * (typically only necessary for controllers with last-modified support)
	 */
	protected final void cacheForSeconds(HttpServletResponse response, int seconds, boolean mustRevalidate) {
		if (this.useExpiresHeader) {
			// HTTP 1.0 header
			response.setDateHeader(HEADER_EXPIRES, System.currentTimeMillis() + seconds * 1000L);
		}
		if (this.useCacheControlHeader) {
			// HTTP 1.1 header
			String headerValue = "max-age=" + seconds;
			if (mustRevalidate || this.alwaysMustRevalidate) {
				headerValue += ", must-revalidate";
			}
			response.setHeader(HEADER_CACHE_CONTROL, headerValue);
		}
	}

	/**
	 * Apply the given cache seconds and generate corresponding HTTP headers,
	 * i.e. allow caching for the given number of seconds in case of a positive
	 * value, prevent caching if given a 0 value, do nothing else.
	 * Does not tell the browser to revalidate the resource.
	 * @param response current HTTP response
	 * @param seconds positive number of seconds into the future that the
	 * response should be cacheable for, 0 to prevent caching
	 * @see #cacheForSeconds(javax.servlet.http.HttpServletResponse, int, boolean)
	 */
	protected final void applyCacheSeconds(HttpServletResponse response, int seconds) {
		applyCacheSeconds(response, seconds, false);
	}

	/**
	 * Apply the given cache seconds and generate respective HTTP headers.
	 * <p>That is, allow caching for the given number of seconds in the
	 * case of a positive value, prevent caching if given a 0 value, else
	 * do nothing (i.e. leave caching to the client).
	 * @param response the current HTTP response
	 * @param seconds the (positive) number of seconds into the future that
	 * the response should be cacheable for; 0 to prevent caching; and
	 * a negative value to leave caching to the client.
	 * @param mustRevalidate whether the client should revalidate the resource
	 * (typically only necessary for controllers with last-modified support)
	 */
	protected final void applyCacheSeconds(HttpServletResponse response, int seconds, boolean mustRevalidate) {
		if (seconds > 0) {
			cacheForSeconds(response, seconds, mustRevalidate);
		}
		else if (seconds == 0) {
			preventCaching(response);
		}
		// Leave caching to the client otherwise.
	}

}
