/*
 * Copyright 2002-2021 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Samples of tests using {@link WebTestClient} with soft assertions.
 *
 * @author Michał Rowicki
 * @since 5.3
 */
public class SoftAssertionTests {

	private WebTestClient client;


	@BeforeEach
	public void setUp() throws Exception {
		this.client = WebTestClient.bindToController(new TestController()).build();
	}


	@Test
	public void test() throws Exception {
		this.client.get().uri("/test")
				.exchange()
				.expectAllSoftly(
						exchange -> exchange.expectStatus().isOk(),
						exchange -> exchange.expectBody(String.class).isEqualTo("It works!")
				);
	}

	@Test
	public void testAllFails() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.client.get().uri("/test")
						.exchange()
						.expectAllSoftly(
								exchange -> exchange.expectStatus().isBadRequest(),
								exchange -> exchange.expectBody(String.class).isEqualTo("It won't work :(")
						)
		).withMessage("[0] Status expected:<400 BAD_REQUEST> but was:<200 OK>\n[1] Response body expected:<It won't work :(> but was:<It works!>");
	}


	@RestController
	static class TestController {

		@GetMapping("/test")
		public String handle() {
			return "It works!";
		}
	}
}
