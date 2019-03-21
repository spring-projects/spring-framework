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

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;

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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link MockMvcWebConnectionBuilderSupport}.
 *
 * @author Rob Winch
 * @author Rossen Stoyanchev
 * @since 4.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
@SuppressWarnings("rawtypes")
public class MockMvcConnectionBuilderSupportTests {

	private final WebClient client = mock(WebClient.class);

	private MockMvcWebConnectionBuilderSupport builder;

	@Autowired
	private WebApplicationContext wac;


	@Before
	public void setup() {
		when(this.client.getWebConnection()).thenReturn(mock(WebConnection.class));
		this.builder = new MockMvcWebConnectionBuilderSupport(this.wac) {};
	}


	@Test(expected = IllegalArgumentException.class)
	public void constructorMockMvcNull() {
		new MockMvcWebConnectionBuilderSupport((MockMvc) null){};
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorContextNull() {
		new MockMvcWebConnectionBuilderSupport((WebApplicationContext) null){};
	}

	@Test
	public void context() throws Exception {
		WebConnection conn = this.builder.createConnection(this.client);

		assertMockMvcUsed(conn, "http://localhost/");
		assertMockMvcNotUsed(conn, "https://example.com/");
	}

	@Test
	public void mockMvc() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
		WebConnection conn = new MockMvcWebConnectionBuilderSupport(mockMvc) {}.createConnection(this.client);

		assertMockMvcUsed(conn, "http://localhost/");
		assertMockMvcNotUsed(conn, "https://example.com/");
	}

	@Test
	public void mockMvcExampleDotCom() throws Exception {
		WebConnection conn = this.builder.useMockMvcForHosts("example.com").createConnection(this.client);

		assertMockMvcUsed(conn, "http://localhost/");
		assertMockMvcUsed(conn, "https://example.com/");
		assertMockMvcNotUsed(conn, "http://other.com/");
	}

	@Test
	public void mockMvcAlwaysUseMockMvc() throws Exception {
		WebConnection conn = this.builder.alwaysUseMockMvc().createConnection(this.client);
		assertMockMvcUsed(conn, "http://other.com/");
	}

	@Test
	public void defaultContextPathEmpty() throws Exception {
		WebConnection conn = this.builder.createConnection(this.client);
		assertThat(getResponse(conn, "http://localhost/abc").getContentAsString(), equalTo(""));
	}

	@Test
	public void defaultContextPathCustom() throws Exception {
		WebConnection conn = this.builder.contextPath("/abc").createConnection(this.client);
		assertThat(getResponse(conn, "http://localhost/abc/def").getContentAsString(), equalTo("/abc"));
	}


	private void assertMockMvcUsed(WebConnection connection, String url) throws Exception {
		assertThat(getResponse(connection, url), notNullValue());
	}

	private void assertMockMvcNotUsed(WebConnection connection, String url) throws Exception {
		assertThat(getResponse(connection, url), nullValue());
	}

	private WebResponse getResponse(WebConnection connection, String url) throws IOException {
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
