/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server.samples;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests with error status codes or error conditions.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ErrorTests {

	private final WebTestClient client = WebTestClient.bindToController(new TestController()).build();


	@Test
	void notFound(){
		this.client.get().uri("/invalid")
				.exchange()
				.expectStatus().isNotFound()
				.expectBody(Void.class);
	}

	@Test
	void serverException() {
		this.client.get().uri("/server-error")
				.exchange()
				.expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
				.expectBody(Void.class);
	}

	@Test // SPR-17363
	void badRequestBeforeRequestBodyConsumed() {
		EntityExchangeResult<Void> result = this.client.post()
				.uri("/post")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(new Person("Dan"))
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody().isEmpty();

		byte[] content = result.getRequestBodyContent();
		assertThat(content).isNotNull();
		assertThat(new String(content, StandardCharsets.UTF_8)).isEqualTo("{\"name\":\"Dan\"}");
	}


	@RestController
	static class TestController {

		@GetMapping("/server-error")
		void handleAndThrowException() {
			throw new IllegalStateException("server error");
		}

		@PostMapping(path = "/post", params = "p")
		void handlePost(@RequestBody Person person) {
		}
	}

}
