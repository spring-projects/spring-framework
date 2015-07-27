/*
 * Copyright 2002-2015 the original author or authors.
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

import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link MockMvcWebConnectionBuilderSupport}.
 *
 * @author Rob Winch
 * @since 4.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
@SuppressWarnings("rawtypes")
public class MockMvcConnectionBuilderSupportTests {

	private final WebConnection delegateConnection = mock(WebConnection.class);

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	private WebConnection connection;

	@Before
	public void setup() {
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

		connection = new MockMvcWebConnectionBuilderSupport(mockMvc){}
				.createConnection(delegateConnection);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorMockMvcNull() {
		new MockMvcWebConnectionBuilderSupport((MockMvc)null){};
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorContextNull() {
		new MockMvcWebConnectionBuilderSupport((WebApplicationContext)null){};
	}

	@Test
	public void context() throws Exception {
		connection = new MockMvcWebConnectionBuilderSupport(wac) {}
				.createConnection(delegateConnection);

		assertMvcProcessed("http://localhost/");
		assertDelegateProcessed("http://example.com/");
	}

	@Test
	public void mockMvc() throws Exception {
		assertMvcProcessed("http://localhost/");
		assertDelegateProcessed("http://example.com/");
	}

	@Test
	public void mockMvcExampleDotCom() throws Exception {
		connection = new MockMvcWebConnectionBuilderSupport(wac) {}
				.useMockMvcForHosts("example.com")
				.createConnection(delegateConnection);

		assertMvcProcessed("http://localhost/");
		assertMvcProcessed("http://example.com/");
		assertDelegateProcessed("http://other.com/");
	}

	@Test
	public void mockMvcAlwaysUseMockMvc() throws Exception {
		connection = new MockMvcWebConnectionBuilderSupport(wac) {}
				.alwaysUseMockMvc()
				.createConnection(delegateConnection);

		assertMvcProcessed("http://other.com/");
	}

	@Test
	public void defaultContextPathEmpty() throws Exception {
		connection = new MockMvcWebConnectionBuilderSupport(wac) {}
				.createConnection(delegateConnection);

		assertThat(getWebResponse("http://localhost/abc").getContentAsString(), equalTo(""));
	}

	@Test
	public void defaultContextPathCustom() throws Exception {
		connection = new MockMvcWebConnectionBuilderSupport(wac) {}
				.contextPath("/abc").createConnection(delegateConnection);

		assertThat(getWebResponse("http://localhost/abc/def").getContentAsString(), equalTo("/abc"));
	}

	private void assertMvcProcessed(String url) throws Exception {
		assertThat(getWebResponse(url), notNullValue());
	}

	private void assertDelegateProcessed(String url) throws Exception {
		assertThat(getWebResponse(url), nullValue());
	}

	private WebResponse getWebResponse(String url) throws IOException {
		return connection.getResponse(new WebRequest(new URL(url)));
	}


	@Configuration
	@EnableWebMvc
	static class Config {

		@RestController
		static class ContextPathController {

			@RequestMapping
			public String contextPath(HttpServletRequest request) {
				return request.getContextPath();
			}
		}
	}

}
