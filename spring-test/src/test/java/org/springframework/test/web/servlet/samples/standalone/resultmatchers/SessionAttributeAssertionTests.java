/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class SessionAttributeAssertionTests {

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
