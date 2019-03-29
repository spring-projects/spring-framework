/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.web.servlet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.hamcrest.CoreMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Integration tests that verify that {@link MockMvc} can be reused multiple
 * times within the same test method without side effects between independent
 * requests.
 * <p>See <a href="https://jira.spring.io/browse/SPR-13260" target="_blank">SPR-13260</a>.
 *
 * @author Sam Brannen
 * @author Rob Winch
 * @since 4.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class MockMvcReuseTests {

	private static final String HELLO = "hello";
	private static final String ENIGMA = "enigma";
	private static final String FOO = "foo";
	private static final String BAR = "bar";

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mvc;


	@Before
	public void setUp() {
		this.mvc = webAppContextSetup(this.wac).build();
	}

	@Test
	public void sessionAttributesAreClearedBetweenInvocations() throws Exception {

		this.mvc.perform(get("/"))
			.andExpect(content().string(HELLO))
			.andExpect(request().sessionAttribute(FOO, nullValue()));

		this.mvc.perform(get("/").sessionAttr(FOO, BAR))
			.andExpect(content().string(HELLO))
			.andExpect(request().sessionAttribute(FOO, BAR));

		this.mvc.perform(get("/"))
			.andExpect(content().string(HELLO))
			.andExpect(request().sessionAttribute(FOO, nullValue()));
	}

	@Test
	public void requestParametersAreClearedBetweenInvocations() throws Exception {
		this.mvc.perform(get("/"))
			.andExpect(content().string(HELLO));

		this.mvc.perform(get("/").param(ENIGMA, ""))
			.andExpect(content().string(ENIGMA));

		this.mvc.perform(get("/"))
			.andExpect(content().string(HELLO));
	}


	@Configuration
	@EnableWebMvc
	static class Config {

		@Bean
		public MyController myController() {
			return new MyController();
		}
	}

	@RestController
	static class MyController {

		@RequestMapping("/")
		public String hello() {
			return HELLO;
		}

		@RequestMapping(path = "/", params = ENIGMA)
		public String enigma() {
			return ENIGMA;
		}
	}

}
