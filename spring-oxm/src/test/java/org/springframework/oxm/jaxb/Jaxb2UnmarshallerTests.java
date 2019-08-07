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

package org.springframework.oxm.jaxb;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.bind.JAXBElement;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.AbstractUnmarshallerTests;
import org.springframework.oxm.jaxb.test.FlightType;
import org.springframework.oxm.jaxb.test.Flights;
import org.springframework.oxm.mime.MimeContainer;
import org.springframework.util.xml.StaxUtils;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Arjen Poutsma
 * @author Biju Kunjummen
 * @author Sam Brannen
 */
public class Jaxb2UnmarshallerTests extends AbstractUnmarshallerTests<Jaxb2Marshaller> {

	private static final String INPUT_STRING = "<tns:flights xmlns:tns=\"http://samples.springframework.org/flight\">" +
			"<tns:flight><tns:number>42</tns:number></tns:flight></tns:flights>";

	@Override
	protected Jaxb2Marshaller createUnmarshaller() throws Exception {
		Jaxb2Marshaller unmarshaller = new Jaxb2Marshaller();
		unmarshaller.setContextPath("org.springframework.oxm.jaxb.test");
		unmarshaller.setSchema(new ClassPathResource("org/springframework/oxm/flight.xsd"));
		unmarshaller.afterPropertiesSet();
		return unmarshaller;
	}

	@Override
	protected void testFlights(Object o) {
		Flights flights = (Flights) o;
		assertNotNull("Flights is null", flights);
		assertEquals("Invalid amount of flight elements", 1, flights.getFlight().size());
		testFlight(flights.getFlight().get(0));
	}

	@Override
	protected void testFlight(Object o) {
		FlightType flight = (FlightType) o;
		assertNotNull("Flight is null", flight);
		assertEquals("Number is invalid", 42L, flight.getNumber());
	}

	@Test
	public void marshalAttachments() throws Exception {
		unmarshaller = new Jaxb2Marshaller();
		unmarshaller.setClassesToBeBound(BinaryObject.class);
		unmarshaller.setMtomEnabled(true);
		unmarshaller.afterPropertiesSet();
		MimeContainer mimeContainer = mock(MimeContainer.class);

		Resource logo = new ClassPathResource("spring-ws.png", getClass());
		DataHandler dataHandler = new DataHandler(new FileDataSource(logo.getFile()));

		given(mimeContainer.isXopPackage()).willReturn(true);
		given(mimeContainer.getAttachment("<6b76528d-7a9c-4def-8e13-095ab89e9bb7@http://springframework.org/spring-ws>")).willReturn(dataHandler);
		given(mimeContainer.getAttachment("<99bd1592-0521-41a2-9688-a8bfb40192fb@http://springframework.org/spring-ws>")).willReturn(dataHandler);
		given(mimeContainer.getAttachment("696cfb9a-4d2d-402f-bb5c-59fa69e7f0b3@spring-ws.png")).willReturn(dataHandler);
		String content = "<binaryObject xmlns='http://springframework.org/spring-ws'>" + "<bytes>" +
				"<xop:Include href='cid:6b76528d-7a9c-4def-8e13-095ab89e9bb7@http://springframework.org/spring-ws' xmlns:xop='http://www.w3.org/2004/08/xop/include'/>" +
				"</bytes>" + "<dataHandler>" +
				"<xop:Include href='cid:99bd1592-0521-41a2-9688-a8bfb40192fb@http://springframework.org/spring-ws' xmlns:xop='http://www.w3.org/2004/08/xop/include'/>" +
				"</dataHandler>" +
				"<swaDataHandler>696cfb9a-4d2d-402f-bb5c-59fa69e7f0b3@spring-ws.png</swaDataHandler>" +
				"</binaryObject>";

		StringReader reader = new StringReader(content);
		Object result = unmarshaller.unmarshal(new StreamSource(reader), mimeContainer);
		assertTrue("Result is not a BinaryObject", result instanceof BinaryObject);
		BinaryObject object = (BinaryObject) result;
		assertNotNull("bytes property not set", object.getBytes());
		assertTrue("bytes property not set", object.getBytes().length > 0);
		assertNotNull("datahandler property not set", object.getSwaDataHandler());
	}

	@Test
	@Override
	@SuppressWarnings("unchecked")
	public void unmarshalPartialStaxSourceXmlStreamReader() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
		streamReader.nextTag(); // skip to flights
		streamReader.nextTag(); // skip to flight
		Source source = StaxUtils.createStaxSource(streamReader);
		JAXBElement<FlightType> element = (JAXBElement<FlightType>) unmarshaller.unmarshal(source);
		FlightType flight = element.getValue();
		testFlight(flight);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void unmarshalAnXmlReferingToAWrappedXmlElementDecl() throws Exception {
		// SPR-10714
		unmarshaller = new Jaxb2Marshaller();
		unmarshaller.setPackagesToScan(new String[] { "org.springframework.oxm.jaxb" });
		unmarshaller.afterPropertiesSet();
		Source source = new StreamSource(new StringReader(
				"<brand-airplane><name>test</name></brand-airplane>"));
		JAXBElement<Airplane> airplane = (JAXBElement<Airplane>) unmarshaller.unmarshal(source);
		assertEquals("Unmarshalling via explicit @XmlRegistry tag should return correct type",
				"test", airplane.getValue().getName());
	}

	@Test
	public void unmarshalFile() throws IOException {
		Resource resource = new ClassPathResource("jaxb2.xml", getClass());
		File file = resource.getFile();

		Flights f = (Flights) unmarshaller.unmarshal(new StreamSource(file));
		testFlights(f);
	}

}
