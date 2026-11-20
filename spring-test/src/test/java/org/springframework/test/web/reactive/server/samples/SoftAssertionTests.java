/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server.samples;

import org.junit.jupiter.api.Test;

import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link WebTestClient} with soft assertions.
 *
 * @author Michał Rowicki
 * @author Sam Brannen
 * @since 5.3.10
 */
class SoftAssertionTests {

	private final WebTestClient webTestClient = WebTestClient.bindToController(new TestController()).build();


	@Test
	void expectAll() {
		this.webTestClient.get().uri("/test").exchange()
			.expectAll(
				responseSpec -> responseSpec.expectStatus().isOk(),
				responseSpec -> responseSpec.expectBody(String.class).isEqualTo("hello")
			);
	}

	@Test
	void expectAllWithMultipleFailures() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() ->
						this.webTestClient.get().uri("/test").exchange()
								.expectAll(
										responseSpec -> responseSpec.expectStatus().isBadRequest(),
										responseSpec -> responseSpec.expectStatus().isOk(),
										responseSpec -> responseSpec.expectBody(String.class).isEqualTo("bogus")
								)
				)
				.withMessage("""
						Multiple Exceptions (2):
						Status expected:<400 BAD_REQUEST> but was:<200 OK>
						Response body expected:<bogus> but was:<hello>""");
	}


	@RestController
	static class TestController {

		@GetMapping("/test")
		String handle() {
			return "hello";
		}
	}

}
