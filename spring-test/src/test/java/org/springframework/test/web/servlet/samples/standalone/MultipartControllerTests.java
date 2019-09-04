/*
 * Copyright 2002-2018 the original author or authors.
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

import org.junit.Assert;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class MultipartControllerTests {

	@Test
	public void multipartRequestWithSingleFile() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile filePart = new MockMultipartFile("file", "orig", null, fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfile").file(filePart).file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	public void multipartRequestWithSingleFileNotPresent() throws Exception {
		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfile"))
				.andExpect(status().isFound());
	}

	@Test
	public void multipartRequestWithFileArray() throws Exception {
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
	public void multipartRequestWithFileArrayNotPresent() throws Exception {
		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfilearray"))
				.andExpect(status().isFound());
	}

	@Test
	public void multipartRequestWithFileArrayNoMultipart() throws Exception {
		standaloneSetup(new MultipartController()).build()
				.perform(post("/multipartfilearray"))
				.andExpect(status().isFound());
	}

	@Test
	public void multipartRequestWithFileList() throws Exception {
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
	public void multipartRequestWithFileListNotPresent() throws Exception {
		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfilelist"))
				.andExpect(status().isFound());
	}

	@Test
	public void multipartRequestWithFileListNoMultipart() throws Exception {
		standaloneSetup(new MultipartController()).build()
				.perform(post("/multipartfilelist"))
				.andExpect(status().isFound());
	}

	@Test
	public void multipartRequestWithOptionalFile() throws Exception {
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
	public void multipartRequestWithOptionalFileNotPresent() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfile").file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attributeDoesNotExist("fileContent"))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	public void multipartRequestWithOptionalFileArray() throws Exception {
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
	public void multipartRequestWithOptionalFileArrayNotPresent() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfilearray").file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attributeDoesNotExist("fileContent"))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	public void multipartRequestWithOptionalFileList() throws Exception {
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
	public void multipartRequestWithOptionalFileListNotPresent() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfilelist").file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attributeDoesNotExist("fileContent"))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	public void multipartRequestWithServletParts() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockPart filePart = new MockPart("file", "orig", fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockPart jsonPart = new MockPart("json", "json", json);
		jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfile").part(filePart).part(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test  // SPR-13317
	public void multipartRequestWrapped() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		Filter filter = new RequestWrappingFilter();
		MockMvc mockMvc = standaloneSetup(new MultipartController()).addFilter(filter).build();

		Map<String, String> jsonMap = Collections.singletonMap("name", "yeeeah");
		mockMvc.perform(multipart("/json").file(jsonPart)).andExpect(model().attribute("json", jsonMap));
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
				Assert.assertArrayEquals(content, file[1].getBytes());
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
				Assert.assertArrayEquals(content, file.get(1).getBytes());
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
				Assert.assertArrayEquals(content, file.get()[1].getBytes());
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
				Assert.assertArrayEquals(content, file.get().get(1).getBytes());
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
