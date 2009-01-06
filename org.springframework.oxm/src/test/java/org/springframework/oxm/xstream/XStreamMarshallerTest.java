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
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.extended.EncodedByteArrayConverter;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import org.custommonkey.xmlunit.XMLTestCase;
import org.easymock.MockControl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.ContentHandler;

import org.springframework.xml.transform.StaxResult;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

public class XStreamMarshallerTest extends XMLTestCase {

    private static final String EXPECTED_STRING = "<flight><flightNumber>42</flightNumber></flight>";

    private XStreamMarshaller marshaller;

    private Flight flight;

    protected void setUp() throws Exception {
        marshaller = new XStreamMarshaller();
        Properties aliases = new Properties();
        aliases.setProperty("flight", Flight.class.getName());
        marshaller.setAliases(aliases);
        flight = new Flight();
        flight.setFlightNumber(42L);
    }

    public void testMarshalDOMResult() throws Exception {
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
    public void testMarshalDOMResultToExistentDocument() throws Exception {
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

    public void testMarshalStreamResultWriter() throws Exception {
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        marshaller.marshal(flight, result);
        assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
    }

    public void testMarshalStreamResultOutputStream() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(os);
        marshaller.marshal(flight, result);
        String s = new String(os.toByteArray(), "UTF-8");
        assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, s);
    }

    public void testMarshalSaxResult() throws Exception {
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

    public void testMarshalStaxResultXMLStreamWriter() throws Exception {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        StringWriter writer = new StringWriter();
        XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
        StaxResult result = new StaxResult(streamWriter);
        marshaller.marshal(flight, result);
        assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
    }

    public void testMarshalStaxResultXMLEventWriter() throws Exception {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        StringWriter writer = new StringWriter();
        XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(writer);
        StaxResult result = new StaxResult(eventWriter);
        marshaller.marshal(flight, result);
        assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
    }

    public void testConverters() throws Exception {
        marshaller.setConverters(new Converter[]{new EncodedByteArrayConverter()});
        byte[] buf = new byte[]{0x1, 0x2};
        StringResult result = new StringResult();
        marshaller.marshal(buf, result);
        assertXMLEqual("<byte-array>AQI=</byte-array>", result.toString());
        StringSource source = new StringSource(result.toString());
        byte[] bufResult = (byte[]) marshaller.unmarshal(source);
        assertTrue("Invalid result", Arrays.equals(buf, bufResult));
    }

    public void testUseAttributesFor() throws Exception {
        marshaller.setUseAttributeForTypes(new Class[]{Long.TYPE});
        StringResult result = new StringResult();
        marshaller.marshal(flight, result);
        String expected = "<flight flightNumber=\"42\" />";
        assertXMLEqual("Marshaller does not use attributes", expected, result.toString());
    }

    public void testUseAttributesForStringClassMap() throws Exception {
        marshaller.setUseAttributeFor(Collections.singletonMap("flightNumber", Long.TYPE));
        StringResult result = new StringResult();
        marshaller.marshal(flight, result);
        String expected = "<flight flightNumber=\"42\" />";
        assertXMLEqual("Marshaller does not use attributes", expected, result.toString());
    }

    public void testUseAttributesForClassStringMap() throws Exception {
        marshaller.setUseAttributeFor(Collections.singletonMap(Flight.class, "flightNumber"));
        StringResult result = new StringResult();
        marshaller.marshal(flight, result);
        String expected = "<flight flightNumber=\"42\" />";
        assertXMLEqual("Marshaller does not use attributes", expected, result.toString());
    }

    public void testOmitField() throws Exception {
        marshaller.addOmittedField(Flight.class, "flightNumber");
        StringResult result = new StringResult();
        marshaller.marshal(flight, result);
        assertXpathNotExists("/flight/flightNumber", result.toString());
    }

    public void testOmitFields() throws Exception {
        Map omittedFieldsMap = Collections.singletonMap(Flight.class, "flightNumber");
        marshaller.setOmittedFields(omittedFieldsMap);
        StringResult result = new StringResult();
        marshaller.marshal(flight, result);
        assertXpathNotExists("/flight/flightNumber", result.toString());
    }

    public void testDriver() throws Exception {
        marshaller.setStreamDriver(new JettisonMappedXmlDriver());
        StringResult result = new StringResult();
        marshaller.marshal(flight, result);
        assertEquals("Invalid result", "{\"flight\":{\"flightNumber\":\"42\"}}", result.toString());
        Object o = marshaller.unmarshal(new StringSource(result.toString()));
        assertTrue("Unmarshalled object is not Flights", o instanceof Flight);
        Flight unflight = (Flight) o;
        assertNotNull("Flight is null", unflight);
        assertEquals("Number is invalid", 42L, unflight.getFlightNumber());
    }

}
