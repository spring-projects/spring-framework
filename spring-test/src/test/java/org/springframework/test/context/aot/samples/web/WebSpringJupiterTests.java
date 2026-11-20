/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@SpringJUnitWebConfig(classes = WebTestConfiguration.class, resourcePath = "classpath:META-INF/web-resources")
@TestPropertySource(properties = "test.engine = jupiter")
public class WebSpringJupiterTests {

	MockMvc mockMvc;

	@Autowired
	WebApplicationContext wac;


	@org.junit.jupiter.api.BeforeEach
	void setUpMockMvc() {
		this.mockMvc = webAppContextSetup(this.wac).build();
	}

	@org.junit.jupiter.api.Test
	void controller(@Value("${test.engine}") String testEngine) throws Exception {
		assertThat(testEngine)
			.as("@Value").isEqualTo("jupiter");
		assertThat(wac.getEnvironment().getProperty("test.engine"))
			.as("Environment").isEqualTo("jupiter");

		mockMvc.perform(get("/hello"))
			.andExpectAll(status().isOk(), content().string("Hello, AOT!"));
	}

	@org.junit.jupiter.api.Test
	void resources() throws Exception {
		this.mockMvc.perform(get("/resources/Spring.js"))
			.andExpectAll(
				content().contentType("text/javascript"),
				content().string(containsString("Spring={};"))
			);
	}

}
