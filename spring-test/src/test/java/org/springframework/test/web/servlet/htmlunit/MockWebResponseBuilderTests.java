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

package org.springframework.test.web.servlet.htmlunit;

import java.net.URL;
import java.util.List;
import javax.servlet.http.Cookie;

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.MockHttpServletResponse;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link MockWebResponseBuilder}.
 *
 * @author Rob Winch
 * @since 4.2
 */
public class MockWebResponseBuilderTests {

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private WebRequest webRequest;

	private MockWebResponseBuilder responseBuilder;


	@Before
	public void setUp() throws Exception {
		this.webRequest = new WebRequest(new URL("http://example.com:80/test/this/here"));
		this.responseBuilder = new MockWebResponseBuilder(System.currentTimeMillis(), this.webRequest, this.response);
	}


	// --- constructor

	@Test(expected = IllegalArgumentException.class)
	public void constructorWithNullWebRequest() {
		new MockWebResponseBuilder(0L, null, this.response);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorWithNullResponse() throws Exception {
		new MockWebResponseBuilder(0L, new WebRequest(new URL("http://example.com:80/test/this/here")), null);
	}


	// --- build

	@Test
	public void buildContent() throws Exception {
		this.response.getWriter().write("expected content");
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getContentAsString(), equalTo("expected content"));
	}

	@Test
	public void buildContentCharset() throws Exception {
		this.response.addHeader("Content-Type", "text/html; charset=UTF-8");
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getContentCharset(), equalTo("UTF-8"));
	}

	@Test
	public void buildContentType() throws Exception {
		this.response.addHeader("Content-Type", "text/html; charset-UTF-8");
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getContentType(), equalTo("text/html"));
	}

	@Test
	public void buildResponseHeaders() throws Exception {
		this.response.addHeader("Content-Type", "text/html");
		this.response.addHeader("X-Test", "value");
		Cookie cookie = new Cookie("cookieA", "valueA");
		cookie.setDomain("domain");
		cookie.setPath("/path");
		cookie.setMaxAge(1800);
		cookie.setSecure(true);
		cookie.setHttpOnly(true);
		this.response.addCookie(cookie);
		WebResponse webResponse = this.responseBuilder.build();

		List<NameValuePair> responseHeaders = webResponse.getResponseHeaders();
		assertThat(responseHeaders.size(), equalTo(3));
		NameValuePair header = responseHeaders.get(0);
		assertThat(header.getName(), equalTo("Content-Type"));
		assertThat(header.getValue(), equalTo("text/html"));
		header = responseHeaders.get(1);
		assertThat(header.getName(), equalTo("X-Test"));
		assertThat(header.getValue(), equalTo("value"));
		header = responseHeaders.get(2);
		assertThat(header.getName(), equalTo("Set-Cookie"));
		assertThat(header.getValue(), startsWith("cookieA=valueA;domain=domain;path=/path;expires="));
		assertThat(header.getValue(), endsWith(";secure;httpOnly"));
	}

	// SPR-14169
	@Test
	public void buildResponseHeadersNullDomainDefaulted() throws Exception {
		Cookie cookie = new Cookie("cookieA", "valueA");
		this.response.addCookie(cookie);
		WebResponse webResponse = this.responseBuilder.build();

		List<NameValuePair> responseHeaders = webResponse.getResponseHeaders();
		assertThat(responseHeaders.size(), equalTo(1));
		NameValuePair header = responseHeaders.get(0);
		assertThat(header.getName(), equalTo("Set-Cookie"));
		assertThat(header.getValue(), equalTo("cookieA=valueA"));
	}

	@Test
	public void buildStatus() throws Exception {
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getStatusCode(), equalTo(200));
		assertThat(webResponse.getStatusMessage(), equalTo("OK"));
	}

	@Test
	public void buildStatusNotOk() throws Exception {
		this.response.setStatus(401);
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getStatusCode(), equalTo(401));
		assertThat(webResponse.getStatusMessage(), equalTo("Unauthorized"));
	}

	@Test
	public void buildStatusWithCustomMessage() throws Exception {
		this.response.sendError(401, "Custom");
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getStatusCode(), equalTo(401));
		assertThat(webResponse.getStatusMessage(), equalTo("Custom"));
	}

	@Test
	public void buildWebRequest() throws Exception {
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getWebRequest(), equalTo(this.webRequest));
	}

}
