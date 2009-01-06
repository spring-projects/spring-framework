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

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;

import org.custommonkey.xmlunit.XMLTestCase;
import static org.easymock.EasyMock.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.jaxb2.FlightType;
import org.springframework.oxm.jaxb2.Flights;
import org.springframework.oxm.jaxb2.ObjectFactory;
import org.springframework.oxm.mime.MimeContainer;
import org.springframework.util.FileCopyUtils;
import org.springframework.xml.transform.StaxResult;
import org.springframework.xml.transform.StringResult;

public class Jaxb2MarshallerTest extends XMLTestCase {

    private static final String CONTEXT_PATH = "org.springframework.oxm.jaxb2";

    private static final String EXPECTED_STRING =
            "<tns:flights xmlns:tns=\"http://samples.springframework.org/flight\">" +
                    "<tns:flight><tns:number>42</tns:number></tns:flight></tns:flights>";

    private Jaxb2Marshaller marshaller;

    private Flights flights;

    protected void setUp() throws Exception {
        marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath(CONTEXT_PATH);
        marshaller.afterPropertiesSet();
        FlightType flight = new FlightType();
        flight.setNumber(42L);
        flights = new Flights();
        flights.getFlight().add(flight);
    }

    public void testMarshalDOMResult() throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        Document document = builder.newDocument();
        DOMResult domResult = new DOMResult(document);
        marshaller.marshal(flights, domResult);
        Document expected = builder.newDocument();
        Element flightsElement = expected.createElementNS("http://samples.springframework.org/flight", "tns:flights");
        expected.appendChild(flightsElement);
        Element flightElement = expected.createElementNS("http://samples.springframework.org/flight", "tns:flight");
        flightsElement.appendChild(flightElement);
        Element numberElement = expected.createElementNS("http://samples.springframework.org/flight", "tns:number");
        flightElement.appendChild(numberElement);
        Text text = expected.createTextNode("42");
        numberElement.appendChild(text);
        assertXMLEqual("Marshaller writes invalid DOMResult", expected, document);
    }

    public void testMarshalStreamResultWriter() throws Exception {
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        marshaller.marshal(flights, result);
        assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
    }

    public void testMarshalStreamResultOutputStream() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(os);
        marshaller.marshal(flights, result);
        assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING,
                new String(os.toByteArray(), "UTF-8"));
    }

    public void testMarshalStaxResultXMLStreamWriter() throws Exception {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        StringWriter writer = new StringWriter();
        XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
        StaxResult result = new StaxResult(streamWriter);
        marshaller.marshal(flights, result);
        assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
    }

    public void testMarshalStaxResultXMLEventWriter() throws Exception {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        StringWriter writer = new StringWriter();
        XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(writer);
        StaxResult result = new StaxResult(eventWriter);
        marshaller.marshal(flights, result);
        assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
    }

    public void testMarshalStaxResultXMLStreamWriterJaxp14() throws Exception {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        StringWriter writer = new StringWriter();
        XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
        StAXResult result = new StAXResult(streamWriter);
        marshaller.marshal(flights, result);
        assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
    }

    public void testMarshalStaxResultXMLEventWriterJaxp14() throws Exception {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        StringWriter writer = new StringWriter();
        XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(writer);
        StAXResult result = new StAXResult(eventWriter);
        marshaller.marshal(flights, result);
        assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
    }

    public void testProperties() throws Exception {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath(CONTEXT_PATH);
        marshaller.setMarshallerProperties(
                Collections.singletonMap(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE));
        marshaller.afterPropertiesSet();
    }

    public void testNoContextPathOrClassesToBeBound() throws Exception {
        try {
            Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
            marshaller.afterPropertiesSet();
            fail("Should have thrown an IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testInvalidContextPath() throws Exception {
        try {
            Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
            marshaller.setContextPath("ab");
            marshaller.afterPropertiesSet();
            fail("Should have thrown an XmlMappingException");
        }
        catch (XmlMappingException ex) {
        }
    }

    public void testMarshalInvalidClass() throws Exception {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(new Class[]{FlightType.class});
        marshaller.afterPropertiesSet();
        Result result = new StreamResult(new StringWriter());
        Flights flights = new Flights();
        try {
            marshaller.marshal(flights, result);
            fail("Should have thrown an MarshallingFailureException");
        }
        catch (XmlMappingException ex) {
            // expected
        }
    }

    public void testMarshalSaxResult() throws Exception {
        ContentHandler handlerMock = createStrictMock(ContentHandler.class);
        handlerMock.setDocumentLocator(isA(Locator.class));
        handlerMock.startDocument();
        handlerMock.startPrefixMapping("", "http://samples.springframework.org/flight");
        handlerMock.startElement(eq("http://samples.springframework.org/flight"), eq("flights"), eq("flights"),
                isA(Attributes.class));
        handlerMock.startElement(eq("http://samples.springframework.org/flight"), eq("flight"), eq("flight"),
                isA(Attributes.class));
        handlerMock.startElement(eq("http://samples.springframework.org/flight"), eq("number"), eq("number"),
                isA(Attributes.class));
        handlerMock.characters(isA(char[].class), eq(0), eq(2));
        handlerMock.endElement("http://samples.springframework.org/flight", "number", "number");
        handlerMock.endElement("http://samples.springframework.org/flight", "flight", "flight");
        handlerMock.endElement("http://samples.springframework.org/flight", "flights", "flights");
        handlerMock.endPrefixMapping("");
        handlerMock.endDocument();
        replay(handlerMock);

        SAXResult result = new SAXResult(handlerMock);
        marshaller.marshal(flights, result);
        verify(handlerMock);
    }

    public void testSupportsContextPath() throws Exception {
        Method createFlights = ObjectFactory.class.getDeclaredMethod("createFlights");
        assertTrue("Jaxb2Marshaller does not support Flights",
                marshaller.supports(createFlights.getGenericReturnType()));
        Method createFlight = ObjectFactory.class.getDeclaredMethod("createFlight", FlightType.class);
        assertTrue("Jaxb2Marshaller does not support JAXBElement<FlightsType>",
                marshaller.supports(createFlight.getGenericReturnType()));
        assertFalse("Jaxb2Marshaller supports non-parameterized JAXBElement", marshaller.supports(JAXBElement.class));
        JAXBElement<Jaxb2MarshallerTest> testElement =
                new JAXBElement<Jaxb2MarshallerTest>(new QName("something"), Jaxb2MarshallerTest.class, null, this);
        assertFalse("Jaxb2Marshaller supports wrong JAXBElement", marshaller.supports(testElement.getClass()));
    }

    public void testSupportsClassesToBeBound() throws Exception {
        marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(new Class[]{Flights.class, FlightType.class});
        marshaller.afterPropertiesSet();
        Method createFlights = ObjectFactory.class.getDeclaredMethod("createFlights");
        assertTrue("Jaxb2Marshaller does not support Flights",
                marshaller.supports(createFlights.getGenericReturnType()));
        Method createFlight = ObjectFactory.class.getDeclaredMethod("createFlight", FlightType.class);
        assertTrue("Jaxb2Marshaller does not support JAXBElement<FlightsType>",
                marshaller.supports(createFlight.getGenericReturnType()));
        assertFalse("Jaxb2Marshaller supports non-parameterized JAXBElement", marshaller.supports(JAXBElement.class));
        JAXBElement<Jaxb2MarshallerTest> testElement =
                new JAXBElement<Jaxb2MarshallerTest>(new QName("something"), Jaxb2MarshallerTest.class, null, this);
        assertFalse("Jaxb2Marshaller supports wrong JAXBElement", marshaller.supports(testElement.getClass()));
    }

    public void testSupportsPrimitives() throws Exception {
        Method primitives = getClass().getDeclaredMethod("primitives", JAXBElement.class, JAXBElement.class,
                JAXBElement.class, JAXBElement.class, JAXBElement.class, JAXBElement.class, JAXBElement.class,
                JAXBElement.class);
        Type[] types = primitives.getGenericParameterTypes();
        for (int i = 0; i < types.length; i++) {
            ParameterizedType type = (ParameterizedType) types[i];
            assertTrue("Jaxb2Marshaller does not support " + type, marshaller.supports(types[i]));
        }
    }

    public void testSupportsStandards() throws Exception {
        Method standards = getClass().getDeclaredMethod("standards", JAXBElement.class, JAXBElement.class,
                JAXBElement.class, JAXBElement.class, JAXBElement.class, JAXBElement.class, JAXBElement.class,
                JAXBElement.class, JAXBElement.class, JAXBElement.class, JAXBElement.class, JAXBElement.class,
                JAXBElement.class, JAXBElement.class);
        Type[] types = standards.getGenericParameterTypes();
        for (int i = 0; i < types.length; i++) {
            ParameterizedType type = (ParameterizedType) types[i];
            assertTrue("Jaxb2Marshaller does not support " + type, marshaller.supports(types[i]));
        }
    }

    public void testMarshalAttachments() throws Exception {
        marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(new Class[]{BinaryObject.class});
        marshaller.setMtomEnabled(true);
        marshaller.afterPropertiesSet();
        MimeContainer mimeContainer = createMock(MimeContainer.class);

        Resource logo = new ClassPathResource("spring-ws.png", getClass());
        DataHandler dataHandler = new DataHandler(new FileDataSource(logo.getFile()));

        expect(mimeContainer.convertToXopPackage()).andReturn(true);
        mimeContainer.addAttachment(isA(String.class), isA(DataHandler.class));
        expectLastCall().times(3);

        replay(mimeContainer);
        byte[] bytes = FileCopyUtils.copyToByteArray(logo.getInputStream());
        BinaryObject object = new BinaryObject(bytes, dataHandler);
        Result result = new StringResult();
        marshaller.marshal(object, result, mimeContainer);
        verify(mimeContainer);
        assertTrue("No XML written", result.toString().length() > 0);
    }

    private void primitives(JAXBElement<Boolean> bool,
                            JAXBElement<Byte> aByte,
                            JAXBElement<Short> aShort,
                            JAXBElement<Integer> anInteger,
                            JAXBElement<Long> aLong,
                            JAXBElement<Float> aFloat,
                            JAXBElement<Double> aDouble,
                            JAXBElement<byte[]> byteArray) {
    }

    private void standards(JAXBElement<String> string,
                           JAXBElement<BigInteger> integer,
                           JAXBElement<BigDecimal> decimal,
                           JAXBElement<Calendar> calendar,
                           JAXBElement<Date> date,
                           JAXBElement<QName> qName,
                           JAXBElement<URI> uri,
                           JAXBElement<XMLGregorianCalendar> xmlGregorianCalendar,
                           JAXBElement<Duration> duration,
                           JAXBElement<Object> object,
                           JAXBElement<Image> image,
                           JAXBElement<DataHandler> dataHandler,
                           JAXBElement<Source> source,
                           JAXBElement<UUID> uuid) {
    }
}
