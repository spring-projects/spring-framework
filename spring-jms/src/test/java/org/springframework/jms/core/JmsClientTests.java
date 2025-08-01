/*
 * Copyright 2002-present the original author or authors.
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

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import org.springframework.jms.InvalidDestinationException;
import org.springframework.jms.MessageNotReadableException;
import org.springframework.jms.StubTextMessage;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolutionException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JmsClient}.
 *
 * @author Juergen Hoeller
 * @since 7.0
 */
@ExtendWith(MockitoExtension.class)
class JmsClientTests {

	@Captor
	private ArgumentCaptor<MessageCreator> messageCreator;

	@Mock(strictness = Mock.Strictness.LENIENT)
	private JmsTemplate jmsTemplate;

	private JmsClient jmsClient;


	@BeforeEach
	void setup() {
		given(this.jmsTemplate.getMessageConverter()).willReturn(new SimpleMessageConverter());
		this.jmsClient = JmsClient.create(this.jmsTemplate);
	}

	@Test
	void send() {
		Destination destination = new Destination() {};
		Message<String> message = createTextMessage();

		this.jmsClient.destination(destination).send(message);
		verify(this.jmsTemplate).send(eq(destination), this.messageCreator.capture());
		assertTextMessage(this.messageCreator.getValue());
	}

	@Test
	void sendName() {
		Message<String> message = createTextMessage();

		this.jmsClient.destination("myQueue").send(message);
		verify(this.jmsTemplate).send(eq("myQueue"), this.messageCreator.capture());
		assertTextMessage(this.messageCreator.getValue());
	}

	@Test
	void convertAndSendPayload() throws JMSException {
		Destination destination = new Destination() {};

		this.jmsClient.destination(destination).send("my Payload");
		verify(this.jmsTemplate).send(eq(destination), this.messageCreator.capture());
		TextMessage textMessage = createTextMessage(this.messageCreator.getValue());
		assertThat(textMessage.getText()).isEqualTo("my Payload");
	}

	@Test
	void convertAndSendPayloadName() throws JMSException {
		this.jmsClient.destination("myQueue").send("my Payload");
		verify(this.jmsTemplate).send(eq("myQueue"), this.messageCreator.capture());
		TextMessage textMessage = createTextMessage(this.messageCreator.getValue());
		assertThat(textMessage.getText()).isEqualTo("my Payload");
	}

	@Test
	void convertAndSendPayloadAndHeaders() {
		Destination destination = new Destination() {};
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "bar");

		this.jmsClient.destination(destination).send("Hello", headers);
		verify(this.jmsTemplate).send(eq(destination), this.messageCreator.capture());
		assertTextMessage(this.messageCreator.getValue()); // see createTextMessage
	}

	@Test
	void convertAndSendPayloadAndHeadersName() {
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "bar");

		this.jmsClient.destination("myQueue").send("Hello", headers);
		verify(this.jmsTemplate).send(eq("myQueue"), this.messageCreator.capture());
		assertTextMessage(this.messageCreator.getValue()); // see createTextMessage
	}

	@Test
	void convertAndSendPayloadAndHeadersWithPostProcessor() throws JMSException {
		Destination destination = new Destination() {};
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "bar");

		this.jmsClient = JmsClient.builder(this.jmsTemplate)
				.messagePostProcessor(msg -> MessageBuilder.fromMessage(msg).setHeader("spring", "framework").build())
				.build();
		this.jmsClient.destination(destination).send("Hello", headers);
		verify(this.jmsTemplate).send(eq(destination), this.messageCreator.capture());
		TextMessage jmsMessage = createTextMessage(this.messageCreator.getValue());
		assertThat(jmsMessage.getObjectProperty("spring")).isEqualTo("framework");
	}

	@Test
	void receive() {
		Destination destination = new Destination() {};
		jakarta.jms.Message jmsMessage = createJmsTextMessage();
		given(this.jmsTemplate.receive(destination)).willReturn(jmsMessage);

		Message<?> message = this.jmsClient.destination(destination).receive().get();
		verify(this.jmsTemplate).receive(destination);
		assertTextMessage(message);
	}

	@Test
	void receiveName() {
		jakarta.jms.Message jmsMessage = createJmsTextMessage();
		given(this.jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		Message<?> message = this.jmsClient.destination("myQueue").receive().get();
		verify(this.jmsTemplate).receive("myQueue");
		assertTextMessage(message);
	}

	@Test
	void receiveSelected() {
		Destination destination = new Destination() {};
		jakarta.jms.Message jmsMessage = createJmsTextMessage();
		given(this.jmsTemplate.receiveSelected(destination, "selector")).willReturn(jmsMessage);

		Message<?> message = this.jmsClient.destination(destination).receive("selector").get();
		verify(this.jmsTemplate).receiveSelected(destination, "selector");
		assertTextMessage(message);
	}

	@Test
	void receiveSelectedName() {
		jakarta.jms.Message jmsMessage = createJmsTextMessage();
		given(this.jmsTemplate.receiveSelected("myQueue", "selector")).willReturn(jmsMessage);

		Message<?> message = this.jmsClient.destination("myQueue").receive("selector").get();
		verify(this.jmsTemplate).receiveSelected("myQueue", "selector");
		assertTextMessage(message);
	}

	@Test
	void receiveAndConvert() {
		Destination destination = new Destination() {};
		jakarta.jms.Message jmsMessage = createJmsTextMessage("my Payload");
		given(this.jmsTemplate.receive(destination)).willReturn(jmsMessage);

		String payload = this.jmsClient.destination(destination).receive(String.class).get();
		assertThat(payload).isEqualTo("my Payload");
		verify(this.jmsTemplate).receive(destination);
	}

	@Test
	void receiveAndConvertName() {
		jakarta.jms.Message jmsMessage = createJmsTextMessage("my Payload");
		given(this.jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		String payload = this.jmsClient.destination("myQueue").receive(String.class).get();
		assertThat(payload).isEqualTo("my Payload");
		verify(this.jmsTemplate).receive("myQueue");
	}

	@Test
	void receiveAndConvertWithConversion() {
		jakarta.jms.Message jmsMessage = createJmsTextMessage("123");
		given(this.jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		this.jmsClient = JmsClient.builder(this.jmsTemplate).messageConverter(new GenericMessageConverter()).build();

		Integer payload = this.jmsClient.destination("myQueue").receive(Integer.class).get();
		assertThat(payload).isEqualTo(Integer.valueOf(123));
		verify(this.jmsTemplate).receive("myQueue");
	}

	@Test
	void receiveAndConvertNoConverter() {
		jakarta.jms.Message jmsMessage = createJmsTextMessage("Hello");
		given(this.jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		assertThatExceptionOfType(org.springframework.messaging.converter.MessageConversionException.class).isThrownBy(() ->
				this.jmsClient.destination("myQueue").receive(Writer.class));
	}

	@Test
	void receiveAndConvertNoInput() {
		given(this.jmsTemplate.receive("myQueue")).willReturn(null);

		assertThat(this.jmsClient.destination("myQueue").receive(String.class)).isEmpty();
	}

	@Test
	void receiveSelectedAndConvert() {
		Destination destination = new Destination() {};
		jakarta.jms.Message jmsMessage = createJmsTextMessage("my Payload");
		given(this.jmsTemplate.receiveSelected(destination, "selector")).willReturn(jmsMessage);

		String payload = this.jmsClient.destination(destination).receive("selector", String.class).get();
		assertThat(payload).isEqualTo("my Payload");
		verify(this.jmsTemplate).receiveSelected(destination, "selector");
	}

	@Test
	void receiveSelectedAndConvertName() {
		jakarta.jms.Message jmsMessage = createJmsTextMessage("my Payload");
		given(this.jmsTemplate.receiveSelected("myQueue", "selector")).willReturn(jmsMessage);

		String payload = this.jmsClient.destination("myQueue").receive("selector", String.class).get();
		assertThat(payload).isEqualTo("my Payload");
		verify(this.jmsTemplate).receiveSelected("myQueue", "selector");
	}

	@Test
	void receiveSelectedAndConvertWithConversion() {
		jakarta.jms.Message jmsMessage = createJmsTextMessage("123");
		given(this.jmsTemplate.receiveSelected("myQueue", "selector")).willReturn(jmsMessage);

		this.jmsClient = JmsClient.builder(this.jmsTemplate).messageConverter(new GenericMessageConverter()).build();

		Integer payload = this.jmsClient.destination("myQueue").receive("selector", Integer.class).get();
		assertThat(payload).isEqualTo(Integer.valueOf(123));
		verify(this.jmsTemplate).receiveSelected("myQueue", "selector");
	}

	@Test
	void receiveSelectedAndConvertNoConverter() {
		jakarta.jms.Message jmsMessage = createJmsTextMessage("Hello");
		given(this.jmsTemplate.receiveSelected("myQueue", "selector")).willReturn(jmsMessage);

		assertThatExceptionOfType(org.springframework.messaging.converter.MessageConversionException.class).isThrownBy(() ->
				this.jmsClient.destination("myQueue").receive("selector", Writer.class));
	}

	@Test
	void receiveSelectedAndConvertNoInput() {
		given(this.jmsTemplate.receiveSelected("myQueue", "selector")).willReturn(null);

		assertThat(this.jmsClient.destination("myQueue").receive("selector", String.class)).isEmpty();
	}

	@Test
	void sendAndReceive() {
		Destination destination = new Destination() {};
		Message<String> request = createTextMessage();
		jakarta.jms.Message replyJmsMessage = createJmsTextMessage();
		given(this.jmsTemplate.sendAndReceive(eq(destination), any())).willReturn(replyJmsMessage);

		Message<?> actual = this.jmsClient.destination(destination).sendAndReceive(request).get();
		verify(this.jmsTemplate, times(1)).sendAndReceive(eq(destination), any());
		assertTextMessage(actual);
	}

	@Test
	void sendAndReceiveName() {
		Message<String> request = createTextMessage();
		jakarta.jms.Message replyJmsMessage = createJmsTextMessage();
		given(this.jmsTemplate.sendAndReceive(eq("myQueue"), any())).willReturn(replyJmsMessage);

		Message<?> actual = this.jmsClient.destination("myQueue").sendAndReceive(request).get();
		verify(this.jmsTemplate, times(1)).sendAndReceive(eq("myQueue"), any());
		assertTextMessage(actual);
	}

	@Test
	void convertSendAndReceivePayload() {
		Destination destination = new Destination() {};
		jakarta.jms.Message replyJmsMessage = createJmsTextMessage("My reply");
		given(this.jmsTemplate.sendAndReceive(eq(destination), any())).willReturn(replyJmsMessage);

		String reply = this.jmsClient.destination(destination).sendAndReceive("my Payload", String.class).get();
		verify(this.jmsTemplate, times(1)).sendAndReceive(eq(destination), any());
		assertThat(reply).isEqualTo("My reply");
	}

	@Test
	void convertSendAndReceivePayloadWithPostProcessor() throws JMSException {
		Destination destination = new Destination() {};
		jakarta.jms.Message replyJmsMessage = createJmsTextMessage("My reply");
		given(this.jmsTemplate.sendAndReceive(eq(destination), any())).willReturn(replyJmsMessage);

		this.jmsClient = JmsClient.builder(this.jmsTemplate)
				.messagePostProcessor(msg -> MessageBuilder.fromMessage(msg).setHeader("spring", "framework").build())
				.build();
		this.jmsClient.destination(destination).sendAndReceive("my Payload", String.class);
		verify(this.jmsTemplate).sendAndReceive(eq(destination), this.messageCreator.capture());
		TextMessage jmsMessage = createTextMessage(this.messageCreator.getValue());
		assertThat(jmsMessage.getObjectProperty("spring")).isEqualTo("framework");
		verify(this.jmsTemplate, times(1)).sendAndReceive(eq(destination), any());
	}

	@Test
	void convertSendAndReceivePayloadName() {
		jakarta.jms.Message replyJmsMessage = createJmsTextMessage("My reply");
		given(this.jmsTemplate.sendAndReceive(eq("myQueue"), any())).willReturn(replyJmsMessage);

		String reply = this.jmsClient.destination("myQueue").sendAndReceive("my Payload", String.class).get();
		verify(this.jmsTemplate, times(1)).sendAndReceive(eq("myQueue"), any());
		assertThat(reply).isEqualTo("My reply");
	}

	@Test
	void convertMessageNotReadableException() {
		willThrow(MessageNotReadableException.class).given(this.jmsTemplate).receive("myQueue");

		assertThatExceptionOfType(MessagingException.class).isThrownBy(() ->
				this.jmsClient.destination("myQueue").receive());
	}

	@Test
	void convertDestinationResolutionExceptionOnSend() {
		Destination destination = new Destination() {};
		willThrow(DestinationResolutionException.class).given(this.jmsTemplate).send(eq(destination), any());

		assertThatExceptionOfType(org.springframework.messaging.core.DestinationResolutionException.class).isThrownBy(() ->
				this.jmsClient.destination(destination).send(createTextMessage()));
	}

	@Test
	void convertDestinationResolutionExceptionOnReceive() {
		Destination destination = new Destination() {};
		willThrow(DestinationResolutionException.class).given(this.jmsTemplate).receive(destination);

		assertThatExceptionOfType(org.springframework.messaging.core.DestinationResolutionException.class).isThrownBy(() ->
				this.jmsClient.destination(destination).receive());
	}

	@Test
	void convertInvalidDestinationExceptionOnSendAndReceiveWithName() {
		willThrow(InvalidDestinationException.class).given(this.jmsTemplate).sendAndReceive(eq("unknownQueue"), any());

		assertThatExceptionOfType(org.springframework.messaging.core.DestinationResolutionException.class).isThrownBy(() ->
				this.jmsClient.destination("unknownQueue").sendAndReceive(createTextMessage()));
	}

	@Test
	void convertInvalidDestinationExceptionOnSendAndReceive() {
		Destination destination = new Destination() {};
		willThrow(InvalidDestinationException.class).given(this.jmsTemplate).sendAndReceive(eq(destination), any());

		assertThatExceptionOfType(org.springframework.messaging.core.DestinationResolutionException.class).isThrownBy(() ->
				this.jmsClient.destination(destination).sendAndReceive(createTextMessage()));
	}

	@Test
	void sendWithDefaults() throws Exception {
		ConnectionFactory connectionFactory = mock();
		Connection connection = mock();
		Session session = mock();
		Queue queue = mock();
		MessageProducer messageProducer = mock();
		TextMessage textMessage = mock();

		given(connectionFactory.createConnection()).willReturn(connection);
		given(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).willReturn(session);
		given(session.createProducer(queue)).willReturn(messageProducer);
		given(session.createTextMessage("just testing")).willReturn(textMessage);

		JmsClient.create(connectionFactory).destination(queue)
				.send("just testing");

		verify(messageProducer).send(textMessage);
		verify(messageProducer).close();
		verify(session).close();
		verify(connection).close();
	}

	@Test
	void sendWithPostProcessor() throws Exception {
		ConnectionFactory connectionFactory = mock();
		Connection connection = mock();
		Session session = mock();
		Queue queue = mock();
		MessageProducer messageProducer = mock();
		TextMessage textMessage = mock();

		given(connectionFactory.createConnection()).willReturn(connection);
		given(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).willReturn(session);
		given(session.createProducer(queue)).willReturn(messageProducer);
		given(session.createTextMessage("just testing")).willReturn(textMessage);

		JmsClient.builder(connectionFactory)
				.messagePostProcessor(msg -> MessageBuilder.fromMessage(msg).setHeader("spring", "framework").build())
				.build()
				.destination(queue).send("just testing");

		verify(textMessage).setObjectProperty("spring", "framework");
		verify(messageProducer).send(textMessage);
		verify(messageProducer).close();
		verify(session).close();
		verify(connection).close();
	}

	@Test
	void sendWithCustomSettings() throws Exception {
		ConnectionFactory connectionFactory = mock();
		Connection connection = mock();
		Session session = mock();
		Queue queue = mock();
		MessageProducer messageProducer = mock();
		TextMessage textMessage = mock();

		given(connectionFactory.createConnection()).willReturn(connection);
		given(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).willReturn(session);
		given(session.createProducer(queue)).willReturn(messageProducer);
		given(session.createTextMessage("just testing")).willReturn(textMessage);

		JmsClient.create(connectionFactory).destination(queue)
				.withDeliveryDelay(0).withDeliveryPersistent(false).withPriority(2).withTimeToLive(3)
				.send("just testing");

		verify(messageProducer).setDeliveryDelay(0);
		verify(messageProducer).send(textMessage, DeliveryMode.NON_PERSISTENT, 2, 3);
		verify(messageProducer).close();
		verify(session).close();
		verify(connection).close();
	}

	@Test
	void receiveWithDefaults() throws Exception {
		ConnectionFactory connectionFactory = mock();
		Connection connection = mock();
		Session session = mock();
		Queue queue = mock();
		MessageConsumer messageConsumer = mock();
		TextMessage textMessage = mock();

		given(connectionFactory.createConnection()).willReturn(connection);
		given(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).willReturn(session);
		given(session.createConsumer(queue, null)).willReturn(messageConsumer);
		given(messageConsumer.receive()).willReturn(textMessage);
		given(textMessage.getText()).willReturn("Hello World!");

		String result = JmsClient.create(connectionFactory).destination(queue)
				.receive(String.class).get();
		assertThat(result).isEqualTo("Hello World!");

		verify(connection).start();
		verify(messageConsumer).close();
		verify(session).close();
		verify(connection).close();
	}

	@Test
	void receiveWithCustomTimeout() throws Exception {
		ConnectionFactory connectionFactory = mock();
		Connection connection = mock();
		Session session = mock();
		Queue queue = mock();
		MessageConsumer messageConsumer = mock();
		TextMessage textMessage = mock();

		given(connectionFactory.createConnection()).willReturn(connection);
		given(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).willReturn(session);
		given(session.createConsumer(queue, null)).willReturn(messageConsumer);
		given(messageConsumer.receive(10)).willReturn(textMessage);
		given(textMessage.getText()).willReturn("Hello World!");

		String result = JmsClient.create(connectionFactory).destination(queue)
				.withReceiveTimeout(10).receive(String.class).get();
		assertThat(result).isEqualTo("Hello World!");

		verify(connection).start();
		verify(messageConsumer).close();
		verify(session).close();
		verify(connection).close();
	}


	private Message<String> createTextMessage(String payload) {
		return MessageBuilder.withPayload(payload).setHeader("foo", "bar").build();
	}

	private Message<String> createTextMessage() {
		return createTextMessage("Hello");
	}

	private jakarta.jms.Message createJmsTextMessage(String payload) {
		StubTextMessage jmsMessage = new StubTextMessage(payload);
		jmsMessage.setStringProperty("foo", "bar");
		return jmsMessage;
	}

	private jakarta.jms.Message createJmsTextMessage() {
		return createJmsTextMessage("Hello");
	}

	private void assertTextMessage(MessageCreator messageCreator) {
		try {
			TextMessage jmsMessage = createTextMessage(messageCreator);
			assertThat(jmsMessage.getText()).as("Wrong body message").isEqualTo("Hello");
			assertThat(jmsMessage.getStringProperty("foo")).as("Invalid foo property").isEqualTo("bar");
		}
		catch (JMSException e) {
			throw new IllegalStateException("Wrong text message", e);
		}
	}

	private void assertTextMessage(Message<?> message) {
		assertThat(message).as("message should not be null").isNotNull();
		assertThat(message.getPayload()).as("Wrong payload").isEqualTo("Hello");
		assertThat(message.getHeaders().get("foo")).as("Invalid foo property").isEqualTo("bar");
	}

	protected TextMessage createTextMessage(MessageCreator creator) throws JMSException {
		Session mock = mock();
		given(mock.createTextMessage(any())).willAnswer(
				(Answer<TextMessage>) invocation ->
						new StubTextMessage((String) invocation.getArguments()[0]));
		jakarta.jms.Message message = creator.createMessage(mock);
		verify(mock).createTextMessage(any());
		return (TextMessage) message;
	}

}
