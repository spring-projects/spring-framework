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

package org.springframework.web.cors;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.*;

/**
 * Test {@link DefaultCorsProcessor} with simple or preflight CORS request.
 *
 * @author Sebastien Deleuze
 */
public class DefaultCorsProcessorTests {

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private DefaultCorsProcessor processor;
	private CorsConfiguration conf;

	@Before
	public void setup() {
		this.request = new MockHttpServletRequest();
		this.request.setRequestURI("/test.html");
		this.request.setRemoteHost("domain1.com");
		this.conf = new CorsConfiguration();
		this.response = new MockHttpServletResponse();
		this.response.setStatus(HttpServletResponse.SC_OK);
		this.processor = new DefaultCorsProcessor();
	}

	@Test(expected = IllegalArgumentException.class)
	public void actualRequestWithoutOriginHeader() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.processor.processActualRequest(this.conf, request, response);
	}

	@Test
	public void actualRequestWithOriginHeader() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.processor.processActualRequest(this.conf, request, response);
		assertFalse(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
	}

	@Test
	public void actualRequestwithOriginHeaderAndAllowedOrigin() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.conf.addAllowedOrigin("*");
		this.processor.processActualRequest(this.conf, request, response);
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("*", response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertFalse(response.containsHeader(CorsUtils.ACCESS_CONTROL_MAX_AGE));
		assertFalse(response.containsHeader(CorsUtils.ACCESS_CONTROL_EXPOSE_HEADERS));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void actualRequestCrendentials() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.conf.addAllowedOrigin("http://domain2.com/home.html");
		this.conf.addAllowedOrigin("http://domain2.com/test.html");
		this.conf.addAllowedOrigin("http://domain2.com/logout.html");
		this.conf.setAllowCredentials(true);
		this.processor.processActualRequest(this.conf, request, response);
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com/test.html", response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals("true", response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void actualRequestCredentialsWithOriginWildcard() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.conf.addAllowedOrigin("*");
		this.conf.setAllowCredentials(true);
		this.processor.processActualRequest(this.conf, request, response);
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com/test.html", response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals("true", response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void actualRequestCaseInsensitiveOriginMatch() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.conf.addAllowedOrigin("http://domain2.com/TEST.html");
		this.processor.processActualRequest(this.conf, request, response);
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void actualRequestExposedHeaders() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.conf.addExposedHeader("header1");
		this.conf.addExposedHeader("header2");
		this.conf.addAllowedOrigin("http://domain2.com/test.html");
		this.processor.processActualRequest(this.conf, request, response);
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com/test.html", response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_EXPOSE_HEADERS));
		assertTrue(response.getHeader(CorsUtils.ACCESS_CONTROL_EXPOSE_HEADERS).contains("header1"));
		assertTrue(response.getHeader(CorsUtils.ACCESS_CONTROL_EXPOSE_HEADERS).contains("header2"));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void preflightRequestAllOriginsAllowed() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.conf.addAllowedOrigin("*");
		this.processor.processPreFlightRequest(this.conf, request, response);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void preflightRequestWrongAllowedMethod() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD, "DELETE");
		this.conf.addAllowedOrigin("*");
		this.processor.processPreFlightRequest(this.conf, request, response);
		assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
	}

	@Test
	public void preflightRequestMatchedAllowedMethod() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.conf.addAllowedOrigin("*");
		this.processor.processPreFlightRequest(this.conf, request, response);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		assertEquals("GET", response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_METHODS));
	}

	@Test(expected = IllegalArgumentException.class)
	public void preflightRequestWithoutOriginHeader() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.processor.processPreFlightRequest(this.conf, request, response);
	}

	@Test
	public void preflightRequestTestWithOriginButWithoutOtherHeaders() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.processor.processPreFlightRequest(this.conf, request, response);
		assertFalse(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
	}

	@Test
	public void preflightRequestWithoutRequestMethod() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.processor.processPreFlightRequest(this.conf, request, response);
		assertFalse(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
	}

	@Test
	public void preflightRequestWithRequestAndMethodHeaderButNoConfig() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.processor.processPreFlightRequest(this.conf, request, response);
		assertFalse(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
	}

	@Test
	public void preflightRequestValidRequestAndConfig() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.conf.addAllowedOrigin("*");
		this.conf.addAllowedMethod("GET");
		this.conf.addAllowedMethod("PUT");
		this.conf.addAllowedHeader("header1");
		this.conf.addAllowedHeader("header2");
		this.processor.processPreFlightRequest(this.conf, request, response);
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("*", response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_METHODS));
		assertEquals("GET,PUT", response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_METHODS));
		assertFalse(response.containsHeader(CorsUtils.ACCESS_CONTROL_MAX_AGE));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void preflightRequestCrendentials() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.conf.addAllowedOrigin("http://domain2.com/home.html");
		this.conf.addAllowedOrigin("http://domain2.com/test.html");
		this.conf.addAllowedOrigin("http://domain2.com/logout.html");
		this.conf.addAllowedHeader("Header1");
		this.conf.setAllowCredentials(true);
		this.processor.processPreFlightRequest(this.conf, request, response);
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com/test.html", response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals("true", response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void preflightRequestCrendentialsWithOriginWildcard() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.conf.addAllowedOrigin("http://domain2.com/home.html");
		this.conf.addAllowedOrigin("*");
		this.conf.addAllowedOrigin("http://domain2.com/logout.html");
		this.conf.addAllowedHeader("Header1");
		this.conf.setAllowCredentials(true);
		this.processor.processPreFlightRequest(this.conf, request, response);
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com/test.html", response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void preflightRequestAllowedHeaders() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_HEADERS, "Header1, Header2");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.conf.addAllowedHeader("Header1");
		this.conf.addAllowedHeader("Header2");
		this.conf.addAllowedHeader("Header3");
		this.conf.addAllowedOrigin("http://domain2.com/test.html");
		this.processor.processPreFlightRequest(this.conf, request, response);
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_HEADERS));
		assertTrue(response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_HEADERS).contains("Header1"));
		assertTrue(response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_HEADERS).contains("Header2"));
		assertFalse(response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_HEADERS).contains("Header3"));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void preflightRequestAllowsAllHeaders() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_HEADERS, "Header1, Header2");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.conf.addAllowedHeader("*");
		this.conf.addAllowedOrigin("http://domain2.com/test.html");
		this.processor.processPreFlightRequest(this.conf, request, response);
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.containsHeader(CorsUtils.ACCESS_CONTROL_ALLOW_HEADERS));
		assertTrue(response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_HEADERS).contains("Header1"));
		assertTrue(response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_HEADERS).contains("Header2"));
		assertFalse(response.getHeader(CorsUtils.ACCESS_CONTROL_ALLOW_HEADERS).contains("*"));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

}