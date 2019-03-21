/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.mock.web;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Test fixture for {@link MockFilterChain}.
 *
 * @author Rob Winch
 */
public class MockFilterChainTests {

	private ServletRequest request;

	private ServletResponse response;

	@Before
	public void setup() {
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
	}

	@Test(expected=IllegalArgumentException.class)
	public void constructorNullServlet() {
		new MockFilterChain((Servlet) null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void constructorNullFilter() {
		new MockFilterChain(mock(Servlet.class), (Filter) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void doFilterNullRequest() throws Exception {
		MockFilterChain chain = new MockFilterChain();
		chain.doFilter(null, this.response);
	}

	@Test(expected = IllegalArgumentException.class)
	public void doFilterNullResponse() throws Exception {
		MockFilterChain chain = new MockFilterChain();
		chain.doFilter(this.request, null);
	}

	@Test
	public void doFilterEmptyChain() throws Exception {
		MockFilterChain chain = new MockFilterChain();
		chain.doFilter(this.request, this.response);

		assertThat(chain.getRequest(), is(request));
		assertThat(chain.getResponse(), is(response));

		try {
			chain.doFilter(this.request, this.response);
			fail("Expected Exception");
		}
		catch (IllegalStateException ex) {
			assertEquals("This FilterChain has already been called!", ex.getMessage());
		}
	}

	@Test
	public void doFilterWithServlet() throws Exception {
		Servlet servlet = mock(Servlet.class);
		MockFilterChain chain = new MockFilterChain(servlet);
		chain.doFilter(this.request, this.response);
		verify(servlet).service(this.request, this.response);
		try {
			chain.doFilter(this.request, this.response);
			fail("Expected Exception");
		}
		catch (IllegalStateException ex) {
			assertEquals("This FilterChain has already been called!", ex.getMessage());
		}
	}

	@Test
	public void doFilterWithServletAndFilters() throws Exception {
		Servlet servlet = mock(Servlet.class);

		MockFilter filter2 = new MockFilter(servlet);
		MockFilter filter1 = new MockFilter(null);
		MockFilterChain chain = new MockFilterChain(servlet, filter1, filter2);

		chain.doFilter(this.request, this.response);

		assertTrue(filter1.invoked);
		assertTrue(filter2.invoked);

		verify(servlet).service(this.request, this.response);

		try {
			chain.doFilter(this.request, this.response);
			fail("Expected Exception");
		}
		catch (IllegalStateException ex) {
			assertEquals("This FilterChain has already been called!", ex.getMessage());
		}
	}


	private static class MockFilter implements Filter {

		private final Servlet servlet;

		private boolean invoked;

		public MockFilter(Servlet servlet) {
			this.servlet = servlet;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {

			this.invoked = true;

			if (this.servlet != null) {
				this.servlet.service(request, response);
			}
			else {
				chain.doFilter(request, response);
			}
		}

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		@Override
		public void destroy() {
		}
	}

}
