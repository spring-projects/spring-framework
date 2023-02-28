/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.web.servlet.samples.client.standalone;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.MultipartControllerTests}.
 *
 * @author Rossen Stoyanchev
 */
public class MultipartControllerTests {

	private final WebTestClient testClient = MockMvcWebTestClient.bindToController(new MultipartController()).build();


	@Test
	public void multipartRequestWithSingleFile() throws Exception {

		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		testClient.post().uri("/multipartfile")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Now try the same with HTTP PUT
		testClient.put().uri("/multipartfile-via-put")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();
	}

	@Test
	public void multipartRequestWithSingleFileNotPresent() {
		testClient.post().uri("/multipartfile")
				.exchange()
				.expectStatus().isFound();
	}

	@Test
	public void multipartRequestWithFileArray() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		testClient.post().uri("/multipartfilearray")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();
	}

	@Test
	public void multipartRequestWithFileArrayNoMultipart() {
		testClient.post().uri("/multipartfilearray")
				.exchange()
				.expectStatus().isFound();
	}

	@Test
	public void multipartRequestWithOptionalFile() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		testClient.post().uri("/optionalfile")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();
	}

	@Test
	public void multipartRequestWithOptionalFileNotPresent() throws Exception {
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		testClient.post().uri("/optionalfile")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();
	}

	@Test
	public void multipartRequestWithOptionalFileArray() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		testClient.post().uri("/optionalfilearray")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();
	}

	@Test
	public void multipartRequestWithOptionalFileArrayNotPresent() throws Exception {
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		testClient.post().uri("/optionalfilearray")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();
	}

	@Test
	public void multipartRequestWithOptionalFileList() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		testClient.post().uri("/optionalfilelist")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();
	}

	@Test
	public void multipartRequestWithOptionalFileListNotPresent() throws Exception {
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		testClient.post().uri("/optionalfilelist")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();
	}

	@Test
	public void multipartRequestWithServletParts() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		testClient.post().uri("/multipartfile")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();
	}

	@Test
	public void multipartRequestWrapped() throws Exception {
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		WebTestClient client = MockMvcWebTestClient.bindToController(new MultipartController())
				.filter(new RequestWrappingFilter())
				.build();

		client.post().uri("/multipartfile")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();
	}


	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	@Controller
	private static class MultipartController {

		@PostMapping("/multipartfile")
		public String processMultipartFile(@RequestParam(required = false) MultipartFile file,
				@RequestPart(required = false) Map<String, String> json) {

			return "redirect:/index";
		}

		@PutMapping("/multipartfile-via-put")
		public String processMultipartFileViaHttpPut(@RequestParam(required = false) MultipartFile file,
				@RequestPart(required = false) Map<String, String> json) {

			return processMultipartFile(file, json);
		}

		@PostMapping("/multipartfilearray")
		public String processMultipartFileArray(@RequestParam(required = false) MultipartFile[] file,
				@RequestPart(required = false) Map<String, String> json) throws IOException {

			if (file != null && file.length > 0) {
				byte[] content = file[0].getBytes();
				assertThat(file[1].getBytes()).isEqualTo(content);
			}
			return "redirect:/index";
		}

		@PostMapping("/multipartfilelist")
		public String processMultipartFileList(@RequestParam(required = false) List<MultipartFile> file,
				@RequestPart(required = false) Map<String, String> json) throws IOException {

			if (file != null && !file.isEmpty()) {
				byte[] content = file.get(0).getBytes();
				assertThat(file.get(1).getBytes()).isEqualTo(content);
			}
			return "redirect:/index";
		}

		@PostMapping("/optionalfile")
		public String processOptionalFile(
				@RequestParam Optional<MultipartFile> file, @RequestPart Map<String, String> json) {

			return "redirect:/index";
		}

		@PostMapping("/optionalfilearray")
		public String processOptionalFileArray(
				@RequestParam Optional<MultipartFile[]> file, @RequestPart Map<String, String> json)
				throws IOException {

			if (file.isPresent()) {
				byte[] content = file.get()[0].getBytes();
				assertThat(file.get()[1].getBytes()).isEqualTo(content);
			}
			return "redirect:/index";
		}

		@PostMapping("/optionalfilelist")
		public String processOptionalFileList(
				@RequestParam Optional<List<MultipartFile>> file, @RequestPart Map<String, String> json)
				throws IOException {

			if (file.isPresent()) {
				byte[] content = file.get().get(0).getBytes();
				assertThat(file.get().get(1).getBytes()).isEqualTo(content);
			}
			return "redirect:/index";
		}

		@PostMapping("/part")
		public String processPart(@RequestParam Part part, @RequestPart Map<String, String> json) {
			return "redirect:/index";
		}

		@PostMapping("/json")
		public String processMultipart(@RequestPart Map<String, String> json) {
			return "redirect:/index";
		}
	}


	private static class RequestWrappingFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws IOException, ServletException {

			request = new HttpServletRequestWrapper(request);
			filterChain.doFilter(request, response);
		}
	}

}
