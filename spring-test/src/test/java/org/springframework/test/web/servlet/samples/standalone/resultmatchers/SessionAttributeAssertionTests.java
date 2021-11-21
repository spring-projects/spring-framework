/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Examples of expectations on created session attributes.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class SessionAttributeAssertionTests {

	private final MockMvc mockMvc = standaloneSetup(new SimpleController())
										.defaultRequest(get("/"))
										.alwaysExpect(status().isOk())
										.build();


	@Test
	void sessionAttributeEqualTo() throws Exception {
		this.mockMvc.perform(get("/"))
			.andExpect(request().sessionAttribute("locale", Locale.UK));

		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() ->
				this.mockMvc.perform(get("/"))
					.andExpect(request().sessionAttribute("locale", Locale.US)))
			.withMessage("Session attribute 'locale' expected:<en_US> but was:<en_GB>");
	}

	@Test
	void sessionAttributeMatcher() throws Exception {
		this.mockMvc.perform(get("/"))
			.andExpect(request().sessionAttribute("bogus", is(nullValue())))
			.andExpect(request().sessionAttribute("locale", is(notNullValue())))
			.andExpect(request().sessionAttribute("locale", equalTo(Locale.UK)));

		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() ->
				this.mockMvc.perform(get("/"))
					.andExpect(request().sessionAttribute("bogus", is(notNullValue()))))
			.withMessageContaining("null");
	}

	@Test
	void sessionAttributeDoesNotExist() throws Exception {
		this.mockMvc.perform(get("/"))
			.andExpect(request().sessionAttributeDoesNotExist("bogus", "enigma"));

		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() ->
				this.mockMvc.perform(get("/"))
					.andExpect(request().sessionAttributeDoesNotExist("locale")))
			.withMessage("Session attribute 'locale' exists");
	}


	@Controller
	@SessionAttributes("locale")
	private static class SimpleController {

		@ModelAttribute
		void populate(Model model) {
			model.addAttribute("locale", Locale.UK);
		}

		@RequestMapping("/")
		String handle() {
			return "view";
		}
	}

}
