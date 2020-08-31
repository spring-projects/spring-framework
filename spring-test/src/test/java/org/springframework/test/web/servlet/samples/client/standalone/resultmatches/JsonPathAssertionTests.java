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

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.Person;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.resultmatchers.JsonPathAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class JsonPathAssertionTests {

	private final WebTestClient client =
			MockMvcWebTestClient.bindToController(new MusicController())
					.alwaysExpect(status().isOk())
					.alwaysExpect(content().contentType(MediaType.APPLICATION_JSON))
					.configureClient()
					.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
					.build();


	@Test
	public void exists() {
		String composerByName = "$.composers[?(@.name == '%s')]";
		String performerByName = "$.performers[?(@.name == '%s')]";

		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath(composerByName, "Johann Sebastian Bach").exists()
				.jsonPath(composerByName, "Johannes Brahms").exists()
				.jsonPath(composerByName, "Edvard Grieg").exists()
				.jsonPath(composerByName, "Robert Schumann").exists()
				.jsonPath(performerByName, "Vladimir Ashkenazy").exists()
				.jsonPath(performerByName, "Yehudi Menuhin").exists()
				.jsonPath("$.composers[0]").exists()
				.jsonPath("$.composers[1]").exists()
				.jsonPath("$.composers[2]").exists()
				.jsonPath("$.composers[3]").exists();
	}

	@Test
	public void doesNotExist() {
		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath("$.composers[?(@.name == 'Edvard Grieeeeeeg')]").doesNotExist()
				.jsonPath("$.composers[?(@.name == 'Robert Schuuuuuuman')]").doesNotExist()
				.jsonPath("$.composers[4]").doesNotExist();
	}

	@Test
	public void equality() {
		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath("$.composers[0].name").isEqualTo("Johann Sebastian Bach")
				.jsonPath("$.performers[1].name").isEqualTo("Yehudi Menuhin");

		// Hamcrest matchers...
		client.get().uri("/music/people")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.composers[0].name").value(equalTo("Johann Sebastian Bach"))
				.jsonPath("$.performers[1].name").value(equalTo("Yehudi Menuhin"));
	}

	@Test
	public void hamcrestMatcher() {
		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath("$.composers[0].name").value(startsWith("Johann"))
				.jsonPath("$.performers[0].name").value(endsWith("Ashkenazy"))
				.jsonPath("$.performers[1].name").value(containsString("di Me"))
				.jsonPath("$.composers[1].name").value(is(in(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms"))));
	}

	@Test
	public void hamcrestMatcherWithParameterizedJsonPath() {
		String composerName = "$.composers[%s].name";
		String performerName = "$.performers[%s].name";

		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath(composerName, 0).value(startsWith("Johann"))
				.jsonPath(performerName, 0).value(endsWith("Ashkenazy"))
				.jsonPath(performerName, 1).value(containsString("di Me"))
				.jsonPath(composerName, 1).value(is(in(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms"))));
	}


	@RestController
	private class MusicController {

		@RequestMapping("/music/people")
		public MultiValueMap<String, Person> get() {
			MultiValueMap<String, Person> map = new LinkedMultiValueMap<>();

			map.add("composers", new Person("Johann Sebastian Bach"));
			map.add("composers", new Person("Johannes Brahms"));
			map.add("composers", new Person("Edvard Grieg"));
			map.add("composers", new Person("Robert Schumann"));

			map.add("performers", new Person("Vladimir Ashkenazy"));
			map.add("performers", new Person("Yehudi Menuhin"));

			return map;
		}
	}

}
