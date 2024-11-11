/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.web.servlet.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.TEXT_PLAIN;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.resultmatchers.ContentAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
class ContentAssertionTests {

	private final WebTestClient testClient =
			MockMvcWebTestClient.bindToController(new SimpleController()).build();

	@Test
	void contentType() {
		testClient.get().uri("/handle").accept(TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.valueOf("text/plain;charset=ISO-8859-1"))
				.expectHeader().contentType("text/plain;charset=ISO-8859-1")
				.expectHeader().contentTypeCompatibleWith("text/plain")
				.expectHeader().contentTypeCompatibleWith(TEXT_PLAIN);

		testClient.get().uri("/handleUtf8")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.valueOf("text/plain;charset=UTF-8"))
				.expectHeader().contentType("text/plain;charset=UTF-8")
				.expectHeader().contentTypeCompatibleWith("text/plain")
				.expectHeader().contentTypeCompatibleWith(TEXT_PLAIN);
	}

	@Test
	void contentAsString() {
		testClient.get().uri("/handle").accept(TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Hello world!");

		testClient.get().uri("/handleUtf8").accept(TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01");

		// Hamcrest matchers...
		testClient.get().uri("/handle").accept(TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).value(equalTo("Hello world!"));
		testClient.get().uri("/handleUtf8")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).value(equalTo("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01"));
	}

	@Test
	void contentAsBytes() {
		testClient.get().uri("/handle").accept(TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectBody(byte[].class).isEqualTo(
				"Hello world!".getBytes(ISO_8859_1));

		testClient.get().uri("/handleUtf8")
				.exchange()
				.expectStatus().isOk()
				.expectBody(byte[].class).isEqualTo(
				"\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01".getBytes(UTF_8));
	}

	@Test
	void contentStringMatcher() {
		testClient.get().uri("/handle").accept(TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).value(containsString("world"));
	}

	@Test
	void characterEncoding() {
		testClient.get().uri("/handle").accept(TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType("text/plain;charset=ISO-8859-1")
				.expectBody(String.class).value(containsString("world"));

		testClient.get().uri("/handleUtf8")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType("text/plain;charset=UTF-8")
				.expectBody(byte[].class)
				.isEqualTo("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01".getBytes(UTF_8));
	}


	@Controller
	private static class SimpleController {

		@RequestMapping(path="/handle", produces="text/plain")
		@ResponseBody
		String handle() {
			return "Hello world!";
		}

		@RequestMapping(path="/handleUtf8", produces="text/plain;charset=UTF-8")
		@ResponseBody
		String handleWithCharset() {
			return "\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01";	// "Hello world!" (Japanese)
		}
	}

}
