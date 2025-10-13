/*
 * Copyright 2002-present the original author or authors.
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
import java.util.stream.Stream;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.web.testfixture.servlet.MockFilterChain;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link OncePerRequestFilter}.
 *
 * @author Rossen Stoyanchev
 * @author Simone Conte
 * @since 5.1.9
 */
class OncePerRequestFilterTests {

	private final TestOncePerRequestFilter filter = new TestOncePerRequestFilter();

	private MockHttpServletRequest request;

	private MockHttpServletResponse response = new MockHttpServletResponse();

	private MockFilterChain filterChain;


	@BeforeEach
	@SuppressWarnings("serial")
	public void setup() throws Exception {
		this.request = new MockHttpServletRequest();
		this.request.setScheme("http");
		this.request.setServerName("localhost");
		this.request.setServerPort(80);
		this.filterChain = spy(new MockFilterChain(new HttpServlet() {}));
	}

	@Test
	void filterInternal() throws ServletException, IOException {
		this.filter.doFilter(this.request, this.response, this.filterChain);

		assertThat(this.filter.didFilter).isTrue();
		assertThat(this.filter.didFilterNestedErrorDispatch).isFalse();
		verifyNoInteractions(this.filterChain);
	}

	@Test
	void filterOnce() throws ServletException, IOException {

		// Already filtered
		this.request.setAttribute(this.filter.getAlreadyFilteredAttributeName(), Boolean.TRUE);

		this.filter.doFilter(this.request, this.response, this.filterChain);
		assertThat(this.filter.didFilter).isFalse();
		assertThat(this.filter.didFilterNestedErrorDispatch).isFalse();
		verify(this.filterChain).doFilter(this.request, this.response);

		// Remove already filtered
		this.request.removeAttribute(this.filter.getAlreadyFilteredAttributeName());
		this.filter.reset();

		this.filter.doFilter(this.request, this.response, this.filterChain);
		assertThat(this.filter.didFilter).isTrue();
		assertThat(this.filter.didFilterNestedErrorDispatch).isFalse();
		verify(this.filterChain).doFilter(this.request, this.response);
	}

	@Test
	void shouldNotFilterErrorDispatch() throws ServletException, IOException {

		initErrorDispatch();

		this.filter.doFilter(this.request, this.response, this.filterChain);
		assertThat(this.filter.didFilter).isFalse();
		assertThat(this.filter.didFilterNestedErrorDispatch).isFalse();
		verify(this.filterChain).doFilter(this.request, this.response);
	}

	@Test
	void shouldNotFilterNestedErrorDispatch() throws ServletException, IOException {

		initErrorDispatch();
		this.request.setAttribute(this.filter.getAlreadyFilteredAttributeName(), Boolean.TRUE);

		this.filter.doFilter(this.request, this.response, this.filterChain);
		assertThat(this.filter.didFilter).isFalse();
		assertThat(this.filter.didFilterNestedErrorDispatch).isFalse();
		verify(this.filterChain).doFilter(this.request, this.response);
	}

	@Test // gh-23196
	public void filterNestedErrorDispatch() throws ServletException, IOException {

		// Opt in for ERROR dispatch
		this.filter.setShouldNotFilterErrorDispatch(false);

		this.request.setAttribute(this.filter.getAlreadyFilteredAttributeName(), Boolean.TRUE);
		initErrorDispatch();

		this.filter.doFilter(this.request, this.response, this.filterChain);
		assertThat(this.filter.didFilter).isFalse();
		assertThat(this.filter.didFilterNestedErrorDispatch).isTrue();
		verify(this.filterChain).doFilter(this.request, this.response);
	}

	@ParameterizedTest
	@MethodSource
	public void shouldNotFilterForUri(String uri, boolean shouldFilterInternal) throws ServletException, IOException {
		ShouldNotFilterRequestFilter filter = new ShouldNotFilterRequestFilter();

		this.request.setRequestURI(uri);

		filter.doFilter(this.request, this.response, this.filterChain);

		assertThat(filter.didFilter).isEqualTo(shouldFilterInternal);
		verify(this.filterChain, times(shouldFilterInternal ? 0 : 1)).doFilter(this.request, this.response);
	}

	static Stream<Arguments> shouldNotFilterForUri() {
		return Stream.of(
				Arguments.of("/skip", false),
				Arguments.of("/skip/something", false),
				Arguments.of("//skip", true),
				Arguments.of("", true),
				Arguments.of("/", true),
				Arguments.of("/do_not_skip", true),
				Arguments.of("//do_not_skip", true)
		);
	}

	@ParameterizedTest
	@MethodSource
	public void shouldFilterForUri(String uri, boolean shouldFilterInternal) throws ServletException, IOException {
		ShouldFilterRequestFilter filter = new ShouldFilterRequestFilter();

		this.request.setRequestURI(uri);

		filter.doFilter(this.request, this.response, this.filterChain);

		assertThat(filter.didFilter).isEqualTo(shouldFilterInternal);
		verify(this.filterChain, times(shouldFilterInternal ? 0 : 1)).doFilter(this.request, this.response);
	}

	static Stream<Arguments> shouldFilterForUri() {
		return Stream.of(
				Arguments.of("/skip", false),
				Arguments.of("/skip/something", false),
				Arguments.of("//skip", false),
				Arguments.of("", false),
				Arguments.of("/", false),
				Arguments.of("/do_not_skip", true),
				Arguments.of("//do_not_skip", false),
				Arguments.of("/do_not_skip/something", true)
		);
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

	private static class ShouldNotFilterRequestFilter extends OncePerRequestFilter {

		private boolean didFilter;

		@Override
		protected boolean shouldNotFilter(HttpServletRequest request) {
			return request.getRequestURI().startsWith("/skip");
		}

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) {

			this.didFilter = true;
		}
	}

	private static class ShouldFilterRequestFilter extends OncePerRequestFilter {

		private boolean didFilter;

		@Override
		protected boolean shouldFilter(HttpServletRequest request) {
			return request.getRequestURI().startsWith("/do_not_skip");
		}

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) {

			this.didFilter = true;
		}
	}

}
