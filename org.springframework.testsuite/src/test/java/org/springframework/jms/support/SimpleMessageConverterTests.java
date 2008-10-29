/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.jms.support;

import junit.framework.TestCase;
import org.easymock.ArgumentsMatcher;
import org.easymock.MockControl;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.test.AssertThrows;

import javax.jms.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for the {@link SimpleMessageConverter} class.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 18.09.2004
 */
public final class SimpleMessageConverterTests extends TestCase {

    public void testStringConversion() throws JMSException {
        MockControl sessionControl = MockControl.createControl(Session.class);
        Session session = (Session) sessionControl.getMock();
        MockControl messageControl = MockControl.createControl(TextMessage.class);
        TextMessage message = (TextMessage) messageControl.getMock();

        String content = "test";

        session.createTextMessage(content);
        sessionControl.setReturnValue(message, 1);
        message.getText();
        messageControl.setReturnValue(content, 1);
        sessionControl.replay();
        messageControl.replay();

        SimpleMessageConverter converter = new SimpleMessageConverter();
        Message msg = converter.toMessage(content, session);
        assertEquals(content, converter.fromMessage(msg));

        sessionControl.verify();
        messageControl.verify();
    }

    public void testByteArrayConversion() throws JMSException {
        MockControl sessionControl = MockControl.createControl(Session.class);
        Session session = (Session) sessionControl.getMock();
        MockControl messageControl = MockControl.createControl(BytesMessage.class);
        BytesMessage message = (BytesMessage) messageControl.getMock();

        byte[] content = "test".getBytes();

        session.createBytesMessage();
        sessionControl.setReturnValue(message, 1);
        message.writeBytes(content);
        messageControl.setVoidCallable(1);
        message.getBodyLength();
        messageControl.setReturnValue(content.length, 1);
        message.readBytes(new byte[content.length]);
        messageControl.setMatcher(new ArgumentsMatcher() {
            public boolean matches(Object[] arg0, Object[] arg1) {
                byte[] one = (byte[]) arg0[0];
                byte[] two = (byte[]) arg1[0];
                return Arrays.equals(one, two);
            }

            public String toString(Object[] arg0) {
                return "bla";
            }
        });
        messageControl.setReturnValue(content.length, 1);
        sessionControl.replay();
        messageControl.replay();

        SimpleMessageConverter converter = new SimpleMessageConverter();
        Message msg = converter.toMessage(content, session);
        assertEquals(content.length, ((byte[]) converter.fromMessage(msg)).length);

        sessionControl.verify();
        messageControl.verify();
    }

    public void testMapConversion() throws JMSException {

        MockControl sessionControl = MockControl.createControl(Session.class);
        Session session = (Session) sessionControl.getMock();
        MockControl messageControl = MockControl.createControl(MapMessage.class);
        MapMessage message = (MapMessage) messageControl.getMock();

        Map content = new HashMap();
        content.put("key1", "value1");
        content.put("key2", "value2");

        session.createMapMessage();
        sessionControl.setReturnValue(message, 1);
        message.setObject("key1", "value1");
        messageControl.setVoidCallable(1);
        message.setObject("key2", "value2");
        messageControl.setVoidCallable(1);
        message.getMapNames();
        messageControl.setReturnValue(Collections.enumeration(content.keySet()));
        message.getObject("key1");
        messageControl.setReturnValue("value1", 1);
        message.getObject("key2");
        messageControl.setReturnValue("value2", 1);

        sessionControl.replay();
        messageControl.replay();

        SimpleMessageConverter converter = new SimpleMessageConverter();
        Message msg = converter.toMessage(content, session);
        assertEquals(content, converter.fromMessage(msg));

        sessionControl.verify();
        messageControl.verify();
    }

    public void testSerializableConversion() throws JMSException {
        MockControl sessionControl = MockControl.createControl(Session.class);
        Session session = (Session) sessionControl.getMock();
        MockControl messageControl = MockControl.createControl(ObjectMessage.class);
        ObjectMessage message = (ObjectMessage) messageControl.getMock();

        Integer content = new Integer(5);

        session.createObjectMessage(content);
        sessionControl.setReturnValue(message, 1);
        message.getObject();
        messageControl.setReturnValue(content, 1);
        sessionControl.replay();
        messageControl.replay();

        SimpleMessageConverter converter = new SimpleMessageConverter();
        Message msg = converter.toMessage(content, session);
        assertEquals(content, converter.fromMessage(msg));

        sessionControl.verify();
        messageControl.verify();
    }

    public void testToMessageThrowsExceptionIfGivenNullObjectToConvert() throws Exception {
        new AssertThrows(MessageConversionException.class) {
            public void test() throws Exception {
                new SimpleMessageConverter().toMessage(null, null);
            }
        }.runTest();
    }

    public void testToMessageThrowsExceptionIfGivenIncompatibleObjectToConvert() throws Exception {
        new AssertThrows(MessageConversionException.class) {
            public void test() throws Exception {
                new SimpleMessageConverter().toMessage(new Object(), null);
            }
        }.runTest();
    }

    public void testToMessageSimplyReturnsMessageAsIsIfSuppliedWithMessage() throws JMSException {

        MockControl sessionControl = MockControl.createControl(Session.class);
        Session session = (Session) sessionControl.getMock();

        MockControl messageControl = MockControl.createControl(ObjectMessage.class);
        ObjectMessage message = (ObjectMessage) messageControl.getMock();

        sessionControl.replay();
        messageControl.replay();

        SimpleMessageConverter converter = new SimpleMessageConverter();
        Message msg = converter.toMessage(message, session);
        assertSame(message, msg);

        sessionControl.verify();
        messageControl.verify();
    }

    public void testFromMessageSimplyReturnsMessageAsIsIfSuppliedWithMessage() throws JMSException {

        MockControl messageControl = MockControl.createControl(Message.class);
        Message message = (Message) messageControl.getMock();

        messageControl.replay();

        SimpleMessageConverter converter = new SimpleMessageConverter();
        Object msg = converter.fromMessage(message);
        assertSame(message, msg);

        messageControl.verify();
    }

    public void testMapConversionWhereMapHasNonStringTypesForKeys() throws JMSException {

        MockControl messageControl = MockControl.createControl(MapMessage.class);
        MapMessage message = (MapMessage) messageControl.getMock();
        messageControl.replay();

        MockControl sessionControl = MockControl.createControl(Session.class);
        final Session session = (Session) sessionControl.getMock();
        session.createMapMessage();
        sessionControl.setReturnValue(message);
        sessionControl.replay();

        final Map content = new HashMap();
        content.put(new Integer(1), "value1");

        final SimpleMessageConverter converter = new SimpleMessageConverter();
        new AssertThrows(MessageConversionException.class) {
            public void test() throws Exception {
                converter.toMessage(content, session);
            }
        }.runTest();

        sessionControl.verify();
    }

    public void testMapConversionWhereMapHasNNullForKey() throws JMSException {

        MockControl messageControl = MockControl.createControl(MapMessage.class);
        MapMessage message = (MapMessage) messageControl.getMock();
        messageControl.replay();

        MockControl sessionControl = MockControl.createControl(Session.class);
        final Session session = (Session) sessionControl.getMock();
        session.createMapMessage();
        sessionControl.setReturnValue(message);
        sessionControl.replay();

        final Map content = new HashMap();
        content.put(null, "value1");

        final SimpleMessageConverter converter = new SimpleMessageConverter();
        new AssertThrows(MessageConversionException.class) {
            public void test() throws Exception {
                converter.toMessage(content, session);
            }
        }.runTest();

        sessionControl.verify();
    }

}
