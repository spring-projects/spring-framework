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

import javax.xml.transform.sax.SAXResult;

import org.easymock.MockControl;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.AbstractMarshallerTestCase;
import org.springframework.oxm.Marshaller;
import org.xml.sax.ContentHandler;

public class CastorMarshallerTest extends AbstractMarshallerTestCase {

    protected Marshaller createMarshaller() throws Exception {
        CastorMarshaller marshaller = new CastorMarshaller();
        ClassPathResource mappingLocation = new ClassPathResource("mapping.xml", CastorMarshaller.class);
        marshaller.setMappingLocation(mappingLocation);
        marshaller.afterPropertiesSet();
        return marshaller;
    }

    protected Object createFlights() {
        Flight flight = new Flight();
        flight.setNumber(42L);
        Flights flights = new Flights();
        flights.addFlight(flight);
        return flights;
    }

    public void testMarshalSaxResult() throws Exception {
        MockControl handlerControl = MockControl.createControl(ContentHandler.class);
        ContentHandler handlerMock = (ContentHandler) handlerControl.getMock();
        handlerMock.startDocument();
        handlerMock.startPrefixMapping("tns", "http://samples.springframework.org/flight");
        handlerMock.startElement("http://samples.springframework.org/flight", "flights", "tns:flights", null);
        handlerControl.setMatcher(MockControl.ALWAYS_MATCHER);
        handlerMock.startElement("http://samples.springframework.org/flight", "flight", "tns:flight", null);
        handlerControl.setMatcher(MockControl.ALWAYS_MATCHER);
        handlerMock.startElement("http://samples.springframework.org/flight", "number", "tns:number", null);
        handlerControl.setMatcher(MockControl.ALWAYS_MATCHER);
        handlerMock.characters(new char[]{'4', '2'}, 0, 2);
        handlerControl.setMatcher(MockControl.ARRAY_MATCHER);
        handlerMock.endElement("http://samples.springframework.org/flight", "number", "tns:number");
        handlerMock.endElement("http://samples.springframework.org/flight", "flight", "tns:flight");
        handlerMock.endElement("http://samples.springframework.org/flight", "flights", "tns:flights");
        handlerMock.endPrefixMapping("tns");
        handlerMock.endDocument();

        handlerControl.replay();
        SAXResult result = new SAXResult(handlerMock);
        marshaller.marshal(flights, result);
        handlerControl.verify();
    }

    public void testSupports() throws Exception {
        assertTrue("CastorMarshaller does not support Flights", marshaller.supports(Flights.class));
        assertTrue("CastorMarshaller does not support Flight", marshaller.supports(Flight.class));
    }


}
