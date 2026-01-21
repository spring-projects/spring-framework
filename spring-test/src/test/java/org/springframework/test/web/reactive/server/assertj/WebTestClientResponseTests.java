/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.reactive.server.assertj;


import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link WebTestClientResponse}.
 *
 * @author Rossen Stoyanchev
 */
public class WebTestClientResponseTests {

	private final WebTestClient client = WebTestClient.bindToController(HelloController.class).build();


	@Test
	void status() {
		ResponseSpec spec = client.get().uri("/greeting").exchange();
		assertThat(WebTestClientResponse.from(spec)).hasStatusOk().hasStatus2xxSuccessful();
	}

	@Test
	void headers() {
		ResponseSpec spec = client.get().uri("/greeting").exchange();

		WebTestClientResponse response = WebTestClientResponse.from(spec);
		assertThat(response).hasStatusOk();
		assertThat(response).headers()
				.containsOnlyHeaders(HttpHeaders.CONTENT_TYPE, HttpHeaders.CONTENT_LENGTH)
				.hasValue(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8")
				.hasValue(HttpHeaders.CONTENT_LENGTH, 11);
	}

	@Test
	void contentType() {
		ResponseSpec spec = client.get().uri("/greeting").exchange();

		WebTestClientResponse response = WebTestClientResponse.from(spec);
		assertThat(response).hasStatusOk();
		assertThat(response).contentType().isEqualTo("text/plain;charset=UTF-8");
		assertThat(response).hasContentTypeCompatibleWith(MediaType.TEXT_PLAIN);
	}

	@Test
	void cookies() {
		ResponseSpec spec = client.get().uri("/cookie").exchange();

		WebTestClientResponse response = WebTestClientResponse.from(spec);
		assertThat(response).hasStatusOk();
		assertThat(response).cookies().hasValue("foo", "bar");
		assertThat(response).body().isEmpty();
	}

	@Test
	void bodyText() {
		ResponseSpec spec = client.get().uri("/greeting").exchange();

		WebTestClientResponse response = WebTestClientResponse.from(spec);
		assertThat(response).hasStatusOk();
		assertThat(response).contentType().isCompatibleWith(MediaType.TEXT_PLAIN);
		assertThat(response).bodyText().isEqualTo("Hello World");
		assertThat(response).hasBodyTextEqualTo("Hello World");
	}

	@Test
	void bodyJson() {
		ResponseSpec spec = client.get().uri("/message").exchange();

		WebTestClientResponse response = WebTestClientResponse.from(spec);
		assertThat(response).hasStatusOk();
		assertThat(response).contentType().isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(response).bodyJson().extractingPath("$.message").asString().isEqualTo("Hello World");
	}


	@SuppressWarnings("unused")
	@RestController
	private static class HelloController {

		@GetMapping("/greeting")
		public String getGreeting() {
			return "Hello World";
		}

		@GetMapping("/message")
		public Map<String, ?> getMessage() {
			return Map.of("message", "Hello World");
		}

		@GetMapping("/cookie")
		public void getCookie(ServerWebExchange exchange) {
			ResponseCookie cookie = ResponseCookie.from("foo", "bar").build();
			exchange.getResponse().addCookie(cookie);
		}
	}

}
