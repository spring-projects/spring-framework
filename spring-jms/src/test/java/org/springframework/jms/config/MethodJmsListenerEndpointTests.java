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

package org.springframework.jms.config;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import jakarta.jms.Destination;
import jakarta.jms.InvalidDestinationException;
import jakarta.jms.JMSException;
import jakarta.jms.ObjectMessage;
import jakarta.jms.QueueSender;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.jms.StubTextMessage;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.ListenerExecutionFailedException;
import org.springframework.jms.listener.adapter.MessagingMessageListenerAdapter;
import org.springframework.jms.listener.adapter.ReplyFailureException;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.jms.support.JmsMessageHeaderAccessor;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class MethodJmsListenerEndpointTests {

	private final DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();

	private final DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();

	private final JmsEndpointSampleBean sample = new JmsEndpointSampleBean();

	private String methodName;


	@BeforeEach
	void setup(TestInfo testInfo) {
		this.methodName = testInfo.getTestMethod().get().getName();
		initializeFactory(this.factory);
	}


	@Test
	void createMessageListenerNoFactory() {
		MethodJmsListenerEndpoint endpoint = new MethodJmsListenerEndpoint();
		endpoint.setBean(this);
		endpoint.setMethod(getTestMethod());

		assertThatIllegalStateException().isThrownBy(() ->
				endpoint.createMessageListener(this.container));
	}

	@Test
	void createMessageListener() {
		MethodJmsListenerEndpoint endpoint = new MethodJmsListenerEndpoint();
		endpoint.setBean(this);
		endpoint.setMethod(getTestMethod());
		endpoint.setMessageHandlerMethodFactory(this.factory);

		assertThat(endpoint.createMessageListener(this.container)).isNotNull();
	}

	@Test
	void setExtraCollaborators() {
		MessageConverter messageConverter = mock();
		DestinationResolver destinationResolver = mock();
		this.container.setMessageConverter(messageConverter);
		this.container.setDestinationResolver(destinationResolver);

		MessagingMessageListenerAdapter listener = createInstance(this.factory,
				getListenerMethod("resolveObjectPayload", MyBean.class), this.container);
		DirectFieldAccessor accessor = new DirectFieldAccessor(listener);
		assertThat(accessor.getPropertyValue("messageConverter")).isSameAs(messageConverter);
		assertThat(accessor.getPropertyValue("destinationResolver")).isSameAs(destinationResolver);
	}

	@Test
	void resolveMessageAndSession() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(jakarta.jms.Message.class, Session.class);

		Session session = mock();
		listener.onMessage(createSimpleJmsTextMessage("test"), session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	void resolveGenericMessage() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(Message.class);

		Session session = mock();
		listener.onMessage(createSimpleJmsTextMessage("test"), session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	void resolveHeaderAndPayload() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(String.class, int.class);

		Session session = mock();
		StubTextMessage message = createSimpleJmsTextMessage("my payload");
		message.setIntProperty("myCounter", 55);
		listener.onMessage(message, session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	void resolveCustomHeaderNameAndPayload() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(String.class, int.class);

		Session session = mock();
		StubTextMessage message = createSimpleJmsTextMessage("my payload");
		message.setIntProperty("myCounter", 24);
		listener.onMessage(message, session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	void resolveCustomHeaderNameAndPayloadWithHeaderNameSet() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(String.class, int.class);

		Session session = mock();
		StubTextMessage message = createSimpleJmsTextMessage("my payload");
		message.setIntProperty("myCounter", 24);
		listener.onMessage(message, session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	void resolveHeaders() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(String.class, Map.class);

		Session session = mock();
		StubTextMessage message = createSimpleJmsTextMessage("my payload");
		message.setIntProperty("customInt", 1234);
		message.setJMSMessageID("abcd-1234");
		listener.onMessage(message, session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	void resolveMessageHeaders() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(MessageHeaders.class);

		Session session = mock();
		StubTextMessage message = createSimpleJmsTextMessage("my payload");
		message.setLongProperty("customLong", 4567L);
		message.setJMSType("myMessageType");
		listener.onMessage(message, session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	void resolveJmsMessageHeaderAccessor() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(JmsMessageHeaderAccessor.class);

		Session session = mock();
		StubTextMessage message = createSimpleJmsTextMessage("my payload");
		message.setBooleanProperty("customBoolean", true);
		message.setJMSPriority(9);
		listener.onMessage(message, session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	void resolveObjectPayload() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(MyBean.class);
		MyBean myBean = new MyBean();
		myBean.name = "myBean name";

		Session session = mock();
		ObjectMessage message = mock();
		given(message.getObject()).willReturn(myBean);

		listener.onMessage(message, session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	void resolveConvertedPayload() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(Integer.class);

		Session session = mock();

		listener.onMessage(createSimpleJmsTextMessage("33"), session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	void processAndReply() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(String.class);
		String body = "echo text";
		String correlationId = "link-1234";
		Destination replyDestination = new Destination() {};

		TextMessage reply = mock();
		QueueSender queueSender = mock();
		Session session = mock();
		given(session.createTextMessage(body)).willReturn(reply);
		given(session.createProducer(replyDestination)).willReturn(queueSender);

		listener.setDefaultResponseDestination(replyDestination);
		StubTextMessage inputMessage = createSimpleJmsTextMessage(body);
		inputMessage.setJMSCorrelationID(correlationId);
		listener.onMessage(inputMessage, session);
		assertDefaultListenerMethodInvocation();

		verify(reply).setJMSCorrelationID(correlationId);
		verify(queueSender).send(reply);
		verify(queueSender).close();
	}

	@Test
	void processAndReplyWithSendToQueue() throws JMSException {
		String methodName = "processAndReplyWithSendTo";
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		MessagingMessageListenerAdapter listener = createInstance(this.factory,
				getListenerMethod(methodName, String.class), container);
		processAndReplyWithSendTo(listener, "replyDestination", false);
		assertListenerMethodInvocation(this.sample, methodName);
	}

	@Test
	void processFromTopicAndReplyWithSendToQueue() throws JMSException {
		String methodName = "processAndReplyWithSendTo";
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setPubSubDomain(true);
		container.setReplyPubSubDomain(false);
		MessagingMessageListenerAdapter listener = createInstance(this.factory,
				getListenerMethod(methodName, String.class), container);
		processAndReplyWithSendTo(listener, "replyDestination", false);
		assertListenerMethodInvocation(this.sample, methodName);
	}

	@Test
	void processAndReplyWithSendToTopic() throws JMSException {
		String methodName = "processAndReplyWithSendTo";
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setPubSubDomain(true);
		MessagingMessageListenerAdapter listener = createInstance(this.factory,
				getListenerMethod(methodName, String.class), container);
		processAndReplyWithSendTo(listener, "replyDestination", true);
		assertListenerMethodInvocation(this.sample, methodName);
	}

	@Test
	void processFromQueueAndReplyWithSendToTopic() throws JMSException {
		String methodName = "processAndReplyWithSendTo";
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setReplyPubSubDomain(true);
		MessagingMessageListenerAdapter listener = createInstance(this.factory,
				getListenerMethod(methodName, String.class), container);
		processAndReplyWithSendTo(listener, "replyDestination", true);
		assertListenerMethodInvocation(this.sample, methodName);
	}

	@Test
	void processAndReplyWithDefaultSendTo() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(String.class);
		processAndReplyWithSendTo(listener, "defaultReply", false);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	void processAndReplyWithCustomReplyQosSettings() throws JMSException {
		String methodName = "processAndReplyWithSendTo";
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		QosSettings replyQosSettings = new QosSettings(1, 6, 6000);
		container.setReplyQosSettings(replyQosSettings);
		MessagingMessageListenerAdapter listener = createInstance(this.factory,
				getListenerMethod(methodName, String.class), container);
		processAndReplyWithSendTo(listener, "replyDestination", false, replyQosSettings);
		assertListenerMethodInvocation(this.sample, methodName);
	}

	@Test
	void processAndReplyWithNullReplyQosSettings() throws JMSException {
		String methodName = "processAndReplyWithSendTo";
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setReplyQosSettings(null);
		MessagingMessageListenerAdapter listener = createInstance(this.factory,
				getListenerMethod(methodName, String.class), container);
		processAndReplyWithSendTo(listener, "replyDestination", false);
		assertListenerMethodInvocation(this.sample, methodName);
	}

	private void processAndReplyWithSendTo(MessagingMessageListenerAdapter listener,
			String replyDestinationName, boolean pubSubDomain) throws JMSException {
		processAndReplyWithSendTo(listener, replyDestinationName, pubSubDomain, null);
	}

	private void processAndReplyWithSendTo(MessagingMessageListenerAdapter listener,
			String replyDestinationName, boolean pubSubDomain,
			QosSettings replyQosSettings) throws JMSException {
		String body = "echo text";
		String correlationId = "link-1234";
		Destination replyDestination = new Destination() {};

		DestinationResolver destinationResolver = mock();
		TextMessage reply = mock();
		QueueSender queueSender = mock();
		Session session = mock();

		given(destinationResolver.resolveDestinationName(session, replyDestinationName, pubSubDomain))
				.willReturn(replyDestination);
		given(session.createTextMessage(body)).willReturn(reply);
		given(session.createProducer(replyDestination)).willReturn(queueSender);

		listener.setDestinationResolver(destinationResolver);
		StubTextMessage inputMessage = createSimpleJmsTextMessage(body);
		inputMessage.setJMSCorrelationID(correlationId);
		listener.onMessage(inputMessage, session);

		verify(destinationResolver).resolveDestinationName(session, replyDestinationName, pubSubDomain);
		verify(reply).setJMSCorrelationID(correlationId);
		if (replyQosSettings != null) {
			verify(queueSender).send(reply, replyQosSettings.getDeliveryMode(),
					replyQosSettings.getPriority(), replyQosSettings.getTimeToLive());
		}
		else {
			verify(queueSender).send(reply);
		}
		verify(queueSender).close();
	}

	@Test
	void emptySendTo() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(String.class);

		TextMessage reply = mock();
		Session session = mock();
		given(session.createTextMessage("content")).willReturn(reply);

		assertThatExceptionOfType(ReplyFailureException.class).isThrownBy(() ->
				listener.onMessage(createSimpleJmsTextMessage("content"), session))
			.withCauseInstanceOf(InvalidDestinationException.class);
	}

	@Test
	void invalidSendTo() {
		assertThatIllegalStateException().isThrownBy(() ->
				createDefaultInstance(String.class))
			.withMessageContaining("firstDestination")
			.withMessageContaining("secondDestination");
	}

	@Test
	void validatePayloadValid() throws JMSException {
		String methodName = "validatePayload";

		DefaultMessageHandlerMethodFactory customFactory = new DefaultMessageHandlerMethodFactory();
		customFactory.setValidator(testValidator("invalid value"));
		initializeFactory(customFactory);

		Method method = getListenerMethod(methodName, String.class);
		MessagingMessageListenerAdapter listener = createInstance(customFactory, method);
		Session session = mock();
		listener.onMessage(createSimpleJmsTextMessage("test"), session); // test is a valid value
		assertListenerMethodInvocation(this.sample, methodName);
	}

	@Test
	void validatePayloadInvalid() throws JMSException {
		DefaultMessageHandlerMethodFactory customFactory = new DefaultMessageHandlerMethodFactory();
		customFactory.setValidator(testValidator("invalid value"));

		Method method = getListenerMethod("validatePayload", String.class);
		MessagingMessageListenerAdapter listener = createInstance(customFactory, method);
		Session session = mock();

		// test is an invalid value
		assertThatExceptionOfType(ListenerExecutionFailedException.class).isThrownBy(() ->
				listener.onMessage(createSimpleJmsTextMessage("invalid value"), session));

	}

	// failure scenario

	@Test
	void invalidPayloadType() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(Integer.class);
		Session session = mock();

		// test is not a valid integer
		assertThatExceptionOfType(ListenerExecutionFailedException.class).isThrownBy(() ->
				listener.onMessage(createSimpleJmsTextMessage("test"), session))
			.withCauseInstanceOf(MessageConversionException.class)
			.withMessageContaining(getDefaultListenerMethod(Integer.class).toGenericString()); // ref to method
	}

	@Test
	void invalidMessagePayloadType() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(Message.class);
		Session session = mock();

		// Message<String> as Message<Integer>
		assertThatExceptionOfType(ListenerExecutionFailedException.class).isThrownBy(() ->
				listener.onMessage(createSimpleJmsTextMessage("test"), session))
			.withCauseInstanceOf(MessageConversionException.class);
	}


	private MessagingMessageListenerAdapter createInstance(
			DefaultMessageHandlerMethodFactory factory, Method method, MessageListenerContainer container) {

		MethodJmsListenerEndpoint endpoint = new MethodJmsListenerEndpoint();
		endpoint.setBean(this.sample);
		endpoint.setMethod(method);
		endpoint.setMessageHandlerMethodFactory(factory);
		return endpoint.createMessageListener(container);
	}

	private MessagingMessageListenerAdapter createInstance(DefaultMessageHandlerMethodFactory factory, Method method) {
		return createInstance(factory, method, new SimpleMessageListenerContainer());
	}

	private MessagingMessageListenerAdapter createDefaultInstance(Class<?>... parameterTypes) {
		return createInstance(this.factory, getDefaultListenerMethod(parameterTypes));
	}

	private StubTextMessage createSimpleJmsTextMessage(String body) {
		return new StubTextMessage(body);
	}

	private Method getListenerMethod(String methodName, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(JmsEndpointSampleBean.class, methodName, parameterTypes);
		assertThat(("no method found with name " + methodName + " and parameters " + Arrays.toString(parameterTypes))).isNotNull();
		return method;
	}

	private Method getDefaultListenerMethod(Class<?>... parameterTypes) {
		return getListenerMethod(this.methodName, parameterTypes);
	}

	private void assertDefaultListenerMethodInvocation() {
		assertListenerMethodInvocation(this.sample, this.methodName);
	}

	private void assertListenerMethodInvocation(JmsEndpointSampleBean bean, String methodName) {
		assertThat((boolean) bean.invocations.get(methodName)).as("Method " + methodName + " should have been invoked").isTrue();
	}

	private void initializeFactory(DefaultMessageHandlerMethodFactory factory) {
		factory.setBeanFactory(new StaticListableBeanFactory());
		factory.afterPropertiesSet();
	}

	private Validator testValidator(final String invalidValue) {
		return new Validator() {
			@Override
			public boolean supports(Class<?> clazz) {
				return String.class.isAssignableFrom(clazz);
			}
			@Override
			public void validate(@Nullable Object target, Errors errors) {
				String value = (String) target;
				if (invalidValue.equals(value)) {
					errors.reject("not a valid value");
				}
			}
		};
	}

	private Method getTestMethod() {
		return ReflectionUtils.findMethod(MethodJmsListenerEndpointTests.class, this.methodName);
	}


	@SendTo("defaultReply") @SuppressWarnings("unused")
	static class JmsEndpointSampleBean {

		private final Map<String, Boolean> invocations = new HashMap<>();

		public void resolveMessageAndSession(jakarta.jms.Message message, Session session) {
			this.invocations.put("resolveMessageAndSession", true);
			assertThat(message).as("Message not injected").isNotNull();
			assertThat(session).as("Session not injected").isNotNull();
		}

		public void resolveGenericMessage(Message<String> message) {
			this.invocations.put("resolveGenericMessage", true);
			assertThat(message).as("Generic message not injected").isNotNull();
			assertThat(message.getPayload()).as("Wrong message payload").isEqualTo("test");
		}

		public void resolveHeaderAndPayload(@Payload String content, @Header int myCounter) {
			this.invocations.put("resolveHeaderAndPayload", true);
			assertThat(content).as("Wrong @Payload resolution").isEqualTo("my payload");
			assertThat(myCounter).as("Wrong @Header resolution").isEqualTo(55);
		}

		public void resolveCustomHeaderNameAndPayload(@Payload String content, @Header("myCounter") int counter) {
			this.invocations.put("resolveCustomHeaderNameAndPayload", true);
			assertThat(content).as("Wrong @Payload resolution").isEqualTo("my payload");
			assertThat(counter).as("Wrong @Header resolution").isEqualTo(24);
		}

		public void resolveCustomHeaderNameAndPayloadWithHeaderNameSet(@Payload String content, @Header(name = "myCounter") int counter) {
			this.invocations.put("resolveCustomHeaderNameAndPayloadWithHeaderNameSet", true);
			assertThat(content).as("Wrong @Payload resolution").isEqualTo("my payload");
			assertThat(counter).as("Wrong @Header resolution").isEqualTo(24);
		}

		public void resolveHeaders(String content, @Headers Map<String, Object> headers) {
			this.invocations.put("resolveHeaders", true);
			assertThat(content).as("Wrong payload resolution").isEqualTo("my payload");
			assertThat(headers).as("headers not injected").isNotNull();
			assertThat(headers.get(JmsHeaders.MESSAGE_ID)).as("Missing JMS message id header").isEqualTo("abcd-1234");
			assertThat(headers.get("customInt")).as("Missing custom header").isEqualTo(1234);
		}

		public void resolveMessageHeaders(MessageHeaders headers) {
			this.invocations.put("resolveMessageHeaders", true);
			assertThat(headers).as("MessageHeaders not injected").isNotNull();
			assertThat(headers.get(JmsHeaders.TYPE)).as("Missing JMS message type header").isEqualTo("myMessageType");
			assertThat((long) headers.get("customLong")).as("Missing custom header").isEqualTo(4567);
		}

		public void resolveJmsMessageHeaderAccessor(JmsMessageHeaderAccessor headers) {
			this.invocations.put("resolveJmsMessageHeaderAccessor", true);
			assertThat(headers).as("MessageHeaders not injected").isNotNull();
			assertThat(headers.getPriority()).as("Missing JMS message priority header").isEqualTo(Integer.valueOf(9));
			assertThat(headers.getHeader("customBoolean")).as("Missing custom header").asInstanceOf(BOOLEAN).isTrue();
		}

		public void resolveObjectPayload(MyBean bean) {
			this.invocations.put("resolveObjectPayload", true);
			assertThat(bean).as("Object payload not injected").isNotNull();
			assertThat(bean.name).as("Wrong content for payload").isEqualTo("myBean name");
		}

		public void resolveConvertedPayload(Integer counter) {
			this.invocations.put("resolveConvertedPayload", true);
			assertThat(counter).as("Payload not injected").isNotNull();
			assertThat(counter).as("Wrong content for payload").isEqualTo(Integer.valueOf(33));
		}

		public String processAndReply(@Payload String content) {
			this.invocations.put("processAndReply", true);
			return content;
		}

		@SendTo("replyDestination")
		public String processAndReplyWithSendTo(String content) {
			this.invocations.put("processAndReplyWithSendTo", true);
			return content;
		}

		public String processAndReplyWithDefaultSendTo(String content) {
			this.invocations.put("processAndReplyWithDefaultSendTo", true);
			return content;
		}

		@SendTo("")
		public String emptySendTo(String content) {
			this.invocations.put("emptySendTo", true);
			return content;
		}

		@SendTo({"firstDestination", "secondDestination"})
		public String invalidSendTo(String content) {
			this.invocations.put("invalidSendTo", true);
			return content;
		}

		public void validatePayload(@Validated String payload) {
			this.invocations.put("validatePayload", true);
		}

		public void invalidPayloadType(@Payload Integer payload) {
			throw new IllegalStateException("Should never be called.");
		}

		public void invalidMessagePayloadType(Message<Integer> message) {
			throw new IllegalStateException("Should never be called.");
		}

	}


	@SuppressWarnings("serial")
	static class MyBean implements Serializable {

		private String name;
	}

}
