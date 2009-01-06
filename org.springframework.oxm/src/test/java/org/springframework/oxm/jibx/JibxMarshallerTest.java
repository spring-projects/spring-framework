/*
 * Copyright 2006 the original author or authors.
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

import org.custommonkey.xmlunit.XMLUnit;

import org.springframework.oxm.AbstractMarshallerTestCase;
import org.springframework.oxm.Marshaller;
import org.springframework.xml.transform.StringResult;

public class JibxMarshallerTest extends AbstractMarshallerTestCase {

    protected Marshaller createMarshaller() throws Exception {
        JibxMarshaller marshaller = new JibxMarshaller();
        marshaller.setTargetClass(Flights.class);
        marshaller.afterPropertiesSet();
        return marshaller;
    }

    protected Object createFlights() {
        Flights flights = new Flights();
        FlightType flight = new FlightType();
        flight.setNumber(42L);
        flights.addFlight(flight);
        return flights;
    }

    public void testAfterPropertiesSetNoContextPath() throws Exception {
        try {
            JibxMarshaller marshaller = new JibxMarshaller();
            marshaller.afterPropertiesSet();
            fail("Should have thrown an IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testIndentation() throws Exception {
        ((JibxMarshaller) marshaller).setIndent(4);
        StringResult result = new StringResult();
        marshaller.marshal(flights, result);
        XMLUnit.setIgnoreWhitespace(false);
        String expected = "<?xml version=\"1.0\"?>\n" +
                "<flights xmlns=\"http://samples.springframework.org/flight\">\n" + "    <flight>\n" +
                "        <number>42</number>\n" + "    </flight>\n" + "</flights>";
        assertXMLEqual(expected, result.toString());
    }

    public void testEncodingAndStandalone() throws Exception {
        ((JibxMarshaller) marshaller).setEncoding("ISO-8859-1");
        ((JibxMarshaller) marshaller).setStandalone(Boolean.TRUE);
        StringResult result = new StringResult();
        marshaller.marshal(flights, result);
        assertTrue("Encoding and standalone not set",
                result.toString().startsWith("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>"));
    }

    public void testSupports() throws Exception {
        assertTrue("JibxMarshaller does not support Flights", marshaller.supports(Flights.class));
        assertTrue("JibxMarshaller does not support FlightType", marshaller.supports(FlightType.class));
        assertFalse("JibxMarshaller supports illegal type", marshaller.supports(getClass()));
    }


}
