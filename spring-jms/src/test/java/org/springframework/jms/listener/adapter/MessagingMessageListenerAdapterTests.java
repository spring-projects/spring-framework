/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jms.listener.adapter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.jms.StubTextMessage;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Stephane Nicoll
 */
public class MessagingMessageListenerAdapterTests {

	private static final Destination sharedReplyDestination = mock();

	private final DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();

	private final SampleBean sample = new SampleBean();


	@BeforeEach
	public void setup() {
		initializeFactory(factory);
	}

	@Test
	public void buildMessageWithStandardMessage() throws JMSException {
		Destination replyTo = new Destination() {};
		Message<String> result = MessageBuilder.withPayload("Response")
				.setHeader("foo", "bar")
				.setHeader(JmsHeaders.TYPE, "msg_type")
				.setHeader(JmsHeaders.REPLY_TO, replyTo)
				.build();

		Session session = mock();
		given(session.createTextMessage("Response")).willReturn(new StubTextMessage("Response"));
		MessagingMessageListenerAdapter listener = getSimpleInstance("echo", Message.class);
		jakarta.jms.Message replyMessage = listener.buildMessage(session, result);

		verify(session).createTextMessage("Response");
		assertThat(replyMessage).as("reply should never be null").isNotNull();
		assertThat(((TextMessage) replyMessage).getText()).isEqualTo("Response");
		assertThat(replyMessage.getStringProperty("foo")).as("custom header not copied").isEqualTo("bar");
		assertThat(replyMessage.getJMSType()).as("type header not copied").isEqualTo("msg_type");
		assertThat(replyMessage.getJMSReplyTo()).as("replyTo header not copied").isEqualTo(replyTo);
	}

	@Test
	public void exceptionInListener() {
		jakarta.jms.Message message = new StubTextMessage("foo");
		Session session = mock();
		MessagingMessageListenerAdapter listener = getSimpleInstance("fail", String.class);
		assertThatExceptionOfType(ListenerExecutionFailedException.class)
			.isThrownBy(() -> listener.onMessage(message, session))
			.havingCause()
			.isExactlyInstanceOf(IllegalArgumentException.class)
			.withMessage("Expected test exception");
	}

	@Test
	public void exceptionInInvocation() {
		jakarta.jms.Message message = new StubTextMessage("foo");
		Session session = mock();
		MessagingMessageListenerAdapter listener = getSimpleInstance("wrongParam", Integer.class);

		assertThatExceptionOfType(ListenerExecutionFailedException.class).isThrownBy(() ->
				listener.onMessage(message, session))
			.withCauseExactlyInstanceOf(MessageConversionException.class);
	}

	@Test
	public void payloadConversionLazilyInvoked() throws JMSException {
		jakarta.jms.Message jmsMessage = mock();
		MessageConverter messageConverter = mock();
		given(messageConverter.fromMessage(jmsMessage)).willReturn("FooBar");
		MessagingMessageListenerAdapter listener = getSimpleInstance("simple", Message.class);
		listener.setMessageConverter(messageConverter);
		Message<?> message = listener.toMessagingMessage(jmsMessage);
		verify(messageConverter, never()).fromMessage(jmsMessage);
		assertThat(message.getPayload()).isEqualTo("FooBar");
		verify(messageConverter, times(1)).fromMessage(jmsMessage);
	}

	@Test
	public void headerConversionLazilyInvoked() throws JMSException {
		jakarta.jms.Message jmsMessage = mock();
		given(jmsMessage.getPropertyNames()).willThrow(new IllegalArgumentException("Header failure"));
		MessagingMessageListenerAdapter listener = getSimpleInstance("simple", Message.class);
		Message<?> message = listener.toMessagingMessage(jmsMessage);

		// Triggers headers resolution
		assertThatIllegalArgumentException().isThrownBy(
				message::getHeaders)
			.withMessageContaining("Header failure");
	}

	@Test
	public void incomingMessageUsesMessageConverter() throws JMSException {
		jakarta.jms.Message jmsMessage = mock();
		Session session = mock();
		MessageConverter messageConverter = mock();
		given(messageConverter.fromMessage(jmsMessage)).willReturn("FooBar");
		MessagingMessageListenerAdapter listener = getSimpleInstance("simple", Message.class);
		listener.setMessageConverter(messageConverter);
		listener.onMessage(jmsMessage, session);
		verify(messageConverter, times(1)).fromMessage(jmsMessage);
		assertThat(sample.simples).hasSize(1);
		assertThat(sample.simples.get(0).getPayload()).isEqualTo("FooBar");
	}

	@Test
	public void replyUsesMessageConverterForPayload() throws JMSException {
		Session session = mock();
		MessageConverter messageConverter = mock();
		given(messageConverter.toMessage("Response", session)).willReturn(new StubTextMessage("Response"));

		Message<String> result = MessageBuilder.withPayload("Response").build();
		MessagingMessageListenerAdapter listener = getSimpleInstance("echo", Message.class);
		listener.setMessageConverter(messageConverter);
		jakarta.jms.Message replyMessage = listener.buildMessage(session, result);

		verify(messageConverter, times(1)).toMessage("Response", session);
		assertThat(replyMessage).as("reply should never be null").isNotNull();
		assertThat(((TextMessage) replyMessage).getText()).isEqualTo("Response");
	}

	@Test
	public void replyPayloadToQueue() throws JMSException {
		Session session = mock();
		Queue replyDestination = mock();
		given(session.createQueue("queueOut")).willReturn(replyDestination);

		MessageProducer messageProducer = mock();
		TextMessage responseMessage = mock();
		given(session.createTextMessage("Response")).willReturn(responseMessage);
		given(session.createProducer(replyDestination)).willReturn(messageProducer);

		MessagingMessageListenerAdapter listener = getPayloadInstance("Response", "replyPayloadToQueue", Message.class);
		listener.onMessage(mock(), session);

		verify(session).createQueue("queueOut");
		verify(session).createTextMessage("Response");
		verify(messageProducer).send(responseMessage);
		verify(messageProducer).close();
	}

	@Test
	public void replyWithCustomTimeToLive() throws JMSException {
		Session session = mock();
		Queue replyDestination = mock();
		given(session.createQueue("queueOut")).willReturn(replyDestination);

		MessageProducer messageProducer = mock();
		TextMessage responseMessage = mock();
		given(session.createTextMessage("Response")).willReturn(responseMessage);
		given(session.createProducer(replyDestination)).willReturn(messageProducer);

		MessagingMessageListenerAdapter listener = getPayloadInstance("Response", "replyPayloadToQueue", Message.class);
		QosSettings settings = new QosSettings();
		settings.setTimeToLive(6000);
		listener.setResponseQosSettings(settings);
		listener.onMessage(mock(), session);
		verify(session).createQueue("queueOut");
		verify(session).createTextMessage("Response");
		verify(messageProducer).send(responseMessage, jakarta.jms.Message.DEFAULT_DELIVERY_MODE,
				jakarta.jms.Message.DEFAULT_PRIORITY, 6000);
		verify(messageProducer).close();
	}

	@Test
	public void replyWithFullQoS() throws JMSException {
		Session session = mock();
		Queue replyDestination = mock();
		given(session.createQueue("queueOut")).willReturn(replyDestination);

		MessageProducer messageProducer = mock();
		TextMessage responseMessage = mock();
		given(session.createTextMessage("Response")).willReturn(responseMessage);
		given(session.createProducer(replyDestination)).willReturn(messageProducer);

		MessagingMessageListenerAdapter listener = getPayloadInstance("Response", "replyPayloadToQueue", Message.class);
		QosSettings settings = new QosSettings(DeliveryMode.NON_PERSISTENT, 6, 6000);
		listener.setResponseQosSettings(settings);
		listener.onMessage(mock(), session);
		verify(session).createQueue("queueOut");
		verify(session).createTextMessage("Response");
		verify(messageProducer).send(responseMessage, DeliveryMode.NON_PERSISTENT, 6, 6000);
		verify(messageProducer).close();
	}

	@Test
	public void replyPayloadToTopic() throws JMSException {
		Session session = mock();
		Topic replyDestination = mock();
		given(session.createTopic("topicOut")).willReturn(replyDestination);

		MessageProducer messageProducer = mock();
		TextMessage responseMessage = mock();
		given(session.createTextMessage("Response")).willReturn(responseMessage);
		given(session.createProducer(replyDestination)).willReturn(messageProducer);

		MessagingMessageListenerAdapter listener = getPayloadInstance("Response", "replyPayloadToTopic", Message.class);
		listener.onMessage(mock(), session);

		verify(session).createTopic("topicOut");
		verify(session).createTextMessage("Response");
		verify(messageProducer).send(responseMessage);
		verify(messageProducer).close();
	}

	@Test
	public void replyPayloadToDestination() throws JMSException {
		Session session = mock();
		MessageProducer messageProducer = mock();
		TextMessage responseMessage = mock();
		given(session.createTextMessage("Response")).willReturn(responseMessage);
		given(session.createProducer(sharedReplyDestination)).willReturn(messageProducer);

		MessagingMessageListenerAdapter listener = getPayloadInstance("Response", "replyPayloadToDestination", Message.class);
		listener.onMessage(mock(), session);

		verify(session, times(0)).createQueue(anyString());
		verify(session).createTextMessage("Response");
		verify(messageProducer).send(responseMessage);
		verify(messageProducer).close();
	}

	@Test
	public void replyPayloadNoDestination() throws JMSException {
		Queue replyDestination = mock();

		Session session = mock();
		MessageProducer messageProducer = mock();
		TextMessage responseMessage = mock();
		given(session.createTextMessage("Response")).willReturn(responseMessage);
		given(session.createProducer(replyDestination)).willReturn(messageProducer);

		MessagingMessageListenerAdapter listener =
				getPayloadInstance("Response", "replyPayloadNoDestination", Message.class);
		listener.setDefaultResponseDestination(replyDestination);
		listener.onMessage(mock(), session);

		verify(session, times(0)).createQueue(anyString());
		verify(session).createTextMessage("Response");
		verify(messageProducer).send(responseMessage);
		verify(messageProducer).close();
	}

	@Test
	public void replyJackson() throws JMSException {
		TextMessage reply = testReplyWithJackson("replyJackson",
				"{\"counter\":42,\"name\":\"Response\",\"description\":\"lengthy description\"}");
		verify(reply).setObjectProperty("foo", "bar");
	}

	@Test
	public void replyJacksonMessageAndJsonView() throws JMSException {
		TextMessage reply = testReplyWithJackson("replyJacksonMessageAndJsonView",
				"{\"name\":\"Response\"}");
		verify(reply).setObjectProperty("foo", "bar");
	}

	@Test
	public void replyJacksonPojoAndJsonView() throws JMSException {
		TextMessage reply = testReplyWithJackson("replyJacksonPojoAndJsonView",
				"{\"name\":\"Response\"}");
		verify(reply, never()).setObjectProperty("foo", "bar");
	}

	public TextMessage testReplyWithJackson(String methodName, String replyContent) throws JMSException {
		Queue replyDestination = mock();

		Session session = mock();
		MessageProducer messageProducer = mock();
		TextMessage responseMessage = mock();
		given(session.createTextMessage(replyContent)).willReturn(responseMessage);
		given(session.createProducer(replyDestination)).willReturn(messageProducer);

		MessagingMessageListenerAdapter listener = getPayloadInstance("Response", methodName, Message.class);
		MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
		messageConverter.setTargetType(MessageType.TEXT);
		listener.setMessageConverter(messageConverter);
		listener.setDefaultResponseDestination(replyDestination);
		listener.onMessage(mock(), session);

		verify(session, times(0)).createQueue(anyString());
		verify(session).createTextMessage(replyContent);
		verify(messageProducer).send(responseMessage);
		verify(messageProducer).close();
		return responseMessage;
	}


	protected MessagingMessageListenerAdapter getSimpleInstance(String methodName, Class<?>... parameterTypes) {
		Method m = ReflectionUtils.findMethod(SampleBean.class, methodName, parameterTypes);
		return createInstance(m);
	}

	protected MessagingMessageListenerAdapter createInstance(Method m) {
		MessagingMessageListenerAdapter adapter = new MessagingMessageListenerAdapter();
		adapter.setHandlerMethod(factory.createInvocableHandlerMethod(sample, m));
		return adapter;
	}

	protected MessagingMessageListenerAdapter getPayloadInstance(final Object payload,
			String methodName, Class<?>... parameterTypes) {

		Method method = ReflectionUtils.findMethod(SampleBean.class, methodName, parameterTypes);
		MessagingMessageListenerAdapter adapter = new MessagingMessageListenerAdapter() {
			@Override
			protected Object extractMessage(jakarta.jms.Message message) {
				return payload;
			}
		};
		adapter.setHandlerMethod(factory.createInvocableHandlerMethod(sample, method));
		return adapter;
	}

	private void initializeFactory(DefaultMessageHandlerMethodFactory factory) {
		factory.setBeanFactory(new StaticListableBeanFactory());
		factory.afterPropertiesSet();
	}


	@SuppressWarnings("unused")
	private static class SampleBean {

		public final List<Message<String>> simples = new ArrayList<>();

		public void simple(Message<String> input) {
			simples.add(input);
		}

		public Message<String> echo(Message<String> input) {
			return MessageBuilder.withPayload(input.getPayload())
					.setHeader(JmsHeaders.TYPE, "reply")
					.build();
		}

		public JmsResponse<String> replyPayloadToQueue(Message<String> input) {
			return JmsResponse.forQueue(input.getPayload(), "queueOut");
		}

		public JmsResponse<String> replyPayloadToTopic(Message<String> input) {
			return JmsResponse.forTopic(input.getPayload(), "topicOut");
		}

		public JmsResponse<String> replyPayloadToDestination(Message<String> input) {
			return JmsResponse.forDestination(input.getPayload(), sharedReplyDestination);
		}

		public JmsResponse<String> replyPayloadNoDestination(Message<String> input) {
			return new JmsResponse<>(input.getPayload(), null);
		}

		public Message<SampleResponse> replyJackson(Message<String> input) {
			return MessageBuilder.withPayload(createSampleResponse(input.getPayload()))
					.setHeader("foo", "bar").build();
		}

		@JsonView(Summary.class)
		public Message<SampleResponse> replyJacksonMessageAndJsonView(Message<String> input) {
			return MessageBuilder.withPayload(createSampleResponse(input.getPayload()))
					.setHeader("foo", "bar").build();
		}

		@JsonView(Summary.class)
		public SampleResponse replyJacksonPojoAndJsonView(Message<String> input) {
			return createSampleResponse(input.getPayload());
		}

		private SampleResponse createSampleResponse(String name) {
			return new SampleResponse(name, "lengthy description");
		}

		public void fail(String input) {
			throw new IllegalArgumentException("Expected test exception");
		}

		public void wrongParam(Integer i) {
			throw new IllegalArgumentException("Should not have been called");
		}
	}

	interface Summary {}
	interface Full extends Summary {}

	@SuppressWarnings("unused")
	private static class SampleResponse {

		private int counter = 42;

		@JsonView(Summary.class)
		private String name;

		@JsonView(Full.class)
		private String description;

		public SampleResponse(String name, String description) {
			this.name = name;
			this.description = description;
		}

		public int getCounter() {
			return counter;
		}

		public void setCounter(int counter) {
			this.counter = counter;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

}
