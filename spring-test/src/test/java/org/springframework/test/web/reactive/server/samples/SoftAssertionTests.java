/*
 * Copyright 2002-2022 the original author or authors.
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
