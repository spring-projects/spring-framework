/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

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
@SpringJUnitWebConfig
@TestInstance(Lifecycle.PER_CLASS)
class MockMvcReuseTests {

	private static final String HELLO = "hello";
	private static final String ENIGMA = "enigma";
	private static final String FOO = "foo";
	private static final String BAR = "bar";

	private final MockMvc mvc;


	MockMvcReuseTests(WebApplicationContext wac) {
		this.mvc = webAppContextSetup(wac).build();
	}


	@Test
	void sessionAttributesAreClearedBetweenInvocations() throws Exception {

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
	void requestParametersAreClearedBetweenInvocations() throws Exception {
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
		MyController myController() {
			return new MyController();
		}
	}

	@RestController
	static class MyController {

		@GetMapping("/")
		String hello() {
			return HELLO;
		}

		@GetMapping(path = "/", params = ENIGMA)
		String enigma() {
			return ENIGMA;
		}
	}

}
