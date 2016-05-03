/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.oxm.xmlbeans;

import java.io.ByteArrayOutputStream;
import javax.xml.transform.stream.StreamResult;

import org.apache.xmlbeans.XmlObject;

import org.junit.Test;

import org.springframework.oxm.AbstractMarshallerTests;
import org.springframework.samples.flight.FlightType;
import org.springframework.samples.flight.FlightsDocument;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
public class XmlBeansMarshallerTests extends
		AbstractMarshallerTests<org.springframework.oxm.xmlbeans.XmlBeansMarshaller> {

	@Override
	protected XmlBeansMarshaller createMarshaller() throws Exception {
		return new XmlBeansMarshaller();
	}

	@Override
	protected Object createFlights() {
		FlightsDocument flightsDocument = FlightsDocument.Factory.newInstance();
		FlightsDocument.Flights flights = flightsDocument.addNewFlights();
		FlightType flightType = flights.addNewFlight();
		flightType.setNumber(42L);
		return flightsDocument;
	}

	@Test(expected = ClassCastException.class)
	public void marshalNonXmlObject() throws Exception {
		marshaller.marshal(new Object(), new StreamResult(new ByteArrayOutputStream()));
	}

	@Test
	public void supports() throws Exception {
		assertTrue("XmlBeansMarshaller does not support XmlObject", marshaller.supports(XmlObject.class));
		assertFalse("XmlBeansMarshaller supports other objects", marshaller.supports(Object.class));
		assertTrue("XmlBeansMarshaller does not support FlightsDocument", marshaller.supports(FlightsDocument.class));
		assertTrue("XmlBeansMarshaller does not support Flights", marshaller.supports(FlightsDocument.Flights.class));
		assertTrue("XmlBeansMarshaller does not support FlightType", marshaller.supports(FlightType.class));
	}

}
