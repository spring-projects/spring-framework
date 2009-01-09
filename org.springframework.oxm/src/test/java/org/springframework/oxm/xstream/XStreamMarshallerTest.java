/*
 * Copyright 2006 the original author or authors.
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

package org.springframework.oxm.xstream;

import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.extended.EncodedByteArrayConverter;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import org.easymock.MockControl;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.ContentHandler;

import org.springframework.util.xml.StaxUtils;

public class XStreamMarshallerTest {

	private static final String EXPECTED_STRING = "<flight><flightNumber>42</flightNumber></flight>";

	private XStreamMarshaller marshaller;

	private AnnotatedFlight flight;

	@Before
	public void createMarshaller() throws Exception {
		marshaller = new XStreamMarshaller();
		Properties aliases = new Properties();
		aliases.setProperty("flight", AnnotatedFlight.class.getName());
		marshaller.setAliases(aliases);
		flight = new AnnotatedFlight();
		flight.setFlightNumber(42L);
	}

	@Test
	public void marshalDOMResult() throws Exception {
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
		assertXMLEqual("Marshaller writes invalid DOMResult", expected, document);
	}

	// see SWS-392
	@Test
	public void marshalDOMResultToExistentDocument() throws Exception {
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
		assertXMLEqual("Marshaller writes invalid DOMResult", expected, existent);
	}

	@Test
	public void marshalStreamResultWriter() throws Exception {
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		marshaller.marshal(flight, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
	}

	@Test
	public void marshalStreamResultOutputStream() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		StreamResult result = new StreamResult(os);
		marshaller.marshal(flight, result);
		String s = new String(os.toByteArray(), "UTF-8");
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, s);
	}

	@Test
	public void marshalSaxResult() throws Exception {
		MockControl handlerControl = MockControl.createStrictControl(ContentHandler.class);
		handlerControl.setDefaultMatcher(MockControl.ALWAYS_MATCHER);
		ContentHandler handlerMock = (ContentHandler) handlerControl.getMock();
		handlerMock.startDocument();
		handlerMock.startElement("", "flight", "flight", null);
		handlerMock.startElement("", "number", "number", null);
		handlerMock.characters(new char[]{'4', '2'}, 0, 2);
		handlerMock.endElement("", "number", "number");
		handlerMock.endElement("", "flight", "flight");
		handlerMock.endDocument();

		handlerControl.replay();
		SAXResult result = new SAXResult(handlerMock);
		marshaller.marshal(flight, result);
		handlerControl.verify();
	}

	@Test
	public void marshalStaxResultXMLStreamWriter() throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		StringWriter writer = new StringWriter();
		XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
		Result result = StaxUtils.createStaxResult(streamWriter);
		marshaller.marshal(flight, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
	}

	@Test
	public void marshalStaxResultXMLEventWriter() throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		StringWriter writer = new StringWriter();
		XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(writer);
		Result result = StaxUtils.createStaxResult(eventWriter);
		marshaller.marshal(flight, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
	}

	@Test
	public void converters() throws Exception {
		marshaller.setConverters(new Converter[]{new EncodedByteArrayConverter()});
		byte[] buf = new byte[]{0x1, 0x2};
		Writer writer = new StringWriter();
		marshaller.marshal(buf, new StreamResult(writer));
		assertXMLEqual("<byte-array>AQI=</byte-array>", writer.toString());
		Reader reader = new StringReader(writer.toString());
		byte[] bufResult = (byte[]) marshaller.unmarshal(new StreamSource(reader));
		assertTrue("Invalid result", Arrays.equals(buf, bufResult));
	}

	@Test
	public void useAttributesFor() throws Exception {
		marshaller.setUseAttributeForTypes(new Class[]{Long.TYPE});
		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		String expected = "<flight flightNumber=\"42\" />";
		assertXMLEqual("Marshaller does not use attributes", expected, writer.toString());
	}

	@Test
	public void useAttributesForStringClassMap() throws Exception {
		marshaller.setUseAttributeFor(Collections.singletonMap("flightNumber", Long.TYPE));
		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		String expected = "<flight flightNumber=\"42\" />";
		assertXMLEqual("Marshaller does not use attributes", expected, writer.toString());
	}

	@Test
	public void useAttributesForClassStringMap() throws Exception {
		marshaller.setUseAttributeFor(Collections.singletonMap(AnnotatedFlight.class, "flightNumber"));
		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		String expected = "<flight flightNumber=\"42\" />";
		assertXMLEqual("Marshaller does not use attributes", expected, writer.toString());
	}

	@Test
	public void omitField() throws Exception {
		marshaller.addOmittedField(AnnotatedFlight.class, "flightNumber");
		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		assertXpathNotExists("/flight/flightNumber", writer.toString());
	}

	@Test
	public void omitFields() throws Exception {
		Map omittedFieldsMap = Collections.singletonMap(AnnotatedFlight.class, "flightNumber");
		marshaller.setOmittedFields(omittedFieldsMap);
		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		assertXpathNotExists("/flight/flightNumber", writer.toString());
	}

	@Test
	public void driver() throws Exception {
		marshaller.setStreamDriver(new JettisonMappedXmlDriver());
		Writer writer = new StringWriter();
		marshaller.marshal(flight, new StreamResult(writer));
		assertEquals("Invalid result", "{\"flight\":{\"flightNumber\":42}}", writer.toString());
		Object o = marshaller.unmarshal(new StreamSource(new StringReader(writer.toString())));
		assertTrue("Unmarshalled object is not Flights", o instanceof AnnotatedFlight);
		AnnotatedFlight unflight = (AnnotatedFlight) o;
		assertNotNull("Flight is null", unflight);
		assertEquals("Number is invalid", 42L, unflight.getFlightNumber());
	}

	@Test
	public void testMarshalStreamResultWriter() throws Exception {
		marshaller.setAnnotatedClass(AnnotatedFlight.class);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		AnnotatedFlight flight = new AnnotatedFlight();
		flight.setFlightNumber(42);
		marshaller.marshal(flight, result);
		String expected = "<flight><number>42</number></flight>";
		assertXMLEqual("Marshaller writes invalid StreamResult", expected, writer.toString());
	}


}
