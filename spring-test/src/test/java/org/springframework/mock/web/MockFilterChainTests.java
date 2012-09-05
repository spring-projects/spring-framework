/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.mock.web;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Test;

/**
 *
 * @author Rob Winch
 *
 */
public class MockFilterChainTests {

	@Test(expected=IllegalArgumentException.class)
	public void constructorNullFilters() {
		new MockFilterChain((Filter[])null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void constructorFiltersContainsNull() {
		new MockFilterChain((Filter)null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void constructorFiltersEmpty() {
		new MockFilterChain(new Filter[] {});
	}

	@Test(expected=IllegalArgumentException.class)
	public void constructorNullServlet() {
		new MockFilterChain((Servlet)null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void constructorServletNullFilters() {
		new MockFilterChain(createMock(Servlet.class), (Filter[]) null);
	}

	@Test
	public void constructorServletEmptyFilters() {
		new MockFilterChain(createMock(Servlet.class), new Filter[] {});
	}

	@Test(expected=IllegalArgumentException.class)
	public void constructorServletFiltersContainsNull() {
		new MockFilterChain(createMock(Servlet.class), (Filter) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void doFilterNullRequest() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		chain.doFilter(null, response);
	}

	@Test(expected = IllegalArgumentException.class)
	public void doFilterNullResponse() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockFilterChain chain = new MockFilterChain();
		chain.doFilter(request, null);
	}

	@Test
	public void doFilterDefaultConstructor() throws Exception {
		ServletRequest request = new MockHttpServletRequest();
		ServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		chain.doFilter(request, response);
		assertThat(chain.getRequest(), is(request));
		assertThat(chain.getResponse(), is(response));
	}

	@Test(expected = IllegalStateException.class)
	public void doFilterDefaultConstructorMultiple() throws Exception {
		ServletRequest request = new MockHttpServletRequest();
		ServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		chain.doFilter(request, response);
		chain.doFilter(request, response);
	}

	@Test
	public void doFilterConstructorFilters() throws Exception {
		Filter filter1 = createMock(Filter.class);
		Filter filter2 = createMock(Filter.class);

		ServletRequest request = new MockHttpServletRequest();
		ServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain(filter1, filter2);
		filter1.doFilter(request, response, chain);
		filter2.doFilter(request, response, chain);
		replay(filter1, filter2);

		chain.doFilter(request, response);
		chain.doFilter(request, response);

		verify(filter1,filter2);

		try {
			chain.doFilter(request, response);
			fail("Expected Exception");
		} catch(IllegalStateException success) {}
	}

	@Test
	public void doFilterConstructorServletFilters() throws Exception {
		Servlet servlet = createMock(Servlet.class);
		Filter filter1 = createMock(Filter.class);
		Filter filter2 = createMock(Filter.class);

		ServletRequest request = new MockHttpServletRequest();
		ServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain(servlet, filter1, filter2);
		filter1.doFilter(request, response, chain);
		filter2.doFilter(request, response, chain);
		servlet.service(request, response);
		replay(servlet, filter1, filter2);

		chain.doFilter(request, response);
		chain.doFilter(request, response);
		chain.doFilter(request, response);

		verify(servlet,filter1,filter2);

		try {
			chain.doFilter(request, response);
			fail("Expected Exception");
		} catch(IllegalStateException success) {}
	}


	@Test
	public void doFilterConstructorServlet() throws Exception {
		Servlet servlet = createMock(Servlet.class);

		ServletRequest request = new MockHttpServletRequest();
		ServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain(servlet);
		servlet.service(request, response);
		replay(servlet);

		chain.doFilter(request, response);

		verify(servlet);

		try {
			chain.doFilter(request, response);
			fail("Expected Exception");
		} catch(IllegalStateException success) {}
	}
}
