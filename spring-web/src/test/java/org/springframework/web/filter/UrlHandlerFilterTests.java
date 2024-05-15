/*
 * Copyright 2002-2024 the original author or authors.
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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.testfixture.servlet.MockFilterChain;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UrlHandlerFilter}.
 *
 * @author Rossen Stoyanchev
 */
public class UrlHandlerFilterTests {

	@Test
	void trimTrailingSlashAndHandle() throws Exception {
		testTrimTrailingSlashAndHandle("/path/**", "/path/123", null);
		testTrimTrailingSlashAndHandle("/path/*", "/path", "/123");
		testTrimTrailingSlashAndHandle("/path/*", "", "/path/123");
	}

	void testTrimTrailingSlashAndHandle(
			String pattern, String servletPath, @Nullable String pathInfo) throws Exception {

		UrlHandlerFilter filter = UrlHandlerFilter.trimTrailingSlash(pattern).andHandleRequest().build();

		boolean hasPathInfo = StringUtils.hasLength(pathInfo);
		String requestURI = servletPath + (hasPathInfo ? pathInfo : "");

		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI + "/");
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
	void noTrailingSlashNoHandling() throws Exception {
		testNoTrailingSlashNoHandling("/path/**", "/path/123");
		testNoTrailingSlashNoHandling("/path/*", "/path/123");
	}

	private static void testNoTrailingSlashNoHandling(
			String pattern, String requestURI) throws ServletException, IOException {

		UrlHandlerFilter filter = UrlHandlerFilter.trimTrailingSlash(pattern).andHandleRequest().build();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI);
		MockFilterChain chain = new MockFilterChain();
		filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

		HttpServletRequest actual = (HttpServletRequest) chain.getRequest();
		assertThat(actual).as("Request should not be wrapped").isSameAs(request);
	}

	@Test
	void trimTrailingSlashAndRedirect() throws Exception {
		HttpStatus status = HttpStatus.PERMANENT_REDIRECT;
		UrlHandlerFilter filter = UrlHandlerFilter.trimTrailingSlash("/path/*").andRedirect(status).build();

		String path = "/path/123";
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterChain chain = new MockFilterChain();
		filter.doFilterInternal(new MockHttpServletRequest("GET", path + "/"), response, chain);

		assertThat(chain.getRequest()).isNull();
		assertThat(response.getStatus()).isEqualTo(status.value());
		assertThat(response.getHeader(HttpHeaders.LOCATION)).isEqualTo(path);
		assertThat(response.isCommitted()).isTrue();
	}

	@Test
	void noTrailingSlashNoRedirect() throws Exception {
		HttpStatus status = HttpStatus.PERMANENT_REDIRECT;
		UrlHandlerFilter filter = UrlHandlerFilter.trimTrailingSlash("/path/*").andRedirect(status).build();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path/123");
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterChain chain = new MockFilterChain();
		filter.doFilterInternal(request, response, chain);

		assertThat(chain.getRequest()).isSameAs(request);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getHeader(HttpHeaders.LOCATION)).isNull();
		assertThat(response.isCommitted()).isFalse();
	}

}
