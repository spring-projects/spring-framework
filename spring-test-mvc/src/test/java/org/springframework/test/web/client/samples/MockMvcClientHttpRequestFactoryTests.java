/*
 * Copyright 2011 the original author or authors.
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

package org.springframework.test.web.client.samples;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Tests dependent on access to resources under the web application root directory.
 *
 * @author Rossen Stoyanchev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration
public class MockMvcClientHttpRequestFactoryTests {

	@Autowired
	private WebApplicationContext wac;

	private RestTemplate restTemplate;


	@Before
	public void setup() {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).alwaysExpect(status().isOk()).build();
		this.restTemplate = new RestTemplate(new MockMvcClientHttpRequestFactory(mockMvc));
	}

	@Test
	public void test() throws Exception {
		String result = this.restTemplate.getForObject("/foo", String.class);
		assertEquals("bar", result);
	}


	@EnableWebMvc
	@Configuration
	@ComponentScan(basePackageClasses=MockMvcClientHttpRequestFactoryTests.class)
	static class MyWebConfig extends WebMvcConfigurerAdapter {
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
