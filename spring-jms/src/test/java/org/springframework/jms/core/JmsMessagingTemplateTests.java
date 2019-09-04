/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jms.core;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotWriteableException;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.jms.InvalidDestinationException;
import org.springframework.jms.MessageNotReadableException;
import org.springframework.jms.StubTextMessage;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessagingMessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolutionException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests for {@link JmsMessagingTemplate}.
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
		this.messagingTemplate = new JmsMessagingTemplate(this.jmsTemplate);
	}

	@Test
	public void validateJmsTemplate() {
		assertSame(this.jmsTemplate, this.messagingTemplate.getJmsTemplate());
	}

	@Test
	public void payloadConverterIsConsistentConstructor() {
		MessageConverter messageConverter = mock(MessageConverter.class);
		given(this.jmsTemplate.getMessageConverter()).willReturn(messageConverter);
		JmsMessagingTemplate messagingTemplate = new JmsMessagingTemplate(this.jmsTemplate);
		messagingTemplate.afterPropertiesSet();
		assertPayloadConverter(messagingTemplate, messageConverter);
	}

	@Test
	public void payloadConverterIsConsistentSetter() {
		MessageConverter messageConverter = mock(MessageConverter.class);
		given(this.jmsTemplate.getMessageConverter()).willReturn(messageConverter);
		JmsMessagingTemplate messagingTemplate = new JmsMessagingTemplate();
		messagingTemplate.setJmsTemplate(this.jmsTemplate);
		messagingTemplate.afterPropertiesSet();
		assertPayloadConverter(messagingTemplate, messageConverter);
	}

	@Test
	public void customConverterAlwaysTakesPrecedence() {
		MessageConverter messageConverter = mock(MessageConverter.class);
		given(this.jmsTemplate.getMessageConverter()).willReturn(messageConverter);
		MessageConverter customMessageConverter = mock(MessageConverter.class);
		JmsMessagingTemplate messagingTemplate = new JmsMessagingTemplate();
		messagingTemplate.setJmsMessageConverter(
				new MessagingMessageConverter(customMessageConverter));
		messagingTemplate.setJmsTemplate(this.jmsTemplate);
		messagingTemplate.afterPropertiesSet();
		assertPayloadConverter(messagingTemplate, customMessageConverter);
	}

	private void assertPayloadConverter(JmsMessagingTemplate messagingTemplate,
			MessageConverter messageConverter) {
		MessageConverter jmsMessageConverter = messagingTemplate.getJmsMessageConverter();
		assertNotNull(jmsMessageConverter);
		assertEquals(MessagingMessageConverter.class, jmsMessageConverter.getClass());
		assertSame(messageConverter, new DirectFieldAccessor(jmsMessageConverter)
				.getPropertyValue("payloadConverter"));
	}

	@Test
	public void send() {
		Destination destination = new Destination() {};
		Message<String> message = createTextMessage();

		this.messagingTemplate.send(destination, message);
		verify(this.jmsTemplate).send(eq(destination), this.messageCreator.capture());
		assertTextMessage(this.messageCreator.getValue());
	}

	@Test
	public void sendName() {
		Message<String> message = createTextMessage();

		this.messagingTemplate.send("myQueue", message);
		verify(this.jmsTemplate).send(eq("myQueue"), this.messageCreator.capture());
		assertTextMessage(this.messageCreator.getValue());
	}

	@Test
	public void sendDefaultDestination() {
		Destination destination = new Destination() {};
		this.messagingTemplate.setDefaultDestination(destination);
		Message<String> message = createTextMessage();

		this.messagingTemplate.send(message);
		verify(this.jmsTemplate).send(eq(destination), this.messageCreator.capture());
		assertTextMessage(this.messageCreator.getValue());
	}

	@Test
	public void sendDefaultDestinationName() {
		this.messagingTemplate.setDefaultDestinationName("myQueue");
		Message<String> message = createTextMessage();

		this.messagingTemplate.send(message);
		verify(this.jmsTemplate).send(eq("myQueue"), this.messageCreator.capture());
		assertTextMessage(this.messageCreator.getValue());
	}

	@Test
	public void sendNoDefaultSet() {
		Message<String> message = createTextMessage();

		this.thrown.expect(IllegalStateException.class);
		this.messagingTemplate.send(message);
	}

	@Test
	public void sendPropertyInjection() {
		JmsMessagingTemplate t = new JmsMessagingTemplate();
		t.setJmsTemplate(this.jmsTemplate);
		t.setDefaultDestinationName("myQueue");
		t.afterPropertiesSet();
		Message<String> message = createTextMessage();

		t.send(message);
		verify(this.jmsTemplate).send(eq("myQueue"), this.messageCreator.capture());
		assertTextMessage(this.messageCreator.getValue());
	}

	@Test
	public void convertAndSendPayload() throws JMSException {
		Destination destination = new Destination() {};

		this.messagingTemplate.convertAndSend(destination, "my Payload");
		verify(this.jmsTemplate).send(eq(destination), this.messageCreator.capture());
		TextMessage textMessage = createTextMessage(this.messageCreator.getValue());
		assertEquals("my Payload", textMessage.getText());
	}

	@Test
	public void convertAndSendPayloadName() throws JMSException {
		this.messagingTemplate.convertAndSend("myQueue", "my Payload");
		verify(this.jmsTemplate).send(eq("myQueue"), this.messageCreator.capture());
		TextMessage textMessage = createTextMessage(this.messageCreator.getValue());
		assertEquals("my Payload", textMessage.getText());
	}

	@Test
	public void convertAndSendDefaultDestination() throws JMSException {
		Destination destination = new Destination() {};
		this.messagingTemplate.setDefaultDestination(destination);

		this.messagingTemplate.convertAndSend("my Payload");
		verify(this.jmsTemplate).send(eq(destination), this.messageCreator.capture());
		TextMessage textMessage = createTextMessage(this.messageCreator.getValue());
		assertEquals("my Payload", textMessage.getText());
	}

	@Test
	public void convertAndSendDefaultDestinationName() throws JMSException {
		this.messagingTemplate.setDefaultDestinationName("myQueue");

		this.messagingTemplate.convertAndSend("my Payload");
		verify(this.jmsTemplate).send(eq("myQueue"), this.messageCreator.capture());
		TextMessage textMessage = createTextMessage(this.messageCreator.getValue());
		assertEquals("my Payload", textMessage.getText());
	}

	@Test
	public void convertAndSendNoDefaultSet() throws JMSException {
		this.thrown.expect(IllegalStateException.class);
		this.messagingTemplate.convertAndSend("my Payload");
	}

	@Test
	public void convertAndSendCustomJmsMessageConverter() throws JMSException {
		this.messagingTemplate.setJmsMessageConverter(new SimpleMessageConverter() {
			@Override
			public javax.jms.Message toMessage(Object object, Session session)
					throws JMSException, org.springframework.jms.support.converter.MessageConversionException {
				throw new org.springframework.jms.support.converter.MessageConversionException("Test exception");
			}
		});

		this.messagingTemplate.convertAndSend("myQueue", "msg to convert");
		verify(this.jmsTemplate).send(eq("myQueue"), this.messageCreator.capture());

		this.thrown.expect(org.springframework.messaging.converter.MessageConversionException.class);
		this.thrown.expectMessage(new StringContains("Test exception"));
		this.messageCreator.getValue().createMessage(mock(Session.class));
	}

	@Test
	public void convertAndSendPayloadAndHeaders() throws JMSException {
		Destination destination = new Destination() {};
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "bar");

		this.messagingTemplate.convertAndSend(destination, "Hello", headers);
		verify(this.jmsTemplate).send(eq(destination), this.messageCreator.capture());
		assertTextMessage(this.messageCreator.getValue()); // see createTextMessage
	}

	@Test
	public void convertAndSendPayloadAndHeadersName() throws JMSException {
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "bar");

		this.messagingTemplate.convertAndSend("myQueue", "Hello", headers);
		verify(this.jmsTemplate).send(eq("myQueue"), this.messageCreator.capture());
		assertTextMessage(this.messageCreator.getValue()); // see createTextMessage
	}

	@Test
	public void receive() {
		Destination destination = new Destination() {};
		javax.jms.Message jmsMessage = createJmsTextMessage();
		given(this.jmsTemplate.receive(destination)).willReturn(jmsMessage);

		Message<?> message = this.messagingTemplate.receive(destination);
		verify(this.jmsTemplate).receive(destination);
		assertTextMessage(message);
	}

	@Test
	public void receiveName() {
		javax.jms.Message jmsMessage = createJmsTextMessage();
		given(this.jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		Message<?> message = this.messagingTemplate.receive("myQueue");
		verify(this.jmsTemplate).receive("myQueue");
		assertTextMessage(message);
	}

	@Test
	public void receiveDefaultDestination() {
		Destination destination = new Destination() {};
		this.messagingTemplate.setDefaultDestination(destination);
		javax.jms.Message jmsMessage = createJmsTextMessage();
		given(this.jmsTemplate.receive(destination)).willReturn(jmsMessage);

		Message<?> message = this.messagingTemplate.receive();
		verify(this.jmsTemplate).receive(destination);
		assertTextMessage(message);
	}

	@Test
	public void receiveDefaultDestinationName() {
		this.messagingTemplate.setDefaultDestinationName("myQueue");
		javax.jms.Message jmsMessage = createJmsTextMessage();
		given(this.jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		Message<?> message = this.messagingTemplate.receive();
		verify(this.jmsTemplate).receive("myQueue");
		assertTextMessage(message);
	}

	@Test
	public void receiveNoDefaultSet() {
		this.thrown.expect(IllegalStateException.class);
		this.messagingTemplate.receive();
	}

	@Test
	public void receiveAndConvert() {
		Destination destination = new Destination() {};
		javax.jms.Message jmsMessage = createJmsTextMessage("my Payload");
		given(this.jmsTemplate.receive(destination)).willReturn(jmsMessage);

		String payload = this.messagingTemplate.receiveAndConvert(destination, String.class);
		assertEquals("my Payload", payload);
		verify(this.jmsTemplate).receive(destination);
	}

	@Test
	public void receiveAndConvertName() {
		javax.jms.Message jmsMessage = createJmsTextMessage("my Payload");
		given(this.jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		String payload = this.messagingTemplate.receiveAndConvert("myQueue", String.class);
		assertEquals("my Payload", payload);
		verify(this.jmsTemplate).receive("myQueue");
	}

	@Test
	public void receiveAndConvertDefaultDestination() {
		Destination destination = new Destination() {};
		this.messagingTemplate.setDefaultDestination(destination);
		javax.jms.Message jmsMessage = createJmsTextMessage("my Payload");
		given(this.jmsTemplate.receive(destination)).willReturn(jmsMessage);

		String payload = this.messagingTemplate.receiveAndConvert(String.class);
		assertEquals("my Payload", payload);
		verify(this.jmsTemplate).receive(destination);
	}

	@Test
	public void receiveAndConvertDefaultDestinationName() {
		this.messagingTemplate.setDefaultDestinationName("myQueue");
		javax.jms.Message jmsMessage = createJmsTextMessage("my Payload");
		given(this.jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		String payload = this.messagingTemplate.receiveAndConvert(String.class);
		assertEquals("my Payload", payload);
		verify(this.jmsTemplate).receive("myQueue");
	}

	@Test
	public void receiveAndConvertWithConversion() {
		javax.jms.Message jmsMessage = createJmsTextMessage("123");
		given(this.jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		this.messagingTemplate.setMessageConverter(new GenericMessageConverter());

		Integer payload = this.messagingTemplate.receiveAndConvert("myQueue", Integer.class);
		assertEquals(Integer.valueOf(123), payload);
		verify(this.jmsTemplate).receive("myQueue");
	}

	@Test
	public void receiveAndConvertNoConverter() {
		javax.jms.Message jmsMessage = createJmsTextMessage("Hello");
		given(this.jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		this.thrown.expect(org.springframework.messaging.converter.MessageConversionException.class);
		this.messagingTemplate.receiveAndConvert("myQueue", Writer.class);
	}

	@Test
	public void receiveAndConvertNoInput() {
		given(this.jmsTemplate.receive("myQueue")).willReturn(null);

		assertNull(this.messagingTemplate.receiveAndConvert("myQueue", String.class));
	}

	@Test
	public void sendAndReceive() {
		Destination destination = new Destination() {};
		Message<String> request = createTextMessage();
		javax.jms.Message replyJmsMessage = createJmsTextMessage();
		given(this.jmsTemplate.sendAndReceive(eq(destination), any())).willReturn(replyJmsMessage);

		Message<?> actual = this.messagingTemplate.sendAndReceive(destination, request);
		verify(this.jmsTemplate, times(1)).sendAndReceive(eq(destination), any());
		assertTextMessage(actual);
	}

	@Test
	public void sendAndReceiveName() {
		Message<String> request = createTextMessage();
		javax.jms.Message replyJmsMessage = createJmsTextMessage();
		given(this.jmsTemplate.sendAndReceive(eq("myQueue"), any())).willReturn(replyJmsMessage);

		Message<?> actual = this.messagingTemplate.sendAndReceive("myQueue", request);
		verify(this.jmsTemplate, times(1)).sendAndReceive(eq("myQueue"), any());
		assertTextMessage(actual);
	}

	@Test
	public void sendAndReceiveDefaultDestination() {
		Destination destination = new Destination() {};
		this.messagingTemplate.setDefaultDestination(destination);
		Message<String> request = createTextMessage();
		javax.jms.Message replyJmsMessage = createJmsTextMessage();
		given(this.jmsTemplate.sendAndReceive(eq(destination), any())).willReturn(replyJmsMessage);

		Message<?> actual = this.messagingTemplate.sendAndReceive(request);
		verify(this.jmsTemplate, times(1)).sendAndReceive(eq(destination), any());
		assertTextMessage(actual);
	}

	@Test
	public void sendAndReceiveDefaultDestinationName() {
		this.messagingTemplate.setDefaultDestinationName("myQueue");
		Message<String> request = createTextMessage();
		javax.jms.Message replyJmsMessage = createJmsTextMessage();
		given(this.jmsTemplate.sendAndReceive(eq("myQueue"), any())).willReturn(replyJmsMessage);

		Message<?> actual = this.messagingTemplate.sendAndReceive(request);
		verify(this.jmsTemplate, times(1)).sendAndReceive(eq("myQueue"), any());
		assertTextMessage(actual);
	}

	@Test
	public void sendAndReceiveNoDefaultSet() {
		Message<String> message = createTextMessage();

		this.thrown.expect(IllegalStateException.class);
		this.messagingTemplate.sendAndReceive(message);
	}

	@Test
	public void convertSendAndReceivePayload() throws JMSException {
		Destination destination = new Destination() {};
		javax.jms.Message replyJmsMessage = createJmsTextMessage("My reply");
		given(this.jmsTemplate.sendAndReceive(eq(destination), any())).willReturn(replyJmsMessage);

		String reply = this.messagingTemplate.convertSendAndReceive(destination, "my Payload", String.class);
		verify(this.jmsTemplate, times(1)).sendAndReceive(eq(destination), any());
		assertEquals("My reply", reply);
	}

	@Test
	public void convertSendAndReceivePayloadName() throws JMSException {
		javax.jms.Message replyJmsMessage = createJmsTextMessage("My reply");
		given(this.jmsTemplate.sendAndReceive(eq("myQueue"), any())).willReturn(replyJmsMessage);

		String reply = this.messagingTemplate.convertSendAndReceive("myQueue", "my Payload", String.class);
		verify(this.jmsTemplate, times(1)).sendAndReceive(eq("myQueue"), any());
		assertEquals("My reply", reply);
	}

	@Test
	public void convertSendAndReceiveDefaultDestination() throws JMSException {
		Destination destination = new Destination() {};
		this.messagingTemplate.setDefaultDestination(destination);
		javax.jms.Message replyJmsMessage = createJmsTextMessage("My reply");
		given(this.jmsTemplate.sendAndReceive(eq(destination), any())).willReturn(replyJmsMessage);

		String reply = this.messagingTemplate.convertSendAndReceive("my Payload", String.class);
		verify(this.jmsTemplate, times(1)).sendAndReceive(eq(destination), any());
		assertEquals("My reply", reply);
	}

	@Test
	public void convertSendAndReceiveDefaultDestinationName() throws JMSException {
		this.messagingTemplate.setDefaultDestinationName("myQueue");
		javax.jms.Message replyJmsMessage = createJmsTextMessage("My reply");
		given(this.jmsTemplate.sendAndReceive(eq("myQueue"), any())).willReturn(replyJmsMessage);

		String reply = this.messagingTemplate.convertSendAndReceive("my Payload", String.class);
		verify(this.jmsTemplate, times(1)).sendAndReceive(eq("myQueue"), any());
		assertEquals("My reply", reply);
	}

	@Test
	public void convertSendAndReceiveNoDefaultSet() throws JMSException {
		this.thrown.expect(IllegalStateException.class);
		this.messagingTemplate.convertSendAndReceive("my Payload", String.class);
	}

	@Test
	public void convertMessageConversionExceptionOnSend() throws JMSException {
		Message<String> message = createTextMessage();
		MessageConverter messageConverter = mock(MessageConverter.class);
		willThrow(org.springframework.jms.support.converter.MessageConversionException.class)
				.given(messageConverter).toMessage(eq(message), any());
		this.messagingTemplate.setJmsMessageConverter(messageConverter);
		invokeMessageCreator();

		this.thrown.expect(org.springframework.messaging.converter.MessageConversionException.class);
		this.messagingTemplate.send("myQueue", message);
	}

	@Test
	public void convertMessageConversionExceptionOnReceive() throws JMSException {
		javax.jms.Message message = createJmsTextMessage();
		MessageConverter messageConverter = mock(MessageConverter.class);
		willThrow(org.springframework.jms.support.converter.MessageConversionException.class)
				.given(messageConverter).fromMessage(message);
		this.messagingTemplate.setJmsMessageConverter(messageConverter);
		given(this.jmsTemplate.receive("myQueue")).willReturn(message);

		this.thrown.expect(org.springframework.messaging.converter.MessageConversionException.class);
		this.messagingTemplate.receive("myQueue");
	}

	@Test
	public void convertMessageNotReadableException() throws JMSException {
		willThrow(MessageNotReadableException.class).given(this.jmsTemplate).receive("myQueue");

		this.thrown.expect(MessagingException.class);
		this.messagingTemplate.receive("myQueue");
	}

	@Test
	public void convertDestinationResolutionExceptionOnSend() {
		Destination destination = new Destination() {};
		willThrow(DestinationResolutionException.class).given(this.jmsTemplate).send(eq(destination), any());

		this.thrown.expect(org.springframework.messaging.core.DestinationResolutionException.class);
		this.messagingTemplate.send(destination, createTextMessage());
	}

	@Test
	public void convertDestinationResolutionExceptionOnReceive() {
		Destination destination = new Destination() {};
		willThrow(DestinationResolutionException.class).given(this.jmsTemplate).receive(destination);

		this.thrown.expect(org.springframework.messaging.core.DestinationResolutionException.class);
		this.messagingTemplate.receive(destination);
	}

	@Test
	public void convertMessageFormatException() throws JMSException {
		Message<String> message = createTextMessage();
		MessageConverter messageConverter = mock(MessageConverter.class);
		willThrow(MessageFormatException.class).given(messageConverter).toMessage(eq(message), any());
		this.messagingTemplate.setJmsMessageConverter(messageConverter);
		invokeMessageCreator();

		this.thrown.expect(org.springframework.messaging.converter.MessageConversionException.class);
		this.messagingTemplate.send("myQueue", message);
	}

	@Test
	public void convertMessageNotWritableException() throws JMSException {
		Message<String> message = createTextMessage();
		MessageConverter messageConverter = mock(MessageConverter.class);
		willThrow(MessageNotWriteableException.class).given(messageConverter).toMessage(eq(message), any());
		this.messagingTemplate.setJmsMessageConverter(messageConverter);
		invokeMessageCreator();

		this.thrown.expect(org.springframework.messaging.converter.MessageConversionException.class);
		this.messagingTemplate.send("myQueue", message);
	}

	@Test
	public void convertInvalidDestinationExceptionOnSendAndReceiveWithName() {
		willThrow(InvalidDestinationException.class).given(this.jmsTemplate).sendAndReceive(eq("unknownQueue"), any());

		this.thrown.expect(org.springframework.messaging.core.DestinationResolutionException.class);
		this.messagingTemplate.sendAndReceive("unknownQueue", createTextMessage());
	}

	@Test
	public void convertInvalidDestinationExceptionOnSendAndReceive() {
		Destination destination = new Destination() {};
		willThrow(InvalidDestinationException.class).given(this.jmsTemplate).sendAndReceive(eq(destination), any());

		this.thrown.expect(org.springframework.messaging.core.DestinationResolutionException.class);
		this.messagingTemplate.sendAndReceive(destination, createTextMessage());
	}

	private void invokeMessageCreator() {
		willAnswer(invocation -> {
			MessageCreator messageCreator = (MessageCreator) invocation.getArguments()[1];
			messageCreator.createMessage(null);
			return null;
		}).given(this.jmsTemplate).send(eq("myQueue"), any());
	}


	private Message<String> createTextMessage(String payload) {
		return MessageBuilder
				.withPayload(payload).setHeader("foo", "bar").build();
	}

	private Message<String> createTextMessage() {
		return createTextMessage("Hello");
	}

	private javax.jms.Message createJmsTextMessage(String payload) {
		try {
			StubTextMessage jmsMessage = new StubTextMessage(payload);
			jmsMessage.setStringProperty("foo", "bar");
			return jmsMessage;
		}
		catch (JMSException e) {
			throw new IllegalStateException("Should not happen", e);
		}
	}

	private javax.jms.Message createJmsTextMessage() {
		return createJmsTextMessage("Hello");
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

	private void assertTextMessage(Message<?> message) {
		assertNotNull("message should not be null", message);
		assertEquals("Wrong payload", "Hello", message.getPayload());
		assertEquals("Invalid foo property", "bar", message.getHeaders().get("foo"));
	}


	protected TextMessage createTextMessage(MessageCreator creator) throws JMSException {
		Session mock = mock(Session.class);
		given(mock.createTextMessage(BDDMockito.any())).willAnswer(
				(Answer<TextMessage>) invocation ->
						new StubTextMessage((String) invocation.getArguments()[0]));
		javax.jms.Message message = creator.createMessage(mock);
		verify(mock).createTextMessage(BDDMockito.any());
		return TextMessage.class.cast(message);
	}

}
