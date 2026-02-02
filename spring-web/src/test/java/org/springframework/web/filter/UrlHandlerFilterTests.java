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

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.testfixture.servlet.MockFilterChain;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.ServletRequestPathUtils;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link UrlHandlerFilter}.
 *
 * @author Rossen Stoyanchev, James Missen
 */
public class UrlHandlerFilterTests {

	@Test
	void requestWrapping() throws Exception {
		testRequestWrapping("/path/**", "", "/path/123", null);
		testRequestWrapping("/path/*", "", "/path", "/123");
		testRequestWrapping("/path/*", "", "", "/path/123");
		testRequestWrapping("/path/**", "/myApp", "/path/123", null); // gh-35975
		testRequestWrapping("/path/*", "/myApp", "/path", "/123"); // gh-35975
		testRequestWrapping("/path/*", "/myApp", "", "/path/123"); // gh-35975
	}

	void testRequestWrapping(String pattern, String contextPath, String servletPath,
			@Nullable String pathInfo) throws Exception {

		UrlHandlerFilter filter = UrlHandlerFilter
				.trailingSlashHandler(pattern).wrapRequest()
				.excludeContextPath(true) // gh-35975
				.build();

		boolean hasPathInfo = StringUtils.hasLength(pathInfo);
		String requestURI = contextPath + servletPath + (hasPathInfo ? pathInfo : "");

		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI + "/");
		request.setContextPath(contextPath);
		request.setServletPath(hasPathInfo ? servletPath : servletPath + "/");
		request.setPathInfo(hasPathInfo ? pathInfo + "/" : pathInfo);

		MockFilterChain chain = new MockFilterChain();
		filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

		HttpServletRequest actual = (HttpServletRequest) chain.getRequest();
		assertThat(actual).isNotNull().isNotSameAs(request);
		assertThat(actual.getRequestURI()).isEqualTo(requestURI);
		assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost" + requestURI);
		assertThat(actual.getServletPath()).isEqualTo(servletPath);
		assertThat(actual.getPathInfo()).isEqualTo(pathInfo);
	}

	@Test
	void redirect() throws Exception {
		HttpStatus status = HttpStatus.PERMANENT_REDIRECT;
		UrlHandlerFilter filter = UrlHandlerFilter.trailingSlashHandler("/path/*").redirect(status).build();

		String path = "/path/123";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", path + "/");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		String queryString = "foo=bar";
		request.setQueryString(queryString);

		filter.doFilterInternal(request, response, chain);

		assertThat(chain.getRequest()).isNull();
		assertThat(response.getStatus()).isEqualTo(status.value());
		assertThat(response.getHeader(HttpHeaders.LOCATION)).isEqualTo(path + "?" + queryString);
		assertThat(response.isCommitted()).isTrue();
	}

	@Test // gh-35882
	void orderedUrlHandling() throws Exception {
		String path = "/path/123";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", path + "/");

		HttpStatus status = HttpStatus.PERMANENT_REDIRECT;

		// Request wrapping
		UrlHandlerFilter filter = UrlHandlerFilter
				.trailingSlashHandler("/path/**").redirect(status)
				.trailingSlashHandler("/path/123/").wrapRequest() // most specific pattern
				.trailingSlashHandler("/path/123/**").redirect(status)
				.sortPatternsBySpecificity(true)
				.build();

		MockFilterChain chain = new MockFilterChain();
		filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

		HttpServletRequest actual = (HttpServletRequest) chain.getRequest();
		assertThat(actual).isNotNull().isNotSameAs(request);
		assertThat(actual.getRequestURL().toString()).isEqualTo("http://localhost" + path);

		// Redirect
		filter = UrlHandlerFilter
				.trailingSlashHandler("/path/**").wrapRequest()
				.trailingSlashHandler("/path/123/").redirect(status) // most specific pattern
				.trailingSlashHandler("/path/123/**").wrapRequest()
				.sortPatternsBySpecificity(true)
				.build();

		MockHttpServletResponse response = new MockHttpServletResponse();

		chain = new MockFilterChain();
		filter.doFilterInternal(request, response, chain);

		assertThat(chain.getRequest()).isNull();
		assertThat(response.getStatus()).isEqualTo(status.value());
		assertThat(response.getHeader(HttpHeaders.LOCATION)).isEqualTo(path);
		assertThat(response.isCommitted()).isTrue();
	}

	@Test
	void noUrlHandling() throws Exception {
		testNoUrlHandling("/path/**", null, "", "/path/123");
		testNoUrlHandling("/path/*", null, "", "/path/123");
		testNoUrlHandling("/**", null, "", "/"); // gh-33444
		testNoUrlHandling("/**", null, "/myApp", "/myApp/"); // gh-33565
		testNoUrlHandling("/path/**", "/path/123/**", "", "/path/123/"); // gh-35882
		testNoUrlHandling("/path/*/*", "/path/123/**", "", "/path/123/abc/"); // gh-35882
		testNoUrlHandling("/path/**", "/path/123/abc/", "", "/path/123/abc/"); // gh-35882
		testNoUrlHandling("/path/**", null, "/myApp", "/myApp/path/123/"); // gh-35975
		testNoUrlHandling("/path/*", null, "/myApp", "/myApp/path/123/"); // gh-35975
	}

	private static void testNoUrlHandling(String pattern, @Nullable String excludePattern, String contextPath,
			String requestURI) throws ServletException, IOException {

		boolean hasExcludePattern = StringUtils.hasLength(excludePattern);

		// No request wrapping
		UrlHandlerFilter.Builder builder = UrlHandlerFilter.trailingSlashHandler(pattern).wrapRequest();
		UrlHandlerFilter filter = hasExcludePattern ? builder.exclude(excludePattern).build() : builder.build();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI);
		request.setContextPath(contextPath);
		MockFilterChain chain = new MockFilterChain();
		filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

		HttpServletRequest actual = (HttpServletRequest) chain.getRequest();
		assertThat(actual).as("Request should not be wrapped").isSameAs(request);

		// No redirect
		HttpStatus status = HttpStatus.PERMANENT_REDIRECT;
		builder = UrlHandlerFilter.trailingSlashHandler(pattern).redirect(status);
		filter = hasExcludePattern ? builder.exclude(excludePattern).build() : builder.build();

		request = new MockHttpServletRequest("GET", requestURI);
		request.setContextPath(contextPath);
		MockHttpServletResponse response = new MockHttpServletResponse();

		chain = new MockFilterChain();
		filter.doFilterInternal(request, response, chain);

		assertThat(chain.getRequest()).isSameAs(request);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getHeader(HttpHeaders.LOCATION)).isNull();
		assertThat(response.isCommitted()).isFalse();
	}

	@Test // gh-35538
	void shouldNotFilterErrorAndAsyncDispatches() {
		UrlHandlerFilter filter = UrlHandlerFilter.trailingSlashHandler("/path/**").wrapRequest().build();

		assertThat(filter.shouldNotFilterAsyncDispatch())
				.as("Should not filter ASYNC dispatch as wrapped request is reused")
				.isTrue();

		assertThat(filter.shouldNotFilterErrorDispatch())
				.as("Should not filter ERROR dispatch as it's an internal, fixed path")
				.isTrue();
	}

	@Test // gh-35538
	void shouldNotCacheParsedPath() throws Exception {
		UrlHandlerFilter filter = UrlHandlerFilter.trailingSlashHandler("/path/*").wrapRequest().build();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path/123/");
		request.setServletPath("/path/123/");

		MockFilterChain chain = new MockFilterChain();
		filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

		assertThat(ServletRequestPathUtils.hasParsedRequestPath(request))
				.as("Path with trailing slash should not be cached")
				.isFalse();
	}

	@Test // gh-35538
	void shouldClearPreviouslyCachedPath() throws Exception {
		UrlHandlerFilter filter = UrlHandlerFilter.trailingSlashHandler("/path/*").wrapRequest().build();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path/123/");
		request.setServletPath("/path/123/");

		ServletRequestPathUtils.parseAndCache(request);
		assertThat(ServletRequestPathUtils.getParsedRequestPath(request).value()).isEqualTo("/path/123/");

		PathServlet servlet = new PathServlet();
		MockFilterChain chain = new MockFilterChain(servlet);
		filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

		assertThat(servlet.getParsedPath()).isNull();
	}

	@Test // gh-35509
	void shouldRespectForwardedPath() throws Exception {
		UrlHandlerFilter filter = UrlHandlerFilter.trailingSlashHandler("/requestURI/*").wrapRequest().build();

		String requestURI = "/requestURI/123/";
		MockHttpServletRequest originalRequest = new MockHttpServletRequest("GET", requestURI);
		originalRequest.setServletPath(requestURI);

		MockFilterChain chain = new MockFilterChain();
		filter.doFilterInternal(originalRequest, new MockHttpServletResponse(), chain);

		HttpServletRequest wrapped = (HttpServletRequest) chain.getRequest();
		assertThat(wrapped).isNotNull().isNotSameAs(originalRequest);
		assertThat(wrapped.getRequestURI()).isEqualTo("/requestURI/123");

		// Change dispatcher type of underlying requests
		originalRequest.setDispatcherType(DispatcherType.FORWARD);
		assertThat(wrapped.getRequestURI())
				.as("Should delegate to underlying request for the requestURI on FORWARD")
				.isEqualTo(requestURI);
	}


	@SuppressWarnings("serial")
	private static class PathServlet extends HttpServlet {

		private String parsedPath;

		public String getParsedPath() {
			return parsedPath;
		}

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response) {
			this.parsedPath = (ServletRequestPathUtils.hasParsedRequestPath(request) ?
					ServletRequestPathUtils.getParsedRequestPath(request).value() : null);
		}
	}

}
