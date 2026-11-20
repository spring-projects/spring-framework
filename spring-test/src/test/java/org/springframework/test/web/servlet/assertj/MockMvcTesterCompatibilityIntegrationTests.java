/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.assertj;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link MockMvcTester} that use the methods that
 * integrate with {@link MockMvc} way of building the requests and
 * asserting the responses.
 *
 * @author Stephane Nicoll
 */
@SpringJUnitConfig
@WebAppConfiguration
class MockMvcTesterCompatibilityIntegrationTests {

	private final MockMvcTester mvc;

	MockMvcTesterCompatibilityIntegrationTests(@Autowired WebApplicationContext wac) {
		this.mvc = MockMvcTester.from(wac);
	}

	@Test
	void performGet() {
		assertThat(this.mvc.perform(get("/greet"))).hasStatusOk();
	}

	@Test
	void performGetWithInvalidMediaTypeAssertion() {
		MvcTestResult result = this.mvc.perform(get("/greet"));
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.withMessageContaining("is compatible with 'application/json'");
	}

	@Test
	void assertHttpStatusCode() {
		assertThat(this.mvc.get().uri("/greet")).matches(status().isOk());
	}


	@Configuration
	@EnableWebMvc
	@Import(TestController.class)
	static class WebConfiguration {
	}

	@RestController
	static class TestController {

		@GetMapping(path = "/greet", produces = "text/plain")
		String greet() {
			return "hello";
		}

		@GetMapping(path = "/message", produces = MediaType.APPLICATION_JSON_VALUE)
		String message() {
			return "{\"message\": \"hello\"}";
		}
	}

}
