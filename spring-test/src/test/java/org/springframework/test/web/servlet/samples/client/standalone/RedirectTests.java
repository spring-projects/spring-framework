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

package org.springframework.test.web.servlet.samples.client.standalone;

import javax.validation.Valid;

import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.RedirectTests}.
 *
 * @author Rossen Stoyanchev
 */
public class RedirectTests {

	private final WebTestClient testClient =
			MockMvcWebTestClient.bindToController(new PersonController()).build();


	@Test
	public void save() throws Exception {
		EntityExchangeResult<Void> exchangeResult =
				testClient.post().uri("/persons?name=Andy")
						.exchange()
						.expectStatus().isFound()
						.expectHeader().location("/persons/Joe")
						.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().size(1))
				.andExpect(model().attributeExists("name"))
				.andExpect(flash().attributeCount(1))
				.andExpect(flash().attribute("message", "success!"));
	}

	@Test
	public void saveSpecial() throws Exception {
		EntityExchangeResult<Void> result =
				testClient.post().uri("/people?name=Andy")
						.exchange()
						.expectStatus().isFound()
						.expectHeader().location("/persons/Joe")
						.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(result)
				.andExpect(model().size(1))
				.andExpect(model().attributeExists("name"))
				.andExpect(flash().attributeCount(1))
				.andExpect(flash().attribute("message", "success!"));
	}

	@Test
	public void saveWithErrors() throws Exception {
		EntityExchangeResult<Void> result =
				testClient.post().uri("/persons").exchange().expectStatus().isOk().expectBody().isEmpty();

		MockMvcWebTestClient.resultActionsFor(result)
				.andExpect(forwardedUrl("persons/add"))
				.andExpect(model().size(1))
				.andExpect(model().attributeExists("person"))
				.andExpect(flash().attributeCount(0));
	}

	@Test
	public void saveSpecialWithErrors() throws Exception {
		EntityExchangeResult<Void> result =
				testClient.post().uri("/people").exchange().expectStatus().isOk().expectBody().isEmpty();

		MockMvcWebTestClient.resultActionsFor(result)
				.andExpect(forwardedUrl("persons/add"))
				.andExpect(model().size(1))
				.andExpect(model().attributeExists("person"))
				.andExpect(flash().attributeCount(0));
	}

	@Test
	public void getPerson() throws Exception {
		EntityExchangeResult<Void> result =
				MockMvcWebTestClient.bindToController(new PersonController())
						.defaultRequest(get("/").flashAttr("message", "success!"))
						.build()
						.get().uri("/persons/Joe")
						.exchange()
						.expectStatus().isOk()
						.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(result)
				.andDo(MockMvcResultHandlers.print())
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
