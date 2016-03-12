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

package org.springframework.jms.config;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.QueueSender;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;

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
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

/**
 * @author Stephane Nicoll
 */
public class MethodJmsListenerEndpointTests {

	private final DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();

	private final DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();

	private final JmsEndpointSampleBean sample = new JmsEndpointSampleBean();


	@Rule
	public final TestName name = new TestName();

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	@Before
	public void setup() {
		initializeFactory(factory);
	}


	@Test
	public void createMessageListenerNoFactory() {
		MethodJmsListenerEndpoint endpoint = new MethodJmsListenerEndpoint();
		endpoint.setBean(this);
		endpoint.setMethod(getTestMethod());

		thrown.expect(IllegalStateException.class);
		endpoint.createMessageListener(container);
	}

	@Test
	public void createMessageListener() {
		MethodJmsListenerEndpoint endpoint = new MethodJmsListenerEndpoint();
		endpoint.setBean(this);
		endpoint.setMethod(getTestMethod());
		endpoint.setMessageHandlerMethodFactory(factory);

		assertNotNull(endpoint.createMessageListener(container));
	}

	@Test
	public void setExtraCollaborators() {
		MessageConverter messageConverter = mock(MessageConverter.class);
		DestinationResolver destinationResolver = mock(DestinationResolver.class);
		this.container.setMessageConverter(messageConverter);
		this.container.setDestinationResolver(destinationResolver);

		MessagingMessageListenerAdapter listener = createInstance(this.factory,
				getListenerMethod("resolveObjectPayload", MyBean.class), container);
		DirectFieldAccessor accessor = new DirectFieldAccessor(listener);
		assertSame(messageConverter, accessor.getPropertyValue("messageConverter"));
		assertSame(destinationResolver, accessor.getPropertyValue("destinationResolver"));
	}

	@Test
	public void resolveMessageAndSession() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(javax.jms.Message.class, Session.class);

		Session session = mock(Session.class);
		listener.onMessage(createSimpleJmsTextMessage("test"), session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	public void resolveGenericMessage() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(Message.class);

		Session session = mock(Session.class);
		listener.onMessage(createSimpleJmsTextMessage("test"), session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	public void resolveHeaderAndPayload() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(String.class, int.class);

		Session session = mock(Session.class);
		StubTextMessage message = createSimpleJmsTextMessage("my payload");
		message.setIntProperty("myCounter", 55);
		listener.onMessage(message, session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	public void resolveCustomHeaderNameAndPayload() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(String.class, int.class);

		Session session = mock(Session.class);
		StubTextMessage message = createSimpleJmsTextMessage("my payload");
		message.setIntProperty("myCounter", 24);
		listener.onMessage(message, session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	public void resolveCustomHeaderNameAndPayloadWithHeaderNameSet() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(String.class, int.class);

		Session session = mock(Session.class);
		StubTextMessage message = createSimpleJmsTextMessage("my payload");
		message.setIntProperty("myCounter", 24);
		listener.onMessage(message, session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	public void resolveHeaders() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(String.class, Map.class);

		Session session = mock(Session.class);
		StubTextMessage message = createSimpleJmsTextMessage("my payload");
		message.setIntProperty("customInt", 1234);
		message.setJMSMessageID("abcd-1234");
		listener.onMessage(message, session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	public void resolveMessageHeaders() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(MessageHeaders.class);

		Session session = mock(Session.class);
		StubTextMessage message = createSimpleJmsTextMessage("my payload");
		message.setLongProperty("customLong", 4567L);
		message.setJMSType("myMessageType");
		listener.onMessage(message, session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	public void resolveJmsMessageHeaderAccessor() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(JmsMessageHeaderAccessor.class);

		Session session = mock(Session.class);
		StubTextMessage message = createSimpleJmsTextMessage("my payload");
		message.setBooleanProperty("customBoolean", true);
		message.setJMSPriority(9);
		listener.onMessage(message, session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	public void resolveObjectPayload() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(MyBean.class);
		MyBean myBean = new MyBean();
		myBean.name = "myBean name";

		Session session = mock(Session.class);
		ObjectMessage message = mock(ObjectMessage.class);
		given(message.getObject()).willReturn(myBean);

		listener.onMessage(message, session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	public void resolveConvertedPayload() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(Integer.class);

		Session session = mock(Session.class);

		listener.onMessage(createSimpleJmsTextMessage("33"), session);
		assertDefaultListenerMethodInvocation();
	}

	@Test
	public void processAndReply() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(String.class);
		String body = "echo text";
		String correlationId = "link-1234";
		Destination replyDestination = new Destination() {};

		TextMessage reply = mock(TextMessage.class);
		QueueSender queueSender = mock(QueueSender.class);
		Session session = mock(Session.class);
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
	public void processAndReplyWithSendToQueue() throws JMSException {
		String methodName = "processAndReplyWithSendTo";
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		MessagingMessageListenerAdapter listener = createInstance(this.factory,
				getListenerMethod(methodName, String.class), container);
		processAndReplyWithSendTo(listener, "replyDestination", false);
		assertListenerMethodInvocation(sample, methodName);
	}

	@Test
	public void processFromTopicAndReplyWithSendToQueue() throws JMSException {
		String methodName = "processAndReplyWithSendTo";
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setPubSubDomain(true);
		container.setReplyPubSubDomain(false);
		MessagingMessageListenerAdapter listener = createInstance(this.factory,
				getListenerMethod(methodName, String.class), container);
		processAndReplyWithSendTo(listener, "replyDestination", false);
		assertListenerMethodInvocation(sample, methodName);
	}

	@Test
	public void processAndReplyWithSendToTopic() throws JMSException {
		String methodName = "processAndReplyWithSendTo";
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setPubSubDomain(true);
		MessagingMessageListenerAdapter listener = createInstance(this.factory,
				getListenerMethod(methodName, String.class), container);
		processAndReplyWithSendTo(listener, "replyDestination", true);
		assertListenerMethodInvocation(sample, methodName);
	}

	@Test
	public void processFromQueueAndReplyWithSendToTopic() throws JMSException {
		String methodName = "processAndReplyWithSendTo";
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setReplyPubSubDomain(true);
		MessagingMessageListenerAdapter listener = createInstance(this.factory,
				getListenerMethod(methodName, String.class), container);
		processAndReplyWithSendTo(listener, "replyDestination", true);
		assertListenerMethodInvocation(sample, methodName);
	}

	@Test
	public void processAndReplyWithDefaultSendTo() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(String.class);
		processAndReplyWithSendTo(listener, "defaultReply", false);
		assertDefaultListenerMethodInvocation();
	}

	private void processAndReplyWithSendTo(MessagingMessageListenerAdapter listener,
			String replyDestinationName, boolean pubSubDomain) throws JMSException {
		String body = "echo text";
		String correlationId = "link-1234";
		Destination replyDestination = new Destination() {};

		DestinationResolver destinationResolver = mock(DestinationResolver.class);
		TextMessage reply = mock(TextMessage.class);
		QueueSender queueSender = mock(QueueSender.class);
		Session session = mock(Session.class);

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
		verify(queueSender).send(reply);
		verify(queueSender).close();
	}

	@Test
	public void emptySendTo() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(String.class);

		TextMessage reply = mock(TextMessage.class);
		Session session = mock(Session.class);
		given(session.createTextMessage("content")).willReturn(reply);

		thrown.expect(ReplyFailureException.class);
		thrown.expectCause(Matchers.isA(InvalidDestinationException.class));
		listener.onMessage(createSimpleJmsTextMessage("content"), session);
	}

	@Test
	public void invalidSendTo() {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("firstDestination");
		thrown.expectMessage("secondDestination");
		createDefaultInstance(String.class);
	}

	@Test
	public void validatePayloadValid() throws JMSException {
		String methodName = "validatePayload";

		DefaultMessageHandlerMethodFactory customFactory = new DefaultMessageHandlerMethodFactory();
		customFactory.setValidator(testValidator("invalid value"));
		initializeFactory(customFactory);

		Method method = getListenerMethod(methodName, String.class);
		MessagingMessageListenerAdapter listener = createInstance(customFactory, method);
		Session session = mock(Session.class);
		listener.onMessage(createSimpleJmsTextMessage("test"), session); // test is a valid value
		assertListenerMethodInvocation(sample, methodName);
	}

	@Test
	public void validatePayloadInvalid() throws JMSException {
		DefaultMessageHandlerMethodFactory customFactory = new DefaultMessageHandlerMethodFactory();
		customFactory.setValidator(testValidator("invalid value"));

		Method method = getListenerMethod("validatePayload", String.class);
		MessagingMessageListenerAdapter listener = createInstance(customFactory, method);
		Session session = mock(Session.class);

		thrown.expect(ListenerExecutionFailedException.class);
		listener.onMessage(createSimpleJmsTextMessage("invalid value"), session); // test is an invalid value

	}

	// failure scenario

	@Test
	public void invalidPayloadType() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(Integer.class);
		Session session = mock(Session.class);

		thrown.expect(ListenerExecutionFailedException.class);
		thrown.expectCause(Matchers.isA(MessageConversionException.class));
		thrown.expectMessage(getDefaultListenerMethod(Integer.class).toGenericString()); // ref to method
		listener.onMessage(createSimpleJmsTextMessage("test"), session); // test is not a valid integer
	}

	@Test
	public void invalidMessagePayloadType() throws JMSException {
		MessagingMessageListenerAdapter listener = createDefaultInstance(Message.class);
		Session session = mock(Session.class);

		thrown.expect(ListenerExecutionFailedException.class);
		thrown.expectCause(Matchers.isA(MessageConversionException.class));
		listener.onMessage(createSimpleJmsTextMessage("test"), session);  // Message<String> as Message<Integer>
	}


	private MessagingMessageListenerAdapter createInstance(
			DefaultMessageHandlerMethodFactory factory, Method method, MessageListenerContainer container) {

		MethodJmsListenerEndpoint endpoint = new MethodJmsListenerEndpoint();
		endpoint.setBean(sample);
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
		assertNotNull("no method found with name " + methodName + " and parameters " + Arrays.toString(parameterTypes));
		return method;
	}

	private Method getDefaultListenerMethod(Class<?>... parameterTypes) {
		return getListenerMethod(name.getMethodName(), parameterTypes);
	}

	private void assertDefaultListenerMethodInvocation() {
		assertListenerMethodInvocation(sample, name.getMethodName());
	}

	private void assertListenerMethodInvocation(JmsEndpointSampleBean bean, String methodName) {
		assertTrue("Method " + methodName + " should have been invoked", bean.invocations.get(methodName));
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
			public void validate(Object target, Errors errors) {
				String value = (String) target;
				if (invalidValue.equals(value)) {
					errors.reject("not a valid value");
				}
			}
		};
	}

	private Method getTestMethod() {
		return ReflectionUtils.findMethod(MethodJmsListenerEndpointTests.class, name.getMethodName());
	}


	@SendTo("defaultReply") @SuppressWarnings("unused")
	static class JmsEndpointSampleBean {

		private final Map<String, Boolean> invocations = new HashMap<>();

		public void resolveMessageAndSession(javax.jms.Message message, Session session) {
			invocations.put("resolveMessageAndSession", true);
			assertNotNull("Message not injected", message);
			assertNotNull("Session not injected", session);
		}

		public void resolveGenericMessage(Message<String> message) {
			invocations.put("resolveGenericMessage", true);
			assertNotNull("Generic message not injected", message);
			assertEquals("Wrong message payload", "test", message.getPayload());
		}

		public void resolveHeaderAndPayload(@Payload String content, @Header int myCounter) {
			invocations.put("resolveHeaderAndPayload", true);
			assertEquals("Wrong @Payload resolution", "my payload", content);
			assertEquals("Wrong @Header resolution", 55, myCounter);
		}

		public void resolveCustomHeaderNameAndPayload(@Payload String content, @Header("myCounter") int counter) {
			invocations.put("resolveCustomHeaderNameAndPayload", true);
			assertEquals("Wrong @Payload resolution", "my payload", content);
			assertEquals("Wrong @Header resolution", 24, counter);
		}

		public void resolveCustomHeaderNameAndPayloadWithHeaderNameSet(@Payload String content, @Header(name = "myCounter") int counter) {
			invocations.put("resolveCustomHeaderNameAndPayloadWithHeaderNameSet", true);
			assertEquals("Wrong @Payload resolution", "my payload", content);
			assertEquals("Wrong @Header resolution", 24, counter);
		}

		public void resolveHeaders(String content, @Headers Map<String, Object> headers) {
			invocations.put("resolveHeaders", true);
			assertEquals("Wrong payload resolution", "my payload", content);
			assertNotNull("headers not injected", headers);
			assertEquals("Missing JMS message id header", "abcd-1234", headers.get(JmsHeaders.MESSAGE_ID));
			assertEquals("Missing custom header", 1234, headers.get("customInt"));
		}

		public void resolveMessageHeaders(MessageHeaders headers) {
			invocations.put("resolveMessageHeaders", true);
			assertNotNull("MessageHeaders not injected", headers);
			assertEquals("Missing JMS message type header", "myMessageType", headers.get(JmsHeaders.TYPE));
			assertEquals("Missing custom header", 4567L, (long) headers.get("customLong"), 0.0);
		}

		public void resolveJmsMessageHeaderAccessor(JmsMessageHeaderAccessor headers) {
			invocations.put("resolveJmsMessageHeaderAccessor", true);
			assertNotNull("MessageHeaders not injected", headers);
			assertEquals("Missing JMS message priority header", Integer.valueOf(9), headers.getPriority());
			assertEquals("Missing custom header", true, headers.getHeader("customBoolean"));
		}

		public void resolveObjectPayload(MyBean bean) {
			invocations.put("resolveObjectPayload", true);
			assertNotNull("Object payload not injected", bean);
			assertEquals("Wrong content for payload", "myBean name", bean.name);
		}

		public void resolveConvertedPayload(Integer counter) {
			invocations.put("resolveConvertedPayload", true);
			assertNotNull("Payload not injected", counter);
			assertEquals("Wrong content for payload", Integer.valueOf(33), counter);
		}

		public String processAndReply(@Payload String content) {
			invocations.put("processAndReply", true);
			return content;
		}

		@SendTo("replyDestination")
		public String processAndReplyWithSendTo(String content) {
			invocations.put("processAndReplyWithSendTo", true);
			return content;
		}

		public String processAndReplyWithDefaultSendTo(String content) {
			invocations.put("processAndReplyWithDefaultSendTo", true);
			return content;
		}

		@SendTo("")
		public String emptySendTo(String content) {
			invocations.put("emptySendTo", true);
			return content;
		}

		@SendTo({"firstDestination", "secondDestination"})
		public String invalidSendTo(String content) {
			invocations.put("invalidSendTo", true);
			return content;
		}

		public void validatePayload(@Validated String payload) {
			invocations.put("validatePayload", true);
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
