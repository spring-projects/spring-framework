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

package org.springframework.oxm.support;

import javax.jms.BytesMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

public class MarshallingMessageConverterTest extends TestCase {

    private MarshallingMessageConverter converter;

    private MockControl marshallerControl;

    private Marshaller marshallerMock;

    private MockControl unmarshallerControl;

    private Unmarshaller unmarshallerMock;

    private MockControl sessionControl;

    private Session sessionMock;

    protected void setUp() throws Exception {
        marshallerControl = MockControl.createControl(Marshaller.class);
        marshallerMock = (Marshaller) marshallerControl.getMock();
        unmarshallerControl = MockControl.createControl(Unmarshaller.class);
        unmarshallerMock = (Unmarshaller) unmarshallerControl.getMock();
        converter = new MarshallingMessageConverter(marshallerMock, unmarshallerMock);
        sessionControl = MockControl.createControl(Session.class);
        sessionMock = (Session) sessionControl.getMock();

    }

    public void testToBytesMessage() throws Exception {
        MockControl bytesMessageControl = MockControl.createControl(BytesMessage.class);
        BytesMessage bytesMessageMock = (BytesMessage) bytesMessageControl.getMock();
        Object toBeMarshalled = new Object();

        sessionControl.expectAndReturn(sessionMock.createBytesMessage(), bytesMessageMock);
        marshallerMock.marshal(toBeMarshalled, new StringResult());
        marshallerControl.setMatcher(MockControl.ALWAYS_MATCHER);
        bytesMessageMock.writeBytes(new byte[0]);
        bytesMessageControl.setMatcher(MockControl.ALWAYS_MATCHER);

        marshallerControl.replay();
        unmarshallerControl.replay();
        sessionControl.replay();
        bytesMessageControl.replay();

        converter.toMessage(toBeMarshalled, sessionMock);

        marshallerControl.verify();
        unmarshallerControl.verify();
        sessionControl.verify();
        bytesMessageControl.verify();
    }

    public void testFromBytesMessage() throws Exception {
        MockControl bytesMessageControl = MockControl.createControl(BytesMessage.class);
        BytesMessage bytesMessageMock = (BytesMessage) bytesMessageControl.getMock();
        Object unmarshalled = new Object();

        bytesMessageControl.expectAndReturn(bytesMessageMock.getBodyLength(), 10);
        bytesMessageMock.readBytes(new byte[0]);
        bytesMessageControl.setMatcher(MockControl.ALWAYS_MATCHER);
        bytesMessageControl.setReturnValue(0);
        unmarshallerMock.unmarshal(new StringSource(""));
        unmarshallerControl.setMatcher(MockControl.ALWAYS_MATCHER);
        unmarshallerControl.setReturnValue(unmarshalled);

        marshallerControl.replay();
        unmarshallerControl.replay();
        sessionControl.replay();
        bytesMessageControl.replay();

        Object result = converter.fromMessage(bytesMessageMock);
        assertEquals("Invalid result", result, unmarshalled);

        marshallerControl.verify();
        unmarshallerControl.verify();
        sessionControl.verify();
        bytesMessageControl.verify();
    }

    public void testToTextMessage() throws Exception {
        converter.setMarshalTo(MarshallingMessageConverter.MARSHAL_TO_TEXT_MESSAGE);
        MockControl textMessageControl = MockControl.createControl(TextMessage.class);
        TextMessage textMessageMock = (TextMessage) textMessageControl.getMock();
        Object toBeMarshalled = new Object();

        sessionControl.expectAndReturn(sessionMock.createTextMessage(""), textMessageMock);
        marshallerMock.marshal(toBeMarshalled, new StringResult());
        marshallerControl.setMatcher(MockControl.ALWAYS_MATCHER);

        marshallerControl.replay();
        unmarshallerControl.replay();
        sessionControl.replay();
        textMessageControl.replay();

        converter.toMessage(toBeMarshalled, sessionMock);

        marshallerControl.verify();
        unmarshallerControl.verify();
        sessionControl.verify();
        textMessageControl.verify();
    }

    public void testFromTextMessage() throws Exception {
        MockControl textMessageControl = MockControl.createControl(TextMessage.class);
        TextMessage textMessageMock = (TextMessage) textMessageControl.getMock();
        Object unmarshalled = new Object();

        unmarshallerMock.unmarshal(new StringSource(""));
        unmarshallerControl.setMatcher(MockControl.ALWAYS_MATCHER);
        unmarshallerControl.setReturnValue(unmarshalled);
        textMessageControl.expectAndReturn(textMessageMock.getText(), "");

        marshallerControl.replay();
        unmarshallerControl.replay();
        sessionControl.replay();
        textMessageControl.replay();

        Object result = converter.fromMessage(textMessageMock);
        assertEquals("Invalid result", result, unmarshalled);

        marshallerControl.verify();
        unmarshallerControl.verify();
        sessionControl.verify();
        textMessageControl.verify();
    }

}