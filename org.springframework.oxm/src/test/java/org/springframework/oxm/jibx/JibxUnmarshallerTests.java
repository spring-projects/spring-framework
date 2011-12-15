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

package org.springframework.oxm.jibx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Ignore;

import org.springframework.oxm.AbstractUnmarshallerTests;
import org.springframework.oxm.Unmarshaller;

/**
 * @author Arjen Poutsma
 */
@org.junit.Ignore // TODO fix this issue https://gist.github.com/1174575
public class JibxUnmarshallerTests extends AbstractUnmarshallerTests {

	@Override
	protected Unmarshaller createUnmarshaller() throws Exception {
		JibxMarshaller unmarshaller = new JibxMarshaller();
		unmarshaller.setTargetClass(Flights.class);
		unmarshaller.afterPropertiesSet();
		return unmarshaller;
	}

	@Override
	protected void testFlights(Object o) {
		Flights flights = (Flights) o;
		assertNotNull("Flights is null", flights);
		assertEquals("Invalid amount of flight elements", 1, flights.sizeFlightList());
		testFlight(flights.getFlight(0));
	}

	@Override
	protected void testFlight(Object o) {
		FlightType flight = (FlightType) o;
		assertNotNull("Flight is null", flight);
		assertEquals("Number is invalid", 42L, flight.getNumber());
	}

	@Override
	@Ignore
	public void unmarshalPartialStaxSourceXmlStreamReader() throws Exception {
		// JiBX does not support reading XML fragments, hence the override here
	}

}
