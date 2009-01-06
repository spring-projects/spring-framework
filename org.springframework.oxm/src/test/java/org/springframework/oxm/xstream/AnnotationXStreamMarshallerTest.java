/*
 * Copyright 2007 the original author or authors.
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

package org.springframework.oxm.xstream;

import java.io.StringWriter;
import javax.xml.transform.stream.StreamResult;

import org.custommonkey.xmlunit.XMLTestCase;

public class AnnotationXStreamMarshallerTest extends XMLTestCase {

    private AnnotationXStreamMarshaller marshaller;

    private static final String EXPECTED_STRING = "<flight><number>42</number></flight>";

    private Flight flight;

    protected void setUp() throws Exception {
        marshaller = new AnnotationXStreamMarshaller();
        marshaller.setAnnotatedClass(Flight.class);
        flight = new Flight();
        flight.setFlightNumber(42L);
    }

    public void testMarshalStreamResultWriter() throws Exception {
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        marshaller.marshal(flight, result);
        assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
    }

}