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

package org.springframework.test.web.servlet.samples.client.standalone.resultmatches;

import javax.validation.Valid;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.resultmatchers.ModelAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class ModelAssertionTests {

	private final WebTestClient client =
			MockMvcWebTestClient.bindToController(new SampleController("a string value", 3, new Person("a name")))
					.controllerAdvice(new ModelAttributeAdvice())
					.alwaysExpect(status().isOk())
					.build();

	@Test
	void attributeEqualTo() throws Exception {
		performRequest(HttpMethod.GET, "/")
			.andExpect(model().attribute("integer", 3))
			.andExpect(model().attribute("string", "a string value"))
			.andExpect(model().attribute("integer", equalTo(3))) // Hamcrest...
			.andExpect(model().attribute("string", equalTo("a string value")))
			.andExpect(model().attribute("globalAttrName", equalTo("Global Attribute Value")));
	}

	@Test
	void attributeExists() throws Exception {
		performRequest(HttpMethod.GET, "/")
			.andExpect(model().attributeExists("integer", "string", "person"))
			.andExpect(model().attribute("integer", notNullValue()))  // Hamcrest...
			.andExpect(model().attribute("INTEGER", nullValue()));
	}

	@Test
	void attributeHamcrestMatchers() throws Exception {
		performRequest(HttpMethod.GET, "/")
			.andExpect(model().attribute("integer", equalTo(3)))
			.andExpect(model().attribute("string", allOf(startsWith("a string"), endsWith("value"))))
			.andExpect(model().attribute("person", hasProperty("name", equalTo("a name"))));
	}

	@Test
	void hasErrors() throws Exception {
		performRequest(HttpMethod.POST, "/persons").andExpect(model().attributeHasErrors("person"));
	}

	@Test
	void hasNoErrors() throws Exception {
		performRequest(HttpMethod.GET, "/").andExpect(model().hasNoErrors());
	}

	private ResultActions performRequest(HttpMethod method, String uri) {
		EntityExchangeResult<Void> result = client.method(method).uri(uri).exchange().expectBody().isEmpty();
		return MockMvcWebTestClient.resultActionsFor(result);
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
