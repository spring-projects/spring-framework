/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.resultmatchers.UrlAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
class UrlAssertionTests {

	private final WebTestClient testClient =
			MockMvcWebTestClient.bindToController(new SimpleController()).build();


	@Test
	void redirect() {
		testClient.get().uri("/persons")
				.exchange()
				.expectStatus().isFound()
				.expectHeader().location("/persons/1");
	}

	@Test
	void redirectPattern() throws Exception {
		EntityExchangeResult<Void> result =
				testClient.get().uri("/persons").exchange().expectBody().isEmpty();

		MockMvcWebTestClient.resultActionsFor(result)
				.andExpect(redirectedUrlPattern("/persons/*"));
	}

	@Test
	void forward() {
		testClient.get().uri("/")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals("Forwarded-Url", "/home");
	}

	@Test
	void forwardPattern() throws Exception {
		EntityExchangeResult<Void> result =
				testClient.get().uri("/").exchange().expectBody().isEmpty();

		MockMvcWebTestClient.resultActionsFor(result)
				.andExpect(forwardedUrlPattern("/ho?e"));
	}


	@Controller
	private static class SimpleController {

		@RequestMapping("/persons")
		public String save() {
			return "redirect:/persons/1";
		}

		@RequestMapping("/")
		public String forward() {
			return "forward:/home";
		}
	}
}
