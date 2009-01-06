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

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.springframework.xml.transform.StaxSource;

public class XStreamUnmarshallerTest extends TestCase {

    protected static final String INPUT_STRING = "<flight><flightNumber>42</flightNumber></flight>";

    private XStreamMarshaller unmarshaller;

    protected void setUp() throws Exception {
        unmarshaller = new XStreamMarshaller();
        Properties aliases = new Properties();
        aliases.setProperty("flight", Flight.class.getName());
        unmarshaller.setAliases(aliases);
    }

    private void testFlight(Object o) {
        assertTrue("Unmarshalled object is not Flights", o instanceof Flight);
        Flight flight = (Flight) o;
        assertNotNull("Flight is null", flight);
        assertEquals("Number is invalid", 42L, flight.getFlightNumber());
    }

    public void testUnmarshalDomSource() throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(INPUT_STRING)));
        DOMSource source = new DOMSource(document);
        Object flight = unmarshaller.unmarshal(source);
        testFlight(flight);
    }

    public void testUnmarshalStaxSourceXmlStreamReader() throws Exception {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
        StaxSource source = new StaxSource(streamReader);
        Object flights = unmarshaller.unmarshal(source);
        testFlight(flights);
    }

    public void testUnmarshalStreamSourceInputStream() throws Exception {
        StreamSource source = new StreamSource(new ByteArrayInputStream(INPUT_STRING.getBytes("UTF-8")));
        Object flights = unmarshaller.unmarshal(source);
        testFlight(flights);
    }

    public void testUnmarshalStreamSourceReader() throws Exception {
        StreamSource source = new StreamSource(new StringReader(INPUT_STRING));
        Object flights = unmarshaller.unmarshal(source);
        testFlight(flights);
    }
}

