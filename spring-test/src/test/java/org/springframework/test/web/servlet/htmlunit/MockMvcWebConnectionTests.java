/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.htmlunit;

import java.io.IOException;

import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.Page;
import org.htmlunit.WebClient;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Integration tests for {@link MockMvcWebConnection}.
 *
 * @author Rob Winch
 * @since 4.2
 */
class MockMvcWebConnectionTests {

	private final WebClient webClient = new WebClient();

	private final MockMvc mockMvc =
			MockMvcBuilders.standaloneSetup(new HelloController(), new ForwardController()).build();


	@Test
	void contextPathNull() throws IOException {
		this.webClient.setWebConnection(new MockMvcWebConnection(this.mockMvc, this.webClient, null));
		Page page = this.webClient.getPage("http://localhost/context/a");
		assertThat(page.getWebResponse().getStatusCode()).isEqualTo(200);
	}

	@Test
	void contextPathExplicit() throws IOException {
		this.webClient.setWebConnection(new MockMvcWebConnection(this.mockMvc, this.webClient, "/context"));
		Page page = this.webClient.getPage("http://localhost/context/a");
		assertThat(page.getWebResponse().getStatusCode()).isEqualTo(200);
	}

	@Test
	void contextPathEmpty() {
		this.webClient.setWebConnection(new MockMvcWebConnection(this.mockMvc, this.webClient, ""));
		// Empty context path (root context) should not match to a URL with a context path
		assertThatExceptionOfType(FailingHttpStatusCodeException.class)
				.isThrownBy(() -> this.webClient.getPage("http://localhost/context/a"))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(404));

		this.webClient.setWebConnection(new MockMvcWebConnection(this.mockMvc, this.webClient));
		// No context is the same providing an empty context path
		assertThatExceptionOfType(FailingHttpStatusCodeException.class)
				.isThrownBy(() -> this.webClient.getPage("http://localhost/context/a"))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(404));
	}

	@Test
	void forward() throws IOException {
		this.webClient.setWebConnection(new MockMvcWebConnection(this.mockMvc, this.webClient, ""));
		Page page = this.webClient.getPage("http://localhost/forward");
		assertThat(page.getWebResponse().getContentAsString()).isEqualTo("hello");
	}

	@Test
	void infiniteForward() {
		this.webClient.setWebConnection(new MockMvcWebConnection(this.mockMvc, this.webClient, ""));
		assertThatIllegalStateException()
				.isThrownBy(() -> this.webClient.getPage("http://localhost/infiniteForward"))
				.withMessage("Forwarded 100 times in a row, potential infinite forward loop");
	}

	@Test
	void contextPathDoesNotStartWithSlash() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MockMvcWebConnection(this.mockMvc, this.webClient, "context"));
	}

	@Test
	void contextPathEndsWithSlash() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MockMvcWebConnection(this.mockMvc, this.webClient, "/context/"));
	}

}
