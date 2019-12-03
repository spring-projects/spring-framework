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

package org.springframework.oxm.xstream;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.springframework.util.xml.StaxUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class XStreamUnmarshallerTests {

	protected static final String INPUT_STRING = "<flight><flightNumber>42</flightNumber></flight>";

	private XStreamMarshaller unmarshaller;

	@BeforeEach
	public void createUnmarshaller() throws Exception {
		unmarshaller = new XStreamMarshaller();
		Map<String, Class<?>> aliases = new HashMap<>();
		aliases.put("flight", Flight.class);
		unmarshaller.setAliases(aliases);
	}

	private void testFlight(Object o) {
		boolean condition = o instanceof Flight;
		assertThat(condition).as("Unmarshalled object is not Flights").isTrue();
		Flight flight = (Flight) o;
		assertThat(flight).as("Flight is null").isNotNull();
		assertThat(flight.getFlightNumber()).as("Number is invalid").isEqualTo(42L);
	}

	@Test
	public void unmarshalDomSource() throws Exception {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = builder.parse(new InputSource(new StringReader(INPUT_STRING)));
		DOMSource source = new DOMSource(document);
		Object flight = unmarshaller.unmarshal(source);
		testFlight(flight);
	}

	@Test
	public void unmarshalStaxSourceXmlStreamReader() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
		Source source = StaxUtils.createStaxSource(streamReader);
		Object flights = unmarshaller.unmarshal(source);
		testFlight(flights);
	}

	@Test
	public void unmarshalStreamSourceInputStream() throws Exception {
		StreamSource source = new StreamSource(new ByteArrayInputStream(INPUT_STRING.getBytes("UTF-8")));
		Object flights = unmarshaller.unmarshal(source);
		testFlight(flights);
	}

	@Test
	public void unmarshalStreamSourceReader() throws Exception {
		StreamSource source = new StreamSource(new StringReader(INPUT_STRING));
		Object flights = unmarshaller.unmarshal(source);
		testFlight(flights);
	}
}

