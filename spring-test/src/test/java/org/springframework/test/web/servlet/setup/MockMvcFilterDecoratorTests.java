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

package org.springframework.test.web.servlet.setup;

import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockMvcFilterDecorator}.
 *
 * @author Rob Winch
 * @author Rossen Stoyanchev
 */
class MockMvcFilterDecoratorTests {

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private MockFilterChain filterChain;

	private MockFilter delegate;

	private MockMvcFilterDecorator filter;


	@BeforeEach
	void setup() {
		request = new MockHttpServletRequest();
		request.setContextPath("/context");
		response = new MockHttpServletResponse();
		filterChain = new MockFilterChain();
		delegate = new MockFilter();
	}


	@Test
	void init() throws Exception {
		FilterConfig config = new MockFilterConfig();
		filter = new MockMvcFilterDecorator(delegate, new String[] {"/"});
		filter.init(config);
		assertThat(delegate.filterConfig).isEqualTo(config);
	}

	@Test
	void destroy() {
		filter = new MockMvcFilterDecorator(delegate, new String[] {"/"});
		filter.destroy();
		assertThat(delegate.destroy).isTrue();
	}

	@Test
	void matchExact() throws Exception {
		assertFilterInvoked("/test", "/test");
	}

	@Test
	void matchExactEmpty() throws Exception {
		assertFilterInvoked("", "");
	}

	@Test
	void matchPathMappingAllFolder() throws Exception {
		assertFilterInvoked("/test/this", "*");
		assertFilterInvoked("/test/this", "/*");
	}

	@Test
	void matchPathMappingAll() throws Exception {
		assertFilterInvoked("/test", "*");
		assertFilterInvoked("/test", "/*");
	}

	@Test
	void matchPathMappingAllContextRoot() throws Exception {
		assertFilterInvoked("", "*");
		assertFilterInvoked("", "/*");
	}

	@Test
	void matchPathMappingContextRootAndSlash() throws Exception {
		assertFilterInvoked("/", "*");
		assertFilterInvoked("/", "/*");
	}

	@Test
	void matchPathMappingFolderPatternWithMultiFolderPath() throws Exception {
		assertFilterInvoked("/test/this/here", "/test/*");
	}

	@Test
	void matchPathMappingFolderPattern() throws Exception {
		assertFilterInvoked("/test/this", "/test/*");
	}

	@Test
	void matchPathMappingNoSuffix() throws Exception {
		assertFilterInvoked("/test/", "/test/*");
	}

	@Test
	void matchPathMappingMissingSlash() throws Exception {
		assertFilterInvoked("/test", "/test/*");
	}

	@Test
	void noMatchPathMappingMulti() throws Exception {
		assertFilterNotInvoked("/this/test/here", "/test/*");
	}

	@Test
	void noMatchPathMappingEnd() throws Exception {
		assertFilterNotInvoked("/this/test", "/test/*");
	}

	@Test
	void noMatchPathMappingEndSuffix() throws Exception {
		assertFilterNotInvoked("/test2/", "/test/*");
	}

	@Test
	void noMatchPathMappingMissingSlash() throws Exception {
		assertFilterNotInvoked("/test2", "/test/*");
	}

	@Test
	void noMatchDispatcherType() throws Exception {
		assertFilterNotInvoked(DispatcherType.FORWARD, DispatcherType.REQUEST, "/test", "/test");
	}

	@Test
	void matchExtensionMulti() throws Exception {
		assertFilterInvoked("/test/this/here.html", "*.html");
	}

	@Test
	void matchExtension() throws Exception {
		assertFilterInvoked("/test/this.html", "*.html");
	}

	@Test
	void matchExtensionNoPrefix() throws Exception {
		assertFilterInvoked("/.html", "*.html");
	}

	@Test
	void matchExtensionNoFolder() throws Exception {
		assertFilterInvoked("/test.html", "*.html");
	}

	@Test
	void noMatchExtensionNoSlash() throws Exception {
		assertFilterNotInvoked(".html", "*.html");
	}

	@Test
	void noMatchExtensionSlashEnd() throws Exception {
		assertFilterNotInvoked("/index.html/", "*.html");
	}

	@Test
	void noMatchExtensionPeriodEnd() throws Exception {
		assertFilterNotInvoked("/index.html.", "*.html");
	}

	@Test
	void noMatchExtensionLarger() throws Exception {
		assertFilterNotInvoked("/index.htm", "*.html");
	}

	@Test
	void noMatchInvalidPattern() throws Exception {
		// pattern uses extension mapping but starts with / (treated as exact match)
		assertFilterNotInvoked("/index.html", "/*.html");
	}

	/*
	 * Below are tests from Table 12-1 of the Servlet Specification
	 */
	@Test
	void specPathMappingMultiFolderPattern() throws Exception {
		assertFilterInvoked("/foo/bar/index.html", "/foo/bar/*");
	}

	@Test
	void specPathMappingMultiFolderPatternAlternate() throws Exception {
		assertFilterInvoked("/foo/bar/index.bop", "/foo/bar/*");
	}

	@Test
	void specPathMappingNoSlash() throws Exception {
		assertFilterInvoked("/baz", "/baz/*");
	}

	@Test
	void specPathMapping() throws Exception {
		assertFilterInvoked("/baz/index.html", "/baz/*");
	}

	@Test
	void specExactMatch() throws Exception {
		assertFilterInvoked("/catalog", "/catalog");
	}

	@Test
	void specExtensionMappingSingleFolder() throws Exception {
		assertFilterInvoked("/catalog/racecar.bop", "*.bop");
	}

	@Test
	void specExtensionMapping() throws Exception {
		assertFilterInvoked("/index.bop", "*.bop");
	}

	private void assertFilterNotInvoked(String requestUri, String pattern) throws Exception {
		assertFilterNotInvoked(DispatcherType.REQUEST, DispatcherType.REQUEST, requestUri, pattern);
	}

	private void assertFilterNotInvoked(
			DispatcherType requestDispatcherType, DispatcherType filterDispatcherType,
			String requestUri, String pattern) throws Exception {

		request.setDispatcherType(requestDispatcherType);
		request.setRequestURI(request.getContextPath() + requestUri);
		filter = new MockMvcFilterDecorator(delegate, null, null, EnumSet.of(filterDispatcherType), pattern);
		filter.doFilter(request, response, filterChain);

		assertThat(delegate.request).isNull();
		assertThat(delegate.response).isNull();
		assertThat(delegate.chain).isNull();

		assertThat(filterChain.getRequest()).isEqualTo(request);
		assertThat(filterChain.getResponse()).isEqualTo(response);
		filterChain = new MockFilterChain();
	}

	private void assertFilterInvoked(String requestUri, String pattern) throws Exception {
		request.setRequestURI(request.getContextPath() + requestUri);
		filter = new MockMvcFilterDecorator(delegate, new String[] {pattern});
		filter.doFilter(request, response, filterChain);

		assertThat(delegate.request).isEqualTo(request);
		assertThat(delegate.response).isEqualTo(response);
		assertThat(delegate.chain).isEqualTo(filterChain);
		delegate = new MockFilter();
	}


	private static class MockFilter implements Filter {

		private FilterConfig filterConfig;

		private ServletRequest request;

		private ServletResponse response;

		private FilterChain chain;

		private boolean destroy;

		@Override
		public void init(FilterConfig filterConfig) {
			this.filterConfig = filterConfig;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
			this.request = request;
			this.response = response;
			this.chain = chain;
		}

		@Override
		public void destroy() {
			this.destroy = true;
		}
	}

}
