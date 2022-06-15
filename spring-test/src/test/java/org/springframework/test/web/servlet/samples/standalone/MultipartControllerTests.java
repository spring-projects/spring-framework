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

package org.springframework.test.web.servlet.samples.standalone;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
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

		MockMultipartHttpServletRequestBuilder requestBuilder;
		switch (url) {
			case "/multipartfile":
				requestBuilder = multipart(url).file(new MockMultipartFile("file", "orig", null, fileContent));
				break;
			case "/multipartfile-via-put":
				requestBuilder = multipart(HttpMethod.PUT, url).file(new MockMultipartFile("file", "orig", null, fileContent));
				break;
			default:
				requestBuilder = multipart(url).part(new MockPart("part", "orig", fileContent));
				break;
		}

		standaloneSetup(new MultipartController()).build()
				.perform(requestBuilder.file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
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
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
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
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
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
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	void multipartRequestWithOptionalFileNotPresent() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfile").file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attributeDoesNotExist("fileContent"))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
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
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	void multipartRequestWithOptionalFileArrayNotPresent() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfilearray").file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attributeDoesNotExist("fileContent"))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
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
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	void multipartRequestWithOptionalFileListNotPresent() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfilelist").file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attributeDoesNotExist("fileContent"))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	void multipartRequestWithDataBindingToFile() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockPart filePart = new MockPart("file", "orig", fileContent);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfilebinding").part(filePart))
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent));
	}

	@Test  // SPR-13317
	void multipartRequestWrapped() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		Filter filter = new RequestWrappingFilter();
		MockMvc mockMvc = standaloneSetup(new MultipartController()).addFilter(filter).build();

		Map<String, String> jsonMap = Collections.singletonMap("name", "yeeeah");
		mockMvc.perform(multipart("/json").file(jsonPart)).andExpect(model().attribute("json", jsonMap));
	}


	@Controller
	private static class MultipartController {

		@PostMapping("/multipartfile")
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

		@PutMapping("/multipartfile-via-put")
		public String processMultipartFileViaHttpPut(@RequestParam(required = false) MultipartFile file,
				@RequestPart(required = false) Map<String, String> json, Model model) throws IOException {

			return processMultipartFile(file, json, model);
		}

		@PostMapping("/multipartfilearray")
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

		@PostMapping("/multipartfilelist")
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

		@PostMapping("/optionalfile")
		public String processOptionalFile(@RequestParam Optional<MultipartFile> file,
				@RequestPart Map<String, String> json, Model model) throws IOException {

			if (file.isPresent()) {
				model.addAttribute("fileContent", file.get().getBytes());
			}
			model.addAttribute("jsonContent", json);

			return "redirect:/index";
		}

		@PostMapping("/optionalfilearray")
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

		@PostMapping("/optionalfilelist")
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

		@PostMapping("/part")
		public String processPart(@RequestPart Part part,
				@RequestPart Map<String, String> json, Model model) throws IOException {

			if (part != null) {
				byte[] content = StreamUtils.copyToByteArray(part.getInputStream());
				model.addAttribute("fileContent", content);
			}
			model.addAttribute("jsonContent", json);

			return "redirect:/index";
		}

		@PostMapping("/json")
		public String processMultipart(@RequestPart Map<String, String> json, Model model) {
			model.addAttribute("json", json);
			return "redirect:/index";
		}

		@PostMapping("/multipartfilebinding")
		public String processMultipartFileBean(
				MultipartFileBean multipartFileBean, Model model, BindingResult bindingResult) throws IOException {

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
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws IOException, ServletException {

			request = new HttpServletRequestWrapper(request);
			filterChain.doFilter(request, response);
		}
	}

}
