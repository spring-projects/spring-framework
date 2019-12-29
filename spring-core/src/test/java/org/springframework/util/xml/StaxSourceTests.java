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

package org.springframework.util.xml;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.springframework.core.testfixture.xml.XmlContent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class StaxSourceTests {

	private static final String XML = "<root xmlns='namespace'><child/></root>";

	private Transformer transformer;

	private XMLInputFactory inputFactory;

	private DocumentBuilder documentBuilder;

	@BeforeEach
	void setUp() throws Exception {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformer = transformerFactory.newTransformer();
		inputFactory = XMLInputFactory.newInstance();
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		documentBuilder = documentBuilderFactory.newDocumentBuilder();
	}

	@Test
	void streamReaderSourceToStreamResult() throws Exception {
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(XML));
		StaxSource source = new StaxSource(streamReader);
		assertThat(source.getXMLStreamReader()).as("Invalid streamReader returned").isEqualTo(streamReader);
		assertThat((Object) source.getXMLEventReader()).as("EventReader returned").isNull();
		StringWriter writer = new StringWriter();
		transformer.transform(source, new StreamResult(writer));
		assertThat(XmlContent.from(writer)).as("Invalid result").isSimilarTo(XML);
	}

	@Test
	void streamReaderSourceToDOMResult() throws Exception {
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(XML));
		StaxSource source = new StaxSource(streamReader);
		assertThat(source.getXMLStreamReader()).as("Invalid streamReader returned").isEqualTo(streamReader);
		assertThat((Object) source.getXMLEventReader()).as("EventReader returned").isNull();

		Document expected = documentBuilder.parse(new InputSource(new StringReader(XML)));
		Document result = documentBuilder.newDocument();
		transformer.transform(source, new DOMResult(result));
		assertThat(XmlContent.of(result)).as("Invalid result").isSimilarTo(expected);
	}

	@Test
	void eventReaderSourceToStreamResult() throws Exception {
		XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(XML));
		StaxSource source = new StaxSource(eventReader);
		assertThat((Object) source.getXMLEventReader()).as("Invalid eventReader returned").isEqualTo(eventReader);
		assertThat(source.getXMLStreamReader()).as("StreamReader returned").isNull();
		StringWriter writer = new StringWriter();
		transformer.transform(source, new StreamResult(writer));
		assertThat(XmlContent.from(writer)).as("Invalid result").isSimilarTo(XML);
	}

	@Test
	void eventReaderSourceToDOMResult() throws Exception {
		XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(XML));
		StaxSource source = new StaxSource(eventReader);
		assertThat((Object) source.getXMLEventReader()).as("Invalid eventReader returned").isEqualTo(eventReader);
		assertThat(source.getXMLStreamReader()).as("StreamReader returned").isNull();

		Document expected = documentBuilder.parse(new InputSource(new StringReader(XML)));
		Document result = documentBuilder.newDocument();
		transformer.transform(source, new DOMResult(result));
		assertThat(XmlContent.of(result)).as("Invalid result").isSimilarTo(expected);
	}
}
