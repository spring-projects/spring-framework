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

package org.springframework.jms.listener.adapter;

import java.lang.reflect.Method;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.jms.StubTextMessage;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Stephane Nicoll
 */
public class MessagingMessageListenerAdapterTests {

	private final DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();

	private final SampleBean sample = new SampleBean();


	@Before
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

		Session session = mock(Session.class);
		given(session.createTextMessage("Response")).willReturn(new StubTextMessage("Response"));
		MessagingMessageListenerAdapter listener = getSimpleInstance("echo", Message.class);
		javax.jms.Message replyMessage = listener.buildMessage(session, result);

		verify(session).createTextMessage("Response");
		assertNotNull("reply should never be null", replyMessage);
		assertEquals("Response", ((TextMessage) replyMessage).getText());
		assertEquals("custom header not copied", "bar", replyMessage.getStringProperty("foo"));
		assertEquals("type header not copied", "msg_type", replyMessage.getJMSType());
		assertEquals("replyTo header not copied", replyTo, replyMessage.getJMSReplyTo());
	}

	@Test
	public void exceptionInListener() {
		javax.jms.Message message = new StubTextMessage("foo");
		Session session = mock(Session.class);
		MessagingMessageListenerAdapter listener = getSimpleInstance("fail", String.class);

		try {
			listener.onMessage(message, session);
			fail("Should have thrown an exception");
		}
		catch (JMSException ex) {
			fail("Should not have thrown a JMS exception");
		}
		catch (ListenerExecutionFailedException ex) {
			assertEquals(IllegalArgumentException.class, ex.getCause().getClass());
			assertEquals("Expected test exception", ex.getCause().getMessage());
		}
	}

	@Test
	public void exceptionInInvocation() {
		javax.jms.Message message = new StubTextMessage("foo");
		Session session = mock(Session.class);
		MessagingMessageListenerAdapter listener = getSimpleInstance("wrongParam", Integer.class);

		try {
			listener.onMessage(message, session);
			fail("Should have thrown an exception");
		}
		catch (JMSException ex) {
			fail("Should not have thrown a JMS exception");
		}
		catch (ListenerExecutionFailedException ex) {
			assertEquals(MessageConversionException.class, ex.getCause().getClass());
		}
	}

	protected MessagingMessageListenerAdapter getSimpleInstance(String methodName, Class... parameterTypes) {
		Method m = ReflectionUtils.findMethod(SampleBean.class, methodName, parameterTypes);
		return createInstance(m);
	}

	protected MessagingMessageListenerAdapter createInstance(Method m) {
		MessagingMessageListenerAdapter adapter = new MessagingMessageListenerAdapter();
		adapter.setHandlerMethod(factory.createInvocableHandlerMethod(sample, m));
		return adapter;
	}

	private void initializeFactory(DefaultMessageHandlerMethodFactory factory) {
		factory.setBeanFactory(new StaticListableBeanFactory());
		factory.afterPropertiesSet();
	}


	@SuppressWarnings("unused")
	private static class SampleBean {

		public Message<String> echo(Message<String> input) {
			return MessageBuilder.withPayload(input.getPayload())
					.setHeader(JmsHeaders.TYPE, "reply")
					.build();
		}

		public void fail(String input) {
			throw new IllegalArgumentException("Expected test exception");
		}

		public void wrongParam(Integer i) {
			throw new IllegalArgumentException("Should not have been called");
		}
	}

}
