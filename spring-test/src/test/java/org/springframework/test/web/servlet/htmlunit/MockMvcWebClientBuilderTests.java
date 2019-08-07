/*
 * Copyright 2002-2016 the original author or authors.
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

import java.io.IOException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.Cookie;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Integration tests for {@link MockMvcWebClientBuilder}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @author Rossen Stoyanchev
 * @since 4.2
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class MockMvcWebClientBuilderTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;


	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}


	@Test(expected = IllegalArgumentException.class)
	public void mockMvcSetupNull() {
		MockMvcWebClientBuilder.mockMvcSetup(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void webAppContextSetupNull() {
		MockMvcWebClientBuilder.webAppContextSetup(null);
	}

	@Test
	public void mockMvcSetupWithDefaultWebClientDelegate() throws Exception {
		WebClient client = MockMvcWebClientBuilder.mockMvcSetup(this.mockMvc).build();

		assertMockMvcUsed(client, "http://localhost/test");
		Assume.group(TestGroup.PERFORMANCE, () -> assertMockMvcNotUsed(client, "https://example.com/"));
	}

	@Test
	public void mockMvcSetupWithCustomWebClientDelegate() throws Exception {
		WebClient otherClient = new WebClient();
		WebClient client = MockMvcWebClientBuilder.mockMvcSetup(this.mockMvc).withDelegate(otherClient).build();

		assertMockMvcUsed(client, "http://localhost/test");
		Assume.group(TestGroup.PERFORMANCE, () -> assertMockMvcNotUsed(client, "https://example.com/"));
	}

	@Test // SPR-14066
	public void cookieManagerShared() throws Exception {
		this.mockMvc = MockMvcBuilders.standaloneSetup(new CookieController()).build();
		WebClient client = MockMvcWebClientBuilder.mockMvcSetup(this.mockMvc).build();

		assertThat(getResponse(client, "http://localhost/").getContentAsString(), equalTo("NA"));
		client.getCookieManager().addCookie(new Cookie("localhost", "cookie", "cookieManagerShared"));
		assertThat(getResponse(client, "http://localhost/").getContentAsString(), equalTo("cookieManagerShared"));
	}

	@Test // SPR-14265
	public void cookiesAreManaged() throws Exception {
		this.mockMvc = MockMvcBuilders.standaloneSetup(new CookieController()).build();
		WebClient client = MockMvcWebClientBuilder.mockMvcSetup(this.mockMvc).build();

		assertThat(getResponse(client, "http://localhost/").getContentAsString(), equalTo("NA"));
		assertThat(postResponse(client, "http://localhost/?cookie=foo").getContentAsString(), equalTo("Set"));
		assertThat(getResponse(client, "http://localhost/").getContentAsString(), equalTo("foo"));
		assertThat(deleteResponse(client, "http://localhost/").getContentAsString(), equalTo("Delete"));
		assertThat(getResponse(client, "http://localhost/").getContentAsString(), equalTo("NA"));
	}

	private void assertMockMvcUsed(WebClient client, String url) throws Exception {
		assertThat(getResponse(client, url).getContentAsString(), equalTo("mvc"));
	}

	private void assertMockMvcNotUsed(WebClient client, String url) throws Exception {
		assertThat(getResponse(client, url).getContentAsString(), not(equalTo("mvc")));
	}

	private WebResponse getResponse(WebClient client, String url) throws IOException {
		return createResponse(client, new WebRequest(new URL(url)));
	}

	private WebResponse postResponse(WebClient client, String url) throws IOException {
		return createResponse(client, new WebRequest(new URL(url), HttpMethod.POST));
	}

	private WebResponse deleteResponse(WebClient client, String url) throws IOException {
		return createResponse(client, new WebRequest(new URL(url), HttpMethod.DELETE));
	}

	private WebResponse createResponse(WebClient client, WebRequest request) throws IOException {
		return client.getWebConnection().getResponse(request);
	}


	@Configuration
	@EnableWebMvc
	static class Config {

		@RestController
		static class ContextPathController {

			@RequestMapping
			public String contextPath(HttpServletRequest request) {
				return "mvc";
			}
		}
	}

	@RestController
	static class CookieController {

		static final String COOKIE_NAME = "cookie";

		@RequestMapping(path = "/", produces = "text/plain")
		String cookie(@CookieValue(name = COOKIE_NAME, defaultValue = "NA") String cookie) {
			return cookie;
		}

		@PostMapping(path = "/", produces = "text/plain")
		String setCookie(@RequestParam String cookie, HttpServletResponse response) {
			response.addCookie(new javax.servlet.http.Cookie(COOKIE_NAME, cookie));
			return "Set";
		}

		@DeleteMapping(path = "/", produces = "text/plain")
		String deleteCookie(HttpServletResponse response) {
			javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie(COOKIE_NAME, "");
			cookie.setMaxAge(0);
			response.addCookie(cookie);
			return "Delete";
		}
	}

}
