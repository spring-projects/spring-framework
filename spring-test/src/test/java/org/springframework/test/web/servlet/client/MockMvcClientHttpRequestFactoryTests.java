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

package org.springframework.test.web.servlet.client;

import java.io.IOException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tests that use a {@link RestTestClient} configured with a
 * {@link MockMvcClientHttpRequestFactory} that is in turn configured with a
 * {@link MockMvc} instance that uses a standalone controller
 *
 * @author Rob Worsnop
 */
@ExtendWith(SpringExtension.class)
public class MockMvcClientHttpRequestFactoryTests {

	private RestTestClient client;

	@BeforeEach
	public void setup() {
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController()).build();
		this.client = RestTestClient.bindTo(mockMvc).build();
	}

	@Test
	public void withResult() {
		client.get()
				.uri("/foo")
				.cookie("session", "12345")
				.exchange()
				.expectCookie().valueEquals("session", "12345")
				.expectBody(String.class)
				.isEqualTo("bar");
	}

	@Test
	public void withError() {
		client.get()
				.uri("/error")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody().isEmpty();
	}

	@Test
	public void withErrorAndBody() {
		client.get().uri("/errorbody")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody(String.class)
				.isEqualTo("some really bad request");
	}

	@RestController
	static class TestController {

		@GetMapping(value = "/foo")
		public void foo(@CookieValue("session") String session, HttpServletResponse response) throws IOException {
			response.getWriter().write("bar");
			response.addCookie(new Cookie("session", session));
		}

		@GetMapping(value = "/error")
		public void handleError(HttpServletResponse response) throws Exception {
			response.sendError(400);
		}

		@GetMapping(value = "/errorbody")
		public void handleErrorWithBody(HttpServletResponse response) throws Exception {
			response.sendError(400);
			response.getWriter().write("some really bad request");
		}
	}

}
