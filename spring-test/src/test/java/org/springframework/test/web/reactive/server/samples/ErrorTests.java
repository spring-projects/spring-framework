/*
 * Copyright 2002-2019 the original author or authors.
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
public class ErrorTests {

	private final WebTestClient client = WebTestClient.bindToController(new TestController()).build();


	@Test
	public void notFound(){
		this.client.get().uri("/invalid")
				.exchange()
				.expectStatus().isNotFound()
				.expectBody(Void.class);
	}

	@Test
	public void serverException() {
		this.client.get().uri("/server-error")
				.exchange()
				.expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
				.expectBody(Void.class);
	}

	@Test // SPR-17363
	public void badRequestBeforeRequestBodyConsumed() {
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
