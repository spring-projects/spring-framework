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

package org.springframework.test.context.junit.jupiter.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.context.junit.SpringJUnitJupiterTestSuite;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Integration tests which demonstrate how to set up a {@link MockMvc}
 * instance in an {@link BeforeEach @BeforeEach} method with the
 * {@link SpringExtension} (registered via a custom
 * {@link SpringJUnitWebConfig @SpringJUnitWebConfig} composed annotation).
 *
 * <p>To run these tests in an IDE that does not have built-in support for the JUnit
 * Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see SpringExtension
 * @see SpringJUnitWebConfig
 * @see org.springframework.test.context.junit.jupiter.web.WebSpringExtensionTests
 */
@SpringJUnitWebConfig(WebConfig.class)
class MultipleWebRequestsSpringExtensionTests {

	MockMvc mockMvc;

	@BeforeEach
	void setUpMockMvc(WebApplicationContext wac) {
		this.mockMvc = webAppContextSetup(wac)
			.alwaysExpect(status().isOk())
			.alwaysExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
			.build();
	}

	@Test
	void getPerson42() throws Exception {
		this.mockMvc.perform(get("/person/42").accept(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.name", is("Dilbert")));
	}

	@Test
	void getPerson99() throws Exception {
		this.mockMvc.perform(get("/person/99").accept(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.name", is("Wally")));
	}

}
