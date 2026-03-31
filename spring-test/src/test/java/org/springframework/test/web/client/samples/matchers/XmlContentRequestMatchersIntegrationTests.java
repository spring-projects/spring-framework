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

package org.springframework.test.web.client.samples.matchers;

import java.util.Arrays;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.test.web.Person;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.hasXPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Examples of defining expectations on XML request content with XMLUnit.
 *
 * @author Rossen Stoyanchev
 * @see ContentRequestMatchersIntegrationTests
 * @see XpathRequestMatchersIntegrationTests
 */
class XmlContentRequestMatchersIntegrationTests {

	private static final String PEOPLE_XML =
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
			"<people><composers>" +
			"<composer><name>Johann Sebastian Bach</name><someBoolean>false</someBoolean><someDouble>21.0</someDouble></composer>" +
			"<composer><name>Johannes Brahms</name><someBoolean>false</someBoolean><someDouble>0.0025</someDouble></composer>" +
			"<composer><name>Edvard Grieg</name><someBoolean>false</someBoolean><someDouble>1.6035</someDouble></composer>" +
			"<composer><name>Robert Schumann</name><someBoolean>false</someBoolean><someDouble>NaN</someDouble></composer>" +
			"</composers></people>";


	private MockRestServiceServer mockServer;

	private RestClient restClient;

	private PeopleWrapper people;


	@BeforeEach
	void setup() {
		List<Person> composers = Arrays.asList(
				new Person("Johann Sebastian Bach").setSomeDouble(21),
				new Person("Johannes Brahms").setSomeDouble(.0025),
				new Person("Edvard Grieg").setSomeDouble(1.6035),
				new Person("Robert Schumann").setSomeDouble(Double.NaN));

		this.people = new PeopleWrapper(composers);

		RestClient.Builder clientBuilder = RestClient.builder().configureMessageConverters(converters ->
				converters.registerDefaults().withXmlConverter(new Jaxb2RootElementHttpMessageConverter()));

		this.mockServer = MockRestServiceServer.createServer(clientBuilder);
		this.restClient = clientBuilder.build();
	}

	@Test
	void xmlEqualTo() {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/xml"))
			.andExpect(content().xml(PEOPLE_XML))
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	void hamcrestNodeMatcher() {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/xml"))
			.andExpect(content().node(hasXPath("/people/composers/composer[1]")))
			.andRespond(withSuccess());

		executeAndVerify();
	}

	private void executeAndVerify() {
		this.restClient.put().uri("/composers").contentType(MediaType.APPLICATION_XML)
				.body(this.people).retrieve().toBodilessEntity();
		this.mockServer.verify();
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
