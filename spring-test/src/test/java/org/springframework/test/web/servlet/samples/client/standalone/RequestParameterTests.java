/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.client.standalone;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.RequestParameterTests}.
 *
 * @author Rossen Stoyanchev
 */
class RequestParameterTests {

	@Test
	void queryParameter() {

		WebTestClient client = MockMvcWebTestClient.bindToController(new PersonController()).build();

		client.get().uri("/search?name=George")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody().jsonPath("$.name").isEqualTo("George");
	}


	@Controller
	private static class PersonController {

		@RequestMapping(value="/search")
		@ResponseBody
		public Person get(@RequestParam String name) {
			return new Person(name);
		}
	}

}
