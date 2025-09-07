/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.servlet.client;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.Person;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests JSON Path assertions with {@link RestTestClient}.
 *
 * @author Rob Worsnop
 */
class JsonPathAssertionTests {

	private final RestTestClient client =
			RestTestClient.bindToController(new MusicController())
					.configureServer(builder ->
							builder.alwaysExpect(status().isOk())
									.alwaysExpect(content().contentType(MediaType.APPLICATION_JSON))
					)
					.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
					.build();


	@Test
	void exists() {
		String composerByName = "$.composers[?(@.name == '%s')]";
		String performerByName = "$.performers[?(@.name == '%s')]";

		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath(composerByName.formatted("Johann Sebastian Bach")).exists()
				.jsonPath(composerByName.formatted("Johannes Brahms")).exists()
				.jsonPath(composerByName.formatted("Edvard Grieg")).exists()
				.jsonPath(composerByName.formatted("Robert Schumann")).exists()
				.jsonPath(performerByName.formatted("Vladimir Ashkenazy")).exists()
				.jsonPath(performerByName.formatted("Yehudi Menuhin")).exists()
				.jsonPath("$.composers[0]").exists()
				.jsonPath("$.composers[1]").exists()
				.jsonPath("$.composers[2]").exists()
				.jsonPath("$.composers[3]").exists();
	}

	@Test
	void doesNotExist() {
		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath("$.composers[?(@.name == 'Edvard Grieeeeeeg')]").doesNotExist()
				.jsonPath("$.composers[?(@.name == 'Robert Schuuuuuuman')]").doesNotExist()
				.jsonPath("$.composers[4]").doesNotExist();
	}

	@Test
	void equality() {
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
	void hamcrestMatcher() {
		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath("$.composers[0].name").value(startsWith("Johann"))
				.jsonPath("$.performers[0].name").value(endsWith("Ashkenazy"))
				.jsonPath("$.performers[1].name").value(containsString("di Me"))
				.jsonPath("$.composers[1].name").value(is(in(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms"))));
	}

	@Test
	void hamcrestMatcherWithParameterizedJsonPath() {
		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath("$.composers[0].name").value(String.class, startsWith("Johann"))
				.jsonPath("$.composers[0].name").value(String.class, s -> assertThat(s).startsWith("Johann"))
				.jsonPath("$.composers[0].name").value(o -> assertThat((String) o).startsWith("Johann"))
				.jsonPath("$.performers[1].name").value(containsString("di Me"))
				.jsonPath("$.composers[1].name").value(is(in(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms"))));
	}

	@Test
	void isEmpty() {
		client.get().uri("/music/instruments")
				.exchange()
				.expectBody()
				.jsonPath("$.clarinets").isEmpty();
	}

	@Test
	void isNotEmpty() {
		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath("$.composers").isNotEmpty();
	}

	@Test
	void hasJsonPath() {
		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath("$.composers").hasJsonPath();
	}

	@Test
	void doesNotHaveJsonPath() {
		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath("$.audience").doesNotHaveJsonPath();
	}

	@Test
	void isBoolean() {
		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath("$.composers[0].someBoolean").isBoolean();
	}

	@Test
	void isNumber() {
		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath("$.composers[0].someDouble").isNumber();
	}

	@Test
	void isMap() {
		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath("$").isMap();
	}

	@Test
	void isArray() {
		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath("$.composers").isArray();
	}

	@RestController
	private static class MusicController {
		@GetMapping("/music/instruments")
		public Map<String, Object> getInstruments() {
			return Map.of("clarinets", List.of());
		}

		@GetMapping("/music/people")
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
