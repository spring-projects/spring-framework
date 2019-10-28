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
import java.util.Arrays;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Unit tests for {@link CorsFilter}.
 * @author Sebastien Deleuze
 */
public class CorsFilterTests {

	private CorsFilter filter;

	private final CorsConfiguration config = new CorsConfiguration();

	@BeforeEach
	public void setup() throws Exception {
		config.setAllowedOrigins(Arrays.asList("https://domain1.com", "https://domain2.com"));
		config.setAllowedMethods(Arrays.asList("GET", "POST"));
		config.setAllowedHeaders(Arrays.asList("header1", "header2"));
		config.setExposedHeaders(Arrays.asList("header3", "header4"));
		config.setMaxAge(123L);
		config.setAllowCredentials(false);
		filter = new CorsFilter(r -> config);
	}

	@Test
	public void nonCorsRequest() throws ServletException, IOException {

		MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/test.html");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
			assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isNull();
		};
		filter.doFilter(request, response, filterChain);
	}

	@Test
	public void sameOriginRequest() throws ServletException, IOException {

		MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "https://domain1.com/test.html");
		request.addHeader(HttpHeaders.ORIGIN, "https://domain1.com");
		request.setScheme("https");
		request.setServerName("domain1.com");
		request.setServerPort(443);
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
			assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isNull();
		};
		filter.doFilter(request, response, filterChain);
	}

	@Test
	public void validActualRequest() throws ServletException, IOException {

		MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/test.html");
		request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		request.addHeader("header2", "foo");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
			assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isEqualTo("header3, header4");
		};
		filter.doFilter(request, response, filterChain);
	}

	@Test
	public void invalidActualRequest() throws ServletException, IOException {

		MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.DELETE.name(), "/test.html");
		request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		request.addHeader("header2", "foo");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) ->
			fail("Invalid requests must not be forwarded to the filter chain");
		filter.doFilter(request, response, filterChain);
		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
	}

	@Test
	public void validPreFlightRequest() throws ServletException, IOException {

		MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.OPTIONS.name(), "/test.html");
		request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name());
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1, header2");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) ->
				fail("Preflight requests must not be forwarded to the filter chain");
		filter.doFilter(request, response, filterChain);

		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("header1, header2");
		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isEqualTo("header3, header4");
		assertThat(Long.parseLong(response.getHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE))).isEqualTo(123L);
	}

	@Test
	public void invalidPreFlightRequest() throws ServletException, IOException {

		MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.OPTIONS.name(), "/test.html");
		request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.DELETE.name());
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1, header2");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) ->
				fail("Preflight requests must not be forwarded to the filter chain");
		filter.doFilter(request, response, filterChain);

		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
	}

}
