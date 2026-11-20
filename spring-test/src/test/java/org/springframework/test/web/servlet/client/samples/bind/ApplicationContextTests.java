/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.client.samples.bind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

/**
 * Sample tests demonstrating "mock" server tests binding to server infrastructure
 * declared in a Spring ApplicationContext.
 *
 * @author Rob Worsnop
 */
@SpringJUnitWebConfig(ApplicationContextTests.WebConfig.class)
class ApplicationContextTests {

	private RestTestClient client;

	private final WebApplicationContext context;


	public ApplicationContextTests(WebApplicationContext context) {
		this.context = context;
	}


	@BeforeEach
	void setUp() {
		this.client = RestTestClient.bindToApplicationContext(context).build();
	}


	@Test
	void test() {
		this.client.get().uri("/test")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("It works!");
	}


	@Configuration
	static class WebConfig {

		@Bean
		public TestController controller() {
			return new TestController();
		}

	}

	@RestController
	static class TestController {

		@GetMapping("/test")
		public String handle() {
			return "It works!";
		}
	}

}
