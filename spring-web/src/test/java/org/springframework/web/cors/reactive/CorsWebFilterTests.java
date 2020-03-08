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
package org.springframework.web.cors.reactive;


import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
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

	@BeforeEach
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
	public void nonCorsRequest() {
		WebFilterChain filterChain = filterExchange -> {
			try {
				HttpHeaders headers = filterExchange.getResponse().getHeaders();
				assertThat(headers.getFirst(ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
				assertThat(headers.getFirst(ACCESS_CONTROL_EXPOSE_HEADERS)).isNull();
			}
			catch (AssertionError ex) {
				return Mono.error(ex);
			}
			return Mono.empty();

		};
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest
						.get("https://domain1.com/test.html")
						.header(HOST, "domain1.com"));
		this.filter.filter(exchange, filterChain).block();
	}

	@Test
	public void sameOriginRequest() {
		WebFilterChain filterChain = filterExchange -> {
			try {
				HttpHeaders headers = filterExchange.getResponse().getHeaders();
				assertThat(headers.getFirst(ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
				assertThat(headers.getFirst(ACCESS_CONTROL_EXPOSE_HEADERS)).isNull();
			}
			catch (AssertionError ex) {
				return Mono.error(ex);
			}
			return Mono.empty();

		};
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest
						.get("https://domain1.com/test.html")
						.header(ORIGIN, "https://domain1.com"));
		this.filter.filter(exchange, filterChain).block();
	}

	@Test
	public void validActualRequest() {
		WebFilterChain filterChain = filterExchange -> {
			try {
				HttpHeaders headers = filterExchange.getResponse().getHeaders();
				assertThat(headers.getFirst(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
				assertThat(headers.getFirst(ACCESS_CONTROL_EXPOSE_HEADERS)).isEqualTo("header3, header4");
			}
			catch (AssertionError ex) {
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
		this.filter.filter(exchange, filterChain).block();
	}

	@Test
	public void invalidActualRequest() throws ServletException, IOException {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest
						.delete("https://domain1.com/test.html")
						.header(HOST, "domain1.com")
						.header(ORIGIN, "https://domain2.com")
						.header("header2", "foo"));

		WebFilterChain filterChain = filterExchange -> Mono.error(
				new AssertionError("Invalid requests must not be forwarded to the filter chain"));
		filter.filter(exchange, filterChain).block();
		assertThat(exchange.getResponse().getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
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

		WebFilterChain filterChain = filterExchange -> Mono.error(
				new AssertionError("Preflight requests must not be forwarded to the filter chain"));
		filter.filter(exchange, filterChain).block();

		HttpHeaders headers = exchange.getResponse().getHeaders();
		assertThat(headers.getFirst(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
		assertThat(headers.getFirst(ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("header1, header2");
		assertThat(headers.getFirst(ACCESS_CONTROL_EXPOSE_HEADERS)).isEqualTo("header3, header4");
		assertThat(Long.parseLong(headers.getFirst(ACCESS_CONTROL_MAX_AGE))).isEqualTo(123L);
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

		WebFilterChain filterChain = filterExchange -> Mono.error(
				new AssertionError("Preflight requests must not be forwarded to the filter chain"));

		filter.filter(exchange, filterChain).block();

		assertThat(exchange.getResponse().getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
	}

}
