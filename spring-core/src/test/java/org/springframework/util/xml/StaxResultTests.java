/*
 * Copyright 2002-2018 the original author or authors.
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

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

/**
 * @author Arjen Poutsma
 */
public class StaxResultTests {

	private static final String XML = "<root xmlns='namespace'><child/></root>";

	private Transformer transformer;

	private XMLOutputFactory inputFactory;

	@Before
	public void setUp() throws Exception {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformer = transformerFactory.newTransformer();
		inputFactory = XMLOutputFactory.newInstance();
	}

	@Test
	public void streamWriterSource() throws Exception {
		StringWriter stringWriter = new StringWriter();
		XMLStreamWriter streamWriter = inputFactory.createXMLStreamWriter(stringWriter);
		Reader reader = new StringReader(XML);
		Source source = new StreamSource(reader);
		StaxResult result = new StaxResult(streamWriter);
		assertEquals("Invalid streamWriter returned", streamWriter, result.getXMLStreamWriter());
		assertNull("EventWriter returned", result.getXMLEventWriter());
		transformer.transform(source, result);
		assertThat("Invalid result", stringWriter.toString(), isSimilarTo(XML));
	}

	@Test
	public void eventWriterSource() throws Exception {
		StringWriter stringWriter = new StringWriter();
		XMLEventWriter eventWriter = inputFactory.createXMLEventWriter(stringWriter);
		Reader reader = new StringReader(XML);
		Source source = new StreamSource(reader);
		StaxResult result = new StaxResult(eventWriter);
		assertEquals("Invalid eventWriter returned", eventWriter, result.getXMLEventWriter());
		assertNull("StreamWriter returned", result.getXMLStreamWriter());
		transformer.transform(source, result);
		assertThat("Invalid result", stringWriter.toString(), isSimilarTo(XML));
	}

}
