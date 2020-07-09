/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.jms.StubTextMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(headerAccessor.getCorrelationId()).isEqualTo("correlation-1234");
		assertThat(headerAccessor.getDestination()).isEqualTo(destination);
		assertThat(headerAccessor.getDeliveryMode()).isEqualTo(Integer.valueOf(1));
		assertThat(headerAccessor.getExpiration()).isEqualTo(1234);

		assertThat(headerAccessor.getMessageId()).isEqualTo("abcd-1234");
		assertThat(headerAccessor.getPriority()).isEqualTo(Integer.valueOf(9));
		assertThat(headerAccessor.getReplyTo()).isEqualTo(replyTo);
		assertThat(headerAccessor.getRedelivered()).isEqualTo(true);
		assertThat(headerAccessor.getType()).isEqualTo("type");
		assertThat(headerAccessor.getTimestamp()).isEqualTo(4567);

		// Making sure replyChannel is not mixed with replyTo
		assertThat(headerAccessor.getReplyChannel()).isNull();

	}
}
