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

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link MockMvcWebConnectionBuilderSupport}.
 *
 * @author Rob Winch
 * @author Rossen Stoyanchev
 * @since 4.2
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
@SuppressWarnings("rawtypes")
public class MockMvcConnectionBuilderSupportTests {

	private final WebClient client = mock(WebClient.class);

	private MockMvcWebConnectionBuilderSupport builder;

	@Autowired
	private WebApplicationContext wac;


	@BeforeEach
	public void setup() {
		given(this.client.getWebConnection()).willReturn(mock(WebConnection.class));
		this.builder = new MockMvcWebConnectionBuilderSupport(this.wac) {};
	}


	@Test
	public void constructorMockMvcNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MockMvcWebConnectionBuilderSupport((MockMvc) null){});
	}

	@Test
	public void constructorContextNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MockMvcWebConnectionBuilderSupport((WebApplicationContext) null){});
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
		assertMockMvcNotUsed(conn, "http://other.example/");
	}

	@Test
	public void mockMvcAlwaysUseMockMvc() throws Exception {
		WebConnection conn = this.builder.alwaysUseMockMvc().createConnection(this.client);
		assertMockMvcUsed(conn, "http://other.example/");
	}

	@Test
	public void defaultContextPathEmpty() throws Exception {
		WebConnection conn = this.builder.createConnection(this.client);
		assertThat(getResponse(conn, "http://localhost/abc").getContentAsString()).isEqualTo("");
	}

	@Test
	public void defaultContextPathCustom() throws Exception {
		WebConnection conn = this.builder.contextPath("/abc").createConnection(this.client);
		assertThat(getResponse(conn, "http://localhost/abc/def").getContentAsString()).isEqualTo("/abc");
	}


	private void assertMockMvcUsed(WebConnection connection, String url) throws Exception {
		assertThat(getResponse(connection, url)).isNotNull();
	}

	private void assertMockMvcNotUsed(WebConnection connection, String url) throws Exception {
		assertThat(getResponse(connection, url)).isNull();
	}

	private WebResponse getResponse(WebConnection connection, String url) throws IOException {
		return connection.getResponse(new WebRequest(new URL(url)));
	}


	@Configuration
	@EnableWebMvc
	static class Config {

		@RestController
		static class ContextPathController {

			@RequestMapping("/def")
			public String contextPath(HttpServletRequest request) {
				return request.getContextPath();
			}
		}
	}

}
