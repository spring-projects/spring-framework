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

package org.springframework.oxm.jaxb;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Collections;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Result;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.AbstractMarshallerTests;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.mime.MimeContainer;
import org.springframework.util.FileCopyUtils;

public class Jaxb2MarshallerTests extends AbstractMarshallerTests {

	private static final String CONTEXT_PATH = "org.springframework.oxm.jaxb";

	private Jaxb2Marshaller marshaller;

	private Flights flights;

	@Override
	public Marshaller createMarshaller() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath(CONTEXT_PATH);
		marshaller.afterPropertiesSet();
		return marshaller;
	}

	@Override
	protected Object createFlights() {
		FlightType flight = new FlightType();
		flight.setNumber(42L);
		flights = new Flights();
		flights.getFlight().add(flight);
		return flights;
	}

	@Test
	public void marshalSAXResult() throws Exception {
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
	public void properties() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath(CONTEXT_PATH);
		marshaller.setMarshallerProperties(
				Collections.<String, Object>singletonMap(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT,
						Boolean.TRUE));
		marshaller.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noContextPathOrClassesToBeBound() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.afterPropertiesSet();
	}

	@Test(expected = JAXBException.class)
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
	public void supportsContextPath() throws Exception {
		Method createFlights = ObjectFactory.class.getDeclaredMethod("createFlights");
		assertTrue("Jaxb2Marshaller does not support Flights", marshaller.supports(createFlights.getReturnType()));
		Method createFlight = ObjectFactory.class.getDeclaredMethod("createFlight", FlightType.class);
		assertTrue("Jaxb2Marshaller does not support JAXBElement<FlightsType>",
				marshaller.supports(createFlight.getReturnType()));
	}

	@Test
	public void supportsClassesToBeBound() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(new Class[]{Flights.class, FlightType.class});
		marshaller.afterPropertiesSet();
		Method createFlights = ObjectFactory.class.getDeclaredMethod("createFlights");
		assertTrue("Jaxb2Marshaller does not support Flights", marshaller.supports(createFlights.getReturnType()));
		Method createFlight = ObjectFactory.class.getDeclaredMethod("createFlight", FlightType.class);
		assertTrue("Jaxb2Marshaller does not support JAXBElement<FlightsType>",
				marshaller.supports(createFlight.getReturnType()));
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
}
