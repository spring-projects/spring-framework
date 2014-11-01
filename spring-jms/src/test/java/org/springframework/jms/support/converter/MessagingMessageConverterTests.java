/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jms.support.converter;

import java.io.Serializable;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.jms.StubTextMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 *
 * @author Stephane Nicoll
 */
public class MessagingMessageConverterTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final MessagingMessageConverter converter = new MessagingMessageConverter();

	@Test
	public void onlyHandlesMessage() throws JMSException {
		thrown.expect(IllegalArgumentException.class);
		converter.toMessage(new Object(), mock(Session.class));
	}

	@Test
	public void simpleObject() throws Exception {
		Session session = mock(Session.class);
		Serializable payload = mock(Serializable.class);
		ObjectMessage jmsMessage = mock(ObjectMessage.class);
		given(session.createObjectMessage(payload)).willReturn(jmsMessage);

		converter.toMessage(MessageBuilder.withPayload(payload).build(), session);
		verify(session).createObjectMessage(payload);
	}

	@Test
	public void fromNull() throws JMSException {
		assertNull(converter.fromMessage(null));
	}

	@Test
	public void customPayloadConverter() throws JMSException {
		TextMessage jmsMsg = new StubTextMessage("1224");

		converter.setPayloadConverter(new SimpleMessageConverter() {
			@Override
			public Object fromMessage(javax.jms.Message message) throws JMSException, MessageConversionException {
				TextMessage textMessage = (TextMessage) message;
				return Long.parseLong(textMessage.getText());
			}
		});

		Message<?> msg = (Message<?>) converter.fromMessage(jmsMsg);
		assertEquals(1224L, msg.getPayload());
	}

	@Test
	public void payloadIsAMessage() throws JMSException {
		final Message<String> message = MessageBuilder.withPayload("Test").setHeader("inside", true).build();
		converter.setPayloadConverter(new SimpleMessageConverter() {
			@Override
			public Object fromMessage(javax.jms.Message jmsMessage) throws JMSException, MessageConversionException {
				return message;
			}
		});
		Message<?> msg = (Message<?>) converter.fromMessage(new StubTextMessage());
		assertEquals(message.getPayload(), msg.getPayload());
		assertEquals(true, msg.getHeaders().get("inside"));
	}

}
