/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.aot.samples.web;

import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = WebTestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = "test.engine = vintage")
@SuppressWarnings("deprecation")
public class WebSpringVintageTests {

	MockMvc mockMvc;

	@Autowired
	WebApplicationContext wac;

	@Value("${test.engine}")
	String testEngine;


	@org.junit.Before
	public void setUpMockMvc() {
		this.mockMvc = webAppContextSetup(this.wac).build();
	}

	@org.junit.Test
	public void test() throws Exception {
		assertThat(testEngine)
			.as("@Value").isEqualTo("vintage");
		assertThat(wac.getEnvironment().getProperty("test.engine"))
			.as("Environment").isEqualTo("vintage");

		mockMvc.perform(get("/hello"))
			.andExpectAll(status().isOk(), content().string("Hello, AOT!"));
	}

}
