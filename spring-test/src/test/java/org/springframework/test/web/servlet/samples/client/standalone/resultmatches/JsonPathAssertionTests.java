/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.client.standalone.resultmatches;

import java.util.List;

import org.hamcrest.MatcherAssert;
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
class JsonPathAssertionTests {

	private final WebTestClient client =
			MockMvcWebTestClient.bindToController(new MusicController())
					.alwaysExpect(status().isOk())
					.alwaysExpect(content().contentType(MediaType.APPLICATION_JSON))
					.configureClient()
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
				.jsonPath("$.composers[0].name").value(v -> MatcherAssert.assertThat(v, equalTo("Johann Sebastian Bach")))
				.jsonPath("$.performers[1].name").value(v -> MatcherAssert.assertThat(v, equalTo("Yehudi Menuhin")));
	}

	@Test
	void hamcrestMatcher() {
		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath("$.composers[0].name").value(String.class, v -> MatcherAssert.assertThat(v, startsWith("Johann")))
				.jsonPath("$.performers[0].name").value(String.class, v -> MatcherAssert.assertThat(v, endsWith("Ashkenazy")))
				.jsonPath("$.performers[1].name").value(String.class, v -> MatcherAssert.assertThat(v, containsString("di Me")))
				.jsonPath("$.composers[1].name").value(v -> MatcherAssert.assertThat(v, is(in(List.of("Johann Sebastian Bach", "Johannes Brahms")))));
	}

	@Test
	void hamcrestMatcherWithParameterizedJsonPath() {
		client.get().uri("/music/people")
				.exchange()
				.expectBody()
				.jsonPath("$.composers[0].name").value(String.class, v -> MatcherAssert.assertThat(v, startsWith("Johann")))
				.jsonPath("$.performers[0].name").value(String.class, v -> MatcherAssert.assertThat(v, endsWith("Ashkenazy")))
				.jsonPath("$.performers[1].name").value(String.class, v -> MatcherAssert.assertThat(v, containsString("di Me")))
				.jsonPath("$.composers[1].name").value(v -> MatcherAssert.assertThat(v, is(in(List.of("Johann Sebastian Bach", "Johannes Brahms")))));
	}


	@RestController
	private static class MusicController {

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
