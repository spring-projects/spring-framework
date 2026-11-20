/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
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
@ContextConfiguration(classes = WebTestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = "test.engine = testng")
public class WebSpringTestNGTests extends AbstractTestNGSpringContextTests {

	MockMvc mockMvc;

	@Autowired
	WebApplicationContext wac;

	@Value("${test.engine}")
	String testEngine;


	@org.testng.annotations.BeforeMethod
	public void setUpMockMvc() {
		this.mockMvc = webAppContextSetup(this.wac).build();
	}

	@org.testng.annotations.Test
	public void test() throws Exception {
		assertThat(testEngine)
			.as("@Value").isEqualTo("testng");
		assertThat(wac.getEnvironment().getProperty("test.engine"))
			.as("Environment").isEqualTo("testng");

		mockMvc.perform(get("/hello"))
			.andExpectAll(status().isOk(), content().string("Hello, AOT!"));
	}

}
