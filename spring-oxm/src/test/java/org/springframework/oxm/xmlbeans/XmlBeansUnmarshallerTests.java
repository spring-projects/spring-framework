/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.StringReader;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.oxm.AbstractUnmarshallerTests;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.ValidationFailureException;
import org.springframework.samples.flight.FlightDocument;
import org.springframework.samples.flight.FlightType;
import org.springframework.samples.flight.FlightsDocument;
import org.springframework.util.xml.StaxUtils;

/**
 * @author Arjen Poutsma
 */
public class XmlBeansUnmarshallerTests extends AbstractUnmarshallerTests {

	@Override
	protected Unmarshaller createUnmarshaller() throws Exception {
		return new XmlBeansMarshaller();
	}

	@Override
	protected void testFlights(Object o) {
		FlightsDocument flightsDocument = (FlightsDocument) o;
		assertNotNull("FlightsDocument is null", flightsDocument);
		FlightsDocument.Flights flights = flightsDocument.getFlights();
		assertEquals("Invalid amount of flight elements", 1, flights.sizeOfFlightArray());
		testFlight(flights.getFlightArray(0));
	}

	@Override
	protected void testFlight(Object o) {
		FlightType flight = null;
		if (o instanceof FlightType) {
			flight = (FlightType) o;
		}
		else if (o instanceof FlightDocument) {
			FlightDocument flightDocument = (FlightDocument) o;
			flight = flightDocument.getFlight();
		}
		assertNotNull("Flight is null", flight);
		assertEquals("Number is invalid", 42L, flight.getNumber());
	}

	@Override
	public void unmarshalPartialStaxSourceXmlStreamReader() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
		streamReader.nextTag(); // skip to flights
		assertEquals("Invalid element", new QName("http://samples.springframework.org/flight", "flights"),
				streamReader.getName());
		streamReader.nextTag(); // skip to flight
		assertEquals("Invalid element", new QName("http://samples.springframework.org/flight", "flight"),
				streamReader.getName());
		Source source = StaxUtils.createStaxSource(streamReader);
		Object flight = unmarshaller.unmarshal(source);
		testFlight(flight);
	}

	@Test(expected = ValidationFailureException.class)
	public void testValidate() throws Exception {
		((XmlBeansMarshaller) unmarshaller).setValidating(true);
		String invalidInput = "<tns:flights xmlns:tns=\"http://samples.springframework.org/flight\">" +
				"<tns:flight><tns:number>abc</tns:number></tns:flight></tns:flights>";
		unmarshaller.unmarshal(new StreamSource(new StringReader(invalidInput)));
	}

}
