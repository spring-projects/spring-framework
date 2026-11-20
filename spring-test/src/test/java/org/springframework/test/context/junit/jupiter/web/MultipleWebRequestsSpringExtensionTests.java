/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
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
