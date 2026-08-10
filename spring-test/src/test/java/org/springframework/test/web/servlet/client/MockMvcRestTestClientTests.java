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
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.multipart.FilePart;
import org.springframework.http.converter.multipart.FormFieldPart;
import org.springframework.http.converter.multipart.Part;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that use a {@link RestTestClient} configured with a {@link MockMvc} instance
 * that uses a standalone controller.
 *
 * @author Rob Worsnop
 * @author Sam Brannen
 * @author Brian Clozel
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

	@Test
	void retrieveMultipart() {
		client.get()
				.uri("/multipart")
				.accept(MediaType.MULTIPART_FORM_DATA)
				.exchange()
				.expectStatus().isOk()
				.expectBody(new ParameterizedTypeReference<MultiValueMap<String, Part>>() {})
				.value(result -> {
					assertThat(result).hasSize(3);
					assertThat(result).containsKeys("text1", "text2", "file1");
					assertThat(result.getFirst("text1")).isInstanceOfSatisfying(FormFieldPart.class,
							part -> assertThat(part.value()).isEqualTo("a"));
					assertThat(result.getFirst("text2")).isInstanceOfSatisfying(FormFieldPart.class,
							part -> assertThat(part.value()).isEqualTo("b"));
					assertThat(result.getFirst("file1")).isInstanceOfSatisfying(FilePart.class,
							part -> assertThat(part.filename()).isEqualTo("file1.txt"));
				});
	}

	@Test
	void writeMultipart() {
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("text1", "value1");
		parts.add("file1", new ByteArrayResource("filecontent1".getBytes()) {
			@Override
			public String getFilename() {
				return "spring.txt";
			}
		});

		client.post()
				.uri("/multipart")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(parts)
				.exchange()
				.expectStatus().isOk();
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

		@GetMapping(path = "/multipart", produces = MediaType.MULTIPART_FORM_DATA_VALUE)
		MultiValueMap<String, Object> writeMultipart() {
			MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
			parts.add("text1", "a");
			parts.add("text2", "b");
			Resource resource = new ByteArrayResource("Lorem ipsum dolor sit amet".getBytes()) {
				@Override
				public String getFilename() {
					return "file1.txt";
				}
			};
			parts.add("file1", resource);
			return parts;
		}

		@PostMapping(path = "/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
		ResponseEntity<Void> readMultipart(@RequestParam MultiValueMap<String, jakarta.servlet.http.Part> parts) throws Exception {
			assertThat(parts.keySet()).containsOnly("text1", "file1");
			jakarta.servlet.http.Part text1 = parts.get("text1").get(0);
			assertThat(text1.getName()).isEqualTo("text1");
			assertThat(text1.getInputStream()).asString(StandardCharsets.UTF_8).isEqualTo("value1");
			jakarta.servlet.http.Part file1 = parts.get("file1").get(0);
			assertThat(file1.getName()).isEqualTo("file1");
			assertThat(file1.getSubmittedFileName()).isEqualTo("spring.txt");
			assertThat(file1.getInputStream()).asString(StandardCharsets.UTF_8).isEqualTo("filecontent1");
			return ResponseEntity.ok().build();
		}
	}

}
