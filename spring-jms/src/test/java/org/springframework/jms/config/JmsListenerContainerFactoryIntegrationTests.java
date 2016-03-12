/*
 * Copyright 2002-2015 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.jms.StubTextMessage;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.Header;
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

	private JmsEndpointSampleInterface listener = sample;


	@Before
	public void setup() {
		initializeFactory(factory);
	}


	@Test
	public void messageConverterUsedIfSet() throws JMSException {
		containerFactory.setMessageConverter(new UpperCaseMessageConverter());

		MethodJmsListenerEndpoint endpoint = createDefaultMethodJmsEndpoint(
				listener.getClass(), "handleIt", String.class, String.class);
		Message message = new StubTextMessage("foo-bar");
		message.setStringProperty("my-header", "my-value");

		invokeListener(endpoint, message);
		assertListenerMethodInvocation("handleIt");
	}

	@Test
	public void parameterAnnotationWithJdkProxy() throws JMSException {
		ProxyFactory pf = new ProxyFactory(sample);
		listener = (JmsEndpointSampleInterface) pf.getProxy();

		containerFactory.setMessageConverter(new UpperCaseMessageConverter());

		MethodJmsListenerEndpoint endpoint = createDefaultMethodJmsEndpoint(
				JmsEndpointSampleInterface.class, "handleIt", String.class, String.class);
		Message message = new StubTextMessage("foo-bar");
		message.setStringProperty("my-header", "my-value");

		invokeListener(endpoint, message);
		assertListenerMethodInvocation("handleIt");
	}

	@Test
	public void parameterAnnotationWithCglibProxy() throws JMSException {
		ProxyFactory pf = new ProxyFactory(sample);
		pf.setProxyTargetClass(true);
		listener = (JmsEndpointSampleBean) pf.getProxy();

		containerFactory.setMessageConverter(new UpperCaseMessageConverter());

		MethodJmsListenerEndpoint endpoint = createDefaultMethodJmsEndpoint(
				JmsEndpointSampleBean.class, "handleIt", String.class, String.class);
		Message message = new StubTextMessage("foo-bar");
		message.setStringProperty("my-header", "my-value");

		invokeListener(endpoint, message);
		assertListenerMethodInvocation("handleIt");
	}


	@SuppressWarnings("unchecked")
	private void invokeListener(JmsListenerEndpoint endpoint, Message message) throws JMSException {
		DefaultMessageListenerContainer messageListenerContainer = containerFactory.createListenerContainer(endpoint);
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

	private MethodJmsListenerEndpoint createMethodJmsEndpoint(DefaultMessageHandlerMethodFactory factory, Method method) {
		MethodJmsListenerEndpoint endpoint = new MethodJmsListenerEndpoint();
		endpoint.setBean(listener);
		endpoint.setMethod(method);
		endpoint.setMessageHandlerMethodFactory(factory);
		return endpoint;
	}

	private MethodJmsListenerEndpoint createDefaultMethodJmsEndpoint(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		return createMethodJmsEndpoint(this.factory, ReflectionUtils.findMethod(clazz, methodName, paramTypes));
	}

	private void initializeFactory(DefaultMessageHandlerMethodFactory factory) {
		factory.setBeanFactory(new StaticListableBeanFactory());
		factory.afterPropertiesSet();
	}


	interface JmsEndpointSampleInterface {

		void handleIt(@Payload String msg, @Header("my-header") String myHeader);
	}


	static class JmsEndpointSampleBean implements JmsEndpointSampleInterface {

		private final Map<String, Boolean> invocations = new HashMap<String, Boolean>();

		public void handleIt(@Payload String msg, @Header("my-header") String myHeader) {
			invocations.put("handleIt", true);
			assertEquals("Unexpected payload message", "FOO-BAR", msg);
			assertEquals("Unexpected header value", "my-value", myHeader);
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
