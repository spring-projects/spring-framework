/*
 * Copyright 2002-2015 the original author or authors.
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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.http.CacheControl;
import org.springframework.web.context.support.WebApplicationObjectSupport;

/**
 * Convenient superclass for any kind of web content generator,
 * like {@link org.springframework.web.servlet.mvc.AbstractController}
 * and {@link org.springframework.web.servlet.mvc.WebContentInterceptor}.
 * Can also be used for custom handlers that have their own
 * {@link org.springframework.web.servlet.HandlerAdapter}.
 *
 * <p>Supports HTTP cache control options. The usage of corresponding
 * HTTP headers can be controlled via the "setCacheSeconds" or "setCacheControl" properties.
 * As of 4.2, its default behavior changed when using only {@link #setCacheSeconds(int)}, sending
 * HTTP response headers that are more in line with current browsers and proxies implementations.
 *
 * <p>Reverting to the previous behavior can be easily done by using one of the nealy deprecated methods
 * {@link #setUseExpiresHeader}, {@link #setUseCacheControlHeader}, {@link #setUseCacheControlNoStore} or
 * {@link #setAlwaysMustRevalidate}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Brian Clozel
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
	private Set<String> supportedMethods;

	private boolean requireSession = false;

	private int cacheSeconds = -1;

	/** Use HTTP 1.0 expires header? */
	private boolean useExpiresHeader = true;

	/** Use HTTP 1.1 cache-control header? */
	private boolean useCacheControlHeader = true;

	/** Use HTTP 1.1 cache-control header value "no-store"? */
	private boolean useCacheControlNoStore = true;

	private boolean alwaysMustRevalidate = false;

	private boolean usePreviousHttpCachingBehavior = false;

	private final SimpleDateFormat dateFormat;

	private CacheControl cacheControl;


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
		dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	/**
	 * Create a new WebContentGenerator.
	 * @param supportedMethods the supported HTTP methods for this content generator
	 */
	public WebContentGenerator(String... supportedMethods) {
		this.supportedMethods = new HashSet<String>(Arrays.asList(supportedMethods));
		dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}


	/**
	 * Set the HTTP methods that this content generator should support.
	 * <p>Default is GET, HEAD and POST for simple form controller types;
	 * unrestricted for general controllers and interceptors.
	 */
	public final void setSupportedMethods(String... methods) {
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
	 * Set the {@link org.springframework.http.CacheControl} instance to build
	 * the Cache-Control HTTP response header.
	 *
	 * @since 4.2
	 */
	public void setCacheControl(CacheControl cacheControl) {
		this.cacheControl = cacheControl;
	}

	/**
	 * Get the {@link org.springframework.http.CacheControl} instance
	 * that builds the Cache-Control HTTP response header.
	 *
	 * @since 4.2
	 */
	public CacheControl getCacheControl() {
		return cacheControl;
	}

	/**
	 * Set whether to use the HTTP 1.0 expires header. Default is "false".
	 * <p>Note: Cache headers will only get applied if caching is enabled
	 * (or explicitly prevented) for the current request.
	 *
	 * @deprecated in favor of {@link #setCacheSeconds} or {@link #setCacheControl}.
	 */
	@Deprecated
	public final void setUseExpiresHeader(boolean useExpiresHeader) {
		this.useExpiresHeader = useExpiresHeader;
		this.usePreviousHttpCachingBehavior = true;
	}

	/**
	 * Return whether the HTTP 1.0 expires header is used.
	 */
	@Deprecated
	public final boolean isUseExpiresHeader() {
		return this.useExpiresHeader;
	}

	/**
	 * Set whether to use the HTTP 1.1 cache-control header. Default is "true".
	 * <p>Note: Cache headers will only get applied if caching is enabled
	 * (or explicitly prevented) for the current request.
	 *
	 * @deprecated in favor of {@link #setCacheSeconds} or {@link #setCacheControl}.
	 */
	@Deprecated
	public final void setUseCacheControlHeader(boolean useCacheControlHeader) {
		this.useCacheControlHeader = useCacheControlHeader;
		this.usePreviousHttpCachingBehavior = true;
	}

	/**
	 * Return whether the HTTP 1.1 cache-control header is used.
	 */
	@Deprecated
	public final boolean isUseCacheControlHeader() {
		return this.useCacheControlHeader;
	}

	/**
	 * Set whether to use the HTTP 1.1 cache-control header value "no-store"
	 * when preventing caching. Default is "true".
	 *
	 * @deprecated in favor of {@link #setCacheSeconds} or {@link #setCacheControl}.
	 */
	@Deprecated
	public final void setUseCacheControlNoStore(boolean useCacheControlNoStore) {
		this.useCacheControlNoStore = useCacheControlNoStore;
		this.usePreviousHttpCachingBehavior = true;
	}

	/**
	 * Return whether the HTTP 1.1 cache-control header value "no-store" is used.
	 */
	@Deprecated
	public final boolean isUseCacheControlNoStore() {
		return this.useCacheControlNoStore;
	}

	/**
	 * An option to add 'must-revalidate' to every Cache-Control header. This
	 * may be useful with annotated controller methods, which can
	 * programmatically do a lastModified calculation as described in
	 * {@link WebRequest#checkNotModified(long)}. Default is "false".
	 *
	 * @deprecated in favor of {@link #setCacheSeconds} or {@link #setCacheControl}.
	 */
	@Deprecated
	public void setAlwaysMustRevalidate(boolean mustRevalidate) {
		this.alwaysMustRevalidate = mustRevalidate;
		this.usePreviousHttpCachingBehavior = true;
	}

	/**
	 * Return whether 'must-revalidate' is added to every Cache-Control header.
	 */
	@Deprecated
	public boolean isAlwaysMustRevalidate() {
		return alwaysMustRevalidate;
	}

	/**
	 * Cache content for the given number of seconds, by writing
	 * cache-related HTTP headers to the response:
	 * <ul>
	 *     <li>seconds == -1 (default value): no generation cache-related headers</li>
	 *     <li>seconds == 0: "Cache-Control: no-store" will prevent caching</li>
	 *     <li>seconds > 0: "Cache-Control: max-age=seconds" will ask to cache content</li>
	 * </ul>
	 * <p>For more specific needs, a custom {@link org.springframework.http.CacheControl} should be used.
	 *
	 * @see #setCacheControl
	 */
	public final void setCacheSeconds(int seconds) {
		this.cacheSeconds = seconds;
		if (!this.usePreviousHttpCachingBehavior) {
			if (cacheSeconds > 0) {
				this.cacheControl = CacheControl.maxAge(seconds, TimeUnit.SECONDS);
			}
			else if (cacheSeconds == 0) {
				this.cacheControl = CacheControl.noStore();
			}
		}
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
	 * @throws ServletException if the request cannot be handled because a check failed
	 */
	protected final void checkAndPrepare(
			HttpServletRequest request, HttpServletResponse response)
			throws ServletException {

		checkAndPrepare(request, response, this.cacheControl);
	}

	@Deprecated
	protected final void checkAndPrepare(
			HttpServletRequest request, HttpServletResponse response, boolean lastModified)
			throws ServletException {

		if (lastModified) {
			checkAndPrepare(request, response, this.cacheControl.mustRevalidate());
		}
		else {
			checkAndPrepare(request, response);
		}
	}

	protected final void checkAndPrepare(
			HttpServletRequest request, HttpServletResponse response, int cacheSeconds) throws ServletException {

		CacheControl cControl;
		if (cacheSeconds > 0) {
			cControl = CacheControl.maxAge(cacheSeconds, TimeUnit.SECONDS);
		}
		else if (cacheSeconds == 0) {
			cControl = CacheControl.noStore();
		}
		else {
			cControl = CacheControl.empty();
		}
		checkRequest(request);
		if (this.usePreviousHttpCachingBehavior) {
			addHttp10CacheHeaders(cacheSeconds, response);
		}
		else {
			String ccValue = cControl.getHeaderValue();
			if (ccValue != null) {
				response.setHeader(HEADER_CACHE_CONTROL, ccValue);
			}
		}
	}

	protected final void checkRequest(HttpServletRequest request) throws ServletException {
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
	}

	/**
	 * Check and prepare the given request and response according to the settings
	 * of this generator. Checks for supported methods and a required session
	 * specified for this generator. Also applies the {@link org.springframework.http.CacheControl}
	 * given as a parameter.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param cacheControl the {@link org.springframework.http.CacheControl} to use
	 * @throws ServletException if the request cannot be handled because a check failed
	 *
	 * @since 4.2
	 */
	protected final void checkAndPrepare(
			HttpServletRequest request, HttpServletResponse response, CacheControl cacheControl)
			throws ServletException {

		checkRequest(request);

		if (this.usePreviousHttpCachingBehavior) {
			addHttp10CacheHeaders(this.cacheSeconds, response);
		}
		else if (cacheControl != null) {
			String ccValue = cacheControl.getHeaderValue();
			if (ccValue != null) {
				response.setHeader(HEADER_CACHE_CONTROL, ccValue);
			}
		}
	}

	protected final void addHttp10CacheHeaders(int cacheSeconds, HttpServletResponse response) {
		if (cacheSeconds > 0) {
			cacheForSeconds(response, cacheSeconds, this.alwaysMustRevalidate);
		}
		else if (cacheSeconds == 0) {
			preventCaching(response);
		}
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
			response.setHeader(HEADER_EXPIRES, dateFormat.format(System.currentTimeMillis() + seconds * 1000L));
		}
		if (this.useCacheControlHeader) {
			// HTTP 1.1 header
			String headerValue = "max-age=" + seconds;
			if (mustRevalidate) {
				headerValue += ", must-revalidate";
			}
			response.setHeader(HEADER_CACHE_CONTROL, headerValue);
		}
	}

	/**
	 * Prevent the response from being cached.
	 * See {@code http://www.mnot.net/cache_docs}.
	 */
	protected final void preventCaching(HttpServletResponse response) {
		response.setHeader(HEADER_PRAGMA, "no-cache");
		if (this.useExpiresHeader) {
			// HTTP 1.0 header
			response.setHeader(HEADER_EXPIRES, dateFormat.format(System.currentTimeMillis()));
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

}
