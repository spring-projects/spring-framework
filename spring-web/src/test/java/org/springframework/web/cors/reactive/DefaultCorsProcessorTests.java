/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.*;
import static org.springframework.http.HttpHeaders.*;

/**
 * {@link DefaultCorsProcessor} tests with simple or pre-flight CORS request.
 *
 * @author Sebastien Deleuze
 */
public class DefaultCorsProcessorTests {

	private MockServerHttpRequest request;
	
	private MockServerHttpResponse response;

	private DefaultCorsProcessor processor;

	private CorsConfiguration conf;


	@Before
	public void setup() {
		this.conf = new CorsConfiguration();
		this.response = new MockServerHttpResponse();
		this.response.setStatusCode(HttpStatus.OK);
		this.processor = new DefaultCorsProcessor();
	}


	@Test
	public void actualRequestWithOriginHeader() throws Exception {
		this.request = actualRequest().build();
		this.processor.processRequest(this.conf, createExchange());
		assertFalse(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpStatus.FORBIDDEN, this.response.getStatusCode());
	}

	@Test
	public void actualRequestWithOriginHeaderAndNullConfig() throws Exception {
		this.request = actualRequest().build();
		this.processor.processRequest(null, createExchange());
		assertFalse(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpStatus.OK, this.response.getStatusCode());
	}

	@Test
	public void actualRequestWithOriginHeaderAndAllowedOrigin() throws Exception {
		this.request = actualRequest().build();
		this.conf.addAllowedOrigin("*");

		this.processor.processRequest(this.conf, createExchange());
		assertTrue(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("*", this.response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertFalse(this.response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_MAX_AGE));
		assertFalse(this.response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
		assertEquals(HttpStatus.OK, this.response.getStatusCode());
	}

	@Test
	public void actualRequestCredentials() throws Exception {
		this.request = actualRequest().build();
		this.conf.addAllowedOrigin("http://domain1.com");
		this.conf.addAllowedOrigin("http://domain2.com");
		this.conf.addAllowedOrigin("http://domain3.com");
		this.conf.setAllowCredentials(true);

		this.processor.processRequest(this.conf, createExchange());
		assertTrue(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com", this.response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(this.response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals("true", this.response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals(HttpStatus.OK, this.response.getStatusCode());
	}

	@Test
	public void actualRequestCredentialsWithOriginWildcard() throws Exception {
		this.request = actualRequest().build();
		this.conf.addAllowedOrigin("*");
		this.conf.setAllowCredentials(true);

		this.processor.processRequest(this.conf, createExchange());
		assertTrue(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com", this.response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(this.response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals("true", this.response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals(HttpStatus.OK, this.response.getStatusCode());
	}

	@Test
	public void actualRequestCaseInsensitiveOriginMatch() throws Exception {
		this.request = actualRequest().build();
		this.conf.addAllowedOrigin("http://DOMAIN2.com");

		this.processor.processRequest(this.conf, createExchange());
		assertTrue(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpStatus.OK, this.response.getStatusCode());
	}

	@Test
	public void actualRequestExposedHeaders() throws Exception {
		this.request = actualRequest().build();
		this.conf.addExposedHeader("header1");
		this.conf.addExposedHeader("header2");
		this.conf.addAllowedOrigin("http://domain2.com");

		this.processor.processRequest(this.conf, createExchange());
		assertTrue(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com", this.response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(this.response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
		assertTrue(this.response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).contains("header1"));
		assertTrue(this.response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).contains("header2"));
		assertEquals(HttpStatus.OK, this.response.getStatusCode());
	}

	@Test
	public void preflightRequestAllOriginsAllowed() throws Exception {
		this.request = preFlightRequest().header(ACCESS_CONTROL_REQUEST_METHOD, "GET").build();
		this.conf.addAllowedOrigin("*");

		this.processor.processRequest(this.conf, createExchange());
		assertEquals(HttpStatus.OK, this.response.getStatusCode());
	}


	@Test
	public void preflightRequestWrongAllowedMethod() throws Exception {
		this.request = preFlightRequest().header(ACCESS_CONTROL_REQUEST_METHOD, "DELETE").build();
		this.conf.addAllowedOrigin("*");

		this.processor.processRequest(this.conf, createExchange());
		assertEquals(HttpStatus.FORBIDDEN, this.response.getStatusCode());
	}

	@Test
	public void preflightRequestMatchedAllowedMethod() throws Exception {
		this.request = preFlightRequest().header(ACCESS_CONTROL_REQUEST_METHOD, "GET").build();
		this.conf.addAllowedOrigin("*");

		this.processor.processRequest(this.conf, createExchange());
		assertEquals(HttpStatus.OK, this.response.getStatusCode());
		assertEquals("GET,HEAD", this.response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
	}

	@Test
	public void preflightRequestTestWithOriginButWithoutOtherHeaders() throws Exception {
		this.request = preFlightRequest().build();

		this.processor.processRequest(this.conf, createExchange());
		assertFalse(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpStatus.FORBIDDEN, this.response.getStatusCode());
	}

	@Test
	public void preflightRequestWithoutRequestMethod() throws Exception {
		this.request = preFlightRequest().header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1").build();

		this.processor.processRequest(this.conf, createExchange());
		assertFalse(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpStatus.FORBIDDEN, this.response.getStatusCode());
	}

	@Test
	public void preflightRequestWithRequestAndMethodHeaderButNoConfig() throws Exception {
		this.request = preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1")
				.build();

		this.processor.processRequest(this.conf, createExchange());
		assertFalse(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpStatus.FORBIDDEN, this.response.getStatusCode());
	}

	@Test
	public void preflightRequestValidRequestAndConfig() throws Exception {
		this.request = preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1")
				.build();
		this.conf.addAllowedOrigin("*");
		this.conf.addAllowedMethod("GET");
		this.conf.addAllowedMethod("PUT");
		this.conf.addAllowedHeader("header1");
		this.conf.addAllowedHeader("header2");

		this.processor.processRequest(this.conf, createExchange());
		assertTrue(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("*", this.response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(this.response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
		assertEquals("GET,PUT", this.response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
		assertFalse(this.response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_MAX_AGE));
		assertEquals(HttpStatus.OK, this.response.getStatusCode());
	}

	@Test
	public void preflightRequestCredentials() throws Exception {
		this.request = preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1")
				.build();
		this.conf.addAllowedOrigin("http://domain1.com");
		this.conf.addAllowedOrigin("http://domain2.com");
		this.conf.addAllowedOrigin("http://domain3.com");
		this.conf.addAllowedHeader("Header1");
		this.conf.setAllowCredentials(true);

		this.processor.processRequest(this.conf, createExchange());
		assertTrue(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com", this.response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(this.response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals("true", this.response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals(HttpStatus.OK, this.response.getStatusCode());
	}

	@Test
	public void preflightRequestCredentialsWithOriginWildcard() throws Exception {
		this.request = preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1")
				.build();
		this.conf.addAllowedOrigin("http://domain1.com");
		this.conf.addAllowedOrigin("*");
		this.conf.addAllowedOrigin("http://domain3.com");
		this.conf.addAllowedHeader("Header1");
		this.conf.setAllowCredentials(true);

		this.processor.processRequest(this.conf, createExchange());
		assertTrue(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com", this.response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpStatus.OK, this.response.getStatusCode());
	}

	@Test
	public void preflightRequestAllowedHeaders() throws Exception {
		this.request = preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1, Header2")
				.build();
		this.conf.addAllowedHeader("Header1");
		this.conf.addAllowedHeader("Header2");
		this.conf.addAllowedHeader("Header3");
		this.conf.addAllowedOrigin("http://domain2.com");

		this.processor.processRequest(this.conf, createExchange());
		assertTrue(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_HEADERS));
		assertTrue(this.response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("Header1"));
		assertTrue(this.response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("Header2"));
		assertFalse(this.response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("Header3"));
		assertEquals(HttpStatus.OK, this.response.getStatusCode());
	}

	@Test
	public void preflightRequestAllowsAllHeaders() throws Exception {
		this.request = preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1, Header2")
				.build();
		this.conf.addAllowedHeader("*");
		this.conf.addAllowedOrigin("http://domain2.com");

		this.processor.processRequest(this.conf, createExchange());
		assertTrue(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_HEADERS));
		assertTrue(this.response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("Header1"));
		assertTrue(this.response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("Header2"));
		assertFalse(this.response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("*"));
		assertEquals(HttpStatus.OK, this.response.getStatusCode());
	}

	@Test
	public void preflightRequestWithEmptyHeaders() throws Exception {
		this.request = preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "")
				.build();
		this.conf.addAllowedHeader("*");
		this.conf.addAllowedOrigin("http://domain2.com");

		this.processor.processRequest(this.conf, createExchange());
		assertTrue(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertFalse(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_HEADERS));
		assertEquals(HttpStatus.OK, this.response.getStatusCode());
	}

	@Test
	public void preflightRequestWithNullConfig() throws Exception {
		this.request = preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.build();
		this.conf.addAllowedOrigin("*");

		this.processor.processRequest(null, createExchange());
		assertFalse(this.response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpStatus.FORBIDDEN, this.response.getStatusCode());
	}


	private MockServerHttpRequest.BaseBuilder<?> actualRequest() {
		return corsRequest(HttpMethod.GET);
	}

	private MockServerHttpRequest.BaseBuilder<?> preFlightRequest() {
		return corsRequest(HttpMethod.OPTIONS);
	}

	private MockServerHttpRequest.BaseBuilder<?> corsRequest(HttpMethod httpMethod) {
		return MockServerHttpRequest
				.method(httpMethod, "http://localhost/test.html")
				.header(HttpHeaders.ORIGIN, "http://domain2.com");
	}

	private DefaultServerWebExchange createExchange() {
		return new DefaultServerWebExchange(this.request, this.response);
	}

}
