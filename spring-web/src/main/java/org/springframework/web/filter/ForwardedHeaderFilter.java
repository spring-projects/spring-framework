/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;


/**
 * Filter that wraps the request in order to override its
 * {@link HttpServletRequest#getServerName() getServerName()},
 * {@link HttpServletRequest#getServerPort() getServerPort()},
 * {@link HttpServletRequest#getScheme() getScheme()}, and
 * {@link HttpServletRequest#isSecure() isSecure()} methods with values derived
 * from "Fowarded" or "X-Forwarded-*" headers. In effect the wrapped request
 * reflects the client-originated protocol and address.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public class ForwardedHeaderFilter extends OncePerRequestFilter {

	private static final Set<String> FORWARDED_HEADER_NAMES;

	static {
		FORWARDED_HEADER_NAMES = new HashSet<String>(4);
		FORWARDED_HEADER_NAMES.add("Forwarded");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Host");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Port");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Proto");
	}


	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String name = headerNames.nextElement();
			if (FORWARDED_HEADER_NAMES.contains(name)) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		filterChain.doFilter(new ForwardedHeaderRequestWrapper(request), response);
	}


	private static class ForwardedHeaderRequestWrapper extends HttpServletRequestWrapper {

		public static final Enumeration<String> EMPTY_HEADER_VALUES =
				Collections.enumeration(Collections.<String>emptyList());


		private final String scheme;

		private final boolean secure;

		private final String host;

		private final int port;

		private final StringBuffer requestUrl;

		private final Map<String, List<String>> headers;


		public ForwardedHeaderRequestWrapper(HttpServletRequest request) {
			super(request);

			HttpRequest httpRequest = new ServletServerHttpRequest(request);
			UriComponents uriComponents = UriComponentsBuilder.fromHttpRequest(httpRequest).build();
			int port = uriComponents.getPort();

			this.scheme = uriComponents.getScheme();
			this.secure = "https".equals(scheme);
			this.host = uriComponents.getHost();
			this.port = (port == -1 ? (this.secure ? 443 : 80) : port);
			this.requestUrl = initRequestUrl(this.scheme, this.host, port, request.getRequestURI());
			this.headers = initHeaders(request);
		}

		private static StringBuffer initRequestUrl(String scheme, String host, int port, String path) {
			StringBuffer sb = new StringBuffer();
			sb.append(scheme).append("://").append(host);
			sb.append(port == -1 ? "" : ":" + port);
			sb.append(path);
			return sb;
		}

		/**
		 * Copy the headers excluding any {@link #FORWARDED_HEADER_NAMES}.
		 */
		private static Map<String, List<String>> initHeaders(HttpServletRequest request) {
			Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String name = headerNames.nextElement();
				headers.put(name, Collections.list(request.getHeaders(name)));
			}
			for (String name : FORWARDED_HEADER_NAMES) {
				headers.remove(name);
			}
			return headers;
		}


		@Override
		public String getScheme() {
			return this.scheme;
		}

		@Override
		public String getServerName() {
			return this.host;
		}

		@Override
		public int getServerPort() {
			return this.port;
		}

		@Override
		public boolean isSecure() {
			return this.secure;
		}

		@Override
		public StringBuffer getRequestURL() {
			return this.requestUrl;
		}

		// Override header accessors in order to not expose forwarded headers

		@Override
		public String getHeader(String name) {
			List<String> value = this.headers.get(name);
			return (CollectionUtils.isEmpty(value) ? null : value.get(0));
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			List<String> value = this.headers.get(name);
			return (CollectionUtils.isEmpty(value) ? EMPTY_HEADER_VALUES : Collections.enumeration(value));
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			return Collections.enumeration(this.headers.keySet());
		}
	}

}
