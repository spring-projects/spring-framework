/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.web.mock.servlet.samples.standalone.resultmatchers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.mock.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.mock.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.mock.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.mock.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Examples of defining expectations on the response content, content type, and
 * the character encoding.
 *
 * @author Rossen Stoyanchev
 *
 * @see JsonPathAssertionTests
 * @see XmlContentAssertionTests
 * @see XpathAssertionTests
 */
public class ContentAssertionTests {

	private MockMvc mockMvc;

	@Before
	public void setup() {
		this.mockMvc = standaloneSetup(new SimpleController()).alwaysExpect(status().isOk()).build();
	}

	@Test
	public void testContentType() throws Exception {
		this.mockMvc.perform(get("/handle"))
			.andExpect(content().mimeType(MediaType.TEXT_PLAIN))
			.andExpect(content().mimeType("text/plain"));

		this.mockMvc.perform(get("/handleUtf8"))
			.andExpect(content().mimeType(MediaType.valueOf("text/plain;charset=UTF-8")))
			.andExpect(content().mimeType("text/plain;charset=UTF-8"));
	}

	@Test
	public void testContentAsString() throws Exception {
		this.mockMvc.perform(get("/handle")).andExpect(content().string("Hello world!"));
		this.mockMvc.perform(get("/handleUtf8")).andExpect(content().string("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01"));

		// Hamcrest matchers...
		this.mockMvc.perform(get("/handle")).andExpect(content().string(equalTo("Hello world!")));
		this.mockMvc.perform(get("/handleUtf8")).andExpect(content().string(equalTo("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01")));
	}

	@Test
	public void testContentAsBytes() throws Exception {
		this.mockMvc.perform(get("/handle")).andExpect(content().bytes("Hello world!".getBytes("ISO-8859-1")));
		this.mockMvc.perform(get("/handleUtf8")).andExpect(content().bytes("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01".getBytes("UTF-8")));
	}

	@Test
	public void testContentStringMatcher() throws Exception {
		this.mockMvc.perform(get("/handle")).andExpect(content().string(containsString("world")));
	}

	@Test
	public void testCharacterEncoding() throws Exception {
		this.mockMvc.perform(get("/handle")).andExpect(content().encoding("ISO-8859-1"));
		this.mockMvc.perform(get("/handleUtf8")).andExpect(content().encoding("UTF-8"));
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
