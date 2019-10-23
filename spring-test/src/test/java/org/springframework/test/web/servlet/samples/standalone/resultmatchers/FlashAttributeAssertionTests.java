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

package org.springframework.test.web.servlet.samples.standalone.resultmatchers;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Examples of expectations on flash attributes.
 *
 * @author Rossen Stoyanchev
 */
public class FlashAttributeAssertionTests {

	private MockMvc mockMvc;


	@BeforeEach
	public void setup() {
		this.mockMvc = standaloneSetup(new PersonController())
				.alwaysExpect(status().isFound())
				.alwaysExpect(flash().attributeCount(3))
				.build();
	}

	@Test
	public void testExists() throws Exception {
		this.mockMvc.perform(post("/persons"))
			.andExpect(flash().attributeExists("one", "two", "three"));
	}

	@Test
	public void testEqualTo() throws Exception {
		this.mockMvc.perform(post("/persons"))
			.andExpect(flash().attribute("one", "1"))
			.andExpect(flash().attribute("two", 2.222))
			.andExpect(flash().attribute("three", new URL("https://example.com")))
			.andExpect(flash().attribute("one", equalTo("1")))	// Hamcrest...
			.andExpect(flash().attribute("two", equalTo(2.222)))
			.andExpect(flash().attribute("three", equalTo(new URL("https://example.com"))));
	}

	@Test
	public void testMatchers() throws Exception {
		this.mockMvc.perform(post("/persons"))
			.andExpect(flash().attribute("one", containsString("1")))
			.andExpect(flash().attribute("two", closeTo(2, 0.5)))
			.andExpect(flash().attribute("three", notNullValue()));
	}


	@Controller
	private static class PersonController {

		@RequestMapping(value="/persons", method=RequestMethod.POST)
		public String save(RedirectAttributes redirectAttrs) throws Exception {
			redirectAttrs.addFlashAttribute("one", "1");
			redirectAttrs.addFlashAttribute("two", 2.222);
			redirectAttrs.addFlashAttribute("three", new URL("https://example.com"));
			return "redirect:/person/1";
		}
	}
}
