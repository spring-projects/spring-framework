/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jms.support;

import java.util.Map;
import javax.jms.Destination;
import javax.jms.JMSException;

import org.junit.Test;

import org.springframework.jms.StubTextMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.Assert.*;

/**
 *
 * @author Stephane Nicoll
 */
public class JmsMessageHeaderAccessorTests {

	@Test
	public void validateJmsHeaders() throws JMSException {
		Destination destination = new Destination() {};
		Destination replyTo = new Destination() {};

		StubTextMessage jmsMessage = new StubTextMessage("test");

		jmsMessage.setJMSCorrelationID("correlation-1234");
		jmsMessage.setJMSPriority(9);
		jmsMessage.setJMSDestination(destination);
		jmsMessage.setJMSDeliveryMode(1);
		jmsMessage.setJMSExpiration(1234L);
		jmsMessage.setJMSMessageID("abcd-1234");
		jmsMessage.setJMSPriority(9);
		jmsMessage.setJMSReplyTo(replyTo);
		jmsMessage.setJMSRedelivered(true);
		jmsMessage.setJMSType("type");
		jmsMessage.setJMSTimestamp(4567L);

		Map<String, Object> mappedHeaders = new SimpleJmsHeaderMapper().toHeaders(jmsMessage);
		Message<String> message = MessageBuilder.withPayload("test").copyHeaders(mappedHeaders).build();
		JmsMessageHeaderAccessor headerAccessor = JmsMessageHeaderAccessor.wrap(message);
		assertEquals("correlation-1234", headerAccessor.getCorrelationId());
		assertEquals(destination, headerAccessor.getDestination());
		assertEquals(Integer.valueOf(1), headerAccessor.getDeliveryMode());
		assertEquals(1234L, headerAccessor.getExpiration(), 0.0);
		assertEquals("abcd-1234", headerAccessor.getMessageId());
		assertEquals(Integer.valueOf(9), headerAccessor.getPriority());
		assertEquals(replyTo, headerAccessor.getReplyTo());
		assertEquals(true, headerAccessor.getRedelivered());
		assertEquals("type", headerAccessor.getType());
		assertEquals(4567L, headerAccessor.getTimestamp(), 0.0);

		// Making sure replyChannel is not mixed with replyTo
		assertNull(headerAccessor.getReplyChannel());

	}
}
