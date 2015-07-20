/*
 * Copyright 2002-2015 the original author or authors.
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
import java.util.Arrays;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;

/**
 * Unit tests for {@link CorsFilter}.
 * @author Sebastien Deleuze
 */
public class CorsFilterTests {

	private CorsFilter filter;

	private final CorsConfiguration config = new CorsConfiguration();

	@Before
	public void setup() throws Exception {
		config.setAllowedOrigins(Arrays.asList("http://domain1.com", "http://domain2.com"));
		config.setAllowedMethods(Arrays.asList("GET", "POST"));
		config.setAllowedHeaders(Arrays.asList("header1", "header2"));
		config.setExposedHeaders(Arrays.asList("header3", "header4"));
		config.setMaxAge(123L);
		config.setAllowCredentials(false);
		filter = new CorsFilter(r -> config);
	}

	@Test
	public void validActualRequest() throws ServletException, IOException {

		MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/test.html");
		request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		request.addHeader("header2", "foo");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertEquals("http://domain2.com", response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
			assertEquals("header3, header4", response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
		};
		filter.doFilter(request, response, filterChain);
	}

	@Test
	public void invalidActualRequest() throws ServletException, IOException {

		MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.DELETE.name(), "/test.html");
		request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		request.addHeader("header2", "foo");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			fail("Invalid requests must not be forwarded to the filter chain");
		};
		filter.doFilter(request, response, filterChain);
		assertNull(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	public void validPreFlightRequest() throws ServletException, IOException {

		MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.OPTIONS.name(), "/test.html");
		request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name());
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1, header2");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) ->
				fail("Preflight requests must not be forwarded to the filter chain");
		filter.doFilter(request, response, filterChain);

		assertEquals("http://domain2.com", response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("header1, header2", response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
		assertEquals("header3, header4", response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
		assertEquals(123L, Long.parseLong(response.getHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE)));
	}

	@Test
	public void invalidPreFlightRequest() throws ServletException, IOException {

		MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.OPTIONS.name(), "/test.html");
		request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.DELETE.name());
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1, header2");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) ->
				fail("Preflight requests must not be forwarded to the filter chain");
		filter.doFilter(request, response, filterChain);

		assertNull(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

}
