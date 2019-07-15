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
import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockFilterChain;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.util.WebUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link OncePerRequestFilter}.
 * @author Rossen Stoyanchev
 * @since 5.1.9
 */
public class OncePerRequestFilterTests {

	private final TestOncePerRequestFilter filter = new TestOncePerRequestFilter();

	private MockHttpServletRequest request;

	private MockFilterChain filterChain;


	@Before
	@SuppressWarnings("serial")
	public void setup() throws Exception {
		this.request = new MockHttpServletRequest();
		this.request.setScheme("http");
		this.request.setServerName("localhost");
		this.request.setServerPort(80);
		this.filterChain = new MockFilterChain(new HttpServlet() {});
	}


	@Test
	public void filterOnce() throws ServletException, IOException {

		// Already filtered
		this.request.setAttribute(this.filter.getAlreadyFilteredAttributeName(), Boolean.TRUE);

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		assertFalse(this.filter.didFilter);
		assertFalse(this.filter.didFilterNestedErrorDispatch);

		// Remove already filtered
		this.request.removeAttribute(this.filter.getAlreadyFilteredAttributeName());
		this.filter.reset();

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		assertTrue(this.filter.didFilter);
		assertFalse(this.filter.didFilterNestedErrorDispatch);
	}

	@Test
	public void shouldNotFilterErrorDispatch() throws ServletException, IOException {

		initErrorDispatch();

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		assertFalse(this.filter.didFilter);
		assertFalse(this.filter.didFilterNestedErrorDispatch);
	}

	@Test
	public void shouldNotFilterNestedErrorDispatch() throws ServletException, IOException {

		initErrorDispatch();
		this.request.setAttribute(this.filter.getAlreadyFilteredAttributeName(), Boolean.TRUE);

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		assertFalse(this.filter.didFilter);
		assertFalse(this.filter.didFilterNestedErrorDispatch);
	}

	@Test // gh-23196
	public void filterNestedErrorDispatch() throws ServletException, IOException {

		// Opt in for ERROR dispatch
		this.filter.setShouldNotFilterErrorDispatch(false);

		this.request.setAttribute(this.filter.getAlreadyFilteredAttributeName(), Boolean.TRUE);
		initErrorDispatch();

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		assertFalse(this.filter.didFilter);
		assertTrue(this.filter.didFilterNestedErrorDispatch);
	}

	private void initErrorDispatch() {
		this.request.setDispatcherType(DispatcherType.ERROR);
		this.request.setAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE, "/error");
	}


	private static class TestOncePerRequestFilter extends OncePerRequestFilter {

		private boolean shouldNotFilter;

		private boolean shouldNotFilterAsyncDispatch = true;

		private boolean shouldNotFilterErrorDispatch = true;

		private boolean didFilter;

		private boolean didFilterNestedErrorDispatch;


		public void setShouldNotFilter(boolean shouldNotFilter) {
			this.shouldNotFilter = shouldNotFilter;
		}

		public void setShouldNotFilterAsyncDispatch(boolean shouldNotFilterAsyncDispatch) {
			this.shouldNotFilterAsyncDispatch = shouldNotFilterAsyncDispatch;
		}

		public void setShouldNotFilterErrorDispatch(boolean shouldNotFilterErrorDispatch) {
			this.shouldNotFilterErrorDispatch = shouldNotFilterErrorDispatch;
		}


		public boolean didFilter() {
			return this.didFilter;
		}

		public boolean didFilterNestedErrorDispatch() {
			return this.didFilterNestedErrorDispatch;
		}

		public void reset() {
			this.didFilter = false;
			this.didFilterNestedErrorDispatch = false;
		}


		@Override
		protected boolean shouldNotFilter(HttpServletRequest request) {
			return this.shouldNotFilter;
		}

		@Override
		protected boolean shouldNotFilterAsyncDispatch() {
			return this.shouldNotFilterAsyncDispatch;
		}

		@Override
		protected boolean shouldNotFilterErrorDispatch() {
			return this.shouldNotFilterErrorDispatch;
		}

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) {

			this.didFilter = true;
		}

		@Override
		protected void doFilterNestedErrorDispatch(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) {

			this.didFilterNestedErrorDispatch = true;
		}
	}

}
