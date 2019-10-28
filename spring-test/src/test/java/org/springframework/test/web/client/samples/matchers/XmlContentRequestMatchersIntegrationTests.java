/*
 * Copyright 2002-2019 the original author or authors.
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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.test.web.Person;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

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
public class XmlContentRequestMatchersIntegrationTests {

	private static final String PEOPLE_XML =
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
			"<people><composers>" +
			"<composer><name>Johann Sebastian Bach</name><someBoolean>false</someBoolean><someDouble>21.0</someDouble></composer>" +
			"<composer><name>Johannes Brahms</name><someBoolean>false</someBoolean><someDouble>0.0025</someDouble></composer>" +
			"<composer><name>Edvard Grieg</name><someBoolean>false</someBoolean><someDouble>1.6035</someDouble></composer>" +
			"<composer><name>Robert Schumann</name><someBoolean>false</someBoolean><someDouble>NaN</someDouble></composer>" +
			"</composers></people>";


	private MockRestServiceServer mockServer;

	private RestTemplate restTemplate;

	private PeopleWrapper people;


	@BeforeEach
	public void setup() {
		List<Person> composers = Arrays.asList(
				new Person("Johann Sebastian Bach").setSomeDouble(21),
				new Person("Johannes Brahms").setSomeDouble(.0025),
				new Person("Edvard Grieg").setSomeDouble(1.6035),
				new Person("Robert Schumann").setSomeDouble(Double.NaN));

		this.people = new PeopleWrapper(composers);

		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new Jaxb2RootElementHttpMessageConverter());

		this.restTemplate = new RestTemplate();
		this.restTemplate.setMessageConverters(converters);

		this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
	}

	@Test
	public void testXmlEqualTo() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/xml"))
			.andExpect(content().xml(PEOPLE_XML))
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	public void testHamcrestNodeMatcher() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/xml"))
			.andExpect(content().node(hasXPath("/people/composers/composer[1]")))
			.andRespond(withSuccess());

		executeAndVerify();
	}

	private void executeAndVerify() throws URISyntaxException {
		this.restTemplate.put(new URI("/composers"), this.people);
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
