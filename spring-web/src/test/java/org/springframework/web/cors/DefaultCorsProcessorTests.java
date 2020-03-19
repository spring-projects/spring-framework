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

package org.springframework.web.cors;

import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

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


	@BeforeEach
	public void setup() {
		this.request = new MockHttpServletRequest();
		this.request.setRequestURI("/test.html");
		this.request.setServerName("domain1.example");
		this.conf = new CorsConfiguration();
		this.response = new MockHttpServletResponse();
		this.response.setStatus(HttpServletResponse.SC_OK);
		this.processor = new DefaultCorsProcessor();
	}

	@Test
	public void requestWithoutOriginHeader() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void sameOriginRequest() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain1.example");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void actualRequestWithOriginHeader() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
	}

	@Test
	public void actualRequestWithOriginHeaderAndNullConfig() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");

		this.processor.processRequest(null, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void actualRequestWithOriginHeaderAndAllowedOrigin() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.conf.addAllowedOrigin("*");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*");
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE)).isFalse();
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isFalse();
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void actualRequestCredentials() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.conf.addAllowedOrigin("https://domain1.com");
		this.conf.addAllowedOrigin("https://domain2.com");
		this.conf.addAllowedOrigin("http://domain3.example");
		this.conf.setAllowCredentials(true);

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void actualRequestCredentialsWithOriginWildcard() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.conf.addAllowedOrigin("*");
		this.conf.setAllowCredentials(true);

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void actualRequestCaseInsensitiveOriginMatch() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.conf.addAllowedOrigin("https://DOMAIN2.com");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void actualRequestExposedHeaders() throws Exception {
		this.request.setMethod(HttpMethod.GET.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.conf.addExposedHeader("header1");
		this.conf.addExposedHeader("header2");
		this.conf.addAllowedOrigin("https://domain2.com");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).contains("header1")).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).contains("header2")).isTrue();
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void preflightRequestAllOriginsAllowed() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.conf.addAllowedOrigin("*");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void preflightRequestWrongAllowedMethod() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE");
		this.conf.addAllowedOrigin("*");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
	}

	@Test
	public void preflightRequestMatchedAllowedMethod() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.conf.addAllowedOrigin("*");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("GET,HEAD");
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
	}

	@Test
	public void preflightRequestTestWithOriginButWithoutOtherHeaders() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
	}

	@Test
	public void preflightRequestWithoutRequestMethod() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
	}

	@Test
	public void preflightRequestWithRequestAndMethodHeaderButNoConfig() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
	}

	@Test
	public void preflightRequestValidRequestAndConfig() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.conf.addAllowedOrigin("*");
		this.conf.addAllowedMethod("GET");
		this.conf.addAllowedMethod("PUT");
		this.conf.addAllowedHeader("header1");
		this.conf.addAllowedHeader("header2");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*");
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("GET,PUT");
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE)).isFalse();
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void preflightRequestCredentials() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.conf.addAllowedOrigin("https://domain1.com");
		this.conf.addAllowedOrigin("https://domain2.com");
		this.conf.addAllowedOrigin("http://domain3.example");
		this.conf.addAllowedHeader("Header1");
		this.conf.setAllowCredentials(true);

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void preflightRequestCredentialsWithOriginWildcard() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.conf.addAllowedOrigin("https://domain1.com");
		this.conf.addAllowedOrigin("*");
		this.conf.addAllowedOrigin("http://domain3.example");
		this.conf.addAllowedHeader("Header1");
		this.conf.setAllowCredentials(true);

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void preflightRequestAllowedHeaders() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1, Header2");
		this.conf.addAllowedHeader("Header1");
		this.conf.addAllowedHeader("Header2");
		this.conf.addAllowedHeader("Header3");
		this.conf.addAllowedOrigin("https://domain2.com");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS).contains("Header1")).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS).contains("Header2")).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS).contains("Header3")).isFalse();
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void preflightRequestAllowsAllHeaders() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1, Header2");
		this.conf.addAllowedHeader("*");
		this.conf.addAllowedOrigin("https://domain2.com");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS).contains("Header1")).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS).contains("Header2")).isTrue();
		assertThat(this.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS).contains("*")).isFalse();
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void preflightRequestWithEmptyHeaders() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "");
		this.conf.addAllowedHeader("*");
		this.conf.addAllowedOrigin("https://domain2.com");

		this.processor.processRequest(this.conf, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).isFalse();
		assertThat(this.response.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
				HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void preflightRequestWithNullConfig() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.conf.addAllowedOrigin("*");

		this.processor.processRequest(null, this.request, this.response);
		assertThat(this.response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
	}

}
