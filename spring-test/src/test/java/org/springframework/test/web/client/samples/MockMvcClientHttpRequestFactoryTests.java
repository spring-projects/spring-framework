/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.client.samples;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests that use a {@link RestTemplate} configured with a
 * {@link MockMvcClientHttpRequestFactory} that is in turn configured with a
 * {@link MockMvc} instance that uses a {@link WebApplicationContext} loaded by
 * the TestContext framework.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
@SpringJUnitWebConfig
@SuppressWarnings({"deprecation", "removal"})
class MockMvcClientHttpRequestFactoryTests {

	private final RestTemplate template;

	MockMvcClientHttpRequestFactoryTests(WebApplicationContext wac) {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
		this.template = new RestTemplate(new MockMvcClientHttpRequestFactory(mockMvc));
	}


	@Test
	void withResult() {
		assertThat(template.getForObject("/foo", String.class)).isEqualTo("bar");
	}

	@Test
	void withError() {
		assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> template.getForEntity("/error", String.class))
				.withMessageContaining("400")
				.withMessageContaining("some bad request");
	}

	@Test
	void withErrorAndBody() {
		assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> template.getForEntity("/errorbody", String.class))
				.withMessageContaining("400")
				.withMessageContaining("some really bad request");
	}


	@EnableWebMvc
	@Configuration(proxyBeanMethods = false)
	@Import(MyController.class)
	static class MyWebConfig implements WebMvcConfigurer {
	}

	@Controller
	static class MyController {

		@GetMapping("/foo")
		@ResponseBody
		String handle() {
			return "bar";
		}

		@GetMapping("/error")
		void handleError(HttpServletResponse response) throws Exception {
			response.sendError(400, "some bad request");
		}

		@GetMapping("/errorbody")
		void handleErrorWithBody(HttpServletResponse response) throws Exception {
			response.sendError(400, "some bad request");
			response.getWriter().write("some really bad request");
		}
	}

}
