/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server.samples.bind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sample tests demonstrating "mock" server tests binding to an annotated
 * controller.
 *
 * @author Rossen Stoyanchev
 */
class ControllerTests {

	private WebTestClient client;


	@BeforeEach
	void setUp() throws Exception {
		this.client = WebTestClient.bindToController(new TestController()).build();
	}


	@Test
	void test() throws Exception {
		this.client.get().uri("/test")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("It works!");
	}


	@RestController
	static class TestController {

		@GetMapping("/test")
		public String handle() {
			return "It works!";
		}
	}

}
