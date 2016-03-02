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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

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
	public void xForwardedHeaders() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(80);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("X-Forwarded-Host", "84.198.58.199");
		request.addHeader("X-Forwarded-Port", "443");
		request.addHeader("foo", "bar");

		MockFilterChain chain = new MockFilterChain(new HttpServlet() {});

		this.filter.doFilter(request, new MockHttpServletResponse(), chain);

		HttpServletRequest actual = (HttpServletRequest) chain.getRequest();
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


	private void testShouldFilter(String headerName) throws ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(headerName, "1");
		assertFalse(this.filter.shouldNotFilter(request));
	}


}
