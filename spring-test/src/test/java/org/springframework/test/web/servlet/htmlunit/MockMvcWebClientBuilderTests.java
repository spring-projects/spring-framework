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

package org.springframework.test.web.servlet.htmlunit;

import java.io.IOException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for {@link MockMvcWebClientBuilder}.
 *
 * @author Rob Winch
 * @since 4.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class MockMvcWebClientBuilderTests {

	private WebClient webClient = new WebClient();

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
	public void mockMvcSetupAndConfigureWebClient() throws Exception {
		this.webClient = MockMvcWebClientBuilder
				.mockMvcSetup(this.mockMvc)
				.configureWebClient(this.webClient);

		assertMvcProcessed("http://localhost/test");
		assertDelegateProcessed("http://example.com/");
	}

	@Test
	public void mockMvcSetupAndCreateWebClient() throws Exception {
		this.webClient = MockMvcWebClientBuilder
				.mockMvcSetup(this.mockMvc)
				.createWebClient();

		assertMvcProcessed("http://localhost/test");
		assertDelegateProcessed("http://example.com/");
	}

	private void assertMvcProcessed(String url) throws Exception {
		assertThat(getWebResponse(url).getContentAsString(), equalTo("mvc"));
	}

	private void assertDelegateProcessed(String url) throws Exception {
		assertThat(getWebResponse(url).getContentAsString(), not(equalTo("mvc")));
	}

	private WebResponse getWebResponse(String url) throws IOException {
		return this.webClient.getWebConnection().getResponse(new WebRequest(new URL(url)));
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

}
