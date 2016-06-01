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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockFilterChain;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ForwardedHeaderFilter}.
 * @author Rossen Stoyanchev
 */
public class ForwardedHeaderFilterTests {

	private final ForwardedHeaderFilter filter = new ForwardedHeaderFilter();

	private MockHttpServletRequest request;

	private MockFilterChain filterChain;


	@Before
	@SuppressWarnings("serial")
	public void setUp() throws Exception {
		this.request = new MockHttpServletRequest();
		this.request.setScheme("http");
		this.request.setServerName("localhost");
		this.request.setServerPort(80);
		this.filterChain = new MockFilterChain(new HttpServlet() {});
	}


	@Test(expected = IllegalArgumentException.class)
	public void contextPathNull() {
		this.filter.setContextPath(null);
	}

	@Test
	public void contextPathEmpty() throws Exception {
		this.filter.setContextPath("");
		assertEquals("", filterAndGetContextPath());
	}

	@Test
	public void contextPathWithExtraSpaces() throws Exception {
		this.filter.setContextPath("  /foo  ");
		assertEquals("/foo", filterAndGetContextPath());
	}

	@Test
	public void contextPathWithNoLeadingSlash() throws Exception {
		this.filter.setContextPath("foo");
		assertEquals("/foo", filterAndGetContextPath());
	}

	@Test
	public void contextPathWithTrailingSlash() throws Exception {
		this.filter.setContextPath("/foo/bar/");
		assertEquals("/foo/bar", filterAndGetContextPath());
	}

	@Test
	public void contextPathWithTrailingSlashes() throws Exception {
		this.filter.setContextPath("/foo/bar/baz///");
		assertEquals("/foo/bar/baz", filterAndGetContextPath());
	}

	@Test
	public void requestUri() throws Exception {
		this.filter.setContextPath("/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app/path");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("/", actual.getContextPath());
		assertEquals("/path", actual.getRequestURI());
	}

	@Test
	public void requestUriWithTrailingSlash() throws Exception {
		this.filter.setContextPath("/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app/path/");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("/", actual.getContextPath());
		assertEquals("/path/", actual.getRequestURI());
	}
	@Test
	public void requestUriEqualsContextPath() throws Exception {
		this.filter.setContextPath("/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("/", actual.getContextPath());
		assertEquals("/", actual.getRequestURI());
	}

	@Test
	public void requestUriRootUrl() throws Exception {
		this.filter.setContextPath("/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app/");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("/", actual.getContextPath());
		assertEquals("/", actual.getRequestURI());
	}

	@Test
	public void shouldFilter() throws Exception {
		testShouldFilter("Forwarded");
		testShouldFilter("X-Forwarded-Host");
		testShouldFilter("X-Forwarded-Port");
		testShouldFilter("X-Forwarded-Proto");
	}

	@Test
	public void shouldNotFilter() throws Exception {
		assertTrue(this.filter.shouldNotFilter(new MockHttpServletRequest()));
	}

	@Test
	public void forwardedRequest() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader("X-Forwarded-Proto", "https");
		this.request.addHeader("X-Forwarded-Host", "84.198.58.199");
		this.request.addHeader("X-Forwarded-Port", "443");
		this.request.addHeader("foo", "bar");

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		assertEquals("https://84.198.58.199/mvc-showcase", actual.getRequestURL().toString());
		assertEquals("https", actual.getScheme());
		assertEquals("84.198.58.199", actual.getServerName());
		assertEquals(443, actual.getServerPort());
		assertTrue(actual.isSecure());

		assertNull(actual.getHeader("X-Forwarded-Proto"));
		assertNull(actual.getHeader("X-Forwarded-Host"));
		assertNull(actual.getHeader("X-Forwarded-Port"));
		assertEquals("bar", actual.getHeader("foo"));
	}

	@Test
	public void requestUriWithForwardedPrefix() throws Exception {
		this.request.addHeader("X-Forwarded-Prefix", "/prefix");
		this.request.setRequestURI("/mvc-showcase");

		HttpServletRequest actual = filterAndGetWrappedRequest();
		assertEquals("http://localhost/prefix/mvc-showcase", actual.getRequestURL()
				.toString());
	}

	@Test
	public void requestUriWithForwardedPrefixTrailingSlash() throws Exception {
		this.request.addHeader("X-Forwarded-Prefix", "/prefix/");
		this.request.setRequestURI("/mvc-showcase");

		HttpServletRequest actual = filterAndGetWrappedRequest();
		assertEquals("http://localhost/prefix/mvc-showcase", actual.getRequestURL()
				.toString());
	}

	@Test
	public void contextPathWithForwardedPrefix() throws Exception {
		this.request.addHeader("X-Forwarded-Prefix", "/prefix");
		this.request.setContextPath("/mvc-showcase");

		String actual = filterAndGetContextPath();
		assertEquals("/prefix/mvc-showcase", actual);
	}

	@Test
	public void contextPathWithForwardedPrefixTrailingSlash() throws Exception {
		this.request.addHeader("X-Forwarded-Prefix", "/prefix/");
		this.request.setContextPath("/mvc-showcase");

		String actual = filterAndGetContextPath();
		assertEquals("/prefix/mvc-showcase", actual);
	}

	private String filterAndGetContextPath() throws ServletException, IOException {
		return filterAndGetWrappedRequest().getContextPath();
	}

	private HttpServletRequest filterAndGetWrappedRequest() throws ServletException, IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilterInternal(this.request, response, this.filterChain);
		return (HttpServletRequest) this.filterChain.getRequest();
	}

	private void testShouldFilter(String headerName) throws ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(headerName, "1");
		assertFalse(this.filter.shouldNotFilter(request));
	}

}
