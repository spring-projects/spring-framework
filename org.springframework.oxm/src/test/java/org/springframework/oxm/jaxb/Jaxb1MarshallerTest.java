/*
 * Copyright 2005 the original author or authors.
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
package org.springframework.oxm.jaxb;

import java.util.Collections;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.jaxb1.FlightType;
import org.springframework.oxm.jaxb1.Flights;
import org.springframework.oxm.jaxb1.FlightsType;
import org.springframework.oxm.jaxb1.impl.FlightTypeImpl;
import org.springframework.oxm.jaxb1.impl.FlightsImpl;

public class Jaxb1MarshallerTest extends AbstractJaxbMarshallerTestCase {

    private static final String CONTEXT_PATH = "org.springframework.oxm.jaxb1";

    protected final Marshaller createMarshaller() throws Exception {
        Jaxb1Marshaller marshaller = new Jaxb1Marshaller();
        marshaller.setContextPaths(new String[]{CONTEXT_PATH});
        marshaller.afterPropertiesSet();
        return marshaller;
    }

    protected Object createFlights() {
        FlightType flight = new FlightTypeImpl();
        flight.setNumber(42L);
        Flights flights = new FlightsImpl();
        flights.getFlight().add(flight);
        return flights;
    }

    public void testProperties() throws Exception {
        Jaxb1Marshaller marshaller = new Jaxb1Marshaller();
        marshaller.setContextPath(CONTEXT_PATH);
        marshaller.setMarshallerProperties(
                Collections.singletonMap(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE));
        marshaller.afterPropertiesSet();
    }

    public void testNoContextPath() throws Exception {
        try {
            Jaxb1Marshaller marshaller = new Jaxb1Marshaller();
            marshaller.afterPropertiesSet();
            fail("Should have thrown an IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testInvalidContextPath() throws Exception {
        try {
            Jaxb1Marshaller marshaller = new Jaxb1Marshaller();
            marshaller.setContextPath("ab");
            marshaller.afterPropertiesSet();
            fail("Should have thrown an XmlMappingException");
        }
        catch (XmlMappingException ex) {
        }
    }

    public void testSupports() throws Exception {
        assertTrue("Jaxb1Marshaller does not support Flights", marshaller.supports(Flights.class));
        assertFalse("Jaxb1Marshaller supports FlightsType", marshaller.supports(FlightsType.class));
    }


}
