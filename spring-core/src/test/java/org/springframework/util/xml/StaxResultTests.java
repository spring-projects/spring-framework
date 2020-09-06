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

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.xml.XmlContent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class StaxResultTests {

	private static final String XML = "<root xmlns='namespace'><child/></root>";

	private Transformer transformer;

	private XMLOutputFactory inputFactory;

	@BeforeEach
	void setUp() throws Exception {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformer = transformerFactory.newTransformer();
		inputFactory = XMLOutputFactory.newInstance();
	}

	@Test
	void streamWriterSource() throws Exception {
		StringWriter stringWriter = new StringWriter();
		XMLStreamWriter streamWriter = inputFactory.createXMLStreamWriter(stringWriter);
		Reader reader = new StringReader(XML);
		Source source = new StreamSource(reader);
		StaxResult result = new StaxResult(streamWriter);
		assertThat(result.getXMLStreamWriter()).as("Invalid streamWriter returned").isEqualTo(streamWriter);
		assertThat(result.getXMLEventWriter()).as("EventWriter returned").isNull();
		transformer.transform(source, result);
		assertThat(XmlContent.from(stringWriter)).as("Invalid result").isSimilarTo(XML);
	}

	@Test
	void eventWriterSource() throws Exception {
		StringWriter stringWriter = new StringWriter();
		XMLEventWriter eventWriter = inputFactory.createXMLEventWriter(stringWriter);
		Reader reader = new StringReader(XML);
		Source source = new StreamSource(reader);
		StaxResult result = new StaxResult(eventWriter);
		assertThat(result.getXMLEventWriter()).as("Invalid eventWriter returned").isEqualTo(eventWriter);
		assertThat(result.getXMLStreamWriter()).as("StreamWriter returned").isNull();
		transformer.transform(source, result);
		assertThat(XmlContent.from(stringWriter)).as("Invalid result").isSimilarTo(XML);
	}

}
