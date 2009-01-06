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

package org.springframework.oxm.jaxb;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.bind.JAXBElement;
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

import junit.framework.TestCase;
import static org.easymock.EasyMock.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.jaxb2.FlightType;
import org.springframework.oxm.jaxb2.Flights;
import org.springframework.oxm.mime.MimeContainer;
import org.springframework.xml.transform.StaxSource;
import org.springframework.xml.transform.StringSource;

public class Jaxb2UnmarshallerTest extends TestCase {

    private static final String INPUT_STRING = "<tns:flights xmlns:tns=\"http://samples.springframework.org/flight\">" +
            "<tns:flight><tns:number>42</tns:number></tns:flight></tns:flights>";

    private Jaxb2Marshaller unmarshaller;

    protected void setUp() throws Exception {
        unmarshaller = new Jaxb2Marshaller();
        unmarshaller.setContextPath("org.springframework.oxm.jaxb2");
        unmarshaller.setSchema(new ClassPathResource("org/springframework/oxm/flight.xsd"));
        unmarshaller.afterPropertiesSet();
    }

    public void testUnmarshalDomSource() throws Exception {
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

    public void testUnmarshalStreamSourceReader() throws Exception {
        StreamSource source = new StreamSource(new StringReader(INPUT_STRING));
        Object flights = unmarshaller.unmarshal(source);
        testFlights(flights);
    }

    public void testUnmarshalStreamSourceInputStream() throws Exception {
        StreamSource source = new StreamSource(new ByteArrayInputStream(INPUT_STRING.getBytes("UTF-8")));
        Object flights = unmarshaller.unmarshal(source);
        testFlights(flights);
    }

    public void testUnmarshalSAXSource() throws Exception {
        XMLReader reader = XMLReaderFactory.createXMLReader();
        SAXSource source = new SAXSource(reader, new InputSource(new StringReader(INPUT_STRING)));
        Object flights = unmarshaller.unmarshal(source);
        testFlights(flights);
    }

    public void testUnmarshalStaxSourceXmlStreamReader() throws Exception {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
        StaxSource source = new StaxSource(streamReader);
        Object flights = unmarshaller.unmarshal(source);
        testFlights(flights);
    }

    public void testUnmarshalStaxSourceXmlEventReader() throws Exception {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(INPUT_STRING));
        StaxSource source = new StaxSource(eventReader);
        Object flights = unmarshaller.unmarshal(source);
        testFlights(flights);
    }

    public void testUnmarshalStaxSourceXmlStreamReaderJaxp14() throws Exception {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
        StAXSource source = new StAXSource(streamReader);
        Object flights = unmarshaller.unmarshal(source);
        testFlights(flights);
    }

    public void testUnmarshalStaxSourceXmlEventReaderJaxp14() throws Exception {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(INPUT_STRING));
        StAXSource source = new StAXSource(eventReader);
        Object flights = unmarshaller.unmarshal(source);
        testFlights(flights);
    }

    public void testMarshalAttachments() throws Exception {
        unmarshaller = new Jaxb2Marshaller();
        unmarshaller.setClassesToBeBound(new Class[]{BinaryObject.class});
        unmarshaller.setMtomEnabled(true);
        unmarshaller.afterPropertiesSet();
        MimeContainer mimeContainer = createMock(MimeContainer.class);

        Resource logo = new ClassPathResource("spring-ws.png", getClass());
        DataHandler dataHandler = new DataHandler(new FileDataSource(logo.getFile()));

        expect(mimeContainer.isXopPackage()).andReturn(true);
        expect(mimeContainer.getAttachment(
                "<6b76528d-7a9c-4def-8e13-095ab89e9bb7@http://springframework.org/spring-ws>"))
                .andReturn(dataHandler);
        expect(mimeContainer.getAttachment(
                "<99bd1592-0521-41a2-9688-a8bfb40192fb@http://springframework.org/spring-ws>"))
                .andReturn(dataHandler);
        expect(mimeContainer.getAttachment("696cfb9a-4d2d-402f-bb5c-59fa69e7f0b3@spring-ws.png"))
                .andReturn(dataHandler);
        replay(mimeContainer);
        String content = "<binaryObject xmlns='http://springframework.org/spring-ws'>" + "<bytes>" +
                "<xop:Include href='cid:6b76528d-7a9c-4def-8e13-095ab89e9bb7@http://springframework.org/spring-ws' xmlns:xop='http://www.w3.org/2004/08/xop/include'/>" +
                "</bytes>" + "<dataHandler>" +
                "<xop:Include href='cid:99bd1592-0521-41a2-9688-a8bfb40192fb@http://springframework.org/spring-ws' xmlns:xop='http://www.w3.org/2004/08/xop/include'/>" +
                "</dataHandler>" +
                "<swaDataHandler>696cfb9a-4d2d-402f-bb5c-59fa69e7f0b3@spring-ws.png</swaDataHandler>" +
                "</binaryObject>";

        Source source = new StringSource(content);
        Object result = unmarshaller.unmarshal(source, mimeContainer);
        assertTrue("Result is not a BinaryObject", result instanceof BinaryObject);
        verify(mimeContainer);
        BinaryObject object = (BinaryObject) result;
        assertNotNull("bytes property not set", object.getBytes());
        assertTrue("bytes property not set", object.getBytes().length > 0);
        assertNotNull("datahandler property not set", object.getSwaDataHandler());
    }

    private void testFlights(Object o) {
        Flights flights = (Flights) o;
        assertNotNull("Flights is null", flights);
        assertEquals("Invalid amount of flight elements", 1, flights.getFlight().size());
        testFlight(flights.getFlight().get(0));
    }

    private void testFlight(Object o) {
        FlightType flight = (FlightType) o;
        assertNotNull("Flight is null", flight);
        assertEquals("Number is invalid", 42L, flight.getNumber());
    }

    public void testUnmarshalPartialStaxSourceXmlStreamReader() throws Exception {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
        streamReader.nextTag(); // skip to flights
        streamReader.nextTag(); // skip to flight
        StaxSource source = new StaxSource(streamReader);
        JAXBElement<FlightType> element = (JAXBElement<FlightType>) unmarshaller.unmarshal(source);
        FlightType flight = element.getValue();
        testFlight(flight);
    }


}
