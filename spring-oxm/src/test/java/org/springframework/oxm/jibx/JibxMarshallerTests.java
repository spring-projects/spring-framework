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

package org.springframework.oxm.jibx;

import java.io.StringWriter;
import javax.xml.transform.stream.StreamResult;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.oxm.AbstractMarshallerTests;

import static org.junit.Assert.*;
import static org.xmlunit.matchers.CompareMatcher.*;

/**
 * NOTE: These tests fail under Eclipse/IDEA because JiBX binding does not occur by
 * default. The Gradle build should succeed, however.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
public class JibxMarshallerTests extends AbstractMarshallerTests<JibxMarshaller> {

	@BeforeClass
	public static void compilerAssumptions() {
		// JiBX compiler is currently not compatible with JDK 9
		Assume.assumeTrue(System.getProperty("java.version").startsWith("1.8."));
	}

	@Override
	protected JibxMarshaller createMarshaller() throws Exception {
		JibxMarshaller marshaller = new JibxMarshaller();
		marshaller.setTargetPackage("org.springframework.oxm.jibx");
		marshaller.afterPropertiesSet();
		return marshaller;
	}

	@Override
	protected Object createFlights() {
		Flights flights = new Flights();
		FlightType flight = new FlightType();
		flight.setNumber(42L);
		flights.addFlight(flight);
		return flights;
	}

	@Test(expected = IllegalArgumentException.class)
	public void afterPropertiesSetNoContextPath() throws Exception {
		JibxMarshaller marshaller = new JibxMarshaller();
		marshaller.afterPropertiesSet();
	}

	@Test
	public void indentation() throws Exception {
		marshaller.setIndent(4);
		StringWriter writer = new StringWriter();
		marshaller.marshal(flights, new StreamResult(writer));
		String expected =
				"<?xml version=\"1.0\"?>\n" + "<flights xmlns=\"http://samples.springframework.org/flight\">\n" +
						"    <flight>\n" + "        <number>42</number>\n" + "    </flight>\n" + "</flights>";
		assertThat(writer.toString(), isSimilarTo(expected).ignoreWhitespace());
	}

	@Test
	public void encodingAndStandalone() throws Exception {
		marshaller.setEncoding("ISO-8859-1");
		marshaller.setStandalone(Boolean.TRUE);
		StringWriter writer = new StringWriter();
		marshaller.marshal(flights, new StreamResult(writer));
		assertTrue("Encoding and standalone not set",
				writer.toString().startsWith("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>"));
	}

	@Test
	public void dtd() throws Exception {
		marshaller.setDocTypeRootElementName("flights");
		marshaller.setDocTypeSystemId("flights.dtd");
		StringWriter writer = new StringWriter();
		marshaller.marshal(flights, new StreamResult(writer));
		assertTrue("doc type not written",
				writer.toString().contains("<!DOCTYPE flights SYSTEM \"flights.dtd\">"));
	}

	@Test
	public void supports() throws Exception {
		assertTrue("JibxMarshaller does not support Flights", marshaller.supports(Flights.class));
		assertTrue("JibxMarshaller does not support FlightType", marshaller.supports(FlightType.class));
		assertFalse("JibxMarshaller supports illegal type", marshaller.supports(getClass()));
	}

}
