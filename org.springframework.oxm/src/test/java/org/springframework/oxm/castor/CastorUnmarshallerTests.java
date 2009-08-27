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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.transform.stream.StreamSource;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.AbstractUnmarshallerTests;
import org.springframework.oxm.Unmarshaller;

/**
 * @author Arjen Poutsma
 */
public class CastorUnmarshallerTests extends AbstractUnmarshallerTests {

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
		assertEquals("Number is invalid", 42L, (long) flight.getNumber());
	}

	@Override
	protected Unmarshaller createUnmarshaller() throws Exception {
		CastorMarshaller marshaller = new CastorMarshaller();
		ClassPathResource mappingLocation = new ClassPathResource("mapping.xml", CastorMarshaller.class);
		marshaller.setMappingLocation(mappingLocation);
		marshaller.afterPropertiesSet();
		return marshaller;
	}

	@Test
	public void unmarshalTargetClass() throws Exception {
		CastorMarshaller unmarshaller = new CastorMarshaller();
		unmarshaller.setTargetClasses(new Class[] { Flights.class } );
		unmarshaller.afterPropertiesSet();
		StreamSource source = new StreamSource(new ByteArrayInputStream(INPUT_STRING.getBytes("UTF-8")));
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);
	}

	@Test
	public void testSetBothTargetClassesAndMapping() throws IOException {
		CastorMarshaller unmarshaller = new CastorMarshaller();
		unmarshaller.setMappingLocation(new ClassPathResource("order-mapping.xml", CastorMarshaller.class));
		unmarshaller.setTargetClasses(new Class[] { Order.class } );
		unmarshaller.afterPropertiesSet();

		String xml = "<order>" +
				"<order-item id=\"1\" quantity=\"15\"/>" +
				"<order-item id=\"3\" quantity=\"20\"/>" +
				"</order>";

		Order order = (Order) unmarshaller.unmarshal(new StreamSource(new StringReader(xml)));
		assertEquals("Invalid amount of items", 2, order.getOrderItemCount());
		OrderItem item = order.getOrderItem(0);
		assertEquals("Invalid items", "1", item.getId());
		assertEquals("Invalid items", 15, (int)item.getQuantity());
		item = order.getOrderItem(1);
		assertEquals("Invalid items", "3", item.getId());
		assertEquals("Invalid items", 20, (int)item.getQuantity());
	}


}
