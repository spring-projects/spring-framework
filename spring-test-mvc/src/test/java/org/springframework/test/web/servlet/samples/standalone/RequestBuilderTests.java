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
package org.springframework.test.web.servlet.samples.standalone;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Demonstrates how to implement and plug in a custom {@link RequestPostProcessor}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestBuilderTests {

	private MockMvc mockMvc;

	@Before
	public void setup() {
		this.mockMvc = standaloneSetup(new SampleController())
				.defaultRequest(get("/").accept(MediaType.TEXT_PLAIN))
				.alwaysExpect(status().isOk()).build();
	}

	@Test
	public void fooHeader() throws Exception {
		this.mockMvc.perform(get("/").with(headers().foo("a=b"))).andExpect(
				content().string("Foo"));
	}

	@Test
	public void barHeader() throws Exception {
		this.mockMvc.perform(get("/").with(headers().bar("a=b"))).andExpect(
				content().string("Bar"));
	}

	private static HeaderRequestPostProcessor headers() {
		return new HeaderRequestPostProcessor();
	}


	/**
	 * Implementation of {@code RequestPostProcessor} with additional request
	 * building methods.
	 */
	private static class HeaderRequestPostProcessor implements RequestPostProcessor {

		private HttpHeaders headers = new HttpHeaders();

		public HeaderRequestPostProcessor foo(String value) {
			this.headers.add("Foo", value);
			return this;
		}

		public HeaderRequestPostProcessor bar(String value) {
			this.headers.add("Bar", value);
			return this;
		}

		public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
			for (String headerName : this.headers.keySet()) {
				request.addHeader(headerName, this.headers.get(headerName));
			}
			return request;
		}
	}

	@Controller
	@RequestMapping("/")
	private static class SampleController {

		@RequestMapping(headers = "Foo")
		@ResponseBody
		public String handleFoo() {
			return "Foo";
		}

		@RequestMapping(headers = "Bar")
		@ResponseBody
		public String handleBar() {
			return "Bar";
		}
	}

}