/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.cors.reactive;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.http.HttpHeaders.VARY;

/**
 * {@link DefaultCorsProcessor} tests with simple or pre-flight CORS request.
 *
 * @author Sebastien Deleuze
 */
public class DefaultCorsProcessorTests {

	private DefaultCorsProcessor processor;

	private CorsConfiguration conf;


	@Before
	public void setup() {
		this.conf = new CorsConfiguration();
		this.processor = new DefaultCorsProcessor();
	}


	@Test
	public void actualRequestWithOriginHeader() throws Exception {
		ServerWebExchange exchange = actualRequest();
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertFalse(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
	}

	@Test
	public void actualRequestWithOriginHeaderAndNullConfig() throws Exception {
		ServerWebExchange exchange = actualRequest();
		this.processor.process(null, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertFalse(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertNull(response.getStatusCode());
	}

	@Test
	public void actualRequestWithOriginHeaderAndAllowedOrigin() throws Exception {
		ServerWebExchange exchange = actualRequest();
		this.conf.addAllowedOrigin("*");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("*", response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertFalse(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_MAX_AGE));
		assertFalse(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertNull(response.getStatusCode());
	}

	@Test
	public void actualRequestCredentials() throws Exception {
		ServerWebExchange exchange = actualRequest();
		this.conf.addAllowedOrigin("http://domain1.com");
		this.conf.addAllowedOrigin("http://domain2.com");
		this.conf.addAllowedOrigin("http://domain3.com");
		this.conf.setAllowCredentials(true);
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com", response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals("true", response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertNull(response.getStatusCode());
	}

	@Test
	public void actualRequestCredentialsWithOriginWildcard() throws Exception {
		ServerWebExchange exchange = actualRequest();
		this.conf.addAllowedOrigin("*");
		this.conf.setAllowCredentials(true);
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com", response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals("true", response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertNull(response.getStatusCode());
	}

	@Test
	public void actualRequestCaseInsensitiveOriginMatch() throws Exception {
		ServerWebExchange exchange = actualRequest();
		this.conf.addAllowedOrigin("http://DOMAIN2.com");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertNull(response.getStatusCode());
	}

	@Test
	public void actualRequestExposedHeaders() throws Exception {
		ServerWebExchange exchange = actualRequest();
		this.conf.addExposedHeader("header1");
		this.conf.addExposedHeader("header2");
		this.conf.addAllowedOrigin("http://domain2.com");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com", response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
		assertTrue(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).contains("header1"));
		assertTrue(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).contains("header2"));
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertNull(response.getStatusCode());
	}

	@Test
	public void preflightRequestAllOriginsAllowed() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(
				preFlightRequest().header(ACCESS_CONTROL_REQUEST_METHOD, "GET"));
		this.conf.addAllowedOrigin("*");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertNull(response.getStatusCode());
	}


	@Test
	public void preflightRequestWrongAllowedMethod() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(
				preFlightRequest().header(ACCESS_CONTROL_REQUEST_METHOD, "DELETE"));
		this.conf.addAllowedOrigin("*");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
	}

	@Test
	public void preflightRequestMatchedAllowedMethod() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(
				preFlightRequest().header(ACCESS_CONTROL_REQUEST_METHOD, "GET"));
		this.conf.addAllowedOrigin("*");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertNull(response.getStatusCode());
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertEquals("GET,HEAD", response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
	}

	@Test
	public void preflightRequestTestWithOriginButWithoutOtherHeaders() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest());
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertFalse(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
	}

	@Test
	public void preflightRequestWithoutRequestMethod() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(
				preFlightRequest().header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1"));
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertFalse(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
	}

	@Test
	public void preflightRequestWithRequestAndMethodHeaderButNoConfig() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1"));

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertFalse(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
	}

	@Test
	public void preflightRequestValidRequestAndConfig() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1"));

		this.conf.addAllowedOrigin("*");
		this.conf.addAllowedMethod("GET");
		this.conf.addAllowedMethod("PUT");
		this.conf.addAllowedHeader("header1");
		this.conf.addAllowedHeader("header2");

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("*", response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
		assertEquals("GET,PUT", response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
		assertFalse(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_MAX_AGE));
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertNull(response.getStatusCode());
	}

	@Test
	public void preflightRequestCredentials() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1"));

		this.conf.addAllowedOrigin("http://domain1.com");
		this.conf.addAllowedOrigin("http://domain2.com");
		this.conf.addAllowedOrigin("http://domain3.com");
		this.conf.addAllowedHeader("Header1");
		this.conf.setAllowCredentials(true);

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com", response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals("true", response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertNull(response.getStatusCode());
	}

	@Test
	public void preflightRequestCredentialsWithOriginWildcard() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1"));

		this.conf.addAllowedOrigin("http://domain1.com");
		this.conf.addAllowedOrigin("*");
		this.conf.addAllowedOrigin("http://domain3.com");
		this.conf.addAllowedHeader("Header1");
		this.conf.setAllowCredentials(true);

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com", response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertNull(response.getStatusCode());
	}

	@Test
	public void preflightRequestAllowedHeaders() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1, Header2"));

		this.conf.addAllowedHeader("Header1");
		this.conf.addAllowedHeader("Header2");
		this.conf.addAllowedHeader("Header3");
		this.conf.addAllowedOrigin("http://domain2.com");

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_HEADERS));
		assertTrue(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("Header1"));
		assertTrue(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("Header2"));
		assertFalse(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("Header3"));
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertNull(response.getStatusCode());
	}

	@Test
	public void preflightRequestAllowsAllHeaders() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1, Header2"));

		this.conf.addAllowedHeader("*");
		this.conf.addAllowedOrigin("http://domain2.com");

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_HEADERS));
		assertTrue(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("Header1"));
		assertTrue(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("Header2"));
		assertFalse(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("*"));
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertNull(response.getStatusCode());
	}

	@Test
	public void preflightRequestWithEmptyHeaders() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, ""));

		this.conf.addAllowedHeader("*");
		this.conf.addAllowedOrigin("http://domain2.com");

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertFalse(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_HEADERS));
		assertThat(response.getHeaders().get(VARY), contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS));
		assertNull(response.getStatusCode());
	}

	@Test
	public void preflightRequestWithNullConfig() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(
				preFlightRequest().header(ACCESS_CONTROL_REQUEST_METHOD, "GET"));
		this.conf.addAllowedOrigin("*");
		this.processor.process(null, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertFalse(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
	}


	private ServerWebExchange actualRequest() {
		return MockServerWebExchange.from(corsRequest(HttpMethod.GET));
	}

	private MockServerHttpRequest.BaseBuilder<?> preFlightRequest() {
		return corsRequest(HttpMethod.OPTIONS);
	}

	private MockServerHttpRequest.BaseBuilder<?> corsRequest(HttpMethod method) {
		return MockServerHttpRequest
				.method(method, "http://localhost/test.html")
				.header(HttpHeaders.ORIGIN, "http://domain2.com");
	}

}
