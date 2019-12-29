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

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.Cookie;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.testfixture.TestGroup;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Integration tests for {@link MockMvcWebClientBuilder}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @author Rossen Stoyanchev
 * @since 4.2
 */
@SpringJUnitWebConfig
class MockMvcWebClientBuilderTests {

	private MockMvc mockMvc;

	MockMvcWebClientBuilderTests(WebApplicationContext wac) {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}


	@Test
	void mockMvcSetupNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> MockMvcWebClientBuilder.mockMvcSetup(null));
	}

	@Test
	void webAppContextSetupNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> MockMvcWebClientBuilder.webAppContextSetup(null));
	}

	@Test
	void mockMvcSetupWithDefaultWebClientDelegate() throws Exception {
		WebClient client = MockMvcWebClientBuilder.mockMvcSetup(this.mockMvc).build();

		assertMockMvcUsed(client, "http://localhost/test");

		if (TestGroup.PERFORMANCE.isActive()) {
			assertMockMvcNotUsed(client, "https://spring.io/");
		}
	}

	@Test
	void mockMvcSetupWithCustomWebClientDelegate() throws Exception {
		WebClient otherClient = new WebClient();
		WebClient client = MockMvcWebClientBuilder.mockMvcSetup(this.mockMvc).withDelegate(otherClient).build();

		assertMockMvcUsed(client, "http://localhost/test");

		if (TestGroup.PERFORMANCE.isActive()) {
			assertMockMvcNotUsed(client, "https://spring.io/");
		}
	}

	@Test // SPR-14066
	void cookieManagerShared() throws Exception {
		this.mockMvc = MockMvcBuilders.standaloneSetup(new CookieController()).build();
		WebClient client = MockMvcWebClientBuilder.mockMvcSetup(this.mockMvc).build();

		assertThat(getResponse(client, "http://localhost/").getContentAsString()).isEqualTo("NA");
		client.getCookieManager().addCookie(new Cookie("localhost", "cookie", "cookieManagerShared"));
		assertThat(getResponse(client, "http://localhost/").getContentAsString()).isEqualTo("cookieManagerShared");
	}

	@Test // SPR-14265
	void cookiesAreManaged() throws Exception {
		this.mockMvc = MockMvcBuilders.standaloneSetup(new CookieController()).build();
		WebClient client = MockMvcWebClientBuilder.mockMvcSetup(this.mockMvc).build();

		assertThat(getResponse(client, "http://localhost/").getContentAsString()).isEqualTo("NA");
		assertThat(postResponse(client, "http://localhost/?cookie=foo").getContentAsString()).isEqualTo("Set");
		assertThat(getResponse(client, "http://localhost/").getContentAsString()).isEqualTo("foo");
		assertThat(deleteResponse(client, "http://localhost/").getContentAsString()).isEqualTo("Delete");
		assertThat(getResponse(client, "http://localhost/").getContentAsString()).isEqualTo("NA");
	}

	private void assertMockMvcUsed(WebClient client, String url) throws Exception {
		assertThat(getResponse(client, url).getContentAsString()).isEqualTo("mvc");
	}

	private void assertMockMvcNotUsed(WebClient client, String url) throws Exception {
		assertThat(getResponse(client, url).getContentAsString()).isNotEqualTo("mvc");
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

			@RequestMapping("/test")
			String contextPath(HttpServletRequest request) {
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
