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
 * @author Stephane Nicoll
 */
public class MessagingMessageConverterTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final MessagingMessageConverter converter = new MessagingMessageConverter();

	@Test
	public void onlyHandlesMessage() throws JMSException {
		this.thrown.expect(IllegalArgumentException.class);
		this.converter.toMessage(new Object(), mock(Session.class));
	}

	@Test
	public void simpleObject() throws Exception {
		Session session = mock(Session.class);
		Serializable payload = mock(Serializable.class);
		ObjectMessage jmsMessage = mock(ObjectMessage.class);
		given(session.createObjectMessage(payload)).willReturn(jmsMessage);

		this.converter.toMessage(MessageBuilder.withPayload(payload).build(), session);
		verify(session).createObjectMessage(payload);
	}

	@Test
	public void fromNull() throws JMSException {
		assertNull(this.converter.fromMessage(null));
	}

	@Test
	public void customPayloadConverter() throws JMSException {
		TextMessage jmsMsg = new StubTextMessage("1224");

		this.converter.setPayloadConverter(new TestMessageConverter());
		Message<?> msg = (Message<?>) this.converter.fromMessage(jmsMsg);
		assertEquals(1224L, msg.getPayload());
	}

	@Test
	public void payloadConversionLazilyInvoked() throws JMSException {
		TextMessage jmsMsg = new StubTextMessage("1224");
		TestMessageConverter converter = new TestMessageConverter();
		this.converter.setPayloadConverter(converter);
		Message<?> msg = (Message<?>) this.converter.fromMessage(jmsMsg);
		assertEquals("Converter should not have been called yet", false, converter.called);
		assertEquals(1224L, msg.getPayload());
		assertEquals("Converter should have been called", true, converter.called);
	}

	@Test
	public void headerConversionLazilyInvoked() throws JMSException {
		javax.jms.Message jmsMsg = mock(javax.jms.Message.class);
		when(jmsMsg.getPropertyNames()).thenThrow(new IllegalArgumentException("Header failure"));

		Message<?> msg = (Message<?>) this.converter.fromMessage(jmsMsg);

		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Header failure");
		msg.getHeaders(); // Triggers headers resolution
	}


	static class TestMessageConverter extends SimpleMessageConverter {

		private boolean called;

		@Override
		public Object fromMessage(javax.jms.Message message) throws JMSException, MessageConversionException {
			if (this.called) {
				throw new java.lang.IllegalStateException("Converter called twice");
			}
			this.called = true;
			TextMessage textMessage = (TextMessage) message;
			return Long.parseLong(textMessage.getText());
		}

	}

}
