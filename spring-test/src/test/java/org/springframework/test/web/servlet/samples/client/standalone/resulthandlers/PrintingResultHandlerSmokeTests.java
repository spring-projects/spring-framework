/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.web.servlet.samples.client.standalone.resulthandlers;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

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
public class PrintingResultHandlerSmokeTests {

	private final WebTestClient testClient =
			MockMvcWebTestClient.bindToController(new SimpleController()).build();


	// Not intended to be executed with the build.
	// Comment out class-level @Disabled to see the output.

	@Test
	public void printViaConsumer() {
		testClient.post().uri("/")
				.contentType(MediaType.TEXT_PLAIN)
				.bodyValue("Hello Request".getBytes(StandardCharsets.UTF_8))
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.consumeWith(System.out::println);
	}

	@Test
	public void returnResultAndPrint() {
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
