/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.web.servlet.samples.standalone.resultmatchers;

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

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Examples of expectations on XML response content with XPath expressions.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see ContentAssertionTests
 * @see XmlContentAssertionTests
 */
public class XpathAssertionTests {

	private static final Map<String, String> musicNamespace =
		Collections.singletonMap("ns", "http://example.org/music/people");

	private MockMvc mockMvc;

	@Before
	public void setup() throws Exception {
		this.mockMvc = standaloneSetup(new MusicController())
				.defaultRequest(get("/").accept(MediaType.APPLICATION_XML, MediaType.parseMediaType("application/xml;charset=UTF-8")))
				.alwaysExpect(status().isOk())
				.alwaysExpect(content().contentType(MediaType.parseMediaType("application/xml;charset=UTF-8")))
				.build();
	}

	@Test
	public void testExists() throws Exception {

		String composer = "/ns:people/composers/composer[%s]";
		String performer = "/ns:people/performers/performer[%s]";

		this.mockMvc.perform(get("/music/people"))
			.andExpect(xpath(composer, musicNamespace, 1).exists())
			.andExpect(xpath(composer, musicNamespace, 2).exists())
			.andExpect(xpath(composer, musicNamespace, 3).exists())
			.andExpect(xpath(composer, musicNamespace, 4).exists())
			.andExpect(xpath(performer, musicNamespace, 1).exists())
			.andExpect(xpath(performer, musicNamespace, 2).exists())
			.andExpect(xpath(composer, musicNamespace, 1).node(notNullValue()));
	}

	@Test
	public void testDoesNotExist() throws Exception {

		String composer = "/ns:people/composers/composer[%s]";
		String performer = "/ns:people/performers/performer[%s]";

		this.mockMvc.perform(get("/music/people"))
			.andExpect(xpath(composer, musicNamespace, 0).doesNotExist())
			.andExpect(xpath(composer, musicNamespace, 5).doesNotExist())
			.andExpect(xpath(performer, musicNamespace, 0).doesNotExist())
			.andExpect(xpath(performer, musicNamespace, 3).doesNotExist())
			.andExpect(xpath(composer, musicNamespace, 0).node(nullValue()));
	}

	@Test
	public void testString() throws Exception {

		String composerName = "/ns:people/composers/composer[%s]/name";
		String performerName = "/ns:people/performers/performer[%s]/name";

		this.mockMvc.perform(get("/music/people"))
			.andExpect(xpath(composerName, musicNamespace, 1).string("Johann Sebastian Bach"))
			.andExpect(xpath(composerName, musicNamespace, 2).string("Johannes Brahms"))
			.andExpect(xpath(composerName, musicNamespace, 3).string("Edvard Grieg"))
			.andExpect(xpath(composerName, musicNamespace, 4).string("Robert Schumann"))
			.andExpect(xpath(performerName, musicNamespace, 1).string("Vladimir Ashkenazy"))
			.andExpect(xpath(performerName, musicNamespace, 2).string("Yehudi Menuhin"))
			.andExpect(xpath(composerName, musicNamespace, 1).string(equalTo("Johann Sebastian Bach"))) // Hamcrest..
			.andExpect(xpath(composerName, musicNamespace, 1).string(startsWith("Johann")))
			.andExpect(xpath(composerName, musicNamespace, 1).string(notNullValue()));
	}

	@Test
	public void testNumber() throws Exception {

		String composerDouble = "/ns:people/composers/composer[%s]/someDouble";

		this.mockMvc.perform(get("/music/people"))
			.andExpect(xpath(composerDouble, musicNamespace, 1).number(21d))
			.andExpect(xpath(composerDouble, musicNamespace, 2).number(.0025))
			.andExpect(xpath(composerDouble, musicNamespace, 3).number(1.6035))
			.andExpect(xpath(composerDouble, musicNamespace, 4).number(Double.NaN))
			.andExpect(xpath(composerDouble, musicNamespace, 1).number(equalTo(21d)))  // Hamcrest..
			.andExpect(xpath(composerDouble, musicNamespace, 3).number(closeTo(1.6, .01)));
	}

	@Test
	public void testBoolean() throws Exception {

		String performerBooleanValue = "/ns:people/performers/performer[%s]/someBoolean";

		this.mockMvc.perform(get("/music/people"))
			.andExpect(xpath(performerBooleanValue, musicNamespace, 1).booleanValue(false))
			.andExpect(xpath(performerBooleanValue, musicNamespace, 2).booleanValue(true));
	}

	@Test
	public void testNodeCount() throws Exception {

		this.mockMvc.perform(get("/music/people"))
			.andExpect(xpath("/ns:people/composers/composer", musicNamespace).nodeCount(4))
			.andExpect(xpath("/ns:people/performers/performer", musicNamespace).nodeCount(2))
			.andExpect(xpath("/ns:people/composers/composer", musicNamespace).nodeCount(equalTo(4))) // Hamcrest..
			.andExpect(xpath("/ns:people/performers/performer", musicNamespace).nodeCount(equalTo(2)));
	}

	// SPR-10704

	@Test
	public void testFeedWithLinefeedChars() throws Exception {

//		Map<String, String> namespace = Collections.singletonMap("ns", "");

		standaloneSetup(new BlogFeedController()).build()
			.perform(get("/blog.atom").accept(MediaType.APPLICATION_ATOM_XML))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_ATOM_XML))
				.andExpect(xpath("//feed/title").string("Test Feed"))
				.andExpect(xpath("//feed/icon").string("http://www.example.com/favicon.ico"));
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

			List<Person> performers = Arrays.asList(
					new Person("Vladimir Ashkenazy").setSomeBoolean(false),
					new Person("Yehudi Menuhin").setSomeBoolean(true));

			return new PeopleWrapper(composers, performers);
		}
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


	@Controller
	public class BlogFeedController {

		@RequestMapping(value="/blog.atom", method = { GET, HEAD })
		@ResponseBody
		public String listPublishedPosts() {
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
					+ "<feed xmlns=\"http://www.w3.org/2005/Atom\">\r\n"
					+ "  <title>Test Feed</title>\r\n"
					+ "  <icon>http://www.example.com/favicon.ico</icon>\r\n"
					+ "</feed>\r\n\r\n";
		}
	}

}
