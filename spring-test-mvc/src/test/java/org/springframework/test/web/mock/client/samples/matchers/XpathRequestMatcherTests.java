/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.test.web.mock.client.samples.matchers;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.mock.client.match.RequestMatchers.content;
import static org.springframework.test.web.mock.client.match.RequestMatchers.requestTo;
import static org.springframework.test.web.mock.client.match.RequestMatchers.xpath;
import static org.springframework.test.web.mock.client.response.ResponseCreators.withSuccess;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.test.web.mock.Person;
import org.springframework.test.web.mock.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 * Examples of defining expectations on XML request content with XPath expressions.
 *
 * @author Rossen Stoyanchev
 *
 * @see ContentRequestMatcherTests
 * @see XmlContentRequestMatcherTests
 */
public class XpathRequestMatcherTests {

	private static final Map<String, String> NS =
			Collections.singletonMap("ns", "http://example.org/music/people");

	private MockRestServiceServer mockServer;

	private RestTemplate restTemplate;

	private PeopleWrapper people;

	@Before
	public void setup() {

		List<Person> composers = Arrays.asList(
				new Person("Johann Sebastian Bach").setSomeDouble(21),
				new Person("Johannes Brahms").setSomeDouble(.0025),
				new Person("Edvard Grieg").setSomeDouble(1.6035),
				new Person("Robert Schumann").setSomeDouble(Double.NaN));

		List<Person> performers = Arrays.asList(
				new Person("Vladimir Ashkenazy").setSomeBoolean(false),
				new Person("Yehudi Menuhin").setSomeBoolean(true));

		this.people = new PeopleWrapper(composers, performers);

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new Jaxb2RootElementHttpMessageConverter());

		this.restTemplate = new RestTemplate();
		this.restTemplate.setMessageConverters(converters);

		this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
	}

	@Test
	public void testExists() throws Exception {

		String composer = "/ns:people/composers/composer[%s]";
		String performer = "/ns:people/performers/performer[%s]";

		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().mimeType("application/xml"))
			.andExpect(xpath(composer, NS, 1).exists())
			.andExpect(xpath(composer, NS, 2).exists())
			.andExpect(xpath(composer, NS, 3).exists())
			.andExpect(xpath(composer, NS, 4).exists())
			.andExpect(xpath(performer, NS, 1).exists())
			.andExpect(xpath(performer, NS, 2).exists())
			.andRespond(withSuccess());

		this.restTemplate.put(new URI("/composers"), this.people);
		this.mockServer.verify();
	}

	@Test
	public void testDoesNotExist() throws Exception {

		String composer = "/ns:people/composers/composer[%s]";
		String performer = "/ns:people/performers/performer[%s]";

		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().mimeType("application/xml"))
			.andExpect(xpath(composer, NS, 0).doesNotExist())
			.andExpect(xpath(composer, NS, 5).doesNotExist())
			.andExpect(xpath(performer, NS, 0).doesNotExist())
			.andExpect(xpath(performer, NS, 3).doesNotExist())
			.andRespond(withSuccess());

		this.restTemplate.put(new URI("/composers"), this.people);
		this.mockServer.verify();
	}

	@Test
	public void testString() throws Exception {

		String composerName = "/ns:people/composers/composer[%s]/name";
		String performerName = "/ns:people/performers/performer[%s]/name";

		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().mimeType("application/xml"))
			.andExpect(xpath(composerName, NS, 1).string("Johann Sebastian Bach"))
			.andExpect(xpath(composerName, NS, 2).string("Johannes Brahms"))
			.andExpect(xpath(composerName, NS, 3).string("Edvard Grieg"))
			.andExpect(xpath(composerName, NS, 4).string("Robert Schumann"))
			.andExpect(xpath(performerName, NS, 1).string("Vladimir Ashkenazy"))
			.andExpect(xpath(performerName, NS, 2).string("Yehudi Menuhin"))
			.andExpect(xpath(composerName, NS, 1).string(equalTo("Johann Sebastian Bach"))) // Hamcrest..
			.andExpect(xpath(composerName, NS, 1).string(startsWith("Johann"))) // Hamcrest..
			.andExpect(xpath(composerName, NS, 1).string(notNullValue())) // Hamcrest..
			.andRespond(withSuccess());

		this.restTemplate.put(new URI("/composers"), this.people);
		this.mockServer.verify();
	}

	@Test
	public void testNumber() throws Exception {

		String composerDouble = "/ns:people/composers/composer[%s]/someDouble";

		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().mimeType("application/xml"))
			.andExpect(xpath(composerDouble, NS, 1).number(21d))
			.andExpect(xpath(composerDouble, NS, 2).number(.0025))
			.andExpect(xpath(composerDouble, NS, 3).number(1.6035))
			.andExpect(xpath(composerDouble, NS, 4).number(Double.NaN))
			.andExpect(xpath(composerDouble, NS, 1).number(equalTo(21d))) // Hamcrest..
			.andExpect(xpath(composerDouble, NS, 3).number(closeTo(1.6, .01))) // Hamcrest..
			.andRespond(withSuccess());

		this.restTemplate.put(new URI("/composers"), this.people);
		this.mockServer.verify();
	}

	@Test
	public void testBoolean() throws Exception {

		String performerBooleanValue = "/ns:people/performers/performer[%s]/someBoolean";

		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().mimeType("application/xml"))
			.andExpect(xpath(performerBooleanValue, NS, 1).booleanValue(false))
			.andExpect(xpath(performerBooleanValue, NS, 2).booleanValue(true))
			.andRespond(withSuccess());

		this.restTemplate.put(new URI("/composers"), this.people);
		this.mockServer.verify();
	}

	@Test
	public void testNodeCount() throws Exception {

		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().mimeType("application/xml"))
			.andExpect(xpath("/ns:people/composers/composer", NS).nodeCount(4))
			.andExpect(xpath("/ns:people/performers/performer", NS).nodeCount(2))
			.andExpect(xpath("/ns:people/composers/composer", NS).nodeCount(lessThan(5))) // Hamcrest..
			.andExpect(xpath("/ns:people/performers/performer", NS).nodeCount(greaterThan(0))) // Hamcrest..
			.andRespond(withSuccess());

		this.restTemplate.put(new URI("/composers"), this.people);
		this.mockServer.verify();
	}


	@SuppressWarnings("unused")
	@XmlRootElement(name="people", namespace="http://example.org/music/people")
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class PeopleWrapper {

		@XmlElementWrapper(name="composers")
		@XmlElement(name="composer")
		private List<Person> composers;

		@XmlElementWrapper(name="performers")
		@XmlElement(name="performer")
		private List<Person> performers;

		public PeopleWrapper() {
		}

		public PeopleWrapper(List<Person> composers, List<Person> performers) {
			this.composers = composers;
			this.performers = performers;
		}

		public List<Person> getComposers() {
			return this.composers;
		}

		public List<Person> getPerformers() {
			return this.performers;
		}
	}
}
