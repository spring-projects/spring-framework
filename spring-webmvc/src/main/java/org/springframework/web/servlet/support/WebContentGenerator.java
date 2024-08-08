/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.servlet.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.context.support.WebApplicationObjectSupport;

/**
 * Convenient superclass for any kind of web content generator,
 * like {@link org.springframework.web.servlet.mvc.AbstractController}
 * and {@link org.springframework.web.servlet.mvc.WebContentInterceptor}.
 * Can also be used for custom handlers that have their own
 * {@link org.springframework.web.servlet.HandlerAdapter}.
 *
 * <p>Supports HTTP cache control options. The usage of corresponding HTTP
 * headers can be controlled via the {@link #setCacheSeconds "cacheSeconds"}
 * and {@link #setCacheControl "cacheControl"} properties.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see #setCacheSeconds
 * @see #setCacheControl
 * @see #setRequireSession
 */
public abstract class WebContentGenerator extends WebApplicationObjectSupport {

	/** HTTP method "GET". */
	public static final String METHOD_GET = "GET";

	/** HTTP method "HEAD". */
	public static final String METHOD_HEAD = "HEAD";

	/** HTTP method "POST". */
	public static final String METHOD_POST = "POST";

	protected static final String HEADER_CACHE_CONTROL = "Cache-Control";


	/** Set of supported HTTP methods. */
	@Nullable
	private Set<String> supportedMethods;

	@Nullable
	private String allowHeader;

	private boolean requireSession = false;

	@Nullable
	private CacheControl cacheControl;

	private int cacheSeconds = -1;

	@Nullable
	private String[] varyByRequestHeaders;


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
			this.supportedMethods = CollectionUtils.newLinkedHashSet(3);
			this.supportedMethods.add(METHOD_GET);
			this.supportedMethods.add(METHOD_HEAD);
			this.supportedMethods.add(METHOD_POST);
		}
		initAllowHeader();
	}

	/**
	 * Create a new WebContentGenerator.
	 * @param supportedMethods the supported HTTP methods for this content generator
	 */
	public WebContentGenerator(String... supportedMethods) {
		setSupportedMethods(supportedMethods);
	}


	/**
	 * Set the HTTP methods that this content generator should support.
	 * <p>Default is GET, HEAD and POST for simple form controller types;
	 * unrestricted for general controllers and interceptors.
	 */
	public final void setSupportedMethods(@Nullable String... methods) {
		if (!ObjectUtils.isEmpty(methods)) {
			this.supportedMethods = new LinkedHashSet<>(Arrays.asList(methods));
		}
		else {
			this.supportedMethods = null;
		}
		initAllowHeader();
	}

	/**
	 * Return the HTTP methods that this content generator supports.
	 */
	@Nullable
	public final String[] getSupportedMethods() {
		return (this.supportedMethods != null ? StringUtils.toStringArray(this.supportedMethods) : null);
	}

	private void initAllowHeader() {
		Collection<String> allowedMethods;
		if (this.supportedMethods == null) {
			allowedMethods = new ArrayList<>(HttpMethod.values().length - 1);
			for (HttpMethod method : HttpMethod.values()) {
				if (method != HttpMethod.TRACE) {
					allowedMethods.add(method.name());
				}
			}
		}
		else if (this.supportedMethods.contains(HttpMethod.OPTIONS.name())) {
			allowedMethods = this.supportedMethods;
		}
		else {
			allowedMethods = new ArrayList<>(this.supportedMethods);
			allowedMethods.add(HttpMethod.OPTIONS.name());

		}
		this.allowHeader = StringUtils.collectionToCommaDelimitedString(allowedMethods);
	}

	/**
	 * Return the "Allow" header value to use in response to an HTTP OPTIONS request
	 * based on the configured {@link #setSupportedMethods supported methods} also
	 * automatically adding "OPTIONS" to the list even if not present as a supported
	 * method. This means subclasses don't have to explicitly list "OPTIONS" as a
	 * supported method as long as HTTP OPTIONS requests are handled before making a
	 * call to {@link #checkRequest(HttpServletRequest)}.
	 * @since 4.3
	 */
	@Nullable
	protected String getAllowHeader() {
		return this.allowHeader;
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
	 * @since 4.2
	 */
	public final void setCacheControl(@Nullable CacheControl cacheControl) {
		this.cacheControl = cacheControl;
	}

	/**
	 * Get the {@link org.springframework.http.CacheControl} instance
	 * that builds the Cache-Control HTTP response header.
	 * @since 4.2
	 */
	@Nullable
	public final CacheControl getCacheControl() {
		return this.cacheControl;
	}

	/**
	 * Cache content for the given number of seconds, by writing
	 * cache-related HTTP headers to the response:
	 * <ul>
	 * <li>seconds == -1 (default value): no generation cache-related headers</li>
	 * <li>seconds == 0: "Cache-Control: no-store" will prevent caching</li>
	 * <li>seconds &gt; 0: "Cache-Control: max-age=seconds" will ask to cache content</li>
	 * </ul>
	 * <p>For more specific needs, a custom {@link org.springframework.http.CacheControl}
	 * should be used.
	 * @see #setCacheControl
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
	 * Configure one or more request header names (e.g. "Accept-Language") to
	 * add to the "Vary" response header to inform clients that the response is
	 * subject to content negotiation and variances based on the value of the
	 * given request headers. The configured request header names are added only
	 * if not already present in the response "Vary" header.
	 * @param varyByRequestHeaders one or more request header names
	 * @since 4.3
	 */
	public final void setVaryByRequestHeaders(@Nullable String... varyByRequestHeaders) {
		this.varyByRequestHeaders = varyByRequestHeaders;
	}

	/**
	 * Return the configured request header names for the "Vary" response header.
	 * @since 4.3
	 */
	@Nullable
	public final String[] getVaryByRequestHeaders() {
		return this.varyByRequestHeaders;
	}


	/**
	 * Check the given request for supported methods and a required session, if any.
	 * @param request current HTTP request
	 * @throws ServletException if the request cannot be handled because a check failed
	 * @since 4.2
	 */
	protected final void checkRequest(HttpServletRequest request) throws ServletException {
		// Check whether we should support the request method.
		String method = request.getMethod();
		if (this.supportedMethods != null && !this.supportedMethods.contains(method)) {
			throw new HttpRequestMethodNotSupportedException(method, this.supportedMethods);
		}

		// Check whether a session is required.
		if (this.requireSession && request.getSession(false) == null) {
			throw new HttpSessionRequiredException("Pre-existing session required but none found");
		}
	}

	/**
	 * Prepare the given response according to the settings of this generator.
	 * Applies the number of cache seconds specified for this generator.
	 * @param response current HTTP response
	 * @since 4.2
	 */
	protected final void prepareResponse(HttpServletResponse response) {
		if (this.cacheControl != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Applying default " + getCacheControl());
			}
			applyCacheControl(response, this.cacheControl);
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Applying default cacheSeconds=" + this.cacheSeconds);
			}
			applyCacheSeconds(response, this.cacheSeconds);
		}
		if (this.varyByRequestHeaders != null) {
			for (String value : getVaryRequestHeadersToAdd(response, this.varyByRequestHeaders)) {
				response.addHeader("Vary", value);
			}
		}
	}

	/**
	 * Set the HTTP Cache-Control header according to the given settings.
	 * @param response current HTTP response
	 * @param cacheControl the pre-configured cache control settings
	 * @since 4.2
	 */
	protected final void applyCacheControl(HttpServletResponse response, CacheControl cacheControl) {
		String ccValue = cacheControl.getHeaderValue();
		if (ccValue != null) {
			// Set computed HTTP 1.1 Cache-Control header
			response.setHeader(HEADER_CACHE_CONTROL, ccValue);
		}
	}

	/**
	 * Apply the given cache seconds and generate corresponding HTTP headers,
	 * i.e. allow caching for the given number of seconds in case of a positive
	 * value, prevent caching if given a 0 value, do nothing else.
	 * Does not tell the browser to revalidate the resource.
	 * @param response current HTTP response
	 * @param cacheSeconds positive number of seconds into the future that the
	 * response should be cacheable for, 0 to prevent caching
	 */
	protected final void applyCacheSeconds(HttpServletResponse response, int cacheSeconds) {
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
		applyCacheControl(response, cControl);
	}


	/**
	 * Check and prepare the given request and response according to the settings
	 * of this generator.
	 * @see #checkRequest(HttpServletRequest)
	 * @see #applyCacheSeconds(HttpServletResponse, int)
	 * @deprecated as of 4.2, since the {@code lastModified} flag is effectively ignored,
	 * with a must-revalidate header only generated if explicitly configured
	 */
	@Deprecated
	protected final void checkAndPrepare(
			HttpServletRequest request, HttpServletResponse response, int cacheSeconds, boolean lastModified)
			throws ServletException {

		checkRequest(request);
		applyCacheSeconds(response, cacheSeconds);
	}

	private Collection<String> getVaryRequestHeadersToAdd(HttpServletResponse response, String[] varyByRequestHeaders) {
		if (!response.containsHeader(HttpHeaders.VARY)) {
			return Arrays.asList(varyByRequestHeaders);
		}
		Collection<String> result = new ArrayList<>(varyByRequestHeaders.length);
		Collections.addAll(result, varyByRequestHeaders);
		for (String header : response.getHeaders(HttpHeaders.VARY)) {
			for (String existing : StringUtils.tokenizeToStringArray(header, ",")) {
				if ("*".equals(existing)) {
					return Collections.emptyList();
				}
				for (String value : varyByRequestHeaders) {
					if (value.equalsIgnoreCase(existing)) {
						result.remove(value);
					}
				}
			}
		}
		return result;
	}

}
