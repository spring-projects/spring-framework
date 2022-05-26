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
import java.util.Enumeration;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.web.testfixture.servlet.MockFilterChain;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ForwardedHeaderFilter}.
 *
 * @author Rossen Stoyanchev
 * @author Eddú Meléndez
 * @author Rob Winch
 */
public class ForwardedHeaderFilterTests {

	private static final String FORWARDED = "forwarded";

	private static final String X_FORWARDED_PROTO = "x-forwarded-proto";  // SPR-14372 (case insensitive)

	private static final String X_FORWARDED_HOST = "x-forwarded-host";

	private static final String X_FORWARDED_PORT = "x-forwarded-port";

	private static final String X_FORWARDED_SSL = "x-forwarded-ssl";

	private static final String X_FORWARDED_PREFIX = "x-forwarded-prefix";

	private static final String X_FORWARDED_FOR = "x-forwarded-for";


	private final ForwardedHeaderFilter filter = new ForwardedHeaderFilter();

	@SuppressWarnings("serial")
	private final MockFilterChain filterChain = new MockFilterChain(new HttpServlet() {});

	private MockHttpServletRequest request;


	@BeforeEach
	@SuppressWarnings("serial")
	public void setup() {
		this.request = new MockHttpServletRequest();
		this.request.setScheme("http");
		this.request.setServerName("localhost");
		this.request.setServerPort(80);
	}

	@Test
	public void shouldFilter() {
		testShouldFilter(FORWARDED);
		testShouldFilter(X_FORWARDED_HOST);
		testShouldFilter(X_FORWARDED_PORT);
		testShouldFilter(X_FORWARDED_PROTO);
		testShouldFilter(X_FORWARDED_SSL);
		testShouldFilter(X_FORWARDED_PREFIX);
		testShouldFilter(X_FORWARDED_FOR);
	}

	private void testShouldFilter(String headerName) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(headerName, "1");
		assertThat(this.filter.shouldNotFilter(request)).isFalse();
	}

	@Test
	public void shouldNotFilter() {
		assertThat(this.filter.shouldNotFilter(new MockHttpServletRequest())).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"https", "wss"})
	public void forwardedRequest(String protocol) throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(X_FORWARDED_PROTO, protocol);
		this.request.addHeader(X_FORWARDED_HOST, "84.198.58.199");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.request.addHeader("foo", "bar");
		this.request.addHeader(X_FORWARDED_FOR, "203.0.113.195");

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		assertThat(actual).isNotNull();
		assertThat(actual.getRequestURL().toString()).isEqualTo(protocol + "://84.198.58.199/mvc-showcase");
		assertThat(actual.getScheme()).isEqualTo(protocol);
		assertThat(actual.getServerName()).isEqualTo("84.198.58.199");
		assertThat(actual.getServerPort()).isEqualTo(443);
		assertThat(actual.isSecure()).isTrue();
		assertThat(actual.getRemoteAddr()).isEqualTo(actual.getRemoteHost()).isEqualTo("203.0.113.195");

		assertThat(actual.getHeader(X_FORWARDED_PROTO)).isNull();
		assertThat(actual.getHeader(X_FORWARDED_HOST)).isNull();
		assertThat(actual.getHeader(X_FORWARDED_PORT)).isNull();
		assertThat(actual.getHeader(X_FORWARDED_FOR)).isNull();
		assertThat(actual.getHeader("foo")).isEqualTo("bar");
	}

	@Test
	public void forwardedRequestInRemoveOnlyMode() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "84.198.58.199");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.request.addHeader(X_FORWARDED_SSL, "on");
		this.request.addHeader("foo", "bar");
		this.request.addHeader(X_FORWARDED_FOR, "203.0.113.195");

		this.filter.setRemoveOnly(true);
		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		assertThat(actual).isNotNull();
		assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/mvc-showcase");
		assertThat(actual.getScheme()).isEqualTo("http");
		assertThat(actual.getServerName()).isEqualTo("localhost");
		assertThat(actual.getServerPort()).isEqualTo(80);
		assertThat(actual.isSecure()).isFalse();
		assertThat(actual.getRemoteAddr()).isEqualTo(MockHttpServletRequest.DEFAULT_REMOTE_ADDR);
		assertThat(actual.getRemoteHost()).isEqualTo(MockHttpServletRequest.DEFAULT_REMOTE_HOST);

		assertThat(actual.getHeader(X_FORWARDED_PROTO)).isNull();
		assertThat(actual.getHeader(X_FORWARDED_HOST)).isNull();
		assertThat(actual.getHeader(X_FORWARDED_PORT)).isNull();
		assertThat(actual.getHeader(X_FORWARDED_SSL)).isNull();
		assertThat(actual.getHeader(X_FORWARDED_FOR)).isNull();
		assertThat(actual.getHeader("foo")).isEqualTo("bar");
	}

	@Test
	public void forwardedRequestWithSsl() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(X_FORWARDED_SSL, "on");
		this.request.addHeader(X_FORWARDED_HOST, "84.198.58.199");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.request.addHeader("foo", "bar");

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		assertThat(actual).isNotNull();
		assertThat(actual.getRequestURL().toString()).isEqualTo("https://84.198.58.199/mvc-showcase");
		assertThat(actual.getScheme()).isEqualTo("https");
		assertThat(actual.getServerName()).isEqualTo("84.198.58.199");
		assertThat(actual.getServerPort()).isEqualTo(443);
		assertThat(actual.isSecure()).isTrue();

		assertThat(actual.getHeader(X_FORWARDED_SSL)).isNull();
		assertThat(actual.getHeader(X_FORWARDED_HOST)).isNull();
		assertThat(actual.getHeader(X_FORWARDED_PORT)).isNull();
		assertThat(actual.getHeader("foo")).isEqualTo("bar");
	}

	@Test // SPR-16983
	public void forwardedRequestWithServletForward() throws Exception {
		this.request.setRequestURI("/foo");
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "www.mycompany.example");
		this.request.addHeader(X_FORWARDED_PORT, "443");

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest wrappedRequest = (HttpServletRequest) this.filterChain.getRequest();

		this.request.setDispatcherType(DispatcherType.FORWARD);
		this.request.setRequestURI("/bar");
		this.filterChain.reset();

		this.filter.doFilter(wrappedRequest, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		assertThat(actual).isNotNull();
		assertThat(actual.getRequestURI()).isEqualTo("/bar");
		assertThat(actual.getRequestURL().toString()).isEqualTo("https://www.mycompany.example/bar");
	}

	@Nested
	class ForwardedPrefix {

		@Test
		public void contextPathEmpty() throws Exception {
			request.addHeader(X_FORWARDED_PREFIX, "");
			assertThat(filterAndGetContextPath()).isEqualTo("");
		}

		@Test
		public void contextPathWithTrailingSlash() throws Exception {
			request.addHeader(X_FORWARDED_PREFIX, "/foo/bar/");
			assertThat(filterAndGetContextPath()).isEqualTo("/foo/bar");
		}

		@Test
		public void contextPathWithTrailingSlashes() throws Exception {
			request.addHeader(X_FORWARDED_PREFIX, "/foo/bar/baz///");
			assertThat(filterAndGetContextPath()).isEqualTo("/foo/bar/baz");
		}

		@Test
		public void contextPathWithForwardedPrefix() throws Exception {
			request.addHeader(X_FORWARDED_PREFIX, "/prefix");
			request.setContextPath("/mvc-showcase");

			String actual = filterAndGetContextPath();
			assertThat(actual).isEqualTo("/prefix");
		}

		@Test
		public void contextPathWithForwardedPrefixTrailingSlash() throws Exception {
			request.addHeader(X_FORWARDED_PREFIX, "/prefix/");
			request.setContextPath("/mvc-showcase");

			String actual = filterAndGetContextPath();
			assertThat(actual).isEqualTo("/prefix");
		}

		private String filterAndGetContextPath() throws ServletException, IOException {
			return filterAndGetWrappedRequest().getContextPath();
		}

		@Test
		public void contextPathPreserveEncoding() throws Exception {
			request.setContextPath("/app%20");
			request.setRequestURI("/app%20/path/");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getContextPath()).isEqualTo("/app%20");
			assertThat(actual.getRequestURI()).isEqualTo("/app%20/path/");
			assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/app%20/path/");
		}

		@Test
		public void requestUri() throws Exception {
			request.addHeader(X_FORWARDED_PREFIX, "/");
			request.setContextPath("/app");
			request.setRequestURI("/app/path");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getContextPath()).isEqualTo("");
			assertThat(actual.getRequestURI()).isEqualTo("/path");
		}

		@Test
		public void requestUriWithTrailingSlash() throws Exception {
			request.addHeader(X_FORWARDED_PREFIX, "/");
			request.setContextPath("/app");
			request.setRequestURI("/app/path/");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getContextPath()).isEqualTo("");
			assertThat(actual.getRequestURI()).isEqualTo("/path/");
		}

		@Test
		public void requestUriPreserveEncoding() throws Exception {
			request.setContextPath("/app");
			request.setRequestURI("/app/path%20with%20spaces/");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getContextPath()).isEqualTo("/app");
			assertThat(actual.getRequestURI()).isEqualTo("/app/path%20with%20spaces/");
			assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/app/path%20with%20spaces/");
		}

		@Test
		public void requestUriEqualsContextPath() throws Exception {
			request.addHeader(X_FORWARDED_PREFIX, "/");
			request.setContextPath("/app");
			request.setRequestURI("/app");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getContextPath()).isEqualTo("");
			assertThat(actual.getRequestURI()).isEqualTo("/");
		}

		@Test
		public void requestUriRootUrl() throws Exception {
			request.addHeader(X_FORWARDED_PREFIX, "/");
			request.setContextPath("/app");
			request.setRequestURI("/app/");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getContextPath()).isEqualTo("");
			assertThat(actual.getRequestURI()).isEqualTo("/");
		}

		@Test
		public void requestUriPreserveSemicolonContent() throws Exception {
			request.setContextPath("");
			request.setRequestURI("/path;a=b/with/semicolon");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getContextPath()).isEqualTo("");
			assertThat(actual.getRequestURI()).isEqualTo("/path;a=b/with/semicolon");
			assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/path;a=b/with/semicolon");
		}

		@Test
		public void caseInsensitiveForwardedPrefix() throws Exception {
			request = new MockHttpServletRequest() {

				@Override // SPR-14372: make it case-sensitive
				public String getHeader(String header) {
					Enumeration<String> names = getHeaderNames();
					while (names.hasMoreElements()) {
						String name = names.nextElement();
						if (name.equals(header)) {
							return super.getHeader(header);
						}
					}
					return null;
				}
			};
			request.addHeader(X_FORWARDED_PREFIX, "/prefix");
			request.setRequestURI("/path");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getRequestURI()).isEqualTo("/prefix/path");
		}

		@Test
		public void requestUriWithForwardedPrefix() throws Exception {
			request.addHeader(X_FORWARDED_PREFIX, "/prefix");
			request.setRequestURI("/mvc-showcase");

			HttpServletRequest actual = filterAndGetWrappedRequest();
			assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/prefix/mvc-showcase");
		}

		@Test
		public void requestUriWithForwardedPrefixTrailingSlash() throws Exception {
			request.addHeader(X_FORWARDED_PREFIX, "/prefix/");
			request.setRequestURI("/mvc-showcase");

			HttpServletRequest actual = filterAndGetWrappedRequest();
			assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/prefix/mvc-showcase");
		}

		@Test
		void shouldConcatenatePrefixes() throws Exception {
			request.addHeader(X_FORWARDED_PREFIX, "/first,/second");
			request.setRequestURI("/mvc-showcase");

			HttpServletRequest actual = filterAndGetWrappedRequest();
			assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/first/second/mvc-showcase");
		}

		@Test
		void shouldConcatenatePrefixesWithTrailingSlashes() throws Exception {
			request.addHeader(X_FORWARDED_PREFIX, "/first/,/second//");
			request.setRequestURI("/mvc-showcase");

			HttpServletRequest actual = filterAndGetWrappedRequest();
			assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/first/second/mvc-showcase");
		}

		@Test
		public void requestURLNewStringBuffer() throws Exception {
			request.addHeader(X_FORWARDED_PREFIX, "/prefix/");
			request.setRequestURI("/mvc-showcase");

			HttpServletRequest actual = filterAndGetWrappedRequest();
			actual.getRequestURL().append("?key=value");
			assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/prefix/mvc-showcase");
		}
	}

	@Nested
	class ForwardedFor {

		@Test
		public void xForwardedForEmpty() throws Exception {
			request.addHeader(X_FORWARDED_FOR, "");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getRemoteAddr()).isEqualTo(MockHttpServletRequest.DEFAULT_REMOTE_ADDR);
			assertThat(actual.getRemoteHost()).isEqualTo(MockHttpServletRequest.DEFAULT_REMOTE_HOST);
			assertThat(actual.getRemotePort()).isEqualTo(MockHttpServletRequest.DEFAULT_SERVER_PORT);
		}

		@Test
		public void xForwardedForSingleIdentifier() throws Exception {
			request.addHeader(X_FORWARDED_FOR, "203.0.113.195");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getRemoteAddr()).isEqualTo(actual.getRemoteHost()).isEqualTo("203.0.113.195");
			assertThat(actual.getRemotePort()).isEqualTo(MockHttpServletRequest.DEFAULT_SERVER_PORT);
		}

		@Test
		public void xForwardedForMultipleIdentifiers() throws Exception {
			request.addHeader(X_FORWARDED_FOR, "203.0.113.195, 70.41.3.18, 150.172.238.178");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getRemoteAddr()).isEqualTo(actual.getRemoteHost()).isEqualTo("203.0.113.195");
			assertThat(actual.getRemotePort()).isEqualTo(MockHttpServletRequest.DEFAULT_SERVER_PORT);
		}

		@Test
		public void forwardedForIpV4Identifier() throws Exception {
			request.addHeader(FORWARDED, "for=203.0.113.195");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getRemoteAddr()).isEqualTo(actual.getRemoteHost()).isEqualTo("203.0.113.195");
			assertThat(actual.getRemotePort()).isEqualTo(MockHttpServletRequest.DEFAULT_SERVER_PORT);
		}

		@Test
		public void forwardedForIpV6Identifier() throws Exception {
			request.addHeader(FORWARDED, "for=\"[2001:db8:cafe::17]\"");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getRemoteAddr()).isEqualTo(actual.getRemoteHost()).isEqualTo("[2001:db8:cafe::17]");
			assertThat(actual.getRemotePort()).isEqualTo(MockHttpServletRequest.DEFAULT_SERVER_PORT);
		}

		@Test
		public void forwardedForIpV4IdentifierWithPort() throws Exception {
			request.addHeader(FORWARDED, "for=\"203.0.113.195:47011\"");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getRemoteAddr()).isEqualTo(actual.getRemoteHost()).isEqualTo("203.0.113.195");
			assertThat(actual.getRemotePort()).isEqualTo(47011);
		}

		@Test
		public void forwardedForIpV6IdentifierWithPort() throws Exception {
			request.addHeader(FORWARDED, "For=\"[2001:db8:cafe::17]:47011\"");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getRemoteAddr()).isEqualTo(actual.getRemoteHost()).isEqualTo("[2001:db8:cafe::17]");
			assertThat(actual.getRemotePort()).isEqualTo(47011);
		}

		@Test
		public void forwardedForMultipleIdentifiers() throws Exception {
			request.addHeader(FORWARDED, "for=203.0.113.195;proto=http, for=\"[2001:db8:cafe::17]\", for=unknown");
			HttpServletRequest actual = filterAndGetWrappedRequest();

			assertThat(actual.getRemoteAddr()).isEqualTo(actual.getRemoteHost()).isEqualTo("203.0.113.195");
			assertThat(actual.getRemotePort()).isEqualTo(MockHttpServletRequest.DEFAULT_SERVER_PORT);
		}

		@Test  // gh-26748
		public void forwardedForInvalidIpV6Address() {
			request.addHeader(FORWARDED, "for=\"2a02:918:175:ab60:45ee:c12c:dac1:808b\"");
			assertThatIllegalArgumentException().isThrownBy(
					ForwardedHeaderFilterTests.this::filterAndGetWrappedRequest);
		}
	}

	@Nested
	class SendRedirect {

		@Test
		public void sendRedirectWithAbsolutePath() throws Exception {
			request.addHeader(X_FORWARDED_PROTO, "https");
			request.addHeader(X_FORWARDED_HOST, "example.com");
			request.addHeader(X_FORWARDED_PORT, "443");

			String redirectedUrl = sendRedirect("/foo/bar");
			assertThat(redirectedUrl).isEqualTo("https://example.com/foo/bar");
		}

		@Test // SPR-16506
		public void sendRedirectWithAbsolutePathQueryParamAndFragment() throws Exception {
			request.addHeader(X_FORWARDED_PROTO, "https");
			request.addHeader(X_FORWARDED_HOST, "example.com");
			request.addHeader(X_FORWARDED_PORT, "443");
			request.setQueryString("oldqp=1");

			String redirectedUrl = sendRedirect("/foo/bar?newqp=2#fragment");
			assertThat(redirectedUrl).isEqualTo("https://example.com/foo/bar?newqp=2#fragment");
		}

		@Test
		public void sendRedirectWithContextPath() throws Exception {
			request.addHeader(X_FORWARDED_PROTO, "https");
			request.addHeader(X_FORWARDED_HOST, "example.com");
			request.addHeader(X_FORWARDED_PORT, "443");
			request.setContextPath("/context");

			String redirectedUrl = sendRedirect("/context/foo/bar");
			assertThat(redirectedUrl).isEqualTo("https://example.com/context/foo/bar");
		}

		@Test
		public void sendRedirectWithRelativePath() throws Exception {
			request.addHeader(X_FORWARDED_PROTO, "https");
			request.addHeader(X_FORWARDED_HOST, "example.com");
			request.addHeader(X_FORWARDED_PORT, "443");
			request.setRequestURI("/parent/");

			String redirectedUrl = sendRedirect("foo/bar");
			assertThat(redirectedUrl).isEqualTo("https://example.com/parent/foo/bar");
		}

		@Test
		public void sendRedirectWithFileInPathAndRelativeRedirect() throws Exception {
			request.addHeader(X_FORWARDED_PROTO, "https");
			request.addHeader(X_FORWARDED_HOST, "example.com");
			request.addHeader(X_FORWARDED_PORT, "443");
			request.setRequestURI("/context/a");

			String redirectedUrl = sendRedirect("foo/bar");
			assertThat(redirectedUrl).isEqualTo("https://example.com/context/foo/bar");
		}

		@Test
		public void sendRedirectWithRelativePathIgnoresFile() throws Exception {
			request.addHeader(X_FORWARDED_PROTO, "https");
			request.addHeader(X_FORWARDED_HOST, "example.com");
			request.addHeader(X_FORWARDED_PORT, "443");
			request.setRequestURI("/parent");

			String redirectedUrl = sendRedirect("foo/bar");
			assertThat(redirectedUrl).isEqualTo("https://example.com/foo/bar");
		}

		@Test
		public void sendRedirectWithLocationDotDotPath() throws Exception {
			request.addHeader(X_FORWARDED_PROTO, "https");
			request.addHeader(X_FORWARDED_HOST, "example.com");
			request.addHeader(X_FORWARDED_PORT, "443");

			String redirectedUrl = sendRedirect("parent/../foo/bar");
			assertThat(redirectedUrl).isEqualTo("https://example.com/foo/bar");
		}

		@Test
		public void sendRedirectWithLocationHasScheme() throws Exception {
			request.addHeader(X_FORWARDED_PROTO, "https");
			request.addHeader(X_FORWARDED_HOST, "example.com");
			request.addHeader(X_FORWARDED_PORT, "443");

			String location = "http://company.example/foo/bar";
			String redirectedUrl = sendRedirect(location);
			assertThat(redirectedUrl).isEqualTo(location);
		}

		@Test
		public void sendRedirectWithLocationSlashSlash() throws Exception {
			request.addHeader(X_FORWARDED_PROTO, "https");
			request.addHeader(X_FORWARDED_HOST, "example.com");
			request.addHeader(X_FORWARDED_PORT, "443");

			String location = "//other.info/foo/bar";
			String redirectedUrl = sendRedirect(location);
			assertThat(redirectedUrl).isEqualTo(("https:" + location));
		}

		@Test
		public void sendRedirectWithLocationSlashSlashParentDotDot() throws Exception {
			request.addHeader(X_FORWARDED_PROTO, "https");
			request.addHeader(X_FORWARDED_HOST, "example.com");
			request.addHeader(X_FORWARDED_PORT, "443");

			String location = "//other.info/parent/../foo/bar";
			String redirectedUrl = sendRedirect(location);
			assertThat(redirectedUrl).isEqualTo(("https:" + location));
		}

		@Test
		public void sendRedirectWithNoXForwardedAndAbsolutePath() throws Exception {
			String redirectedUrl = sendRedirect("/foo/bar");
			assertThat(redirectedUrl).isEqualTo("/foo/bar");
		}

		@Test
		public void sendRedirectWithNoXForwardedAndDotDotPath() throws Exception {
			String redirectedUrl = sendRedirect("../foo/bar");
			assertThat(redirectedUrl).isEqualTo("../foo/bar");
		}

		@Test
		public void sendRedirectWhenRequestOnlyAndXForwardedThenUsesRelativeRedirects() throws Exception {
			request.addHeader(X_FORWARDED_PROTO, "https");
			request.addHeader(X_FORWARDED_HOST, "example.com");
			request.addHeader(X_FORWARDED_PORT, "443");
			filter.setRelativeRedirects(true);
			String location = sendRedirect("/a");

			assertThat(location).isEqualTo("/a");
		}

		@Test
		public void sendRedirectWhenRequestOnlyAndNoXForwardedThenUsesRelativeRedirects() throws Exception {
			filter.setRelativeRedirects(true);
			String location = sendRedirect("/a");

			assertThat(location).isEqualTo("/a");
		}

		private String sendRedirect(final String location) throws ServletException, IOException {
			Filter redirectFilter = new OncePerRequestFilter() {
				@Override
				protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
						FilterChain chain) throws IOException {

					res.sendRedirect(location);
				}
			};
			MockHttpServletResponse response = new MockHttpServletResponse();
			FilterChain filterChain = new MockFilterChain(mock(HttpServlet.class), filter, redirectFilter);
			filterChain.doFilter(request, response);
			return response.getRedirectedUrl();
		}
	}

	private HttpServletRequest filterAndGetWrappedRequest() throws ServletException, IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilterInternal(this.request, response, this.filterChain);
		return (HttpServletRequest) this.filterChain.getRequest();
	}

}
