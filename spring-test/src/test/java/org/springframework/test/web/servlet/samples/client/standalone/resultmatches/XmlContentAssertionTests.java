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
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.resultmatchers.XmlContentAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class XmlContentAssertionTests {

	private static final String PEOPLE_XML =
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
					"<people><composers>" +
					"<composer><name>Johann Sebastian Bach</name><someBoolean>false</someBoolean><someDouble>21.0</someDouble></composer>" +
					"<composer><name>Johannes Brahms</name><someBoolean>false</someBoolean><someDouble>0.0025</someDouble></composer>" +
					"<composer><name>Edvard Grieg</name><someBoolean>false</someBoolean><someDouble>1.6035</someDouble></composer>" +
					"<composer><name>Robert Schumann</name><someBoolean>false</someBoolean><someDouble>NaN</someDouble></composer>" +
					"</composers></people>";


	private final WebTestClient testClient =
			MockMvcWebTestClient.bindToController(new MusicController())
					.alwaysExpect(status().isOk())
					.alwaysExpect(content().contentType(MediaType.parseMediaType("application/xml;charset=UTF-8")))
					.configureClient()
					.defaultHeader(HttpHeaders.ACCEPT, "application/xml;charset=UTF-8")
					.build();


	@Test
	public void testXmlEqualTo() {
		testClient.get().uri("/music/people")
				.exchange()
				.expectBody().xml(PEOPLE_XML);
	}

	@Test
	public void testNodeHamcrestMatcher() {
		testClient.get().uri("/music/people")
				.exchange()
				.expectBody().xpath("/people/composers/composer[1]").exists();
	}


	@Controller
	private static class MusicController {

		@RequestMapping(value="/music/people")
		public @ResponseBody PeopleWrapper getPeople() {

			List<Person> composers = Arrays.asList(
					new Person("Johann Sebastian Bach").setSomeDouble(21),
					new Person("Johannes Brahms").setSomeDouble(.0025),
					new Person("Edvard Grieg").setSomeDouble(1.6035),
					new Person("Robert Schumann").setSomeDouble(Double.NaN));

			return new PeopleWrapper(composers);
		}
	}

	@SuppressWarnings("unused")
	@XmlRootElement(name="people")
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class PeopleWrapper {

		@XmlElementWrapper(name="composers")
		@XmlElement(name="composer")
		private List<Person> composers;

		public PeopleWrapper() {
		}

		public PeopleWrapper(List<Person> composers) {
			this.composers = composers;
		}

		public List<Person> getComposers() {
			return this.composers;
		}
	}
}
