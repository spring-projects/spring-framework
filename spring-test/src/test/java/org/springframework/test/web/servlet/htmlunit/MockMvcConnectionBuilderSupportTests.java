/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.htmlunit;

import java.io.IOException;
import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import org.htmlunit.WebClient;
import org.htmlunit.WebConnection;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
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
@SpringJUnitWebConfig
@SuppressWarnings("rawtypes")
class MockMvcConnectionBuilderSupportTests {

	private final WebClient client = mock();

	private MockMvcWebConnectionBuilderSupport builder;

	@Autowired
	private WebApplicationContext wac;


	@BeforeEach
	void setup() {
		given(this.client.getWebConnection()).willReturn(mock());
		this.builder = new MockMvcWebConnectionBuilderSupport(this.wac) {};
	}


	@Test
	void constructorMockMvcNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MockMvcWebConnectionBuilderSupport((MockMvc) null){});
	}

	@Test
	void constructorContextNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MockMvcWebConnectionBuilderSupport((WebApplicationContext) null){});
	}

	@Test
	void context() throws Exception {
		WebConnection conn = this.builder.createConnection(this.client);

		assertMockMvcUsed(conn, "http://localhost/");
		assertMockMvcNotUsed(conn, "https://example.com/");
	}

	@Test
	void mockMvc() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
		WebConnection conn = new MockMvcWebConnectionBuilderSupport(mockMvc) {}.createConnection(this.client);

		assertMockMvcUsed(conn, "http://localhost/");
		assertMockMvcNotUsed(conn, "https://example.com/");
	}

	@Test
	void mockMvcExampleDotCom() throws Exception {
		WebConnection conn = this.builder.useMockMvcForHosts("example.com").createConnection(this.client);

		assertMockMvcUsed(conn, "http://localhost/");
		assertMockMvcUsed(conn, "https://example.com/");
		assertMockMvcNotUsed(conn, "http://other.example/");
	}

	@Test
	void mockMvcAlwaysUseMockMvc() throws Exception {
		WebConnection conn = this.builder.alwaysUseMockMvc().createConnection(this.client);
		assertMockMvcUsed(conn, "http://other.example/");
	}

	@Test
	void defaultContextPathEmpty() throws Exception {
		WebConnection conn = this.builder.createConnection(this.client);
		assertThat(getResponse(conn, "http://localhost/abc").getContentAsString()).isEmpty();
	}

	@Test
	void defaultContextPathCustom() throws Exception {
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
		return connection.getResponse(new WebRequest(URI.create(url).toURL()));
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
