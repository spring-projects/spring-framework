/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UrlPathHelper;

/**
 * A UriComponentsBuilder that extracts information from an HttpServletRequest.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletUriComponentsBuilder extends UriComponentsBuilder {

	/**
	 * Default constructor. Protected to prevent direct instantiation.
	 *
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
	 * Prepare a builder from the host, port, scheme, and context path of
	 * an HttpServletRequest.
	 */
	public static ServletUriComponentsBuilder fromContextPath(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = fromRequest(request);
		builder.replacePath(request.getContextPath());
		builder.replaceQuery(null);
		return builder;
	}

	/**
	 * Prepare a builder from the host, port, scheme, context path, and
	 * servlet mapping of an HttpServletRequest. The results may vary depending
	 * on the type of servlet mapping used.
	 *
	 * <p>If the servlet is mapped by name, e.g. {@code "/main/*"}, the path
	 * will end with "/main". If the servlet is mapped otherwise, e.g.
	 * {@code "/"} or {@code "*.do"}, the result will be the same as
	 * if calling {@link #fromContextPath(HttpServletRequest)}.
	 */
	public static ServletUriComponentsBuilder fromServletMapping(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = fromContextPath(request);
		if (StringUtils.hasText(new UrlPathHelper().getPathWithinServletMapping(request))) {
			builder.path(request.getServletPath());
		}
		return builder;
	}

	/**
	 * Prepare a builder from the host, port, scheme, and path of
	 * an HttpSevletRequest.
	 */
	public static ServletUriComponentsBuilder fromRequestUri(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = fromRequest(request);
		builder.replacePath(request.getRequestURI());
		builder.replaceQuery(null);
		return builder;
	}

	/**
	 * Prepare a builder by copying the scheme, host, port, path, and
	 * query string of an HttpServletRequest.
	 */
	public static ServletUriComponentsBuilder fromRequest(HttpServletRequest request) {
		String scheme = request.getScheme();
		int port = request.getServerPort();

		ServletUriComponentsBuilder builder = new ServletUriComponentsBuilder();
		builder.scheme(scheme);
		builder.host(request.getServerName());
		if ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443)) {
			builder.port(port);
		}
		builder.path(request.getRequestURI());
		builder.query(request.getQueryString());
		return builder;
	}

	/**
	 * Same as {@link #fromContextPath(HttpServletRequest)} except the
	 * request is obtained through {@link RequestContextHolder}.
	 */
	public static ServletUriComponentsBuilder fromCurrentContextPath() {
		return fromContextPath(getCurrentRequest());
	}

	/**
	 * Same as {@link #fromServletMapping(HttpServletRequest)} except the
	 * request is obtained through {@link RequestContextHolder}.
	 */
	public static ServletUriComponentsBuilder fromCurrentServletMapping() {
		return fromServletMapping(getCurrentRequest());
	}

	/**
	 * Same as {@link #fromRequestUri(HttpServletRequest)} except the
	 * request is obtained through {@link RequestContextHolder}.
	 */
	public static ServletUriComponentsBuilder fromCurrentRequestUri() {
		return fromRequestUri(getCurrentRequest());
	}

	/**
	 * Same as {@link #fromRequest(HttpServletRequest)} except the
	 * request is obtained through {@link RequestContextHolder}.
	 */
	public static ServletUriComponentsBuilder fromCurrentRequest() {
		return fromRequest(getCurrentRequest());
	}

	private static HttpServletRequest getCurrentRequest() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		Assert.state(requestAttributes != null, "Could not find current request via RequestContextHolder");
		Assert.isInstanceOf(ServletRequestAttributes.class, requestAttributes);
		HttpServletRequest servletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
		Assert.state(servletRequest != null, "Could not find current HttpServletRequest");
		return servletRequest;
	}

}
