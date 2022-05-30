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

package org.springframework.test.web.servlet.htmlunit;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;


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


	@BeforeEach
	public void setup() throws Exception {
		this.webRequest = new WebRequest(new URL("http://company.example:80/test/this/here"));
		this.responseBuilder = new MockWebResponseBuilder(System.currentTimeMillis(), this.webRequest, this.response);
	}


	// --- constructor

	@Test
	public void constructorWithNullWebRequest() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MockWebResponseBuilder(0L, null, this.response));
	}

	@Test
	public void constructorWithNullResponse() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MockWebResponseBuilder(0L, new WebRequest(new URL("http://company.example:80/test/this/here")), null));
	}


	// --- build

	@Test
	public void buildContent() throws Exception {
		this.response.getWriter().write("expected content");
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getContentAsString()).isEqualTo("expected content");
	}

	@Test
	public void buildContentCharset() throws Exception {
		this.response.addHeader("Content-Type", "text/html; charset=UTF-8");
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getContentCharset()).isEqualTo(StandardCharsets.UTF_8);
	}

	@Test
	public void buildContentType() throws Exception {
		this.response.addHeader("Content-Type", "text/html; charset-UTF-8");
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getContentType()).isEqualTo("text/html");
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
		assertThat(responseHeaders.size()).isEqualTo(3);
		NameValuePair header = responseHeaders.get(0);
		assertThat(header.getName()).isEqualTo("Content-Type");
		assertThat(header.getValue()).isEqualTo("text/html");
		header = responseHeaders.get(1);
		assertThat(header.getName()).isEqualTo("X-Test");
		assertThat(header.getValue()).isEqualTo("value");
		header = responseHeaders.get(2);
		assertThat(header.getName()).isEqualTo("Set-Cookie");
		assertThat(header.getValue())
				.startsWith("cookieA=valueA; Path=/path; Domain=domain; Max-Age=1800; Expires=")
				.endsWith("; Secure; HttpOnly");
	}

	// SPR-14169
	@Test
	public void buildResponseHeadersNullDomainDefaulted() throws Exception {
		Cookie cookie = new Cookie("cookieA", "valueA");
		this.response.addCookie(cookie);
		WebResponse webResponse = this.responseBuilder.build();

		List<NameValuePair> responseHeaders = webResponse.getResponseHeaders();
		assertThat(responseHeaders.size()).isEqualTo(1);
		NameValuePair header = responseHeaders.get(0);
		assertThat(header.getName()).isEqualTo("Set-Cookie");
		assertThat(header.getValue()).isEqualTo("cookieA=valueA");
	}

	@Test
	public void buildStatus() throws Exception {
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getStatusCode()).isEqualTo(200);
		assertThat(webResponse.getStatusMessage()).isEqualTo("OK");
	}

	@Test
	public void buildStatusNotOk() throws Exception {
		this.response.setStatus(401);
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getStatusCode()).isEqualTo(401);
		assertThat(webResponse.getStatusMessage()).isEqualTo("Unauthorized");
	}

	@Test
	public void buildStatusWithCustomMessage() throws Exception {
		this.response.sendError(401, "Custom");
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getStatusCode()).isEqualTo(401);
		assertThat(webResponse.getStatusMessage()).isEqualTo("Custom");
	}

	@Test
	public void buildWebRequest() throws Exception {
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getWebRequest()).isEqualTo(this.webRequest);
	}

}
