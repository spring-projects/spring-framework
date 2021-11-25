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

package org.springframework.web.filter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UrlPathHelper;

/**
 * Extract values from "Forwarded" and "X-Forwarded-*" headers, wrap the request
 * and response, and make they reflect the client-originated protocol and
 * address in the following methods:
 * <ul>
 * <li>{@link HttpServletRequest#getServerName() getServerName()}
 * <li>{@link HttpServletRequest#getServerPort() getServerPort()}
 * <li>{@link HttpServletRequest#getScheme() getScheme()}
 * <li>{@link HttpServletRequest#isSecure() isSecure()}
 * <li>{@link HttpServletResponse#sendRedirect(String) sendRedirect(String)}.
 * </ul>
 *
 * <p>There are security considerations for forwarded headers since an application
 * cannot know if the headers were added by a proxy, as intended, or by a malicious
 * client. This is why a proxy at the boundary of trust should be configured to
 * remove untrusted Forwarded headers that come from the outside.
 *
 * <p>You can also configure the ForwardedHeaderFilter with {@link #setRemoveOnly removeOnly},
 * in which case it removes but does not use the headers.
 *
 * @author Rossen Stoyanchev
 * @author Eddú Meléndez
 * @author Rob Winch
 * @since 4.3
 * @see <a href="https://tools.ietf.org/html/rfc7239">https://tools.ietf.org/html/rfc7239</a>
 */
public class ForwardedHeaderFilter extends OncePerRequestFilter {

	private static final Set<String> FORWARDED_HEADER_NAMES =
			Collections.newSetFromMap(new LinkedCaseInsensitiveMap<>(10, Locale.ENGLISH));

	static {
		FORWARDED_HEADER_NAMES.add("Forwarded");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Host");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Port");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Proto");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Prefix");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Ssl");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-For");
	}


	private boolean removeOnly;

	private boolean relativeRedirects;


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
	 * Use this property to enable relative redirects as explained in
	 * {@link RelativeRedirectFilter}, and also using the same response wrapper
	 * as that filter does, or if both are configured, only one will wrap.
	 * <p>By default, if this property is set to false, in which case calls to
	 * {@link HttpServletResponse#sendRedirect(String)} are overridden in order
	 * to turn relative into absolute URLs, also taking into account forwarded
	 * headers.
	 * @param relativeRedirects whether to use relative redirects
	 * @since 4.3.10
	 */
	public void setRelativeRedirects(boolean relativeRedirects) {
		this.relativeRedirects = relativeRedirects;
	}


	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		for (String headerName : FORWARDED_HEADER_NAMES) {
			if (request.getHeader(headerName) != null) {
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
			ForwardedHeaderRemovingRequest wrappedRequest = new ForwardedHeaderRemovingRequest(request);
			filterChain.doFilter(wrappedRequest, response);
		}
		else {
			HttpServletRequest wrappedRequest =
					new ForwardedHeaderExtractingRequest(request);

			HttpServletResponse wrappedResponse = this.relativeRedirects ?
					RelativeRedirectResponseWrapper.wrapIfNecessary(response, HttpStatus.SEE_OTHER) :
					new ForwardedHeaderExtractingResponse(response, wrappedRequest);

			filterChain.doFilter(wrappedRequest, wrappedResponse);
		}
	}

	@Override
	protected void doFilterNestedErrorDispatch(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		doFilterInternal(request, response, filterChain);
	}

	/**
	 * Hide "Forwarded" or "X-Forwarded-*" headers.
	 */
	private static class ForwardedHeaderRemovingRequest extends HttpServletRequestWrapper {

		private final Set<String> headerNames;

		public ForwardedHeaderRemovingRequest(HttpServletRequest request) {
			super(request);
			this.headerNames = headerNames(request);
		}

		private static Set<String> headerNames(HttpServletRequest request) {
			Set<String> headerNames = Collections.newSetFromMap(new LinkedCaseInsensitiveMap<>(Locale.ENGLISH));
			Enumeration<String> names = request.getHeaderNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				if (!FORWARDED_HEADER_NAMES.contains(name)) {
					headerNames.add(name);
				}
			}
			return Collections.unmodifiableSet(headerNames);
		}

		// Override header accessors to not expose forwarded headers

		@Override
		@Nullable
		public String getHeader(String name) {
			if (FORWARDED_HEADER_NAMES.contains(name)) {
				return null;
			}
			return super.getHeader(name);
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			if (FORWARDED_HEADER_NAMES.contains(name)) {
				return Collections.emptyEnumeration();
			}
			return super.getHeaders(name);
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			return Collections.enumeration(this.headerNames);
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

		@Nullable
		private final InetSocketAddress remoteAddress;

		private final ForwardedPrefixExtractor forwardedPrefixExtractor;


		ForwardedHeaderExtractingRequest(HttpServletRequest servletRequest) {
			super(servletRequest);

			ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
			UriComponents uriComponents = UriComponentsBuilder.fromHttpRequest(request).build();
			int port = uriComponents.getPort();

			this.scheme = uriComponents.getScheme();
			this.secure = "https".equals(this.scheme) || "wss".equals(this.scheme);
			this.host = uriComponents.getHost();
			this.port = (port == -1 ? (this.secure ? 443 : 80) : port);

			this.remoteAddress = UriComponentsBuilder.parseForwardedFor(request, request.getRemoteAddress());

			String baseUrl = this.scheme + "://" + this.host + (port == -1 ? "" : ":" + port);
			Supplier<HttpServletRequest> delegateRequest = () -> (HttpServletRequest) getRequest();
			this.forwardedPrefixExtractor = new ForwardedPrefixExtractor(delegateRequest, baseUrl);
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
			return this.forwardedPrefixExtractor.getContextPath();
		}

		@Override
		public String getRequestURI() {
			return this.forwardedPrefixExtractor.getRequestUri();
		}

		@Override
		public StringBuffer getRequestURL() {
			return this.forwardedPrefixExtractor.getRequestUrl();
		}

		@Override
		@Nullable
		public String getRemoteHost() {
			return (this.remoteAddress != null ? this.remoteAddress.getHostString() : super.getRemoteHost());
		}

		@Override
		@Nullable
		public String getRemoteAddr() {
			return (this.remoteAddress != null ? this.remoteAddress.getHostString() : super.getRemoteAddr());
		}

		@Override
		public int getRemotePort() {
			return (this.remoteAddress != null ? this.remoteAddress.getPort() : super.getRemotePort());
		}
	}


	/**
	 * Responsible for the contextPath, requestURI, and requestURL with forwarded
	 * headers in mind, and also taking into account changes to the path of the
	 * underlying delegate request (e.g. on a Servlet FORWARD).
	 */
	private static class ForwardedPrefixExtractor {

		private final Supplier<HttpServletRequest> delegate;

		private final String baseUrl;

		private String actualRequestUri;

		@Nullable
		private final String forwardedPrefix;

		@Nullable
		private String requestUri;

		private String requestUrl;


		/**
		 * Constructor with required information.
		 * @param delegateRequest supplier for the current
		 * {@link HttpServletRequestWrapper#getRequest() delegate request} which
		 * may change during a forward (e.g. Tomcat.
		 * @param baseUrl the host, scheme, and port based on forwarded headers
		 */
		public ForwardedPrefixExtractor(Supplier<HttpServletRequest> delegateRequest, String baseUrl) {
			this.delegate = delegateRequest;
			this.baseUrl = baseUrl;
			this.actualRequestUri = delegateRequest.get().getRequestURI();

			this.forwardedPrefix = initForwardedPrefix(delegateRequest.get());
			this.requestUri = initRequestUri();
			this.requestUrl = initRequestUrl(); // Keep the order: depends on requestUri
		}

		@Nullable
		private static String initForwardedPrefix(HttpServletRequest request) {
			String result = null;
			Enumeration<String> names = request.getHeaderNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				if ("X-Forwarded-Prefix".equalsIgnoreCase(name)) {
					result = request.getHeader(name);
				}
			}
			if (result != null) {
				StringBuilder prefix = new StringBuilder(result.length());
				String[] rawPrefixes = StringUtils.tokenizeToStringArray(result, ",");
				for (String rawPrefix : rawPrefixes) {
					int endIndex = rawPrefix.length();
					while (endIndex > 0 && rawPrefix.charAt(endIndex - 1) == '/') {
						endIndex--;
					}
					prefix.append((endIndex != rawPrefix.length() ? rawPrefix.substring(0, endIndex) : rawPrefix));
				}
				return prefix.toString();
			}
			return null;
		}

		@Nullable
		private String initRequestUri() {
			if (this.forwardedPrefix != null) {
				return this.forwardedPrefix +
						UrlPathHelper.rawPathInstance.getPathWithinApplication(this.delegate.get());
			}
			return null;
		}

		private String initRequestUrl() {
			return this.baseUrl + (this.requestUri != null ? this.requestUri : this.delegate.get().getRequestURI());
		}


		public String getContextPath() {
			return (this.forwardedPrefix != null ? this.forwardedPrefix : this.delegate.get().getContextPath());
		}

		public String getRequestUri() {
			if (this.requestUri == null) {
				return this.delegate.get().getRequestURI();
			}
			recalculatePathsIfNecessary();
			return this.requestUri;
		}

		public StringBuffer getRequestUrl() {
			recalculatePathsIfNecessary();
			return new StringBuffer(this.requestUrl);
		}

		private void recalculatePathsIfNecessary() {
			if (!this.actualRequestUri.equals(this.delegate.get().getRequestURI())) {
				// Underlying path change (e.g. Servlet FORWARD).
				this.actualRequestUri = this.delegate.get().getRequestURI();
				this.requestUri = initRequestUri();
				this.requestUrl = initRequestUrl(); // Keep the order: depends on requestUri
			}
		}
	}


	private static class ForwardedHeaderExtractingResponse extends HttpServletResponseWrapper {

		private static final String FOLDER_SEPARATOR = "/";

		private final HttpServletRequest request;


		ForwardedHeaderExtractingResponse(HttpServletResponse response, HttpServletRequest request) {
			super(response);
			this.request = request;
		}


		@Override
		public void sendRedirect(String location) throws IOException {

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(location);
			UriComponents uriComponents = builder.build();

			// Absolute location
			if (uriComponents.getScheme() != null) {
				super.sendRedirect(location);
				return;
			}

			// Network-path reference
			if (location.startsWith("//")) {
				String scheme = this.request.getScheme();
				super.sendRedirect(builder.scheme(scheme).toUriString());
				return;
			}

			String path = uriComponents.getPath();
			if (path != null) {
				// Relative to Servlet container root or to current request
				path = (path.startsWith(FOLDER_SEPARATOR) ? path :
						StringUtils.applyRelativePath(this.request.getRequestURI(), path));
			}

			String result = UriComponentsBuilder
					.fromHttpRequest(new ServletServerHttpRequest(this.request))
					.replacePath(path)
					.replaceQuery(uriComponents.getQuery())
					.fragment(uriComponents.getFragment())
					.build().normalize().toUriString();

			super.sendRedirect(result);
		}
	}

}
