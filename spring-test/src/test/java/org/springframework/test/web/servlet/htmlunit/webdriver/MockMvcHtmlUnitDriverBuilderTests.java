/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.test.web.servlet.htmlunit.webdriver;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @author Rob Winch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class MockMvcHtmlUnitDriverBuilderTests {

	public static final String EXPECTED_BODY = "MockMvcHtmlUnitDriverBuilderTests mvc";

	@Autowired
	WebApplicationContext context;

	MockMvc mockMvc;

	HtmlUnitDriver driver;

	@Before
	public void setup() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void mockMvcSetupNull() {
		MockMvcHtmlUnitDriverBuilder.mockMvcSetup(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void webAppContextSetupNull() {
		MockMvcHtmlUnitDriverBuilder.webAppContextSetup(null);
	}

	@Test
	public void mockMvcSetupConfigureDriver() throws Exception {
		driver = MockMvcHtmlUnitDriverBuilder
				.mockMvcSetup(mockMvc)
				.configureDriver(new WebConnectionHtmlUnitDriver());

		assertMvcProcessed("http://localhost/test");
		assertDelegateProcessed("http://example.com/");
	}

	@Test
	public void mockMvcSetupCreateDriver() throws Exception {
		driver = MockMvcHtmlUnitDriverBuilder
				.mockMvcSetup(mockMvc)
				.createDriver();

		assertMvcProcessed("http://localhost/test");
		assertDelegateProcessed("http://example.com/");
	}

	@Test
	public void javascriptEnabledDefaultEnabled() {
		driver = MockMvcHtmlUnitDriverBuilder
				.mockMvcSetup(mockMvc)
				.createDriver();

		assertThat(driver.isJavascriptEnabled(), equalTo(true));
	}

	@Test
	public void javascriptEnabledDisabled() {
		driver = MockMvcHtmlUnitDriverBuilder
				.mockMvcSetup(mockMvc)
				.javascriptEnabled(false)
				.createDriver();

		assertThat(driver.isJavascriptEnabled(), equalTo(false));
	}

	private void assertMvcProcessed(String url) throws Exception {
		assertThat(get(url), containsString(EXPECTED_BODY));
	}

	private void assertDelegateProcessed(String url) throws Exception {
		assertThat(get(url), not(containsString(EXPECTED_BODY)));
	}

	private String get(String url) throws IOException {
		driver.get(url);
		return driver.getPageSource();
	}

	@Configuration
	@EnableWebMvc
	static class Config {
		@RestController
		static class ContextPathController {
			@RequestMapping
			public String contextPath(HttpServletRequest request) {
				return EXPECTED_BODY;
			}
		}
	}

}