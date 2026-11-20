/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.client.samples;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tests with error status codes or error conditions.
 *
 * @author Rob Worsnop
 */
class ErrorTests {

	private final RestTestClient client = RestTestClient.bindToController(new TestController()).build();


	@Test
	void notFound(){
		this.client.get().uri("/invalid")
				.exchange()
				.expectStatus().isNotFound();
	}

	@Test
	void serverException() {
		this.client.get().uri("/server-error")
				.exchange()
				.expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}


	@RestController
	private static class TestController {

		@GetMapping("/server-error")
		void handleAndThrowException() {
			throw new IllegalStateException("server error");
		}

	}
}
