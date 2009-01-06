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

package org.springframework.oxm.jaxb;

import javax.xml.transform.sax.SAXResult;

import org.easymock.MockControl;
import org.xml.sax.ContentHandler;

import org.springframework.oxm.AbstractMarshallerTestCase;

public abstract class AbstractJaxbMarshallerTestCase extends AbstractMarshallerTestCase {

    public void testMarshalSaxResult() throws Exception {
        MockControl handlerControl = MockControl.createStrictControl(ContentHandler.class);
        ContentHandler handlerMock = (ContentHandler) handlerControl.getMock();
        handlerMock.setDocumentLocator(null);
        handlerControl.setMatcher(MockControl.ALWAYS_MATCHER);
        handlerMock.startDocument();
        handlerMock.startPrefixMapping("", "http://samples.springframework.org/flight");
        handlerMock.startElement("http://samples.springframework.org/flight", "flights", "flights", null);
        handlerControl.setMatcher(MockControl.ALWAYS_MATCHER);
        handlerMock.startElement("http://samples.springframework.org/flight", "flight", "flight", null);
        handlerControl.setMatcher(MockControl.ALWAYS_MATCHER);
        handlerMock.startElement("http://samples.springframework.org/flight", "number", "number", null);
        handlerControl.setMatcher(MockControl.ALWAYS_MATCHER);
        handlerMock.characters(new char[]{'4', '2'}, 0, 2);
        handlerControl.setMatcher(MockControl.ALWAYS_MATCHER);
        handlerMock.endElement("http://samples.springframework.org/flight", "number", "number");
        handlerMock.endElement("http://samples.springframework.org/flight", "flight", "flight");
        handlerMock.endElement("http://samples.springframework.org/flight", "flights", "flights");
        handlerMock.endPrefixMapping("");
        handlerMock.endDocument();

        handlerControl.replay();
        SAXResult result = new SAXResult(handlerMock);
        marshaller.marshal(flights, result);
        handlerControl.verify();
    }


}
