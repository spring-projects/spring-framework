/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.client;

import java.io.IOException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tests that use a {@link RestTestClient} configured with a {@link MockMvc} instance
 * that uses a standalone controller.
 *
 * @author Rob Worsnop
 * @author Sam Brannen
 * @since 7.0
 */
class MockMvcRestTestClientTests {

	private final RestTestClient client;

	MockMvcRestTestClientTests() {
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController()).build();
		this.client = RestTestClient.bindTo(mockMvc).build();
	}


	@Test
	void withResult() {
		client.get()
				.uri("/foo")
				.cookie("session", "12345")
				.exchange()
				.expectCookie().valueEquals("session", "12345")
				.expectBody(String.class)
				.isEqualTo("bar");
	}

	@Test
	void withError() {
		client.get()
				.uri("/error")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody().isEmpty();
	}

	@Test
	void withErrorAndBody() {
		client.get().uri("/errorbody")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody(String.class)
				.isEqualTo("some really bad request");
	}

	@RestController
	static class TestController {

		@GetMapping("/foo")
		void foo(@CookieValue("session") String session, HttpServletResponse response) throws IOException {
			response.getWriter().write("bar");
			response.addCookie(new Cookie("session", session));
		}

		@GetMapping("/error")
		void handleError(HttpServletResponse response) throws Exception {
			response.sendError(400);
		}

		@GetMapping("/errorbody")
		void handleErrorWithBody(HttpServletResponse response) throws Exception {
			response.sendError(400);
			response.getWriter().write("some really bad request");
		}
	}

}
