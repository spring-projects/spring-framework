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

package org.springframework.test.web.servlet.samples.client.standalone.resultmatches;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.resultmatchers.ContentAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class ContentAssertionTests {

	private final WebTestClient testClient =
			MockMvcWebTestClient.bindToController(new SimpleController()).build();

	@Test
	public void testContentType() {
		testClient.get().uri("/handle").accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.valueOf("text/plain;charset=ISO-8859-1"))
				.expectHeader().contentType("text/plain;charset=ISO-8859-1")
				.expectHeader().contentTypeCompatibleWith("text/plain")
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN);

		testClient.get().uri("/handleUtf8")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.valueOf("text/plain;charset=UTF-8"))
				.expectHeader().contentType("text/plain;charset=UTF-8")
				.expectHeader().contentTypeCompatibleWith("text/plain")
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN);
	}

	@Test
	public void testContentAsString() {

		testClient.get().uri("/handle").accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Hello world!");

		testClient.get().uri("/handleUtf8").accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01");

		// Hamcrest matchers...
		testClient.get().uri("/handle").accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).value(equalTo("Hello world!"));
		testClient.get().uri("/handleUtf8")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).value(equalTo("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01"));
	}

	@Test
	public void testContentAsBytes() {

		testClient.get().uri("/handle").accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectBody(byte[].class).isEqualTo(
				"Hello world!".getBytes(StandardCharsets.ISO_8859_1));

		testClient.get().uri("/handleUtf8")
				.exchange()
				.expectStatus().isOk()
				.expectBody(byte[].class).isEqualTo(
				"\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void testContentStringMatcher() {
		testClient.get().uri("/handle").accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).value(containsString("world"));
	}

	@Test
	public void testCharacterEncoding() {

		testClient.get().uri("/handle").accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType("text/plain;charset=ISO-8859-1")
				.expectBody(String.class).value(containsString("world"));

		testClient.get().uri("/handleUtf8")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType("text/plain;charset=UTF-8")
				.expectBody(byte[].class)
				.isEqualTo("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01".getBytes(StandardCharsets.UTF_8));
	}


	@Controller
	private static class SimpleController {

		@RequestMapping(value="/handle", produces="text/plain")
		@ResponseBody
		public String handle() {
			return "Hello world!";
		}

		@RequestMapping(value="/handleUtf8", produces="text/plain;charset=UTF-8")
		@ResponseBody
		public String handleWithCharset() {
			return "\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01";	// "Hello world! (Japanese)
		}
	}

}
