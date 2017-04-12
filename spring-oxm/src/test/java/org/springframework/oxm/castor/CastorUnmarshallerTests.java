/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.oxm.castor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.AbstractUnmarshallerTests;
import org.springframework.oxm.MarshallingException;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 * @author Jakub Narloch
 * @author Sam Brannen
 */
public class CastorUnmarshallerTests extends AbstractUnmarshallerTests<CastorMarshaller> {

	/**
	 * Represents the xml with additional attribute that is not mapped in Castor config.
	 */
	protected static final String EXTRA_ATTRIBUTES_STRING =
			"<tns:flights xmlns:tns=\"http://samples.springframework.org/flight\">" +
			"<tns:flight status=\"canceled\"><tns:number>42</tns:number></tns:flight></tns:flights>";

	/**
	 * Represents the xml with additional element that is not mapped in Castor config.
	 */
	protected static final String EXTRA_ELEMENTS_STRING =
			"<tns:flights xmlns:tns=\"http://samples.springframework.org/flight\">" +
			"<tns:flight><tns:number>42</tns:number><tns:date>2011-06-14</tns:date>" +
			"</tns:flight></tns:flights>";


	@Override
	protected CastorMarshaller createUnmarshaller() throws Exception {
		CastorMarshaller marshaller = new CastorMarshaller();
		ClassPathResource mappingLocation = new ClassPathResource("mapping.xml", CastorMarshaller.class);
		marshaller.setMappingLocation(mappingLocation);
		marshaller.afterPropertiesSet();
		return marshaller;
	}

	@Override
	protected void testFlights(Object o) {
		Flights flights = (Flights) o;
		assertNotNull("Flights is null", flights);
		assertEquals("Invalid amount of flight elements", 1, flights.getFlightCount());
		testFlight(flights.getFlight()[0]);
	}

	@Override
	protected void testFlight(Object o) {
		Flight flight = (Flight) o;
		assertNotNull("Flight is null", flight);
		assertThat("Number is invalid", flight.getNumber(), equalTo(42L));
	}


	@Test
	public void unmarshalTargetClass() throws Exception {
		CastorMarshaller unmarshaller = new CastorMarshaller();
		unmarshaller.setTargetClasses(new Class[] {Flights.class});
		unmarshaller.afterPropertiesSet();
		StreamSource source = new StreamSource(new ByteArrayInputStream(INPUT_STRING.getBytes("UTF-8")));
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	public void setBothTargetClassesAndMapping() throws IOException {
		CastorMarshaller unmarshaller = new CastorMarshaller();
		unmarshaller.setMappingLocation(new ClassPathResource("order-mapping.xml", CastorMarshaller.class));
		unmarshaller.setTargetClasses(new Class[] {Order.class});
		unmarshaller.afterPropertiesSet();

		String xml = "<order>" +
				"<order-item id=\"1\" quantity=\"15\"/>" +
				"<order-item id=\"3\" quantity=\"20\"/>" +
				"</order>";

		Order order = (Order) unmarshaller.unmarshal(new StreamSource(new StringReader(xml)));
		assertEquals("Invalid amount of items", 2, order.getOrderItemCount());
		OrderItem item = order.getOrderItem(0);
		assertEquals("Invalid items", "1", item.getId());
		assertThat("Invalid items", item.getQuantity(), equalTo(15));
		item = order.getOrderItem(1);
		assertEquals("Invalid items", "3", item.getId());
		assertThat("Invalid items", item.getQuantity(), equalTo(20));
	}

	@Test
	public void whitespacePreserveTrue() throws Exception {
		unmarshaller.setWhitespacePreserve(true);
		Object result = unmarshalFlights();
		testFlights(result);
	}

	@Test
	public void whitespacePreserveFalse() throws Exception {
		unmarshaller.setWhitespacePreserve(false);
		Object result = unmarshalFlights();
		testFlights(result);
	}

	@Test
	public void ignoreExtraAttributesTrue() throws Exception {
		unmarshaller.setIgnoreExtraAttributes(true);
		Object result = unmarshal(EXTRA_ATTRIBUTES_STRING);
		testFlights(result);
	}

	@Test(expected = MarshallingException.class)
	public void ignoreExtraAttributesFalse() throws Exception {
		unmarshaller.setIgnoreExtraAttributes(false);
		unmarshal(EXTRA_ATTRIBUTES_STRING);
	}

	@Test
	@Ignore("Not working yet")
	public void ignoreExtraElementsTrue() throws Exception {
		unmarshaller.setIgnoreExtraElements(true);
		unmarshaller.setValidating(false);
		Object result = unmarshal(EXTRA_ELEMENTS_STRING);
		testFlights(result);
	}

	@Test(expected = MarshallingException.class)
	public void ignoreExtraElementsFalse() throws Exception {
		unmarshaller.setIgnoreExtraElements(false);
		unmarshal(EXTRA_ELEMENTS_STRING);
	}

	@Test
	public void rootObject() throws Exception {
		Flights flights = new Flights();
		unmarshaller.setRootObject(flights);
		Object result = unmarshalFlights();
		testFlights(result);
		assertSame("Result Flights is different object.", flights, result);
	}

	@Test
	public void clearCollectionsTrue() throws Exception {
		Flights flights = new Flights();
		flights.setFlight(new Flight[]{new Flight()});
		unmarshaller.setRootObject(flights);
		unmarshaller.setClearCollections(true);
		Object result = unmarshalFlights();

		assertSame("Result Flights is different object.", flights, result);
		assertEquals("Result Flights has incorrect number of Flight.", 1, ((Flights) result).getFlightCount());
		testFlights(result);
	}

	@Test
	@Ignore("Fails on the build server for some reason")
	public void clearCollectionsFalse() throws Exception {
		Flights flights = new Flights();
		flights.setFlight(new Flight[] {new Flight(), null});
		unmarshaller.setRootObject(flights);
		unmarshaller.setClearCollections(false);
		Object result = unmarshalFlights();

		assertSame("Result Flights is different object.", flights, result);
		assertEquals("Result Flights has incorrect number of Flight.", 3, ((Flights) result).getFlightCount());
		assertNull("Flight shouldn't have number.", flights.getFlight(0).getNumber());
		assertNull("Null Flight was expected.", flights.getFlight()[1]);
		testFlight(flights.getFlight()[2]);
	}

	@Test
	public void unmarshalStreamSourceWithXmlOptions() throws Exception {
		final AtomicReference<XMLReader> result = new AtomicReference<>();
		CastorMarshaller marshaller = new CastorMarshaller() {
			@Override
			protected Object unmarshalSaxReader(XMLReader xmlReader, InputSource inputSource) {
				result.set(xmlReader);
				return null;
			}
		};

		// 1. external-general-entities and dtd support disabled (default)
		marshaller.unmarshal(new StreamSource("1"));
		assertNotNull(result.get());
		assertEquals(true, result.get().getFeature("http://apache.org/xml/features/disallow-doctype-decl"));
		assertEquals(false, result.get().getFeature("http://xml.org/sax/features/external-general-entities"));

		// 2. external-general-entities and dtd support enabled
		result.set(null);
		marshaller.setSupportDtd(true);
		marshaller.setProcessExternalEntities(true);
		marshaller.unmarshal(new StreamSource("1"));
		assertNotNull(result.get());
		assertEquals(false, result.get().getFeature("http://apache.org/xml/features/disallow-doctype-decl"));
		assertEquals(true, result.get().getFeature("http://xml.org/sax/features/external-general-entities"));
	}

	@Test
	public void unmarshalSaxSourceWithXmlOptions() throws Exception {
		final AtomicReference<XMLReader> result = new AtomicReference<>();
		CastorMarshaller marshaller = new CastorMarshaller() {
			@Override
			protected Object unmarshalSaxReader(XMLReader xmlReader, InputSource inputSource) {
				result.set(xmlReader);
				return null;
			}
		};

		// 1. external-general-entities and dtd support disabled (default)
		marshaller.unmarshal(new SAXSource(new InputSource("1")));
		assertNotNull(result.get());
		assertEquals(true, result.get().getFeature("http://apache.org/xml/features/disallow-doctype-decl"));
		assertEquals(false, result.get().getFeature("http://xml.org/sax/features/external-general-entities"));

		// 2. external-general-entities and dtd support enabled
		result.set(null);
		marshaller.setSupportDtd(true);
		marshaller.setProcessExternalEntities(true);
		marshaller.unmarshal(new SAXSource(new InputSource("1")));
		assertNotNull(result.get());
		assertEquals(false, result.get().getFeature("http://apache.org/xml/features/disallow-doctype-decl"));
		assertEquals(true, result.get().getFeature("http://xml.org/sax/features/external-general-entities"));
	}

	private Object unmarshalFlights() throws Exception {
		return unmarshal(INPUT_STRING);
	}

	private Object unmarshal(String xml) throws Exception {
		StreamSource source = new StreamSource(new StringReader(xml));
		return unmarshaller.unmarshal(source);
	}

}
