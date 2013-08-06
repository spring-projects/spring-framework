/*
 * Copyright 2002-2013 the original author or authors.
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

import java.io.ByteArrayInputStream;

import javax.xml.transform.stream.StreamSource;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.oxm.AbstractUnmarshallerTests;
import org.springframework.oxm.Unmarshaller;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 *
 * NOTE: These tests fail under Eclipse/IDEA because JiBX binding does
 * not occur by default. The Gradle build should succeed, however.
 */
public class JibxUnmarshallerTests extends AbstractUnmarshallerTests {

	protected static final String INPUT_STRING_WITH_SPECIAL_CHARACTERS =
			"<tns:flights xmlns:tns=\"http://samples.springframework.org/flight\">" +
					"<tns:flight><tns:airline>Air Libert\u00e9</tns:airline><tns:number>42</tns:number></tns:flight></tns:flights>";

	@BeforeClass
	public static void compilerAssumptions() {
		Assume.group(TestGroup.CUSTOM_COMPILATION);
	}

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

	@Test
	public void unmarshalStreamSourceInputStreamUsingNonDefaultEncoding() throws Exception {
		String encoding = "ISO-8859-1";
		((JibxMarshaller)unmarshaller).setEncoding(encoding);

		StreamSource source = new StreamSource(new ByteArrayInputStream(INPUT_STRING_WITH_SPECIAL_CHARACTERS.getBytes(encoding)));
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);

		FlightType flight = ((Flights)flights).getFlight(0);
		assertEquals("Airline is invalid", "Air Libert\u00e9", flight.getAirline());
	}

}
