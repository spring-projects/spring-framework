/*
 * Copyright 2002-2015 the original author or authors.
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * @author Rossen Stoyanchev
 */
public class FileUploadControllerTests {

	private static final Charset CHARSET = Charset.forName("UTF-8");


	@Test
	public void multipartRequest() throws Exception {

		byte[] fileContent = "bar".getBytes(CHARSET);
		MockMultipartFile filePart = new MockMultipartFile("file", "orig", null, fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(CHARSET);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		MockMvc mockMvc = standaloneSetup(new MultipartController()).build();
		mockMvc.perform(fileUpload("/test").file(filePart).file(jsonPart))
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	// SPR-13317

	@Test
	public void multipartRequestWrapped() throws Exception {

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(CHARSET);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		Filter filter = new RequestWrappingFilter();
		MockMvc mockMvc = standaloneSetup(new MultipartController()).addFilter(filter).build();

		Map<String, String> jsonMap = Collections.singletonMap("name", "yeeeah");
		mockMvc.perform(fileUpload("/testJson").file(jsonPart)).andExpect(model().attribute("json", jsonMap));
	}


	@Controller
	private static class MultipartController {

		@RequestMapping(value = "/test", method = RequestMethod.POST)
		public String processMultipart(@RequestParam MultipartFile file,
				@RequestPart Map<String, String> json, Model model) throws IOException {

			model.addAttribute("jsonContent", json);
			model.addAttribute("fileContent", file.getBytes());

			return "redirect:/index";
		}

		@RequestMapping(value = "/testJson", method = RequestMethod.POST)
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