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

package org.springframework.test.web.servlet.samples.standalone;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Jaebin Joo
 * @author Sam Brannen
 */
class MultipartControllerTests {

	@ParameterizedTest
	@ValueSource(strings = {"/multipartfile", "/multipartfile-via-put", "/part"})
	void multipartRequestWithSingleFileOrPart(String url) throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		MockMultipartHttpServletRequestBuilder requestBuilder = switch (url) {
			case "/multipartfile" -> multipart(url).file(new MockMultipartFile("file", "orig", null, fileContent));
			case "/multipartfile-via-put" ->
					multipart(HttpMethod.PUT, url).file(new MockMultipartFile("file", "orig", null, fileContent));
			default -> multipart(url).part(new MockPart("part", "orig", fileContent));
		};

		standaloneSetup(new MultipartController()).build()
				.perform(requestBuilder.file(jsonPart))
				.andExpect(status().isFound());
	}

	@Test
	void multipartRequestWithSingleFileNotPresent() throws Exception {
		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfile"))
				.andExpect(status().isFound());
	}

	@Test
	void multipartRequestWithFileArray() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile filePart1 = new MockMultipartFile("file", "orig", null, fileContent);
		MockMultipartFile filePart2 = new MockMultipartFile("file", "orig", null, fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfilearray").file(filePart1).file(filePart2).file(jsonPart))
				.andExpect(status().isFound());
	}

	@Test
	void multipartRequestWithFileArrayNotPresent() throws Exception {
		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfilearray"))
				.andExpect(status().isFound());
	}

	@Test
	void multipartRequestWithFileArrayNoMultipart() throws Exception {
		standaloneSetup(new MultipartController()).build()
				.perform(post("/multipartfilearray"))
				.andExpect(status().isFound());
	}

	@Test
	void multipartRequestWithFileList() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile filePart1 = new MockMultipartFile("file", "orig", null, fileContent);
		MockMultipartFile filePart2 = new MockMultipartFile("file", "orig", null, fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfilelist").file(filePart1).file(filePart2).file(jsonPart))
				.andExpect(status().isFound());
	}

	@Test
	void multipartRequestWithFileListNotPresent() throws Exception {
		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfilelist"))
				.andExpect(status().isFound());
	}

	@Test
	void multipartRequestWithFileListNoMultipart() throws Exception {
		standaloneSetup(new MultipartController()).build()
				.perform(post("/multipartfilelist"))
				.andExpect(status().isFound());
	}

	@Test
	void multipartRequestWithOptionalFile() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile filePart = new MockMultipartFile("file", "orig", null, fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfile").file(filePart).file(jsonPart))
				.andExpect(status().isFound());
	}

	@Test
	void multipartRequestWithOptionalFileNotPresent() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfile").file(jsonPart))
				.andExpect(status().isFound());
	}

	@Test
	void multipartRequestWithOptionalFileArray() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile filePart1 = new MockMultipartFile("file", "orig", null, fileContent);
		MockMultipartFile filePart2 = new MockMultipartFile("file", "orig", null, fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfilearray").file(filePart1).file(filePart2).file(jsonPart))
				.andExpect(status().isFound());
	}

	@Test
	void multipartRequestWithOptionalFileArrayNotPresent() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfilearray").file(jsonPart))
				.andExpect(status().isFound());
	}

	@Test
	void multipartRequestWithOptionalFileList() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile filePart1 = new MockMultipartFile("file", "orig", null, fileContent);
		MockMultipartFile filePart2 = new MockMultipartFile("file", "orig", null, fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfilelist").file(filePart1).file(filePart2).file(jsonPart))
				.andExpect(status().isFound());
	}

	@Test
	void multipartRequestWithOptionalFileListNotPresent() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfilelist").file(jsonPart))
				.andExpect(status().isFound());
	}

	@Test
	void multipartRequestWithDataBindingToFile() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockPart filePart = new MockPart("file", "orig", fileContent);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfilebinding").part(filePart))
				.andExpect(status().isFound());
	}

	@Test  // SPR-13317
	void multipartRequestWrapped() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		Filter filter = new RequestWrappingFilter();
		MockMvc mockMvc = standaloneSetup(new MultipartController()).addFilter(filter).build();

		mockMvc.perform(multipart("/json").file(jsonPart)).andExpect(status().isFound());
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

			if (!CollectionUtils.isEmpty(file)) {
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
		public String processPart(@RequestPart Part part, @RequestPart Map<String, String> json) {
			return "redirect:/index";
		}

		@PostMapping("/json")
		public String processMultipart(@RequestPart Map<String, String> json) {
			return "redirect:/index";
		}

		@PostMapping("/multipartfilebinding")
		public String processMultipartFileBean(
				MultipartFileBean multipartFileBean, RedirectAttributes model, BindingResult bindingResult)
				throws IOException {

			if (!bindingResult.hasErrors()) {
				MultipartFile file = multipartFileBean.getFile();
				if (file != null) {
					model.addAttribute("fileContent", file.getBytes());
				}
			}
			return "redirect:/index";
		}
	}


	private static class MultipartFileBean {

		private MultipartFile file;

		public MultipartFile getFile() {
			return file;
		}

		@SuppressWarnings("unused")
		public void setFile(MultipartFile file) {
			this.file = file;
		}
	}


	private static class RequestWrappingFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(
				HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
				throws IOException, ServletException {

			request = new HttpServletRequestWrapper(request);
			filterChain.doFilter(request, response);
		}
	}

}
