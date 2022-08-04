/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.thoughtworks.xstream.converters.extended.EncodedByteArrayConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JsonWriter;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xmlunit.builder.Input;
import org.xmlunit.xpath.JAXPXPathEngine;

import org.springframework.core.testfixture.xml.XmlContent;
import org.springframework.util.xml.StaxUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Juergen Hoeller
 */
class XStreamMarshallerTests {

	private static final String EXPECTED_STRING = "<flight><flightNumber>42</flightNumber></flight>";

	private final Flight flight = new Flight();

	private XStreamMarshaller marshaller;


	@BeforeEach
	void createMarshaller() {
		marshaller = new XStreamMarshaller();
		marshaller.setTypePermissions(AnyTypePermission.ANY);
		marshaller.setAliases(Collections.singletonMap("flight", Flight.class.getName()));
		flight.setFlightNumber(42L);
	}


	@Test
	void marshalDOMResult() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
		Document document = builder.newDocument();
		DOMResult domResult = new DOMResult(document);
		marshaller.marshal(flight, domResult);
		Document expected = builder.newDocument();
		Element flightElement = expected.createElement("flight");
		expected.appendChild(flightElement);
		Element numberElement = expected.createElement("flightNumber");
		flightElement.appendChild(numberElement);
		Text text = expected.createTextNode("42");
		numberElement.appendChild(text);
		assertThat(XmlContent.of(document)).isSimilarTo(expected);
	}

	// see SWS-392
	@Test
	void marshalDOMResultToExistentDocument() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
		Document existent = builder.newDocument();
		Element rootElement = existent.createElement("root");
		Element flightsElement = existent.createElement("flights");
		rootElement.appendChild(flightsElement);
		existent.appendChild(rootElement);

		// marshall into the existent document
		DOMResult domResult = new DOMResult(flightsElement);
		marshaller.marshal(flight, domResult);

		Document expected = builder.newDocument();
		Element eRootElement = expected.createElement("root");
		Element eFlightsElement = expected.createElement("flights");
		Element eFlightElement = expected.createElement("flight");
		eRootElement.appendChild(eFlightsElement);
		eFlightsElement.appendChild(eFlightElement);
		expected.appendChild(eRootElement);
		Element eNumberElement = expected.createElement("flightNumber");
		eFlightElement.appendChild(eNumberElement);
		Text text = expected.createTextNode("42");
		eNumberElement.appendChild(text);
		assertThat(XmlContent.of(existent)).isSimilarTo(expected);
	}

	@Test
	void marshalStreamResultWriter() throws Exception {
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		marshaller.marshal(flight, result);
		assertThat(XmlContent.from(writer)).isSimilarTo(EXPECTED_STRING);
	}

	@Test
	void marshalStreamResultOutputStream() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		StreamResult result = new StreamResult(os);
		marshaller.marshal(flight, result);
		String s = os.toString("UTF-8");
		assertThat(XmlContent.of(s)).isSimilarTo(EXPECTED_STRING);
	}

	@Test
	void marshalSaxResult() throws Exception {
		ContentHandler contentHandler = mock(ContentHandler.class);
		SAXResult result = new SAXResult(contentHandler);
		marshaller.marshal(flight, result);
		InOrder ordered = inOrder(contentHandler);
		ordered.verify(contentHandler).startDocument();
		ordered.verify(contentHandler).startElement(eq(""), eq("flight"), eq("flight"), isA(Attributes.class));
		ordered.verify(contentHandler).startElement(eq(""), eq("flightNumber"), eq("flightNumber"), isA(Attributes.class));
		ordered.verify(contentHandler).characters(isA(char[].class), eq(0), eq(2));
		ordered.verify(contentHandler).endElement("", "flightNumber", "flightNumber");
		ordered.verify(contentHandler).endElement("", "flight", "flight");
		ordered.verify(contentHandler).endDocument();
	}

	@Test
	void marshalStaxResultXMLStreamWriter() throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		StringWriter writer = new StringWriter();
		XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
		Result result = StaxUtils.createStaxResult(streamWriter);
		marshaller.marshal(flight, result);
		assertThat(XmlContent.from(writer)).isSimilarTo(EXPECTED_STRING);
	}

	@Test
	void marshalStaxResultXMLStreamWriterDefaultNamespace() throws Exception {
		QNameMap map = new QNameMap();
		map.setDefaultNamespace("https://example.com");
		map.setDefaultPrefix("spr");
		StaxDriver driver = new StaxDriver(map);
		marshaller.setStreamDriver(driver);

		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		StringWriter writer = new StringWriter();
		XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
		Result result = StaxUtils.createStaxResult(streamWriter);
		marshaller.marshal(flight, result);
		assertThat(XmlContent.from(writer)).isSimilarTo(
				"<spr:flight xmlns:spr=\"https://example.com\"><spr:flightNumber>42</spr:flightNumber></spr:flight>");
	}

	@Test
	void marshalStaxResultXMLEventWriter() throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		StringWriter writer = new StringWriter();
		XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(writer);
		Result result = StaxUtils.createStaxResult(eventWriter);
		marshaller.marshal(flight, result);
		assertThat(XmlContent.from(writer)).isSimilarTo(EXPECTED_STRING);
	}

	@Test
	void converters() throws Exception {
		marshaller.setConverters(new EncodedByteArrayConverter());
		byte[] buf = {0x1, 0x2};

		// Execute multiple times concurrently to ensure there are no concurrency issues.
		// See https://github.com/spring-projects/spring-framework/issues/25017
		IntStream.rangeClosed(1, 100).parallel().forEach(n -> {
			try {
				Writer writer = new StringWriter();
				marshaller.marshal(buf, new StreamResult(writer));
				assertThat(XmlContent.from(writer)).isSimilarTo("<byte-array>AQI=</byte-array>");
				Reader reader = new StringReader(writer.toString());
				byte[] bufResult = (byte[]) marshaller.unmarshal(new StreamSource(reader));
				assertThat(bufResult).as("Invalid result").isEqualTo(buf);
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	@Test
	void useAttributesFor() throws Exception {
		marshaller.setUseAttributeForTypes(Long.TYPE);
		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		String expected = "<flight flightNumber=\"42\" />";
		assertThat(XmlContent.from(writer)).isSimilarTo(expected);
	}

	@Test
	void useAttributesForStringClassMap() throws Exception {
		marshaller.setUseAttributeFor(Collections.singletonMap("flightNumber", Long.TYPE));
		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		String expected = "<flight flightNumber=\"42\" />";
		assertThat(XmlContent.from(writer)).isSimilarTo(expected);
	}

	@Test
	void useAttributesForClassStringMap() throws Exception {
		marshaller.setUseAttributeFor(Collections.singletonMap(Flight.class, "flightNumber"));
		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		String expected = "<flight flightNumber=\"42\" />";
		assertThat(XmlContent.from(writer)).isSimilarTo(expected);
	}

	@Test
	void useAttributesForClassStringListMap() throws Exception {
		marshaller.setUseAttributeFor(Collections.singletonMap(Flight.class, Collections.singletonList("flightNumber")));
		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		String expected = "<flight flightNumber=\"42\" />";
		assertThat(XmlContent.from(writer)).isSimilarTo(expected);
	}

	@Test
	void aliasesByTypeStringClassMap() throws Exception {
		Map<String, Class<?>> aliases = new HashMap<>();
		aliases.put("flight", Flight.class);
		FlightSubclass flight = new FlightSubclass();
		flight.setFlightNumber(42);
		marshaller.setAliasesByType(aliases);

		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		assertThat(XmlContent.from(writer)).isSimilarTo(EXPECTED_STRING);
	}

	@Test
	void aliasesByTypeStringStringMap() throws Exception {
		Map<String, String> aliases = new HashMap<>();
		aliases.put("flight", Flight.class.getName());
		FlightSubclass flight = new FlightSubclass();
		flight.setFlightNumber(42);
		marshaller.setAliasesByType(aliases);

		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		assertThat(XmlContent.from(writer)).isSimilarTo(EXPECTED_STRING);
	}

	@Test
	void fieldAliases() throws Exception {
		marshaller.setFieldAliases(Collections.singletonMap("org.springframework.oxm.xstream.Flight.flightNumber", "flightNo"));
		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		String expected = "<flight><flightNo>42</flightNo></flight>";
		assertThat(XmlContent.from(writer)).isSimilarTo(expected);
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void omitFields() throws Exception {
		Map omittedFieldsMap = Collections.singletonMap(Flight.class, "flightNumber");
		marshaller.setOmittedFields(omittedFieldsMap);
		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		assertXpathDoesNotExist("/flight/flightNumber", writer.toString());
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void implicitCollections() throws Exception {
		Flights flights = new Flights();
		flights.getFlights().add(flight);
		flights.getStrings().add("42");

		Map<String, Class<?>> aliases = new HashMap<>();
		aliases.put("flight", Flight.class);
		aliases.put("flights", Flights.class);
		marshaller.setAliases(aliases);

		Map implicitCollections = Collections.singletonMap(Flights.class, "flights,strings");
		marshaller.setImplicitCollections(implicitCollections);

		Writer writer = new StringWriter();
		marshaller.marshal(flights, new StreamResult(writer));
		String result = writer.toString();
		assertXpathDoesNotExist("/flights/flights", result);
		assertXpathExists("/flights/flight", result);
		assertXpathDoesNotExist("/flights/strings", result);
		assertXpathExists("/flights/string", result);
	}

	@Test
	void jettisonDriver() throws Exception {
		marshaller.setStreamDriver(new JettisonMappedXmlDriver());
		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		assertThat(writer.toString()).as("Invalid result").isEqualTo("{\"flight\":{\"flightNumber\":42}}");
		Object o = marshaller.unmarshal(new StreamSource(new StringReader(writer.toString())));
		assertThat(o instanceof Flight).as("Unmarshalled object is not Flights").isTrue();
		Flight unflight = (Flight) o;
		assertThat(unflight).as("Flight is null").isNotNull();
		assertThat(unflight.getFlightNumber()).as("Number is invalid").isEqualTo(42L);
	}

	@Test
	void jsonDriver() throws Exception {
		marshaller.setStreamDriver(new JsonHierarchicalStreamDriver() {
			@Override
			public HierarchicalStreamWriter createWriter(Writer writer) {
				return new JsonWriter(writer, JsonWriter.DROP_ROOT_MODE,
						new JsonWriter.Format(new char[0], new char[0],
								JsonWriter.Format.SPACE_AFTER_LABEL | JsonWriter.Format.COMPACT_EMPTY_ELEMENT));
			}
		});

		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		assertThat(writer.toString()).as("Invalid result").isEqualTo("{\"flightNumber\": 42}");
	}

	@Test
	void annotatedMarshalStreamResultWriter() throws Exception {
		marshaller.setAnnotatedClasses(Flight.class);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		Flight flight = new Flight();
		flight.setFlightNumber(42);
		marshaller.marshal(flight, result);
		String expected = "<flight><number>42</number></flight>";
		assertThat(XmlContent.from(writer)).isSimilarTo(expected);
	}


	private static void assertXpathExists(String xPathExpression, String inXMLString){
		Source source = Input.fromString(inXMLString).build();
		Iterable<Node> nodes = new JAXPXPathEngine().selectNodes(xPathExpression, source);
		assertThat(nodes).as("Expecting to find matches for Xpath " + xPathExpression).hasSizeGreaterThan(0);
	}

	private static void assertXpathDoesNotExist(String xPathExpression, String inXMLString){
		Source source = Input.fromString(inXMLString).build();
		Iterable<Node> nodes = new JAXPXPathEngine().selectNodes(xPathExpression, source);
		assertThat(nodes).as("Should be zero matches for Xpath " + xPathExpression).isEmpty();
	}

}
