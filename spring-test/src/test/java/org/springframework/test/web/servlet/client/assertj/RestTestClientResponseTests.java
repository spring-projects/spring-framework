/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.client.assertj;


import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RestTestClientResponse}.
 *
 * @author Rossen Stoyanchev
 */
class RestTestClientResponseTests {

	private final RestTestClient client =
			RestTestClient.bindToController(HelloController.class)
					.configureMessageConverters(HttpMessageConverters.Builder::registerDefaults)
					.build();


	@Test
	void status() {
		ResponseSpec spec = client.get().uri("/greeting").exchange();
		assertThat(RestTestClientResponse.from(spec)).hasStatusOk().hasStatus2xxSuccessful();
	}

	@Test
	void headers() {
		RestTestClient.ResponseSpec spec = client.get().uri("/greeting").exchange();

		RestTestClientResponse response = RestTestClientResponse.from(spec);
		assertThat(response).hasStatusOk();
		assertThat(response).headers()
				.containsOnlyHeaders(HttpHeaders.CONTENT_TYPE, HttpHeaders.CONTENT_LENGTH)
				.hasValue(HttpHeaders.CONTENT_TYPE, "text/plain;charset=ISO-8859-1")
				.hasValue(HttpHeaders.CONTENT_LENGTH, 11);
	}

	@Test
	void contentType() {
		ResponseSpec spec = client.get().uri("/greeting").exchange();

		RestTestClientResponse response = RestTestClientResponse.from(spec);
		assertThat(response).hasStatusOk();
		assertThat(response).contentType().isEqualTo("text/plain;charset=ISO-8859-1");
		assertThat(response).hasContentTypeCompatibleWith(MediaType.TEXT_PLAIN);
	}

	@Test
	void cookies() {
		ResponseSpec spec = client.get().uri("/cookie").exchange();

		RestTestClientResponse response = RestTestClientResponse.from(spec);
		assertThat(response).hasStatusOk();
		assertThat(response).cookies().hasValue("foo", "bar");
		assertThat(response).body().isEmpty();
	}

	@Test
	void bodyText() {
		ResponseSpec spec = client.get().uri("/greeting").exchange();

		RestTestClientResponse response = RestTestClientResponse.from(spec);
		assertThat(response).hasStatusOk();
		assertThat(response).contentType().isCompatibleWith(MediaType.TEXT_PLAIN);
		assertThat(response).bodyText().isEqualTo("Hello World");
		assertThat(response).hasBodyTextEqualTo("Hello World");
	}

	@Test
	void bodyJson() {
		ResponseSpec spec = client.get().uri("/message").exchange();

		RestTestClientResponse response = RestTestClientResponse.from(spec);
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
		public void getCookie(HttpServletResponse response) {
			response.addCookie(new Cookie("foo", "bar"));
		}
	}

}
