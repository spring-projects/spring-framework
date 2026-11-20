/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.htmlunit.webdriver;

import jakarta.servlet.http.HttpServletRequest;
import org.htmlunit.util.Cookie;
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
	void mockMvcSetupWithCustomDriverDelegate() {
		WebConnectionHtmlUnitDriver otherDriver = new WebConnectionHtmlUnitDriver();
		this.driver = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(this.mockMvc).withDelegate(otherDriver).build();

		assertMockMvcUsed("http://localhost/test");
	}

	@Test
	void mockMvcSetupWithDefaultDriverDelegate() {
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
	void cookieManagerShared() {
		WebConnectionHtmlUnitDriver otherDriver = new WebConnectionHtmlUnitDriver();
		this.mockMvc = MockMvcBuilders.standaloneSetup(new CookieController()).build();
		this.driver = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(this.mockMvc).withDelegate(otherDriver).build();

		assertThat(get("http://localhost/")).isEmpty();
		Cookie cookie = new Cookie("localhost", "cookie", "cookieManagerShared");
		otherDriver.getWebClient().getCookieManager().addCookie(cookie);
		assertThat(get("http://localhost/")).isEqualTo("cookieManagerShared");
	}


	private void assertMockMvcUsed(String url) {
		assertThat(get(url)).contains(EXPECTED_BODY);
	}

	private String get(String url) {
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
