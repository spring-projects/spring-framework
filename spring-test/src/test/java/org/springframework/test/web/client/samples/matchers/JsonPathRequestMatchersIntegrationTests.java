/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.web.client.samples.matchers;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.Person;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Examples of defining expectations on JSON request content with
 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expressions.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see org.springframework.test.web.client.match.JsonPathRequestMatchers
 * @see org.springframework.test.web.client.match.JsonPathRequestMatchersTests
 */
class JsonPathRequestMatchersIntegrationTests {

	private static final MultiValueMap<String, Person> people = new LinkedMultiValueMap<>();

	static {
		people.add("composers", new Person("Johann Sebastian Bach"));
		people.add("composers", new Person("Johannes Brahms"));
		people.add("composers", new Person("Edvard Grieg"));
		people.add("composers", new Person("Robert Schumann"));
		people.add("performers", new Person("Vladimir Ashkenazy"));
		people.add("performers", new Person("Yehudi Menuhin"));
	}


	private final RestTemplate restTemplate =
			new RestTemplate(Collections.singletonList(new JacksonJsonHttpMessageConverter()));

	private final MockRestServiceServer mockServer = MockRestServiceServer.createServer(this.restTemplate);


	@Test
	void exists() {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers[0]").exists())
			.andExpect(jsonPath("$.composers[1]").exists())
			.andExpect(jsonPath("$.composers[2]").exists())
			.andExpect(jsonPath("$.composers[3]").exists())
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	void doesNotExist() {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers[?(@.name == 'Edvard Grieeeeeeg')]").doesNotExist())
			.andExpect(jsonPath("$.composers[?(@.name == 'Robert Schuuuuuuman')]").doesNotExist())
			.andExpect(jsonPath("$.composers[4]").doesNotExist())
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	void value() {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers[0].name").value("Johann Sebastian Bach"))
			.andExpect(jsonPath("$.performers[1].name").value("Yehudi Menuhin"))
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	void hamcrestMatchers() {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers[0].name").value(equalTo("Johann Sebastian Bach")))
			.andExpect(jsonPath("$.performers[1].name").value(equalTo("Yehudi Menuhin")))
			.andExpect(jsonPath("$.composers[0].name", startsWith("Johann")))
			.andExpect(jsonPath("$.performers[0].name", endsWith("Ashkenazy")))
			.andExpect(jsonPath("$.performers[1].name", containsString("di Me")))
			.andExpect(jsonPath("$.composers[1].name", is(in(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms")))))
			.andExpect(jsonPath("$.composers[:3].name", hasItem("Johannes Brahms")))
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	void hamcrestMatchersWithParameterizedJsonPaths() {
		String composerName = "$.composers[%s].name";
		String performerName = "$.performers[%s].name";

		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath(composerName, 0).value(startsWith("Johann")))
			.andExpect(jsonPath(performerName, 0).value(endsWith("Ashkenazy")))
			.andExpect(jsonPath(performerName, 1).value(containsString("di Me")))
			.andExpect(jsonPath(composerName, 1).value(is(in(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms")))))
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	void isArray() {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers").isArray())
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	void isString() {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers[0].name").isString())
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	void isNumber() {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers[0].someDouble").isNumber())
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	void isBoolean() {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers[0].someBoolean").isBoolean())
			.andRespond(withSuccess());

		executeAndVerify();
	}

	private void executeAndVerify() {
		this.restTemplate.put(URI.create("/composers"), people);
		this.mockServer.verify();
	}

}
