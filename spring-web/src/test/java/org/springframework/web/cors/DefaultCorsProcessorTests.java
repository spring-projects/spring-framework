/*
 * Copyright 2002-2016 the original author or authors.
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

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;

import static org.junit.Assert.*;

/**
 * Test {@link DefaultCorsProcessor} with simple or preflight CORS request.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
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


	@Test
	public void actualRequestWithOriginHeader() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertFalse(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_FORBIDDEN, this.response.getStatus());
	}

	@Test
	public void actualRequestWithOriginHeaderAndNullConfig() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");

		this.processor.processRequest(null, this.request, this.response);
		assertFalse(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
	}

	@Test
	public void actualRequestWithOriginHeaderAndAllowedOrigin() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.conf.addAllowedOrigin("*");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("*", this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertFalse(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE));
		assertFalse(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
	}

	@Test
	public void actualRequestCredentials() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.conf.addAllowedOrigin("http://domain1.com");
		this.conf.addAllowedOrigin("http://domain2.com");
		this.conf.addAllowedOrigin("http://domain3.com");
		this.conf.setAllowCredentials(true);

		this.processor.processRequest(this.conf, this.request, this.response);
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com", this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals("true", this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
	}

	@Test
	public void actualRequestCredentialsWithOriginWildcard() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.conf.addAllowedOrigin("*");
		this.conf.setAllowCredentials(true);

		this.processor.processRequest(this.conf, this.request, this.response);
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com", this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals("true", this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
	}

	@Test
	public void actualRequestCaseInsensitiveOriginMatch() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.conf.addAllowedOrigin("http://DOMAIN2.com");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
	}

	@Test
	public void actualRequestExposedHeaders() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.conf.addExposedHeader("header1");
		this.conf.addExposedHeader("header2");
		this.conf.addAllowedOrigin("http://domain2.com");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com", this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
		assertTrue(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).contains("header1"));
		assertTrue(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).contains("header2"));
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
	}

	@Test
	public void preflightRequestAllOriginsAllowed() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.conf.addAllowedOrigin("*");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
	}

	@Test
	public void preflightRequestWrongAllowedMethod() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE");
		this.conf.addAllowedOrigin("*");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertEquals(HttpServletResponse.SC_FORBIDDEN, this.response.getStatus());
	}

	@Test
	public void preflightRequestMatchedAllowedMethod() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.conf.addAllowedOrigin("*");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
		assertEquals("GET", this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
	}

	@Test
	public void preflightRequestTestWithOriginButWithoutOtherHeaders() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertFalse(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_FORBIDDEN, this.response.getStatus());
	}

	@Test
	public void preflightRequestWithoutRequestMethod() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertFalse(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_FORBIDDEN, this.response.getStatus());
	}

	@Test
	public void preflightRequestWithRequestAndMethodHeaderButNoConfig() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertFalse(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_FORBIDDEN, this.response.getStatus());
	}

	@Test
	public void preflightRequestValidRequestAndConfig() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.conf.addAllowedOrigin("*");
		this.conf.addAllowedMethod("GET");
		this.conf.addAllowedMethod("PUT");
		this.conf.addAllowedHeader("header1");
		this.conf.addAllowedHeader("header2");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("*", this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
		assertEquals("GET,PUT", this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
		assertFalse(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE));
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
	}

	@Test
	public void preflightRequestCredentials() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.conf.addAllowedOrigin("http://domain1.com");
		this.conf.addAllowedOrigin("http://domain2.com");
		this.conf.addAllowedOrigin("http://domain3.com");
		this.conf.addAllowedHeader("Header1");
		this.conf.setAllowCredentials(true);

		this.processor.processRequest(this.conf, this.request, this.response);
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com", this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals("true", this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
	}

	@Test
	public void preflightRequestCredentialsWithOriginWildcard() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.conf.addAllowedOrigin("http://domain1.com");
		this.conf.addAllowedOrigin("*");
		this.conf.addAllowedOrigin("http://domain3.com");
		this.conf.addAllowedHeader("Header1");
		this.conf.setAllowCredentials(true);

		this.processor.processRequest(this.conf, this.request, this.response);
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com", this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
	}

	@Test
	public void preflightRequestAllowedHeaders() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1, Header2");
		this.conf.addAllowedHeader("Header1");
		this.conf.addAllowedHeader("Header2");
		this.conf.addAllowedHeader("Header3");
		this.conf.addAllowedOrigin("http://domain2.com");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
		assertTrue(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS).contains("Header1"));
		assertTrue(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS).contains("Header2"));
		assertFalse(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS).contains("Header3"));
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
	}

	@Test
	public void preflightRequestAllowsAllHeaders() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1, Header2");
		this.conf.addAllowedHeader("*");
		this.conf.addAllowedOrigin("http://domain2.com");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
		assertTrue(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS).contains("Header1"));
		assertTrue(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS).contains("Header2"));
		assertFalse(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS).contains("*"));
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
	}

	@Test
	public void preflightRequestWithNullConfig() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.conf.addAllowedOrigin("*");

		this.processor.processRequest(null, this.request, this.response);
		assertFalse(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_FORBIDDEN, this.response.getStatus());
	}

}
