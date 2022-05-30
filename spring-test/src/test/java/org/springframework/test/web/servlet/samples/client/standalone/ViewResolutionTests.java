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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.ui.Model;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.springframework.web.servlet.view.xml.MarshallingView;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.RequestParameterTests}.
 *
 * @author Rossen Stoyanchev
 */
class ViewResolutionTests {

	@Test
	void jspOnly() throws Exception {
		WebTestClient testClient =
				MockMvcWebTestClient.bindToController(new PersonController())
						.viewResolvers(new InternalResourceViewResolver("/WEB-INF/", ".jsp"))
						.build();

		EntityExchangeResult<Void> result = testClient.get().uri("/person/Corea")
				.exchange()
				.expectStatus().isOk()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(result)
				.andExpect(status().isOk())
				.andExpect(model().size(1))
				.andExpect(model().attributeExists("person"))
				.andExpect(forwardedUrl("/WEB-INF/person/show.jsp"));
	}

	@Test
	void jsonOnly() {
		WebTestClient testClient =
				MockMvcWebTestClient.bindToController(new PersonController())
						.singleView(new MappingJackson2JsonView())
						.build();

		testClient.get().uri("/person/Corea")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectBody().jsonPath("$.person.name", "Corea");
	}

	@Test
	void xmlOnly() {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(Person.class);

		WebTestClient testClient =
				MockMvcWebTestClient.bindToController(new PersonController())
						.singleView(new MarshallingView(marshaller))
						.build();

		testClient.get().uri("/person/Corea")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_XML)
				.expectBody().xpath("/person/name/text()").isEqualTo("Corea");
	}

	@Test
	void contentNegotiation() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(Person.class);

		List<View> viewList = new ArrayList<>();
		viewList.add(new MappingJackson2JsonView());
		viewList.add(new MarshallingView(marshaller));

		ContentNegotiationManager manager = new ContentNegotiationManager(
				new HeaderContentNegotiationStrategy(), new FixedContentNegotiationStrategy(MediaType.TEXT_HTML));

		ContentNegotiatingViewResolver cnViewResolver = new ContentNegotiatingViewResolver();
		cnViewResolver.setDefaultViews(viewList);
		cnViewResolver.setContentNegotiationManager(manager);
		cnViewResolver.afterPropertiesSet();

		WebTestClient testClient =
				MockMvcWebTestClient.bindToController(new PersonController())
						.viewResolvers(cnViewResolver, new InternalResourceViewResolver())
						.build();

		EntityExchangeResult<Void> result = testClient.get().uri("/person/Corea")
				.exchange()
				.expectStatus().isOk()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(result)
				.andExpect(model().size(1))
				.andExpect(model().attributeExists("person"))
				.andExpect(forwardedUrl("person/show"));

		testClient.get().uri("/person/Corea")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectBody().jsonPath("$.person.name", "Corea");

		testClient.get().uri("/person/Corea")
				.accept(MediaType.APPLICATION_XML)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_XML)
				.expectBody().xpath("/person/name/text()").isEqualTo("Corea");
	}

	@Test
	void defaultViewResolver() throws Exception {
		WebTestClient client = MockMvcWebTestClient.bindToController(new PersonController()).build();

		EntityExchangeResult<Void> result = client.get().uri("/person/Corea")
				.exchange()
				.expectStatus().isOk()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(result)
				.andExpect(model().attribute("person", hasProperty("name", equalTo("Corea"))))
				.andExpect(forwardedUrl("person/show"));  // InternalResourceViewResolver
	}


	@Controller
	private static class PersonController {

		@GetMapping("/person/{name}")
		String show(@PathVariable String name, Model model) {
			Person person = new Person(name);
			model.addAttribute(person);
			return "person/show";
		}
	}

}
