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
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UrlPathHelper;

/**
 * A builder for {@link UriComponents} that offers static factory methods to 
 * extract information from an {@code HttpServletRequest}. 
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
	 * Return a builder initialized with the host, port, scheme, and the 
	 * context path of the given request.
	 */
	public static ServletUriComponentsBuilder fromContextPath(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = fromRequest(request);
		builder.replacePath(new UrlPathHelper().getContextPath(request));
		builder.replaceQuery(null);
		return builder;
	}

	/**
	 * Return a builder initialized with the host, port, scheme, context path, 
	 * and the servlet mapping of the given request.
	 * 
	 * <p>For example if the servlet is mapped by name, i.e. {@code "/main/*"}, 
	 * then the resulting path will be {@code /appContext/main}. If the servlet
	 * path is not mapped by name, i.e. {@code "/"} or {@code "*.html"}, then 
	 * the resulting path will contain the context path only.
	 */
	public static ServletUriComponentsBuilder fromServletMapping(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = fromContextPath(request);
		if (StringUtils.hasText(new UrlPathHelper().getPathWithinServletMapping(request))) {
			builder.path(request.getServletPath());
		}
		return builder;
	}

	/**
	 * Return a builder initialized with all available information in the given
	 * request including scheme, host, port, path, and query string.
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
		builder.path(new UrlPathHelper().getRequestUri(request));
		builder.query(request.getQueryString());
		return builder;
	}
	
	/**
	 * Equivalent to {@link #fromContextPath(HttpServletRequest)} except the
	 * request is obtained via {@link RequestContextHolder}.
	 */
	public static ServletUriComponentsBuilder fromCurrentContextPath() {
		return fromContextPath(getCurrentRequest());
	}

	/**
	 * Equivalent to {@link #fromServletMapping(HttpServletRequest)} except the
	 * request is obtained via {@link RequestContextHolder}.
	 */
	public static ServletUriComponentsBuilder fromCurrentServletMapping() {
		return fromServletMapping(getCurrentRequest());
	}

	/**
	 * Equivalent to {@link #fromRequest(HttpServletRequest)} except the
	 * request is obtained via {@link RequestContextHolder}.
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
