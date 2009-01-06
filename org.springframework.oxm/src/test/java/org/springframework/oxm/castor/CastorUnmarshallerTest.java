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
package org.springframework.oxm.castor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.transform.stream.StreamSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.AbstractUnmarshallerTestCase;
import org.springframework.oxm.Unmarshaller;

public class CastorUnmarshallerTest extends AbstractUnmarshallerTestCase {

    protected void testFlights(Object o) {
        Flights flights = (Flights) o;
        assertNotNull("Flights is null", flights);
        assertEquals("Invalid amount of flight elements", 1, flights.getFlightCount());
        testFlight(flights.getFlight()[0]);
    }

    protected void testFlight(Object o) {
        Flight flight = (Flight) o;
        assertNotNull("Flight is null", flight);
        assertEquals("Number is invalid", 42L, flight.getNumber());
    }

    protected Unmarshaller createUnmarshaller() throws Exception {
        CastorMarshaller marshaller = new CastorMarshaller();
        ClassPathResource mappingLocation = new ClassPathResource("mapping.xml", CastorMarshaller.class);
        marshaller.setMappingLocation(mappingLocation);
        marshaller.afterPropertiesSet();
        return marshaller;
    }

    public void testUnmarshalTargetClass() throws Exception {
        CastorMarshaller unmarshaller = new CastorMarshaller();
        unmarshaller.setTargetClass(Flights.class);
        unmarshaller.afterPropertiesSet();
        StreamSource source = new StreamSource(new ByteArrayInputStream(INPUT_STRING.getBytes("UTF-8")));
        Object flights = unmarshaller.unmarshal(source);
        testFlights(flights);
    }

    public void testSetBothTargetClassAndMapping() throws IOException {
        try {
            CastorMarshaller marshaller = new CastorMarshaller();
            marshaller.setMappingLocation(new ClassPathResource("mapping.xml", CastorMarshaller.class));
            marshaller.setTargetClass(getClass());
            marshaller.afterPropertiesSet();
            fail("IllegalArgumentException expected");
        }
        catch (IllegalArgumentException ex) {
            // expected
        }
    }

}
