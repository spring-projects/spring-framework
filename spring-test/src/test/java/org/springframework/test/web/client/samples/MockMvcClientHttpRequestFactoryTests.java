/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.web.client.samples;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests that use a {@link RestTemplate} configured with a
 * {@link MockMvcClientHttpRequestFactory} that is in turn configured with a
 * {@link MockMvc} instance that uses a {@link WebApplicationContext} loaded by
 * the TestContext framework.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration
public class MockMvcClientHttpRequestFactoryTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;


	@BeforeEach
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).alwaysExpect(status().isOk()).build();
	}

	@Test
	public void test() throws Exception {
		RestTemplate template = new RestTemplate(new MockMvcClientHttpRequestFactory(this.mockMvc));
		String result = template.getForObject("/foo", String.class);
		assertThat(result).isEqualTo("bar");
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testAsyncTemplate() throws Exception {
		org.springframework.web.client.AsyncRestTemplate template = new org.springframework.web.client.AsyncRestTemplate(
				new MockMvcClientHttpRequestFactory(this.mockMvc));
		ListenableFuture<ResponseEntity<String>> entity = template.getForEntity("/foo", String.class);
		assertThat(entity.get().getBody()).isEqualTo("bar");
	}


	@EnableWebMvc
	@Configuration
	@ComponentScan(basePackageClasses=MockMvcClientHttpRequestFactoryTests.class)
	static class MyWebConfig implements WebMvcConfigurer {
	}

	@Controller
	static class MyController {

		@RequestMapping(value="/foo", method=RequestMethod.GET)
		@ResponseBody
		public String handle() {
			return "bar";
		}
	}

}
