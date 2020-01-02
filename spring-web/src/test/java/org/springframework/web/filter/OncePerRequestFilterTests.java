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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockFilterChain;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OncePerRequestFilter}.
 *
 * @author Rossen Stoyanchev
 * @since 5.1.9
 */
public class OncePerRequestFilterTests {

	private final TestOncePerRequestFilter filter = new TestOncePerRequestFilter();

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
	public void filterOnce() throws ServletException, IOException {

		// Already filtered
		this.request.setAttribute(this.filter.getAlreadyFilteredAttributeName(), Boolean.TRUE);

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		assertThat(this.filter.didFilter).isFalse();
		assertThat(this.filter.didFilterNestedErrorDispatch).isFalse();

		// Remove already filtered
		this.request.removeAttribute(this.filter.getAlreadyFilteredAttributeName());
		this.filter.reset();

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		assertThat(this.filter.didFilter).isTrue();
		assertThat(this.filter.didFilterNestedErrorDispatch).isFalse();
	}

	@Test
	public void shouldNotFilterErrorDispatch() throws ServletException, IOException {

		initErrorDispatch();

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		assertThat(this.filter.didFilter).isFalse();
		assertThat(this.filter.didFilterNestedErrorDispatch).isFalse();
	}

	@Test
	public void shouldNotFilterNestedErrorDispatch() throws ServletException, IOException {

		initErrorDispatch();
		this.request.setAttribute(this.filter.getAlreadyFilteredAttributeName(), Boolean.TRUE);

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		assertThat(this.filter.didFilter).isFalse();
		assertThat(this.filter.didFilterNestedErrorDispatch).isFalse();
	}

	@Test // gh-23196
	public void filterNestedErrorDispatch() throws ServletException, IOException {

		// Opt in for ERROR dispatch
		this.filter.setShouldNotFilterErrorDispatch(false);

		this.request.setAttribute(this.filter.getAlreadyFilteredAttributeName(), Boolean.TRUE);
		initErrorDispatch();

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		assertThat(this.filter.didFilter).isFalse();
		assertThat(this.filter.didFilterNestedErrorDispatch).isTrue();
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


		public void setShouldNotFilterErrorDispatch(boolean shouldNotFilterErrorDispatch) {
			this.shouldNotFilterErrorDispatch = shouldNotFilterErrorDispatch;
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
				FilterChain filterChain) throws ServletException, IOException {

			this.didFilterNestedErrorDispatch = true;
			super.doFilterNestedErrorDispatch(request, response, filterChain);
		}
	}

}
