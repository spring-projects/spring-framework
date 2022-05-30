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

package org.springframework.test.web.servlet.samples.standalone;

import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Redirect scenarios including saving and retrieving flash attributes.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class RedirectTests {

	private MockMvc mockMvc;


	@BeforeEach
	public void setup() {
		this.mockMvc = standaloneSetup(new PersonController()).build();
	}


	@Test
	public void save() throws Exception {
		this.mockMvc.perform(post("/persons").param("name", "Andy"))
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/persons/Joe"))
			.andExpect(model().size(1))
			.andExpect(model().attributeExists("name"))
			.andExpect(flash().attributeCount(1))
			.andExpect(flash().attribute("message", "success!"));
	}

	@Test
	public void saveSpecial() throws Exception {
		this.mockMvc.perform(post("/people").param("name", "Andy"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/persons/Joe"))
				.andExpect(model().size(1))
				.andExpect(model().attributeExists("name"))
				.andExpect(flash().attributeCount(1))
				.andExpect(flash().attribute("message", "success!"));
	}

	@Test
	public void saveWithErrors() throws Exception {
		this.mockMvc.perform(post("/persons"))
			.andExpect(status().isOk())
			.andExpect(forwardedUrl("persons/add"))
			.andExpect(model().size(1))
			.andExpect(model().attributeExists("person"))
			.andExpect(flash().attributeCount(0));
	}

	@Test
	public void saveSpecialWithErrors() throws Exception {
		this.mockMvc.perform(post("/people"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("persons/add"))
				.andExpect(model().size(1))
				.andExpect(model().attributeExists("person"))
				.andExpect(flash().attributeCount(0));
	}

	@Test
	public void getPerson() throws Exception {
		this.mockMvc.perform(get("/persons/Joe").flashAttr("message", "success!"))
			.andExpect(status().isOk())
			.andExpect(forwardedUrl("persons/index"))
			.andExpect(model().size(2))
			.andExpect(model().attribute("person", new Person("Joe")))
			.andExpect(model().attribute("message", "success!"))
			.andExpect(flash().attributeCount(0));
	}


	@Controller
	private static class PersonController {

		@GetMapping("/persons/{name}")
		public String getPerson(@PathVariable String name, Model model) {
			model.addAttribute(new Person(name));
			return "persons/index";
		}

		@PostMapping("/persons")
		public String save(@Valid Person person, Errors errors, RedirectAttributes redirectAttrs) {
			if (errors.hasErrors()) {
				return "persons/add";
			}
			redirectAttrs.addAttribute("name", "Joe");
			redirectAttrs.addFlashAttribute("message", "success!");
			return "redirect:/persons/{name}";
		}

		@PostMapping("/people")
		public Object saveSpecial(@Valid Person person, Errors errors, RedirectAttributes redirectAttrs) {
			if (errors.hasErrors()) {
				return "persons/add";
			}
			redirectAttrs.addAttribute("name", "Joe");
			redirectAttrs.addFlashAttribute("message", "success!");
			return new StringBuilder("redirect:").append("/persons").append("/{name}");
		}
	}

}
