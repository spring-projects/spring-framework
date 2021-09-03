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

import javax.validation.Valid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Examples of expectations on the content of the model prepared by the controller.
 *
 * @author Rossen Stoyanchev
 */
public class ModelAssertionTests {

	private MockMvc mockMvc;


	@BeforeEach
	void setup() {
		SampleController controller = new SampleController("a string value", 3, new Person("a name"));

		this.mockMvc = standaloneSetup(controller)
				.defaultRequest(get("/"))
				.alwaysExpect(status().isOk())
				.setControllerAdvice(new ModelAttributeAdvice())
				.build();
	}

	@Test
	void attributeEqualTo() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(model().attribute("integer", 3))
			.andExpect(model().attribute("string", "a string value"))
			.andExpect(model().attribute("integer", equalTo(3))) // Hamcrest...
			.andExpect(model().attribute("string", equalTo("a string value")))
			.andExpect(model().attribute("globalAttrName", equalTo("Global Attribute Value")));
	}

	@Test
	void attributeExists() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(model().attributeExists("integer", "string", "person"))
			.andExpect(model().attribute("integer", notNullValue()))  // Hamcrest...
			.andExpect(model().attribute("INTEGER", nullValue()));
	}

	@Test
	void attributeHamcrestMatchers() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(model().attribute("integer", equalTo(3)))
			.andExpect(model().attribute("string", allOf(startsWith("a string"), endsWith("value"))))
			.andExpect(model().attribute("person", hasProperty("name", equalTo("a name"))));
	}

	@Test
	void hasErrors() throws Exception {
		mockMvc.perform(post("/persons")).andExpect(model().attributeHasErrors("person"));
	}

	@Test
	void hasNoErrors() throws Exception {
		mockMvc.perform(get("/")).andExpect(model().hasNoErrors());
	}


	@Controller
	private static class SampleController {

		private final Object[] values;

		SampleController(Object... values) {
			this.values = values;
		}

		@RequestMapping("/")
		String handle(Model model) {
			for (Object value : this.values) {
				model.addAttribute(value);
			}
			return "view";
		}

		@PostMapping("/persons")
		String create(@Valid Person person, BindingResult result, Model model) {
			return "view";
		}
	}

	@ControllerAdvice
	private static class ModelAttributeAdvice {

		@ModelAttribute("globalAttrName")
		String getAttribute() {
			return "Global Attribute Value";
		}
	}

}
