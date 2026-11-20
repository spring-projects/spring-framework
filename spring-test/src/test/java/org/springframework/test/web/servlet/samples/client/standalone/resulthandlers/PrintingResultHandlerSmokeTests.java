/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.client.standalone.resulthandlers;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.resulthandlers.PrintingResultHandlerSmokeTests}.
 *
 * @author Rossen Stoyanchev
 */
@Disabled
class PrintingResultHandlerSmokeTests {

	private final WebTestClient testClient =
			MockMvcWebTestClient.bindToController(new SimpleController()).build();


	// Not intended to be executed with the build.
	// Comment out class-level @Disabled to see the output.

	@Test
	void printViaConsumer() {
		testClient.post().uri("/")
				.contentType(MediaType.TEXT_PLAIN)
				.bodyValue("Hello Request".getBytes(StandardCharsets.UTF_8))
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.consumeWith(System.out::println);
	}

	@Test
	void returnResultAndPrint() {
		EntityExchangeResult<String> result = testClient.post().uri("/")
				.contentType(MediaType.TEXT_PLAIN)
				.bodyValue("Hello Request".getBytes(StandardCharsets.UTF_8))
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.returnResult();

		System.out.println(result);
	}


	@RestController
	private static class SimpleController {

		@PostMapping("/")
		public String hello(HttpServletResponse response) {
			response.addCookie(new Cookie("enigma", "42"));
			return "Hello Response";
		}
	}

}
