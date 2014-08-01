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

package org.springframework.jms.config;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.jms.StubTextMessage;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Stephane Nicoll
 */
public class JmsListenerContainerFactoryIntegrationTests {

	private final DefaultJmsListenerContainerFactory containerFactory = new DefaultJmsListenerContainerFactory();

	private final DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();

	private final JmsEndpointSampleBean sample = new JmsEndpointSampleBean();


	@Before
	public void setup() {
		initializeFactory(factory);
	}

	@Test
	public void messageConverterUsedIfSet() throws JMSException {
		containerFactory.setMessageConverter(new UpperCaseMessageConverter());

		MethodJmsListenerEndpoint endpoint = createDefaultMethodJmsEndpoint("expectFooBarUpperCase", String.class);
		Message message = new StubTextMessage("foo-bar");

		invokeListener(endpoint, message);
		assertListenerMethodInvocation("expectFooBarUpperCase");
	}

	@SuppressWarnings("unchecked")
	private void invokeListener(JmsListenerEndpoint endpoint, Message message) throws JMSException {
		DefaultMessageListenerContainer messageListenerContainer =
				containerFactory.createListenerContainer(endpoint);
		Object listener = messageListenerContainer.getMessageListener();
		if (listener instanceof SessionAwareMessageListener) {
			((SessionAwareMessageListener<Message>) listener).onMessage(message, mock(Session.class));
		}
		else {
			((MessageListener) listener).onMessage(message);
		}
	}

	private void assertListenerMethodInvocation(String methodName) {
		assertTrue("Method " + methodName + " should have been invoked", sample.invocations.get(methodName));
	}


	private MethodJmsListenerEndpoint createMethodJmsEndpoint(
			DefaultMessageHandlerMethodFactory factory, Method method) {
		MethodJmsListenerEndpoint endpoint = new MethodJmsListenerEndpoint();
		endpoint.setBean(sample);
		endpoint.setMethod(method);
		endpoint.setMessageHandlerMethodFactory(factory);
		return endpoint;
	}

	private MethodJmsListenerEndpoint createDefaultMethodJmsEndpoint(String methodName, Class<?>... parameterTypes) {
		return createMethodJmsEndpoint(this.factory, getListenerMethod(methodName, parameterTypes));
	}

	private Method getListenerMethod(String methodName, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(JmsEndpointSampleBean.class, methodName, parameterTypes);
		assertNotNull("no method found with name " + methodName + " and parameters " + Arrays.toString(parameterTypes));
		return method;
	}


	private void initializeFactory(DefaultMessageHandlerMethodFactory factory) {
		factory.setBeanFactory(new StaticListableBeanFactory());
		factory.afterPropertiesSet();
	}


	static class JmsEndpointSampleBean {

		private final Map<String, Boolean> invocations = new HashMap<String, Boolean>();

		public void expectFooBarUpperCase(@Payload String msg) {
			invocations.put("expectFooBarUpperCase", true);
			assertEquals("Unexpected payload message", "FOO-BAR", msg);
		}
	}


	private static class UpperCaseMessageConverter implements MessageConverter {

		@Override
		public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
			return new StubTextMessage(object.toString().toUpperCase());
		}

		@Override
		public Object fromMessage(Message message) throws JMSException, MessageConversionException {
			String content = ((TextMessage) message).getText();
			return content.toUpperCase();
		}
	}

}
