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

package org.springframework.web.servlet.support;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * UriComponentsBuilder with additional static factory methods to create links
 * based on the current HttpServletRequest.
 *
 * <p><strong>Note:</strong> This class uses values from "Forwarded"
 * (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
 * "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" headers,
 * if present, in order to reflect the client-originated protocol and address.
 * Consider using the {@code ForwardedHeaderFilter} in order to choose from a
 * central place whether to extract and use, or to discard such headers.
 * See the Spring Framework reference for more on this filter.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletUriComponentsBuilder extends UriComponentsBuilder {

	@Nullable
	private String originalPath;


	/**
	 * Default constructor. Protected to prevent direct instantiation.
	 * @see #fromContextPath(HttpServletRequest)
	 * @see #fromServletMapping(HttpServletRequest)
	 * @see #fromRequest(HttpServletRequest)
	 * @see #fromCurrentContextPath()
	 * @see #fromCurrentServletMapping()
 	 * @see #fromCurrentRequest()
	 */
	protected ServletUriComponentsBuilder() {
	}

	/**
	 * Create a deep copy of the given ServletUriComponentsBuilder.
	 * @param other the other builder to copy from
	 */
	protected ServletUriComponentsBuilder(ServletUriComponentsBuilder other) {
		super(other);
		this.originalPath = other.originalPath;
	}


	// Factory methods based on a HttpServletRequest

	/**
	 * Prepare a builder from the host, port, scheme, and context path of the
	 * given HttpServletRequest.
	 * <p><strong>Note:</strong> As of 5.1, this method ignores
	 * {@code "Forwarded"} and {@code "X-Forwarded-*"} headers that specify the
	 * client-originated address. Consider using the {@code ForwardedHeaderFilter}
	 * to extract and use, or to discard such headers.
	 */
	public static ServletUriComponentsBuilder fromContextPath(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = initFromRequest(request);
		builder.replacePath(request.getContextPath());
		return builder;
	}

	/**
	 * Prepare a builder from the host, port, scheme, context path, and
	 * servlet mapping of the given HttpServletRequest.
	 * <p>If the servlet is mapped by name, e.g. {@code "/main/*"}, the path
	 * will end with "/main". If the servlet is mapped otherwise, e.g.
	 * {@code "/"} or {@code "*.do"}, the result will be the same as
	 * if calling {@link #fromContextPath(HttpServletRequest)}.
	 * <p><strong>Note:</strong> As of 5.1, this method ignores
	 * {@code "Forwarded"} and {@code "X-Forwarded-*"} headers that specify the
	 * client-originated address. Consider using the {@code ForwardedHeaderFilter}
	 * to extract and use, or to discard such headers.
	 */
	public static ServletUriComponentsBuilder fromServletMapping(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = fromContextPath(request);
		if (StringUtils.hasText(new UrlPathHelper().getPathWithinServletMapping(request))) {
			builder.path(request.getServletPath());
		}
		return builder;
	}

	/**
	 * Prepare a builder from the host, port, scheme, and path (but not the query)
	 * of the HttpServletRequest.
	 * <p><strong>Note:</strong> As of 5.1, this method ignores
	 * {@code "Forwarded"} and {@code "X-Forwarded-*"} headers that specify the
	 * client-originated address. Consider using the {@code ForwardedHeaderFilter}
	 * to extract and use, or to discard such headers.
	 */
	public static ServletUriComponentsBuilder fromRequestUri(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = initFromRequest(request);
		builder.initPath(request.getRequestURI());
		return builder;
	}

	/**
	 * Prepare a builder by copying the scheme, host, port, path, and
	 * query string of an HttpServletRequest.
	 * <p><strong>Note:</strong> As of 5.1, this method ignores
	 * {@code "Forwarded"} and {@code "X-Forwarded-*"} headers that specify the
	 * client-originated address. Consider using the {@code ForwardedHeaderFilter}
	 * to extract and use, or to discard such headers.
	 */
	public static ServletUriComponentsBuilder fromRequest(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = initFromRequest(request);
		builder.initPath(request.getRequestURI());
		builder.query(request.getQueryString());
		return builder;
	}

	/**
	 * Initialize a builder with a scheme, host,and port (but not path and query).
	 */
	private static ServletUriComponentsBuilder initFromRequest(HttpServletRequest request) {
		String scheme = request.getScheme();
		String host = request.getServerName();
		int port = request.getServerPort();

		ServletUriComponentsBuilder builder = new ServletUriComponentsBuilder();
		builder.scheme(scheme);
		builder.host(host);
		if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
			builder.port(port);
		}
		return builder;
	}


	// Alternative methods relying on RequestContextHolder to find the request

	/**
	 * Same as {@link #fromContextPath(HttpServletRequest)} except the
	 * request is obtained through {@link RequestContextHolder}.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * <p>As of 4.3.15, this method replaces the contextPath with the value
	 * of "X-Forwarded-Prefix" rather than prepending, thus aligning with
	 * {@code ForwardedHeaderFilter}.
	 */
	public static ServletUriComponentsBuilder fromCurrentContextPath() {
		return fromContextPath(getCurrentRequest());
	}

	/**
	 * Same as {@link #fromServletMapping(HttpServletRequest)} except the
	 * request is obtained through {@link RequestContextHolder}.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * <p>As of 4.3.15, this method replaces the contextPath with the value
	 * of "X-Forwarded-Prefix" rather than prepending, thus aligning with
	 * {@code ForwardedHeaderFilter}.
	 */
	public static ServletUriComponentsBuilder fromCurrentServletMapping() {
		return fromServletMapping(getCurrentRequest());
	}

	/**
	 * Same as {@link #fromRequestUri(HttpServletRequest)} except the
	 * request is obtained through {@link RequestContextHolder}.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * <p>As of 4.3.15, this method replaces the contextPath with the value
	 * of "X-Forwarded-Prefix" rather than prepending, thus aligning with
	 * {@code ForwardedHeaderFilter}.
	 */
	public static ServletUriComponentsBuilder fromCurrentRequestUri() {
		return fromRequestUri(getCurrentRequest());
	}

	/**
	 * Same as {@link #fromRequest(HttpServletRequest)} except the
	 * request is obtained through {@link RequestContextHolder}.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * <p>As of 4.3.15, this method replaces the contextPath with the value
	 * of "X-Forwarded-Prefix" rather than prepending, thus aligning with
	 * {@code ForwardedHeaderFilter}.
	 */
	public static ServletUriComponentsBuilder fromCurrentRequest() {
		return fromRequest(getCurrentRequest());
	}

	/**
	 * Obtain current request through {@link RequestContextHolder}.
	 */
	protected static HttpServletRequest getCurrentRequest() {
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		Assert.state(attrs instanceof ServletRequestAttributes, "No current ServletRequestAttributes");
		return ((ServletRequestAttributes) attrs).getRequest();
	}


	private void initPath(String path) {
		this.originalPath = path;
		replacePath(path);
	}

	/**
	 * Remove any path extension from the {@link HttpServletRequest#getRequestURI()
	 * requestURI}. This method must be invoked before any calls to {@link #path(String)}
	 * or {@link #pathSegment(String...)}.
	 * <pre>
	 * GET http://foo.com/rest/books/6.json
	 *
	 * ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequestUri(this.request);
	 * String ext = builder.removePathExtension();
	 * String uri = builder.path("/pages/1.{ext}").buildAndExpand(ext).toUriString();
	 * assertEquals("http://foo.com/rest/books/6/pages/1.json", result);
	 * </pre>
	 * @return the removed path extension for possible re-use, or {@code null}
	 * @since 4.0
	 */
	@Nullable
	public String removePathExtension() {
		String extension = null;
		if (this.originalPath != null) {
			extension = UriUtils.extractFileExtension(this.originalPath);
			if (!StringUtils.isEmpty(extension)) {
				int end = this.originalPath.length() - (extension.length() + 1);
				replacePath(this.originalPath.substring(0, end));
			}
			this.originalPath = null;
		}
		return extension;
	}

	@Override
	public ServletUriComponentsBuilder cloneBuilder() {
		return new ServletUriComponentsBuilder(this);
	}

}
