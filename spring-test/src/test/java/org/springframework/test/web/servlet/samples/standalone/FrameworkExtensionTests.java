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

import java.security.Principal;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurerAdapter;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Demonstrates use of SPI extension points:
 * <ul>
 * <li> {@link org.springframework.test.web.servlet.request.RequestPostProcessor}
 * for extending request building with custom methods.
 * <li> {@link org.springframework.test.web.servlet.setup.MockMvcConfigurer
 * MockMvcConfigurer} for extending MockMvc building with some automatic setup.
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class FrameworkExtensionTests {

	private MockMvc mockMvc;


	@Before
	public void setup() {
		this.mockMvc = standaloneSetup(new SampleController()).apply(defaultSetup()).build();
	}

	@Test
	public void fooHeader() throws Exception {
		this.mockMvc.perform(get("/").with(headers().foo("a=b"))).andExpect(content().string("Foo"));
	}

	@Test
	public void barHeader() throws Exception {
		this.mockMvc.perform(get("/").with(headers().bar("a=b"))).andExpect(content().string("Bar"));
	}

	private static TestMockMvcConfigurer defaultSetup() {
		return new TestMockMvcConfigurer();
	}

	private static TestRequestPostProcessor headers() {
		return new TestRequestPostProcessor();
	}


	/**
	 * Test {@code RequestPostProcessor}.
	 */
	private static class TestRequestPostProcessor implements RequestPostProcessor {

		private HttpHeaders headers = new HttpHeaders();


		public TestRequestPostProcessor foo(String value) {
			this.headers.add("Foo", value);
			return this;
		}

		public TestRequestPostProcessor bar(String value) {
			this.headers.add("Bar", value);
			return this;
		}

		@Override
		public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
			for (String headerName : this.headers.keySet()) {
				request.addHeader(headerName, this.headers.get(headerName));
			}
			return request;
		}
	}


	/**
	 * Test {@code MockMvcConfigurer}.
	 */
	private static class TestMockMvcConfigurer extends MockMvcConfigurerAdapter {

		@Override
		public void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
			builder.alwaysExpect(status().isOk());
		}

		@Override
		public RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder,
				WebApplicationContext context) {
			return request -> {
				request.setUserPrincipal(mock(Principal.class));
				return request;
			};
		}
	}


	@Controller
	@RequestMapping("/")
	private static class SampleController {

		@RequestMapping(headers = "Foo")
		@ResponseBody
		public String handleFoo(Principal principal) {
			Assert.notNull(principal, "Principal must not be null");
			return "Foo";
		}

		@RequestMapping(headers = "Bar")
		@ResponseBody
		public String handleBar(Principal principal) {
			Assert.notNull(principal, "Principal must not be null");
			return "Bar";
		}
	}

}
