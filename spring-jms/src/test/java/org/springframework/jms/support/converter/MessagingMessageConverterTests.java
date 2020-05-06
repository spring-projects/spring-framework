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

package org.springframework.jms.support.converter;

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.jupiter.api.Test;

import org.springframework.jms.StubTextMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Stephane Nicoll
 */
public class MessagingMessageConverterTests {

	private final MessagingMessageConverter converter = new MessagingMessageConverter();


	@Test
	public void onlyHandlesMessage() throws JMSException {
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.converter.toMessage(new Object(), mock(Session.class)));
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
	public void customPayloadConverter() throws JMSException {
		TextMessage jmsMsg = new StubTextMessage("1224");

		this.converter.setPayloadConverter(new TestMessageConverter());
		Message<?> msg = (Message<?>) this.converter.fromMessage(jmsMsg);
		assertThat(msg.getPayload()).isEqualTo(1224L);
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
