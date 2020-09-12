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

package org.springframework.test.web.servlet.samples.client.standalone;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

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

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/multipartfile")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", json));
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

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/multipartfilearray")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", json));
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

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/optionalfile")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWithOptionalFileNotPresent() throws Exception {
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/optionalfile")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attributeDoesNotExist("fileContent"))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWithOptionalFileArray() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/optionalfilearray")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWithOptionalFileArrayNotPresent() throws Exception {
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/optionalfilearray")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attributeDoesNotExist("fileContent"))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWithOptionalFileList() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/optionalfilelist")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWithOptionalFileListNotPresent() throws Exception {
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/optionalfilelist")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attributeDoesNotExist("fileContent"))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWithServletParts() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/multipartfile")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWrapped() throws Exception {
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		WebTestClient client = MockMvcWebTestClient.bindToController(new MultipartController())
				.filter(new RequestWrappingFilter())
				.build();

		EntityExchangeResult<Void> exchangeResult = client.post().uri("/multipartfile")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attribute("jsonContent", json));
	}


	@Controller
	private static class MultipartController {

		@RequestMapping(value = "/multipartfile", method = RequestMethod.POST)
		public String processMultipartFile(@RequestParam(required = false) MultipartFile file,
				@RequestPart(required = false) Map<String, String> json, Model model) throws IOException {

			if (file != null) {
				model.addAttribute("fileContent", file.getBytes());
			}
			if (json != null) {
				model.addAttribute("jsonContent", json);
			}

			return "redirect:/index";
		}

		@RequestMapping(value = "/multipartfilearray", method = RequestMethod.POST)
		public String processMultipartFileArray(@RequestParam(required = false) MultipartFile[] file,
				@RequestPart(required = false) Map<String, String> json, Model model) throws IOException {

			if (file != null && file.length > 0) {
				byte[] content = file[0].getBytes();
				assertThat(file[1].getBytes()).isEqualTo(content);
				model.addAttribute("fileContent", content);
			}
			if (json != null) {
				model.addAttribute("jsonContent", json);
			}

			return "redirect:/index";
		}

		@RequestMapping(value = "/multipartfilelist", method = RequestMethod.POST)
		public String processMultipartFileList(@RequestParam(required = false) List<MultipartFile> file,
				@RequestPart(required = false) Map<String, String> json, Model model) throws IOException {

			if (file != null && !file.isEmpty()) {
				byte[] content = file.get(0).getBytes();
				assertThat(file.get(1).getBytes()).isEqualTo(content);
				model.addAttribute("fileContent", content);
			}
			if (json != null) {
				model.addAttribute("jsonContent", json);
			}

			return "redirect:/index";
		}

		@RequestMapping(value = "/optionalfile", method = RequestMethod.POST)
		public String processOptionalFile(@RequestParam Optional<MultipartFile> file,
				@RequestPart Map<String, String> json, Model model) throws IOException {

			if (file.isPresent()) {
				model.addAttribute("fileContent", file.get().getBytes());
			}
			model.addAttribute("jsonContent", json);

			return "redirect:/index";
		}

		@RequestMapping(value = "/optionalfilearray", method = RequestMethod.POST)
		public String processOptionalFileArray(@RequestParam Optional<MultipartFile[]> file,
				@RequestPart Map<String, String> json, Model model) throws IOException {

			if (file.isPresent()) {
				byte[] content = file.get()[0].getBytes();
				assertThat(file.get()[1].getBytes()).isEqualTo(content);
				model.addAttribute("fileContent", content);
			}
			model.addAttribute("jsonContent", json);

			return "redirect:/index";
		}

		@RequestMapping(value = "/optionalfilelist", method = RequestMethod.POST)
		public String processOptionalFileList(@RequestParam Optional<List<MultipartFile>> file,
				@RequestPart Map<String, String> json, Model model) throws IOException {

			if (file.isPresent()) {
				byte[] content = file.get().get(0).getBytes();
				assertThat(file.get().get(1).getBytes()).isEqualTo(content);
				model.addAttribute("fileContent", content);
			}
			model.addAttribute("jsonContent", json);

			return "redirect:/index";
		}

		@RequestMapping(value = "/part", method = RequestMethod.POST)
		public String processPart(@RequestParam Part part,
				@RequestPart Map<String, String> json, Model model) throws IOException {

			model.addAttribute("fileContent", part.getInputStream());
			model.addAttribute("jsonContent", json);

			return "redirect:/index";
		}

		@RequestMapping(value = "/json", method = RequestMethod.POST)
		public String processMultipart(@RequestPart Map<String, String> json, Model model) {
			model.addAttribute("json", json);
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
