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

package org.springframework.util.xml;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

/**
 * @author Arjen Poutsma
 */
public class StaxSourceTests {

	private static final String XML = "<root xmlns='namespace'><child/></root>";

	private Transformer transformer;

	private XMLInputFactory inputFactory;

	private DocumentBuilder documentBuilder;

	@Before
	public void setUp() throws Exception {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformer = transformerFactory.newTransformer();
		inputFactory = XMLInputFactory.newInstance();
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		documentBuilder = documentBuilderFactory.newDocumentBuilder();
	}

	@Test
	public void streamReaderSourceToStreamResult() throws Exception {
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(XML));
		StaxSource source = new StaxSource(streamReader);
		assertEquals("Invalid streamReader returned", streamReader, source.getXMLStreamReader());
		assertNull("EventReader returned", source.getXMLEventReader());
		StringWriter writer = new StringWriter();
		transformer.transform(source, new StreamResult(writer));
		assertThat("Invalid result", writer.toString(), isSimilarTo(XML));
	}

	@Test
	public void streamReaderSourceToDOMResult() throws Exception {
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(XML));
		StaxSource source = new StaxSource(streamReader);
		assertEquals("Invalid streamReader returned", streamReader, source.getXMLStreamReader());
		assertNull("EventReader returned", source.getXMLEventReader());

		Document expected = documentBuilder.parse(new InputSource(new StringReader(XML)));
		Document result = documentBuilder.newDocument();
		transformer.transform(source, new DOMResult(result));
		assertThat("Invalid result", result, isSimilarTo(expected));
	}

	@Test
	public void eventReaderSourceToStreamResult() throws Exception {
		XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(XML));
		StaxSource source = new StaxSource(eventReader);
		assertEquals("Invalid eventReader returned", eventReader, source.getXMLEventReader());
		assertNull("StreamReader returned", source.getXMLStreamReader());
		StringWriter writer = new StringWriter();
		transformer.transform(source, new StreamResult(writer));
		assertThat("Invalid result", writer.toString(), isSimilarTo(XML));
	}

	@Test
	public void eventReaderSourceToDOMResult() throws Exception {
		XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(XML));
		StaxSource source = new StaxSource(eventReader);
		assertEquals("Invalid eventReader returned", eventReader, source.getXMLEventReader());
		assertNull("StreamReader returned", source.getXMLStreamReader());

		Document expected = documentBuilder.parse(new InputSource(new StringReader(XML)));
		Document result = documentBuilder.newDocument();
		transformer.transform(source, new DOMResult(result));
		assertThat("Invalid result", result, isSimilarTo(expected));
	}
}
