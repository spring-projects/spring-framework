/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UrlPathHelper;

/**
 * Extract values from "Forwarded" and "X-Forwarded-*" headers in order to wrap
 * and override the following from the request and response:
 * {@link HttpServletRequest#getServerName() getServerName()},
 * {@link HttpServletRequest#getServerPort() getServerPort()},
 * {@link HttpServletRequest#getScheme() getScheme()},
 * {@link HttpServletRequest#isSecure() isSecure()}, and
 * {@link HttpServletResponse#sendRedirect(String) sendRedirect(String)}.
 * In effect the wrapped request and response reflect the client-originated
 * protocol and address.
 *
 * <p><strong>Note:</strong> This filter can also be used in a
 * {@link #setRemoveOnly removeOnly} mode where "Forwarded" and "X-Forwarded-*"
 * headers are only eliminated without being used.
 *
 * @author Rossen Stoyanchev
 * @author Eddú Meléndez
 * @author Rob Winch
 * @since 4.3
 * @see <a href="https://tools.ietf.org/html/rfc7239">https://tools.ietf.org/html/rfc7239</a>
 */
public class ForwardedHeaderFilter extends OncePerRequestFilter {

	private static final Set<String> FORWARDED_HEADER_NAMES =
			Collections.newSetFromMap(new LinkedCaseInsensitiveMap<>(5, Locale.ENGLISH));

	static {
		FORWARDED_HEADER_NAMES.add("Forwarded");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Host");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Port");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Proto");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Prefix");
	}


	private final UrlPathHelper pathHelper;

	private boolean removeOnly;

	private boolean relativeRedirects;


	public ForwardedHeaderFilter() {
		this.pathHelper = new UrlPathHelper();
		this.pathHelper.setUrlDecode(false);
		this.pathHelper.setRemoveSemicolonContent(false);
	}


	/**
	 * Enables mode in which any "Forwarded" or "X-Forwarded-*" headers are
	 * removed only and the information in them ignored.
	 * @param removeOnly whether to discard and ignore forwarded headers
	 * @since 4.3.9
	 */
	public void setRemoveOnly(boolean removeOnly) {
		this.removeOnly = removeOnly;
	}

	/**
	 * Use this property to enable relative redirects as explained in and also
	 * using the same response wrapper as {@link RelativeRedirectFilter} does.
	 * Or if both filters are used, only one will wrap the response.
	 * <p>By default, if this property is set to false, in which case calls to
	 * {@link HttpServletResponse#sendRedirect(String)} are overridden in order
	 * to turn relative into absolute URLs since (which Servlet containers are
	 * also required to do) also taking forwarded headers into consideration.
	 * @param relativeRedirects whether to use relative redirects
	 * @since 4.3.10
	 */
	public void setRelativeRedirects(boolean relativeRedirects) {
		this.relativeRedirects = relativeRedirects;
	}


	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
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

		if (this.removeOnly) {
			ForwardedHeaderRemovingRequest theRequest = new ForwardedHeaderRemovingRequest(request);
			filterChain.doFilter(theRequest, response);
		}
		else {
			HttpServletRequest theRequest = new ForwardedHeaderExtractingRequest(request, this.pathHelper);
			HttpServletResponse theResponse = (this.relativeRedirects ?
					RelativeRedirectResponseWrapper.wrapIfNecessary(response, HttpStatus.SEE_OTHER) :
					new ForwardedHeaderExtractingResponse(response, theRequest));
			filterChain.doFilter(theRequest, theResponse);
		}
	}


	/**
	 * Hide "Forwarded" or "X-Forwarded-*" headers.
	 */
	private static class ForwardedHeaderRemovingRequest extends HttpServletRequestWrapper {

		private final Map<String, List<String>> headers;

		public ForwardedHeaderRemovingRequest(HttpServletRequest request) {
			super(request);
			this.headers = initHeaders(request);
		}

		private static Map<String, List<String>> initHeaders(HttpServletRequest request) {
			Map<String, List<String>> headers = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
			Enumeration<String> names = request.getHeaderNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				if (!FORWARDED_HEADER_NAMES.contains(name)) {
					headers.put(name, Collections.list(request.getHeaders(name)));
				}
			}
			return headers;
		}

		// Override header accessors to not expose forwarded headers

		@Override
		@Nullable
		public String getHeader(String name) {
			List<String> value = this.headers.get(name);
			return (CollectionUtils.isEmpty(value) ? null : value.get(0));
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			List<String> value = this.headers.get(name);
			return (Collections.enumeration(value != null ? value : Collections.emptySet()));
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			return Collections.enumeration(this.headers.keySet());
		}
	}


	/**
	 * Extract and use "Forwarded" or "X-Forwarded-*" headers.
	 */
	private static class ForwardedHeaderExtractingRequest extends ForwardedHeaderRemovingRequest {

		@Nullable
		private final String scheme;

		private final boolean secure;

		@Nullable
		private final String host;

		private final int port;

		private final String contextPath;

		private final String requestUri;

		private final String requestUrl;

		public ForwardedHeaderExtractingRequest(HttpServletRequest request, UrlPathHelper pathHelper) {
			super(request);

			HttpRequest httpRequest = new ServletServerHttpRequest(request);
			UriComponents uriComponents = UriComponentsBuilder.fromHttpRequest(httpRequest).build();
			int port = uriComponents.getPort();

			this.scheme = uriComponents.getScheme();
			this.secure = "https".equals(scheme);
			this.host = uriComponents.getHost();
			this.port = (port == -1 ? (this.secure ? 443 : 80) : port);

			String prefix = getForwardedPrefix(request);
			this.contextPath = (prefix != null ? prefix : request.getContextPath());
			this.requestUri = this.contextPath + pathHelper.getPathWithinApplication(request);
			this.requestUrl = this.scheme + "://" + this.host + (port == -1 ? "" : ":" + port) + this.requestUri;
		}

		@Nullable
		private static String getForwardedPrefix(HttpServletRequest request) {
			String prefix = null;
			Enumeration<String> names = request.getHeaderNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				if ("X-Forwarded-Prefix".equalsIgnoreCase(name)) {
					prefix = request.getHeader(name);
				}
			}
			if (prefix != null) {
				while (prefix.endsWith("/")) {
					prefix = prefix.substring(0, prefix.length() - 1);
				}
			}
			return prefix;
		}

		@Override
		@Nullable
		public String getScheme() {
			return this.scheme;
		}

		@Override
		@Nullable
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
		public String getContextPath() {
			return this.contextPath;
		}

		@Override
		public String getRequestURI() {
			return this.requestUri;
		}

		@Override
		public StringBuffer getRequestURL() {
			return new StringBuffer(this.requestUrl);
		}
	}


	private static class ForwardedHeaderExtractingResponse extends HttpServletResponseWrapper {

		private static final String FOLDER_SEPARATOR = "/";

		private final HttpServletRequest request;

		public ForwardedHeaderExtractingResponse(HttpServletResponse response, HttpServletRequest request) {
			super(response);
			this.request = request;
		}

		@Override
		public void sendRedirect(String location) throws IOException {
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(location);

			// Absolute location
			if (builder.build().getScheme() != null) {
				super.sendRedirect(location);
				return;
			}

			// Network-path reference
			if (location.startsWith("//")) {
				String scheme = this.request.getScheme();
				super.sendRedirect(builder.scheme(scheme).toUriString());
				return;
			}

			// Relative to Servlet container root or to current request
			String path = (location.startsWith(FOLDER_SEPARATOR) ? location :
					StringUtils.applyRelativePath(this.request.getRequestURI(), location));

			String result = UriComponentsBuilder
					.fromHttpRequest(new ServletServerHttpRequest(this.request))
					.replacePath(path)
					.build().normalize().toUriString();

			super.sendRedirect(result);
		}
	}

}
