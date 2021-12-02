/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.web.servlet.htmlunit.webdriver;

import java.io.IOException;

import com.gargoylesoftware.htmlunit.util.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Integration tests for {@link MockMvcHtmlUnitDriverBuilder}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 */
@SpringJUnitWebConfig
class MockMvcHtmlUnitDriverBuilderTests {

	private static final String EXPECTED_BODY = "MockMvcHtmlUnitDriverBuilderTests mvc";

	private MockMvc mockMvc;

	private HtmlUnitDriver driver;

	MockMvcHtmlUnitDriverBuilderTests(WebApplicationContext wac) {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}


	@Test
	void webAppContextSetupNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> MockMvcHtmlUnitDriverBuilder.webAppContextSetup(null));
	}

	@Test
	void mockMvcSetupNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> MockMvcHtmlUnitDriverBuilder.mockMvcSetup(null));
	}

	@Test
	void mockMvcSetupWithCustomDriverDelegate() throws Exception {
		WebConnectionHtmlUnitDriver otherDriver = new WebConnectionHtmlUnitDriver();
		this.driver = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(this.mockMvc).withDelegate(otherDriver).build();

		assertMockMvcUsed("http://localhost/test");
	}

	@Test
	void mockMvcSetupWithDefaultDriverDelegate() throws Exception {
		this.driver = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(this.mockMvc).build();

		assertMockMvcUsed("http://localhost/test");
	}

	@Test
	void javaScriptEnabledByDefault() {
		this.driver = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(this.mockMvc).build();
		assertThat(this.driver.isJavascriptEnabled()).isTrue();
	}

	@Test
	void javaScriptDisabled() {
		this.driver = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(this.mockMvc).javascriptEnabled(false).build();
		assertThat(this.driver.isJavascriptEnabled()).isFalse();
	}

	@Test // SPR-14066
	void cookieManagerShared() throws Exception {
		WebConnectionHtmlUnitDriver otherDriver = new WebConnectionHtmlUnitDriver();
		this.mockMvc = MockMvcBuilders.standaloneSetup(new CookieController()).build();
		this.driver = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(this.mockMvc).withDelegate(otherDriver).build();

		assertThat(get("http://localhost/")).isEqualTo("");
		Cookie cookie = new Cookie("localhost", "cookie", "cookieManagerShared");
		otherDriver.getWebClient().getCookieManager().addCookie(cookie);
		assertThat(get("http://localhost/")).isEqualTo("cookieManagerShared");
	}


	private void assertMockMvcUsed(String url) throws Exception {
		assertThat(get(url)).contains(EXPECTED_BODY);
	}

	private String get(String url) throws IOException {
		this.driver.get(url);
		return this.driver.getPageSource();
	}


	@Configuration
	@EnableWebMvc
	static class Config {

		@RestController
		static class ContextPathController {

			@RequestMapping("/test")
			String contextPath(HttpServletRequest request) {
				return EXPECTED_BODY;
			}
		}
	}

	@RestController
	static class CookieController {

		@RequestMapping(path = "/", produces = "text/plain")
		String cookie(@CookieValue("cookie") String cookie) {
			return cookie;
		}
	}

}
