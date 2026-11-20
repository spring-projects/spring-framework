/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.standalone.resultmatchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Examples of expectations on forwarded or redirected URLs.
 *
 * @author Rossen Stoyanchev
 */
class UrlAssertionTests {

	private MockMvc mockMvc;

	@BeforeEach
	void setup() {
		this.mockMvc = standaloneSetup(new SimpleController()).build();
	}

	@Test
	void redirect() throws Exception {
		this.mockMvc.perform(get("/persons")).andExpect(redirectedUrl("/persons/1"));
	}

	@Test
	void redirectPattern() throws Exception {
		this.mockMvc.perform(get("/persons")).andExpect(redirectedUrlPattern("/persons/*"));
	}

	@Test
	void forward() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(forwardedUrl("/home"));
	}

	@Test
	void forwardPattern() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(forwardedUrlPattern("/ho?e"));
	}

	@Controller
	private static class SimpleController {

		@RequestMapping("/persons")
		public String save() {
			return "redirect:/persons/1";
		}

		@RequestMapping("/")
		public String forward() {
			return "forward:/home";
		}
	}
}
