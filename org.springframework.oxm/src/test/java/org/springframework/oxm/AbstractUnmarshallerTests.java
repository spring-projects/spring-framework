/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.oxm;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.springframework.util.xml.StaxUtils;

/**
 * @author Arjen Poutsma
 */
public abstract class AbstractUnmarshallerTests {

	protected Unmarshaller unmarshaller;

	protected static final String INPUT_STRING =
			"<tns:flights xmlns:tns=\"http://samples.springframework.org/flight\">" +
					"<tns:flight><tns:number>42</tns:number></tns:flight></tns:flights>";

	@Before
	public final void setUp() throws Exception {
		unmarshaller = createUnmarshaller();
	}

	protected abstract Unmarshaller createUnmarshaller() throws Exception;

	protected abstract void testFlights(Object o);

	protected abstract void testFlight(Object o);

	@Test
	public void unmarshalDomSource() throws Exception {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = builder.newDocument();
		Element flightsElement = document.createElementNS("http://samples.springframework.org/flight", "tns:flights");
		document.appendChild(flightsElement);
		Element flightElement = document.createElementNS("http://samples.springframework.org/flight", "tns:flight");
		flightsElement.appendChild(flightElement);
		Element numberElement = document.createElementNS("http://samples.springframework.org/flight", "tns:number");
		flightElement.appendChild(numberElement);
		Text text = document.createTextNode("42");
		numberElement.appendChild(text);
		DOMSource source = new DOMSource(document);
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	public void unmarshalStreamSourceReader() throws Exception {
		StreamSource source = new StreamSource(new StringReader(INPUT_STRING));
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	public void unmarshalStreamSourceInputStream() throws Exception {
		StreamSource source = new StreamSource(new ByteArrayInputStream(INPUT_STRING.getBytes("UTF-8")));
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	public void unmarshalSAXSource() throws Exception {
		XMLReader reader = XMLReaderFactory.createXMLReader();
		SAXSource source = new SAXSource(reader, new InputSource(new StringReader(INPUT_STRING)));
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	public void unmarshalStaxSourceXmlStreamReader() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
		Source source = StaxUtils.createStaxSource(streamReader);
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	public void unmarshalStaxSourceXmlEventReader() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(INPUT_STRING));
		Source source = StaxUtils.createStaxSource(eventReader);
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	public void unmarshalJaxp14StaxSourceXmlStreamReader() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
		StAXSource source = new StAXSource(streamReader);
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	public void unmarshalJaxp14StaxSourceXmlEventReader() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(INPUT_STRING));
		StAXSource source = new StAXSource(eventReader);
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	public void unmarshalPartialStaxSourceXmlStreamReader() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
		streamReader.nextTag(); // skip to flights
		assertEquals("Invalid element", new QName("http://samples.springframework.org/flight", "flights"),
				streamReader.getName());
		streamReader.nextTag(); // skip to flight
		assertEquals("Invalid element", new QName("http://samples.springframework.org/flight", "flight"),
				streamReader.getName());
		Source source = StaxUtils.createStaxSource(streamReader);
		Object flight = unmarshaller.unmarshal(source);
		testFlight(flight);
	}
}
