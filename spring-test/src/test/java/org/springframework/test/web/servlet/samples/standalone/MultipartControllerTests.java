/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.servlet.samples.standalone;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class MultipartControllerTests {

	@Test
	public void multipartRequest() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile filePart = new MockMultipartFile("file", "orig", null, fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/test-multipartfile").file(filePart).file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	public void multipartRequestWithServletParts() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockPart filePart = new MockPart("file", "orig", new ByteArrayInputStream(fileContent));

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockPart jsonPart = new MockPart("json", "json", new ByteArrayInputStream(json));
		jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/test-multipartfile").part(filePart).part(jsonPart))
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
		mockMvc.perform(multipart("/test-json").file(jsonPart)).andExpect(model().attribute("json", jsonMap));
	}


	@Controller
	private static class MultipartController {

		@RequestMapping(value = "/test-multipartfile", method = RequestMethod.POST)
		public String processMultipartFile(@RequestParam MultipartFile file,
				@RequestPart Map<String, String> json, Model model) throws IOException {

			model.addAttribute("jsonContent", json);
			model.addAttribute("fileContent", file.getBytes());

			return "redirect:/index";
		}

		@RequestMapping(value = "/test-part", method = RequestMethod.POST)
		public String processPart(@RequestParam Part part,
				@RequestPart Map<String, String> json, Model model) throws IOException {

			model.addAttribute("jsonContent", json);
			model.addAttribute("fileContent", part.getInputStream());

			return "redirect:/index";
		}

		@RequestMapping(value = "/test-json", method = RequestMethod.POST)
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