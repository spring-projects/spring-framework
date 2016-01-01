/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.htmlunit.webdriver.MockMvcHtmlUnitDriverBuilder.*;

/**
 * Integration tests for {@link MockMvcHtmlUnitDriverBuilder}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class MockMvcHtmlUnitDriverBuilderTests {

	private static final String EXPECTED_BODY = "MockMvcHtmlUnitDriverBuilderTests mvc";

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	private HtmlUnitDriver driver;

	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void webAppContextSetupNull() {
		webAppContextSetup(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void mockMvcSetupNull() {
		mockMvcSetup(null);
	}

	@Test
	public void mockMvcSetupWithCustomDriverDelegate() throws Exception {
		WebConnectionHtmlUnitDriver preconfiguredDriver = new WebConnectionHtmlUnitDriver();
		this.driver = mockMvcSetup(this.mockMvc).withDelegate(preconfiguredDriver).build();

		assertMvcProcessed("http://localhost/test");
		Assume.group(TestGroup.PERFORMANCE, () -> assertDelegateProcessed("http://example.com/"));
	}

	@Test
	public void mockMvcSetupWithDefaultDriverDelegate() throws Exception {
		this.driver = mockMvcSetup(this.mockMvc).build();

		assertMvcProcessed("http://localhost/test");
		Assume.group(TestGroup.PERFORMANCE, () -> assertDelegateProcessed("http://example.com/"));
	}

	@Test
	public void javaScriptEnabledByDefault() {
		this.driver = mockMvcSetup(this.mockMvc).build();

		assertTrue(this.driver.isJavascriptEnabled());
	}

	@Test
	public void javaScriptDisabled() {
		this.driver = mockMvcSetup(this.mockMvc).javascriptEnabled(false).build();

		assertFalse(this.driver.isJavascriptEnabled());
	}

	private void assertMvcProcessed(String url) throws Exception {
		assertThat(get(url), containsString(EXPECTED_BODY));
	}

	private void assertDelegateProcessed(String url) throws Exception {
		assertThat(get(url), not(containsString(EXPECTED_BODY)));
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

			@RequestMapping
			public String contextPath(HttpServletRequest request) {
				return EXPECTED_BODY;
			}
		}
	}

}