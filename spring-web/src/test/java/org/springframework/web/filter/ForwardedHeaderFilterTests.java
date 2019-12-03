/*
 * Copyright 2002-2019 the original author or authors.
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

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.test.MockFilterChain;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ForwardedHeaderFilter}.
 *
 * @author Rossen Stoyanchev
 * @author Eddú Meléndez
 * @author Rob Winch
 */
public class ForwardedHeaderFilterTests {

	private static final String X_FORWARDED_PROTO = "x-forwarded-proto";  // SPR-14372 (case insensitive)
	private static final String X_FORWARDED_HOST = "x-forwarded-host";
	private static final String X_FORWARDED_PORT = "x-forwarded-port";
	private static final String X_FORWARDED_PREFIX = "x-forwarded-prefix";
	private static final String X_FORWARDED_SSL = "x-forwarded-ssl";


	private final ForwardedHeaderFilter filter = new ForwardedHeaderFilter();

	private MockHttpServletRequest request;

	private MockFilterChain filterChain;


	@BeforeEach
	@SuppressWarnings("serial")
	public void setup() throws Exception {
		this.request = new MockHttpServletRequest();
		this.request.setScheme("http");
		this.request.setServerName("localhost");
		this.request.setServerPort(80);
		this.filterChain = new MockFilterChain(new HttpServlet() {});
	}


	@Test
	public void contextPathEmpty() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "");
		assertThat(filterAndGetContextPath()).isEqualTo("");
	}

	@Test
	public void contextPathWithTrailingSlash() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/foo/bar/");
		assertThat(filterAndGetContextPath()).isEqualTo("/foo/bar");
	}

	@Test
	public void contextPathWithTrailingSlashes() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/foo/bar/baz///");
		assertThat(filterAndGetContextPath()).isEqualTo("/foo/bar/baz");
	}

	@Test
	public void contextPathWithForwardedPrefix() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/prefix");
		this.request.setContextPath("/mvc-showcase");

		String actual = filterAndGetContextPath();
		assertThat(actual).isEqualTo("/prefix");
	}

	@Test
	public void contextPathWithForwardedPrefixTrailingSlash() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/prefix/");
		this.request.setContextPath("/mvc-showcase");

		String actual = filterAndGetContextPath();
		assertThat(actual).isEqualTo("/prefix");
	}

	private String filterAndGetContextPath() throws ServletException, IOException {
		return filterAndGetWrappedRequest().getContextPath();
	}

	private HttpServletRequest filterAndGetWrappedRequest() throws ServletException, IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilterInternal(this.request, response, this.filterChain);
		return (HttpServletRequest) this.filterChain.getRequest();
	}


	@Test
	public void contextPathPreserveEncoding() throws Exception {
		this.request.setContextPath("/app%20");
		this.request.setRequestURI("/app%20/path/");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertThat(actual.getContextPath()).isEqualTo("/app%20");
		assertThat(actual.getRequestURI()).isEqualTo("/app%20/path/");
		assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/app%20/path/");
	}

	@Test
	public void requestUri() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app/path");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertThat(actual.getContextPath()).isEqualTo("");
		assertThat(actual.getRequestURI()).isEqualTo("/path");
	}

	@Test
	public void requestUriWithTrailingSlash() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app/path/");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertThat(actual.getContextPath()).isEqualTo("");
		assertThat(actual.getRequestURI()).isEqualTo("/path/");
	}

	@Test
	public void requestUriPreserveEncoding() throws Exception {
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app/path%20with%20spaces/");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertThat(actual.getContextPath()).isEqualTo("/app");
		assertThat(actual.getRequestURI()).isEqualTo("/app/path%20with%20spaces/");
		assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/app/path%20with%20spaces/");
	}

	@Test
	public void requestUriEqualsContextPath() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertThat(actual.getContextPath()).isEqualTo("");
		assertThat(actual.getRequestURI()).isEqualTo("/");
	}

	@Test
	public void requestUriRootUrl() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app/");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertThat(actual.getContextPath()).isEqualTo("");
		assertThat(actual.getRequestURI()).isEqualTo("/");
	}

	@Test
	public void requestUriPreserveSemicolonContent() throws Exception {
		this.request.setContextPath("");
		this.request.setRequestURI("/path;a=b/with/semicolon");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertThat(actual.getContextPath()).isEqualTo("");
		assertThat(actual.getRequestURI()).isEqualTo("/path;a=b/with/semicolon");
		assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/path;a=b/with/semicolon");
	}

	@Test
	public void caseInsensitiveForwardedPrefix() throws Exception {
		this.request = new MockHttpServletRequest() {

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
		this.request.addHeader(X_FORWARDED_PREFIX, "/prefix");
		this.request.setRequestURI("/path");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertThat(actual.getRequestURI()).isEqualTo("/prefix/path");
	}

	@Test
	public void shouldFilter() {
		testShouldFilter("Forwarded");
		testShouldFilter(X_FORWARDED_HOST);
		testShouldFilter(X_FORWARDED_PORT);
		testShouldFilter(X_FORWARDED_PROTO);
		testShouldFilter(X_FORWARDED_SSL);
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

	@Test
	public void forwardedRequest() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "84.198.58.199");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.request.addHeader("foo", "bar");

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		assertThat(actual.getRequestURL().toString()).isEqualTo("https://84.198.58.199/mvc-showcase");
		assertThat(actual.getScheme()).isEqualTo("https");
		assertThat(actual.getServerName()).isEqualTo("84.198.58.199");
		assertThat(actual.getServerPort()).isEqualTo(443);
		assertThat(actual.isSecure()).isTrue();

		assertThat(actual.getHeader(X_FORWARDED_PROTO)).isNull();
		assertThat(actual.getHeader(X_FORWARDED_HOST)).isNull();
		assertThat(actual.getHeader(X_FORWARDED_PORT)).isNull();
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

		this.filter.setRemoveOnly(true);
		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/mvc-showcase");
		assertThat(actual.getScheme()).isEqualTo("http");
		assertThat(actual.getServerName()).isEqualTo("localhost");
		assertThat(actual.getServerPort()).isEqualTo(80);
		assertThat(actual.isSecure()).isFalse();

		assertThat(actual.getHeader(X_FORWARDED_PROTO)).isNull();
		assertThat(actual.getHeader(X_FORWARDED_HOST)).isNull();
		assertThat(actual.getHeader(X_FORWARDED_PORT)).isNull();
		assertThat(actual.getHeader(X_FORWARDED_SSL)).isNull();
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

	@Test
	public void requestUriWithForwardedPrefix() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/prefix");
		this.request.setRequestURI("/mvc-showcase");

		HttpServletRequest actual = filterAndGetWrappedRequest();
		assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/prefix/mvc-showcase");
	}

	@Test
	public void requestUriWithForwardedPrefixTrailingSlash() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/prefix/");
		this.request.setRequestURI("/mvc-showcase");

		HttpServletRequest actual = filterAndGetWrappedRequest();
		assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/prefix/mvc-showcase");
	}

	@Test
	public void requestURLNewStringBuffer() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/prefix/");
		this.request.setRequestURI("/mvc-showcase");

		HttpServletRequest actual = filterAndGetWrappedRequest();
		actual.getRequestURL().append("?key=value");
		assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost/prefix/mvc-showcase");
	}

	@Test
	public void sendRedirectWithAbsolutePath() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");

		String redirectedUrl = sendRedirect("/foo/bar");
		assertThat(redirectedUrl).isEqualTo("https://example.com/foo/bar");
	}

	@Test // SPR-16506
	public void sendRedirectWithAbsolutePathQueryParamAndFragment() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.request.setQueryString("oldqp=1");

		String redirectedUrl = sendRedirect("/foo/bar?newqp=2#fragment");
		assertThat(redirectedUrl).isEqualTo("https://example.com/foo/bar?newqp=2#fragment");
	}

	@Test
	public void sendRedirectWithContextPath() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.request.setContextPath("/context");

		String redirectedUrl = sendRedirect("/context/foo/bar");
		assertThat(redirectedUrl).isEqualTo("https://example.com/context/foo/bar");
	}

	@Test
	public void sendRedirectWithRelativePath() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.request.setRequestURI("/parent/");

		String redirectedUrl = sendRedirect("foo/bar");
		assertThat(redirectedUrl).isEqualTo("https://example.com/parent/foo/bar");
	}

	@Test
	public void sendRedirectWithFileInPathAndRelativeRedirect() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.request.setRequestURI("/context/a");

		String redirectedUrl = sendRedirect("foo/bar");
		assertThat(redirectedUrl).isEqualTo("https://example.com/context/foo/bar");
	}

	@Test
	public void sendRedirectWithRelativePathIgnoresFile() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.request.setRequestURI("/parent");

		String redirectedUrl = sendRedirect("foo/bar");
		assertThat(redirectedUrl).isEqualTo("https://example.com/foo/bar");
	}

	@Test
	public void sendRedirectWithLocationDotDotPath() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");

		String redirectedUrl = sendRedirect("parent/../foo/bar");
		assertThat(redirectedUrl).isEqualTo("https://example.com/foo/bar");
	}

	@Test
	public void sendRedirectWithLocationHasScheme() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");

		String location = "http://company.example/foo/bar";
		String redirectedUrl = sendRedirect(location);
		assertThat(redirectedUrl).isEqualTo(location);
	}

	@Test
	public void sendRedirectWithLocationSlashSlash() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");

		String location = "//other.info/foo/bar";
		String redirectedUrl = sendRedirect(location);
		assertThat(redirectedUrl).isEqualTo(("https:" + location));
	}

	@Test
	public void sendRedirectWithLocationSlashSlashParentDotDot() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");

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
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.filter.setRelativeRedirects(true);
		String location = sendRedirect("/a");

		assertThat(location).isEqualTo("/a");
	}

	@Test
	public void sendRedirectWhenRequestOnlyAndNoXForwardedThenUsesRelativeRedirects() throws Exception {
		this.filter.setRelativeRedirects(true);
		String location = sendRedirect("/a");

		assertThat(location).isEqualTo("/a");
	}

	private String sendRedirect(final String location) throws ServletException, IOException {
		Filter filter = new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
					FilterChain chain) throws IOException {

				res.sendRedirect(location);
			}
		};

		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = new MockFilterChain(mock(HttpServlet.class), this.filter, filter);
		filterChain.doFilter(request, response);

		return response.getRedirectedUrl();
	}

}
