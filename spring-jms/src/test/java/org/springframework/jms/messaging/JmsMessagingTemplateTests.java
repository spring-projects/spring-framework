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

package org.springframework.jms.messaging;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.jms.StubTextMessage;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 *
 * @author Stephane Nicoll
 */
public class JmsMessagingTemplateTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Captor
	private ArgumentCaptor<MessageCreator> messageCreator;

	@Mock
	private JmsTemplate jmsTemplate;

	private JmsMessagingTemplate messagingTemplate;


	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		messagingTemplate = new JmsMessagingTemplate(jmsTemplate);
	}

	@Test
	public void send() {
		Destination destination = new Destination() {};
		Message<String> message = createTextMessage();

		messagingTemplate.send(destination, message);
		verify(jmsTemplate).send(eq(destination), messageCreator.capture());
		assertTextMessage(messageCreator.getValue());
	}

	@Test
	public void sendName() {
		Message<String> message = createTextMessage();

		messagingTemplate.send("myQueue", message);
		verify(jmsTemplate).send(eq("myQueue"), messageCreator.capture());
		assertTextMessage(messageCreator.getValue());
	}

	@Test
	public void sendDefaultDestination() {
		Destination destination = new Destination() {};
		messagingTemplate.setDefaultDestination(destination);
		Message<String> message = createTextMessage();

		messagingTemplate.send(message);
		verify(jmsTemplate).send(eq(destination), messageCreator.capture());
		assertTextMessage(messageCreator.getValue());
	}

	@Test
	public void sendDefaultDestinationName() {
		messagingTemplate.setDefaultDestinationName("myQueue");
		Message<String> message = createTextMessage();

		messagingTemplate.send(message);
		verify(jmsTemplate).send(eq("myQueue"), messageCreator.capture());
		assertTextMessage(messageCreator.getValue());
	}

	@Test
	public void sendNoDefaultSet() {
		Message<String> message = createTextMessage();

		thrown.expect(IllegalStateException.class);
		messagingTemplate.send(message);
	}

	@Test
	public void sendPropertyInjection() {
		JmsMessagingTemplate t = new JmsMessagingTemplate();
		t.setJmsTemplate(jmsTemplate);
		t.setDefaultDestinationName("myQueue");
		t.afterPropertiesSet();
		Message<String> message = createTextMessage();

		t.send(message);
		verify(jmsTemplate).send(eq("myQueue"), messageCreator.capture());
		assertTextMessage(messageCreator.getValue());
	}

	@Test
	public void convertAndSendPayload() throws JMSException {
		Destination destination = new Destination() {};

		messagingTemplate.convertAndSend(destination, "my Payload");
		verify(jmsTemplate).send(eq(destination), messageCreator.capture());
		TextMessage textMessage = createTextMessage(messageCreator.getValue());
		assertEquals("my Payload", textMessage.getText());
	}

	@Test
	public void convertAndSendPayloadName() throws JMSException {
		messagingTemplate.convertAndSend("myQueue", "my Payload");
		verify(jmsTemplate).send(eq("myQueue"), messageCreator.capture());
		TextMessage textMessage = createTextMessage(messageCreator.getValue());
		assertEquals("my Payload", textMessage.getText());
	}

	@Test
	public void convertAndSendDefaultDestination() throws JMSException {
		Destination destination = new Destination() {};
		messagingTemplate.setDefaultDestination(destination);

		messagingTemplate.convertAndSend("my Payload");
		verify(jmsTemplate).send(eq(destination), messageCreator.capture());
		TextMessage textMessage = createTextMessage(messageCreator.getValue());
		assertEquals("my Payload", textMessage.getText());
	}

	@Test
	public void convertAndSendDefaultDestinationName() throws JMSException {
		messagingTemplate.setDefaultDestinationName("myQueue");

		messagingTemplate.convertAndSend("my Payload");
		verify(jmsTemplate).send(eq("myQueue"), messageCreator.capture());
		TextMessage textMessage = createTextMessage(messageCreator.getValue());
		assertEquals("my Payload", textMessage.getText());
	}

	@Test
	public void convertAndSendNoDefaultSet() throws JMSException {
		thrown.expect(IllegalStateException.class);
		messagingTemplate.convertAndSend("my Payload");
	}

	@Test
	public void convertAndSendCustomJmsMessageConverter() throws JMSException {
		messagingTemplate.setJmsMessageConverter(new SimpleMessageConverter() {
			@Override
			public javax.jms.Message toMessage(Object object, Session session)
					throws JMSException, MessageConversionException {
				throw new MessageConversionException("Test exception");
			}
		});

		messagingTemplate.convertAndSend("myQueue", "msg to convert");
		verify(jmsTemplate).send(eq("myQueue"), messageCreator.capture());

		thrown.expect(MessageConversionException.class);
		thrown.expectMessage("Test exception");
		messageCreator.getValue().createMessage(mock(Session.class));
	}

	@Test
	public void convertAndSendPayloadAndHeaders() throws JMSException {
		Destination destination = new Destination() {};
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "bar");

		messagingTemplate.convertAndSend(destination, "Hello", headers);
		verify(jmsTemplate).send(eq(destination), messageCreator.capture());
		assertTextMessage(messageCreator.getValue()); // see createTextMessage
	}

	@Test
	public void convertAndSendPayloadAndHeadersName() throws JMSException {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "bar");

		messagingTemplate.convertAndSend("myQueue", "Hello", headers);
		verify(jmsTemplate).send(eq("myQueue"), messageCreator.capture());
		assertTextMessage(messageCreator.getValue()); // see createTextMessage
	}


	private Message<String> createTextMessage() {
		return MessageBuilder
				.withPayload("Hello").setHeader("foo", "bar").build();
	}

	private void assertTextMessage(MessageCreator messageCreator) {
		try {
			TextMessage jmsMessage = createTextMessage(messageCreator);
			assertEquals("Wrong body message", "Hello", jmsMessage.getText());
			assertEquals("Invalid foo property", "bar", jmsMessage.getStringProperty("foo"));
		}
		catch (JMSException e) {
			throw new IllegalStateException("Wrong text message", e);
		}
	}


	protected TextMessage createTextMessage(MessageCreator creator) throws JMSException {
		Session mock = mock(Session.class);
		given(mock.createTextMessage(any())).willAnswer(new Answer<TextMessage>() {
			@Override
			public TextMessage answer(InvocationOnMock invocation) throws Throwable {
				return new StubTextMessage((String) invocation.getArguments()[0]);
			}
		});
		javax.jms.Message message = creator.createMessage(mock);
		verify(mock).createTextMessage(any());
		return TextMessage.class.cast(message);
	}

}
