/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.oxm;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import org.springframework.util.xml.StaxUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
public abstract class AbstractUnmarshallerTests<U extends Unmarshaller> {

	protected U unmarshaller;

	protected static final String INPUT_STRING =
			"<tns:flights xmlns:tns=\"http://samples.springframework.org/flight\">" +
					"<tns:flight><tns:number>42</tns:number></tns:flight></tns:flights>";

	@BeforeEach
	public final void setUp() throws Exception {
		unmarshaller = createUnmarshaller();
	}

	protected abstract U createUnmarshaller() throws Exception;

	protected abstract void testFlights(Object o);

	protected abstract void testFlight(Object o);

	@Test
	void unmarshalDomSource() throws Exception {
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
	void unmarshalStreamSourceReader() throws Exception {
		StreamSource source = new StreamSource(new StringReader(INPUT_STRING));
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	void unmarshalStreamSourceInputStream() throws Exception {
		StreamSource source = new StreamSource(new ByteArrayInputStream(
				INPUT_STRING.getBytes(StandardCharsets.UTF_8)));
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	void unmarshalSAXSource() throws Exception {
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		saxParserFactory.setNamespaceAware(true);
		SAXParser saxParser = saxParserFactory.newSAXParser();
		XMLReader reader = saxParser.getXMLReader();
		SAXSource source = new SAXSource(reader, new InputSource(new StringReader(INPUT_STRING)));
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	void unmarshalStaxSourceXmlStreamReader() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
		Source source = StaxUtils.createStaxSource(streamReader);
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	void unmarshalStaxSourceXmlEventReader() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(INPUT_STRING));
		Source source = StaxUtils.createStaxSource(eventReader);
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	void unmarshalJaxp14StaxSourceXmlStreamReader() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
		StAXSource source = new StAXSource(streamReader);
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	void unmarshalJaxp14StaxSourceXmlEventReader() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(INPUT_STRING));
		StAXSource source = new StAXSource(eventReader);
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	protected void unmarshalPartialStaxSourceXmlStreamReader() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
		streamReader.nextTag(); // skip to flights
		assertThat(streamReader.getName()).as("Invalid element").isEqualTo(new QName("http://samples.springframework.org/flight", "flights"));
		streamReader.nextTag(); // skip to flight
		assertThat(streamReader.getName()).as("Invalid element").isEqualTo(new QName("http://samples.springframework.org/flight", "flight"));
		Source source = StaxUtils.createStaxSource(streamReader);
		Object flight = unmarshaller.unmarshal(source);
		testFlight(flight);
	}

}
