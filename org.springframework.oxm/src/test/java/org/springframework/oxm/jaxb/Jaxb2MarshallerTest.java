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

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.mime.MimeContainer;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.xml.StaxUtils;

public class Jaxb2MarshallerTest {

	private static final String CONTEXT_PATH = "org.springframework.oxm.jaxb";

	private static final String EXPECTED_STRING =
			"<tns:flights xmlns:tns=\"http://samples.springframework.org/flight\">" +
					"<tns:flight><tns:number>42</tns:number></tns:flight></tns:flights>";

	private Jaxb2Marshaller marshaller;

	private Flights flights;

	@Before
	public void createMarshaller() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath(CONTEXT_PATH);
		marshaller.afterPropertiesSet();
		FlightType flight = new FlightType();
		flight.setNumber(42L);
		flights = new Flights();
		flights.getFlight().add(flight);
	}

	@Test
	public void marshalDOMResult() throws Exception {
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

	@Test
	public void marshalStreamResultWriter() throws Exception {
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		marshaller.marshal(flights, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
	}

	@Test
	public void marshalStreamResultOutputStream() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		StreamResult result = new StreamResult(os);
		marshaller.marshal(flights, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING,
				new String(os.toByteArray(), "UTF-8"));
	}

	@Test
	public void marshalStaxResultXMLStreamWriter() throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		StringWriter writer = new StringWriter();
		XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
		Result result = StaxUtils.createStaxResult(streamWriter);
		marshaller.marshal(flights, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
	}

	@Test
	public void marshalStaxResultXMLEventWriter() throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		StringWriter writer = new StringWriter();
		XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(writer);
		Result result = StaxUtils.createStaxResult(eventWriter);
		marshaller.marshal(flights, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
	}

	@Test
	public void marshalStaxResultXMLStreamWriterJaxp14() throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		StringWriter writer = new StringWriter();
		XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
		StAXResult result = new StAXResult(streamWriter);
		marshaller.marshal(flights, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
	}

	@Test
	public void marshalStaxResultXMLEventWriterJaxp14() throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		StringWriter writer = new StringWriter();
		XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(writer);
		StAXResult result = new StAXResult(eventWriter);
		marshaller.marshal(flights, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
	}

	@Test
	public void properties() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath(CONTEXT_PATH);
		marshaller.setMarshallerProperties(
				Collections.singletonMap(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE));
		marshaller.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noContextPathOrClassesToBeBound() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.afterPropertiesSet();
	}

	@Test(expected = XmlMappingException.class)
	public void testInvalidContextPath() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath("ab");
		marshaller.afterPropertiesSet();
	}

	@Test(expected = XmlMappingException.class)
	public void marshalInvalidClass() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(new Class[]{FlightType.class});
		marshaller.afterPropertiesSet();
		Result result = new StreamResult(new StringWriter());
		Flights flights = new Flights();
		marshaller.marshal(flights, result);
	}

	@Test
	public void marshalSaxResult() throws Exception {
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

	@Test
	public void supportsContextPath() throws Exception {
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

	@Test
	public void supportsClassesToBeBound() throws Exception {
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

	@Test
	public void supportsPrimitives() throws Exception {
		Method primitives = getClass()
				.getDeclaredMethod("primitives", JAXBElement.class, JAXBElement.class, JAXBElement.class,
						JAXBElement.class, JAXBElement.class, JAXBElement.class, JAXBElement.class, JAXBElement.class);
		Type[] types = primitives.getGenericParameterTypes();
		for (Type type : types) {
			assertTrue("Jaxb2Marshaller does not support " + type, marshaller.supports(type));
		}
	}

	@Test
	public void supportsStandards() throws Exception {
		Method standards = getClass()
				.getDeclaredMethod("standards", JAXBElement.class, JAXBElement.class, JAXBElement.class,
						JAXBElement.class, JAXBElement.class, JAXBElement.class, JAXBElement.class, JAXBElement.class,
						JAXBElement.class, JAXBElement.class, JAXBElement.class, JAXBElement.class, JAXBElement.class,
						JAXBElement.class);
		Type[] types = standards.getGenericParameterTypes();
		for (Type type : types) {
			assertTrue("Jaxb2Marshaller does not support " + type, marshaller.supports(type));
		}
	}

	@Test
	public void marshalAttachments() throws Exception {
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
		StringWriter writer = new StringWriter();
		marshaller.marshal(object, new StreamResult(writer), mimeContainer);
		verify(mimeContainer);
		assertTrue("No XML written", writer.toString().length() > 0);
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
