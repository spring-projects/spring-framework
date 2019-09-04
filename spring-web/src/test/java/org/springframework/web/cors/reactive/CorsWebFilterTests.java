/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.web.cors.reactive;


import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.WebFilterChain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_MAX_AGE;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.HOST;
import static org.springframework.http.HttpHeaders.ORIGIN;

/**
 * Unit tests for {@link CorsWebFilter}.
 * @author Sebastien Deleuze
 */
public class CorsWebFilterTests {

	private CorsWebFilter filter;

	private final CorsConfiguration config = new CorsConfiguration();

	@Before
	public void setup() throws Exception {
		config.setAllowedOrigins(Arrays.asList("https://domain1.com", "https://domain2.com"));
		config.setAllowedMethods(Arrays.asList("GET", "POST"));
		config.setAllowedHeaders(Arrays.asList("header1", "header2"));
		config.setExposedHeaders(Arrays.asList("header3", "header4"));
		config.setMaxAge(123L);
		config.setAllowCredentials(false);
		filter = new CorsWebFilter(r -> config);
	}

	@Test
	public void validActualRequest() {
		WebFilterChain filterChain = (filterExchange) -> {
			try {
				HttpHeaders headers = filterExchange.getResponse().getHeaders();
				assertEquals("https://domain2.com", headers.getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
				assertEquals("header3, header4", headers.getFirst(ACCESS_CONTROL_EXPOSE_HEADERS));
			} catch (AssertionError ex) {
				return Mono.error(ex);
			}
			return Mono.empty();

		};
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest
						.get("https://domain1.com/test.html")
						.header(HOST, "domain1.com")
						.header(ORIGIN, "https://domain2.com")
						.header("header2", "foo"));
		this.filter.filter(exchange, filterChain);
	}

	@Test
	public void invalidActualRequest() throws ServletException, IOException {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest
						.delete("https://domain1.com/test.html")
						.header(HOST, "domain1.com")
						.header(ORIGIN, "https://domain2.com")
						.header("header2", "foo"));

		WebFilterChain filterChain = (filterExchange) -> Mono.error(
				new AssertionError("Invalid requests must not be forwarded to the filter chain"));
		filter.filter(exchange, filterChain);

		assertNull(exchange.getResponse().getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	public void validPreFlightRequest() throws ServletException, IOException {

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest
						.options("https://domain1.com/test.html")
						.header(HOST, "domain1.com")
						.header(ORIGIN, "https://domain2.com")
						.header(ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
						.header(ACCESS_CONTROL_REQUEST_HEADERS, "header1, header2")
		);

		WebFilterChain filterChain = (filterExchange) -> Mono.error(
				new AssertionError("Preflight requests must not be forwarded to the filter chain"));
		filter.filter(exchange, filterChain);

		HttpHeaders headers = exchange.getResponse().getHeaders();
		assertEquals("https://domain2.com", headers.getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("header1, header2", headers.getFirst(ACCESS_CONTROL_ALLOW_HEADERS));
		assertEquals("header3, header4", headers.getFirst(ACCESS_CONTROL_EXPOSE_HEADERS));
		assertEquals(123L, Long.parseLong(headers.getFirst(ACCESS_CONTROL_MAX_AGE)));
	}

	@Test
	public void invalidPreFlightRequest() throws ServletException, IOException {

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest
						.options("https://domain1.com/test.html")
						.header(HOST, "domain1.com")
						.header(ORIGIN, "https://domain2.com")
						.header(ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.DELETE.name())
						.header(ACCESS_CONTROL_REQUEST_HEADERS, "header1, header2"));

		WebFilterChain filterChain = (filterExchange) -> Mono.error(
				new AssertionError("Preflight requests must not be forwarded to the filter chain"));

		filter.filter(exchange, filterChain);

		assertNull(exchange.getResponse().getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
	}

}
