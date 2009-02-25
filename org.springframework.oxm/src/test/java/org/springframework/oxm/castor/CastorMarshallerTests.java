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

package org.springframework.oxm.castor;

import javax.xml.transform.sax.SAXResult;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.AbstractMarshallerTests;
import org.springframework.oxm.Marshaller;

/**
 * @author Arjen Poutsma
 */
public class CastorMarshallerTests extends AbstractMarshallerTests {

	@Override
	protected Marshaller createMarshaller() throws Exception {
		CastorMarshaller marshaller = new CastorMarshaller();
		ClassPathResource mappingLocation = new ClassPathResource("mapping.xml", CastorMarshaller.class);
		marshaller.setMappingLocation(mappingLocation);
		marshaller.afterPropertiesSet();
		return marshaller;
	}

	@Override
	protected Object createFlights() {
		Flight flight = new Flight();
		flight.setNumber(42L);
		Flights flights = new Flights();
		flights.addFlight(flight);
		return flights;
	}

	@Test
	public void marshalSaxResult() throws Exception {
		ContentHandler handlerMock = createMock(ContentHandler.class);
		handlerMock.startDocument();
		handlerMock.startPrefixMapping("tns", "http://samples.springframework.org/flight");
		handlerMock.startElement(eq("http://samples.springframework.org/flight"), eq("flights"), eq("tns:flights"),
				isA(Attributes.class));
		handlerMock.startElement(eq("http://samples.springframework.org/flight"), eq("flight"), eq("tns:flight"),
				isA(Attributes.class));
		handlerMock.startElement(eq("http://samples.springframework.org/flight"), eq("number"), eq("tns:number"),
				isA(Attributes.class));
		handlerMock.characters(aryEq(new char[]{'4', '2'}), eq(0), eq(2));
		handlerMock.endElement("http://samples.springframework.org/flight", "number", "tns:number");
		handlerMock.endElement("http://samples.springframework.org/flight", "flight", "tns:flight");
		handlerMock.endElement("http://samples.springframework.org/flight", "flights", "tns:flights");
		handlerMock.endPrefixMapping("tns");
		handlerMock.endDocument();

		replay(handlerMock);
		SAXResult result = new SAXResult(handlerMock);
		marshaller.marshal(flights, result);
		verify(handlerMock);
	}

	@Test
	public void supports() throws Exception {
		assertTrue("CastorMarshaller does not support Flights", marshaller.supports(Flights.class));
		assertTrue("CastorMarshaller does not support Flight", marshaller.supports(Flight.class));
	}

}
