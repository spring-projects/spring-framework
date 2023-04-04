/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http.converter.xml;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.web.testfixture.http.MockHttpInputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test fixture for {@link Jaxb2CollectionHttpMessageConverter}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class Jaxb2CollectionHttpMessageConverterTests {

	private Jaxb2CollectionHttpMessageConverter<?> converter;

	private Type rootElementListType;

	private Type rootElementSetType;

	private Type typeListType;

	private Type typeSetType;


	@BeforeEach
	public void setup() {
		converter = new Jaxb2CollectionHttpMessageConverter<Collection<Object>>();
		rootElementListType = new ParameterizedTypeReference<List<RootElement>>() {}.getType();
		rootElementSetType = new ParameterizedTypeReference<Set<RootElement>>() {}.getType();
		typeListType = new ParameterizedTypeReference<List<TestType>>() {}.getType();
		typeSetType = new ParameterizedTypeReference<Set<TestType>>() {}.getType();
	}


	@Test
	public void canRead() {
		assertThat(converter.canRead(rootElementListType, null, null)).isTrue();
		assertThat(converter.canRead(rootElementSetType, null, null)).isTrue();
		assertThat(converter.canRead(typeSetType, null, null)).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readXmlRootElementList() throws Exception {
		String content = "<list><rootElement><type s=\"1\"/></rootElement><rootElement><type s=\"2\"/></rootElement></list>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));
		List<RootElement> result = (List<RootElement>) converter.read(rootElementListType, null, inputMessage);

		assertThat(result).as("Invalid result").hasSize(2);
		assertThat(result.get(0).type.s).as("Invalid result").isEqualTo("1");
		assertThat(result.get(1).type.s).as("Invalid result").isEqualTo("2");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readXmlRootElementSet() throws Exception {
		String content = "<set><rootElement><type s=\"1\"/></rootElement><rootElement><type s=\"2\"/></rootElement></set>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));
		Set<RootElement> result = (Set<RootElement>) converter.read(rootElementSetType, null, inputMessage);

		assertThat(result).as("Invalid result").hasSize(2);
		assertThat(result.contains(new RootElement("1"))).as("Invalid result").isTrue();
		assertThat(result.contains(new RootElement("2"))).as("Invalid result").isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readXmlTypeList() throws Exception {
		String content = "<list><foo s=\"1\"/><bar s=\"2\"/></list>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));
		List<TestType> result = (List<TestType>) converter.read(typeListType, null, inputMessage);

		assertThat(result).as("Invalid result").hasSize(2);
		assertThat(result.get(0).s).as("Invalid result").isEqualTo("1");
		assertThat(result.get(1).s).as("Invalid result").isEqualTo("2");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readXmlTypeSet() throws Exception {
		String content = "<set><foo s=\"1\"/><bar s=\"2\"/></set>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));
		Set<TestType> result = (Set<TestType>) converter.read(typeSetType, null, inputMessage);

		assertThat(result).as("Invalid result").hasSize(2);
		assertThat(result.contains(new TestType("1"))).as("Invalid result").isTrue();
		assertThat(result.contains(new TestType("2"))).as("Invalid result").isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readXmlRootElementExternalEntityDisabled() throws Exception {
		Resource external = new ClassPathResource("external.txt", getClass());
		String content =  "<!DOCTYPE root [" +
				"  <!ELEMENT external ANY >\n" +
				"  <!ENTITY ext SYSTEM \"" + external.getURI() + "\" >]>" +
				"  <list><rootElement><type s=\"1\"/><external>&ext;</external></rootElement></list>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));

		converter = new Jaxb2CollectionHttpMessageConverter<Collection<Object>>() {
			@Override
			protected XMLInputFactory createXmlInputFactory() {
				XMLInputFactory inputFactory = super.createXmlInputFactory();
				inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, true);
				return inputFactory;
			}
		};

		try {
			Collection<RootElement> result = converter.read(rootElementListType, null, inputMessage);
			assertThat(result).hasSize(1);
			assertThat(result.iterator().next().external).isEmpty();
		}
		catch (HttpMessageNotReadableException ex) {
			// Some parsers raise an exception
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readXmlRootElementExternalEntityEnabled() throws Exception {
		Resource external = new ClassPathResource("external.txt", getClass());
		String content =  "<!DOCTYPE root [" +
				"  <!ELEMENT external ANY >\n" +
				"  <!ENTITY ext SYSTEM \"" + external.getURI() + "\" >]>" +
				"  <list><rootElement><type s=\"1\"/><external>&ext;</external></rootElement></list>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));

		Jaxb2CollectionHttpMessageConverter<?> c = new Jaxb2CollectionHttpMessageConverter<Collection<Object>>() {
			@Override
			protected XMLInputFactory createXmlInputFactory() {
				XMLInputFactory inputFactory = XMLInputFactory.newInstance();
				inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
				return inputFactory;
			}
		};

		Collection<RootElement> result = c.read(rootElementListType, null, inputMessage);
		assertThat(result).hasSize(1);
		assertThat(result.iterator().next().external).isEqualTo("Foo Bar");
	}

	@Test
	public void testXmlBomb() throws Exception {
		// https://en.wikipedia.org/wiki/Billion_laughs
		// https://msdn.microsoft.com/en-us/magazine/ee335713.aspx
		String content = """
				<?xml version="1.0"?>
				<!DOCTYPE lolz [
					<!ENTITY lol "lol">
					<!ELEMENT lolz (#PCDATA)>
					<!ENTITY lol1 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
					<!ENTITY lol2 "&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;">
					<!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
					<!ENTITY lol4 "&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;">
					<!ENTITY lol5 "&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;">
					<!ENTITY lol6 "&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;">
					<!ENTITY lol7 "&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;">
					<!ENTITY lol8 "&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;">
					<!ENTITY lol9 "&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;">
				]>
				<list><rootElement><external>&lol9;</external></rootElement></list>""";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));
		assertThatExceptionOfType(HttpMessageNotReadableException.class)
				.isThrownBy(() -> this.converter.read(this.rootElementListType, null, inputMessage))
				.withMessageContaining("\"lol9\"");
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
		public boolean equals(@Nullable Object o) {
			if (this == o) {
				return true;
			}
			if (o instanceof RootElement other) {
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
		public boolean equals(@Nullable Object o) {
			if (this == o) {
				return true;
			}
			if (o instanceof TestType other) {
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
