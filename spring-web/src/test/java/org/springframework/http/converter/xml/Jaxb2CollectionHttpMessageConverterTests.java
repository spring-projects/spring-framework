/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.converter.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.stream.XMLInputFactory;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MockHttpInputMessage;

/**
 * Test fixture for {@link Jaxb2CollectionHttpMessageConverter}.
 *
 * @author Arjen Poutsma
 */
public class Jaxb2CollectionHttpMessageConverterTests {

	private Jaxb2CollectionHttpMessageConverter<?> converter;

	private Type rootElementListType;

	private Type rootElementSetType;

	private Type typeListType;

	private Type typeSetType;


	@Before
	public void setUp() {
		converter = new Jaxb2CollectionHttpMessageConverter<Collection<Object>>();
		rootElementListType = new ParameterizedTypeReference<List<RootElement>>() {}.getType();
		rootElementSetType = new ParameterizedTypeReference<Set<RootElement>>() {}.getType();
		typeListType = new ParameterizedTypeReference<List<TestType>>() {}.getType();
		typeSetType = new ParameterizedTypeReference<Set<TestType>>() {}.getType();
	}

	@Test
	public void canRead() throws Exception {
		assertTrue(converter.canRead(rootElementListType, null, null));
		assertTrue(converter.canRead(rootElementSetType, null, null));
		assertTrue(converter.canRead(typeSetType, null, null));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readXmlRootElementList() throws Exception {
		String content = "<list><rootElement><type s=\"1\"/></rootElement><rootElement><type s=\"2\"/></rootElement></list>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes("UTF-8"));

		List<RootElement> result = (List<RootElement>) converter.read(rootElementListType, null, inputMessage);

		assertEquals("Invalid result", 2, result.size());
		assertEquals("Invalid result", "1", result.get(0).type.s);
		assertEquals("Invalid result", "2", result.get(1).type.s);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readXmlRootElementSet() throws Exception {
		String content = "<set><rootElement><type s=\"1\"/></rootElement><rootElement><type s=\"2\"/></rootElement></set>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes("UTF-8"));

		Set<RootElement> result = (Set<RootElement>) converter.read(rootElementSetType, null, inputMessage);

		assertEquals("Invalid result", 2, result.size());
		assertTrue("Invalid result", result.contains(new RootElement("1")));
		assertTrue("Invalid result", result.contains(new RootElement("2")));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readXmlTypeList() throws Exception {
		String content = "<list><foo s=\"1\"/><bar s=\"2\"/></list>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes("UTF-8"));

		List<TestType> result = (List<TestType>) converter.read(typeListType, null, inputMessage);

		assertEquals("Invalid result", 2, result.size());
		assertEquals("Invalid result", "1", result.get(0).s);
		assertEquals("Invalid result", "2", result.get(1).s);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readXmlTypeSet() throws Exception {
		String content = "<set><foo s=\"1\"/><bar s=\"2\"/></set>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes("UTF-8"));

		Set<TestType> result = (Set<TestType>) converter.read(typeSetType, null, inputMessage);

		assertEquals("Invalid result", 2, result.size());
		assertTrue("Invalid result", result.contains(new TestType("1")));
		assertTrue("Invalid result", result.contains(new TestType("2")));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readXmlRootElementExternalEntityDisabled() throws Exception {

		Resource external = new ClassPathResource("external.txt", getClass());
		String content =  "<!DOCTYPE root [" +
				"  <!ELEMENT external ANY >\n" +
				"  <!ENTITY ext SYSTEM \"" + external.getURI() + "\" >]>" +
				"  <list><rootElement><type s=\"1\"/><external>&ext;</external></rootElement></list>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes("UTF-8"));

		Collection<RootElement> result = converter.read(rootElementListType, null, inputMessage);
		assertEquals(1, result.size());
		assertEquals("", result.iterator().next().external);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readXmlRootElementExternalEntityEnabled() throws Exception {

		Resource external = new ClassPathResource("external.txt", getClass());
		String content =  "<!DOCTYPE root [" +
				"  <!ELEMENT external ANY >\n" +
				"  <!ENTITY ext SYSTEM \"" + external.getURI() + "\" >]>" +
				"  <list><rootElement><type s=\"1\"/><external>&ext;</external></rootElement></list>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes("UTF-8"));

		Jaxb2CollectionHttpMessageConverter<?> c = new Jaxb2CollectionHttpMessageConverter<Collection<Object>>() {
			@Override
			protected XMLInputFactory createXmlInputFactory() {
				XMLInputFactory inputFactory = XMLInputFactory.newInstance();
				inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
				return inputFactory;
			}
		};

		Collection<RootElement> result = c.read(rootElementListType, null, inputMessage);
		assertEquals(1, result.size());
		assertEquals("Foo Bar", result.iterator().next().external);
	}


	@XmlRootElement
	public static class RootElement {

		public RootElement() {
		}

		public RootElement(String s) {
			this.type = new TestType(s);
		}

		@XmlElement
		public TestType type = new TestType();

		@XmlElement(required=false)
		public String external;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o instanceof RootElement) {
				RootElement other = (RootElement) o;
				return this.type.equals(other.type);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return type.hashCode();
		}
	}

	@XmlType
	public static class TestType {

		public TestType() {
		}

		public TestType(String s) {
			this.s = s;
		}

		@XmlAttribute
		public String s = "Hello World";

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o instanceof TestType) {
				TestType other = (TestType) o;
				return this.s.equals(other.s);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return s.hashCode();
		}
	}

}
