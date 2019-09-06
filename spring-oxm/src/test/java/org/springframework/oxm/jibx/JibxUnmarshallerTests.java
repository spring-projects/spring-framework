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

package org.springframework.oxm.jibx;

import java.io.ByteArrayInputStream;

import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;

import org.springframework.oxm.AbstractUnmarshallerTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.JRE.JAVA_8;

/**
 * NOTE: These tests fail under Eclipse/IDEA because JiBX binding does
 * not occur by default. The Gradle build should succeed, however.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
@Deprecated
@EnabledOnJre(JAVA_8) // JiBX compiler is currently not compatible with JDK 9
public class JibxUnmarshallerTests extends AbstractUnmarshallerTests<JibxMarshaller> {

	protected static final String INPUT_STRING_WITH_SPECIAL_CHARACTERS =
			"<tns:flights xmlns:tns=\"http://samples.springframework.org/flight\">" +
					"<tns:flight><tns:airline>Air Libert\u00e9</tns:airline><tns:number>42</tns:number></tns:flight></tns:flights>";


	@Override
	protected JibxMarshaller createUnmarshaller() throws Exception {
		JibxMarshaller unmarshaller = new JibxMarshaller();
		unmarshaller.setTargetClass(Flights.class);
		unmarshaller.afterPropertiesSet();
		return unmarshaller;
	}

	@Override
	protected void testFlights(Object o) {
		Flights flights = (Flights) o;
		assertThat(flights).as("Flights is null").isNotNull();
		assertThat(flights.sizeFlightList()).as("Invalid amount of flight elements").isEqualTo(1);
		testFlight(flights.getFlight(0));
	}

	@Override
	protected void testFlight(Object o) {
		FlightType flight = (FlightType) o;
		assertThat(flight).as("Flight is null").isNotNull();
		assertThat(flight.getNumber()).as("Number is invalid").isEqualTo(42L);
	}


	@Test
	@Override
	public void unmarshalPartialStaxSourceXmlStreamReader() throws Exception {
		// JiBX does not support reading XML fragments, hence the override here
	}

	@Test
	public void unmarshalStreamSourceInputStreamUsingNonDefaultEncoding() throws Exception {
		String encoding = "ISO-8859-1";
		unmarshaller.setEncoding(encoding);

		StreamSource source = new StreamSource(new ByteArrayInputStream(INPUT_STRING_WITH_SPECIAL_CHARACTERS.getBytes(encoding)));
		Object flights = unmarshaller.unmarshal(source);
		testFlights(flights);

		FlightType flight = ((Flights)flights).getFlight(0);
		assertThat(flight.getAirline()).as("Airline is invalid").isEqualTo("Air Libert\u00e9");
	}

}
