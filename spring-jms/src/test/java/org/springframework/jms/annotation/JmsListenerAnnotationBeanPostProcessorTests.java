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

package org.springframework.jms.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.AbstractJmsListenerEndpoint;
import org.springframework.jms.config.JmsListenerContainerTestFactory;
import org.springframework.jms.config.JmsListenerEndpoint;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.MessageListenerTestContainer;
import org.springframework.jms.config.MethodJmsListenerEndpoint;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
public class JmsListenerAnnotationBeanPostProcessorTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	@Test
	public void simpleMessageListener() throws Exception {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				Config.class, SimpleMessageListenerTestBean.class);

		JmsListenerContainerTestFactory factory = context.getBean(JmsListenerContainerTestFactory.class);
		assertEquals("One container should have been registered", 1, factory.getListenerContainers().size());
		MessageListenerTestContainer container = factory.getListenerContainers().get(0);

		JmsListenerEndpoint endpoint = container.getEndpoint();
		assertEquals("Wrong endpoint type", MethodJmsListenerEndpoint.class, endpoint.getClass());
		MethodJmsListenerEndpoint methodEndpoint = (MethodJmsListenerEndpoint) endpoint;
		assertEquals(SimpleMessageListenerTestBean.class, methodEndpoint.getBean().getClass());
		assertEquals(SimpleMessageListenerTestBean.class.getMethod("handleIt", String.class), methodEndpoint.getMethod());
		assertEquals(SimpleMessageListenerTestBean.class.getMethod("handleIt", String.class), methodEndpoint.getMostSpecificMethod());

		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer();
		methodEndpoint.setupListenerContainer(listenerContainer);
		assertNotNull(listenerContainer.getMessageListener());

		assertTrue("Should have been started " + container, container.isStarted());
		context.close(); // Close and stop the listeners
		assertTrue("Should have been stopped " + container, container.isStopped());
	}

	@Test
	public void metaAnnotationIsDiscovered() throws Exception {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				Config.class, MetaAnnotationTestBean.class);

		try {
			JmsListenerContainerTestFactory factory = context.getBean(JmsListenerContainerTestFactory.class);
			assertEquals("one container should have been registered", 1, factory.getListenerContainers().size());

			JmsListenerEndpoint endpoint = factory.getListenerContainers().get(0).getEndpoint();
			assertEquals("Wrong endpoint type", MethodJmsListenerEndpoint.class, endpoint.getClass());
			MethodJmsListenerEndpoint methodEndpoint = (MethodJmsListenerEndpoint) endpoint;
			assertEquals(MetaAnnotationTestBean.class, methodEndpoint.getBean().getClass());
			assertEquals(MetaAnnotationTestBean.class.getMethod("handleIt", String.class), methodEndpoint.getMethod());
			assertEquals(MetaAnnotationTestBean.class.getMethod("handleIt", String.class), methodEndpoint.getMostSpecificMethod());
			assertEquals("metaTestQueue", ((AbstractJmsListenerEndpoint) endpoint).getDestination());
		}
		finally {
			context.close();
		}
	}

	@Test
	public void sendToAnnotationFoundOnProxy() throws Exception {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				Config.class, ProxyConfig.class, ProxyTestBean.class);
		try {
			JmsListenerContainerTestFactory factory = context.getBean(JmsListenerContainerTestFactory.class);
			assertEquals("one container should have been registered", 1, factory.getListenerContainers().size());

			JmsListenerEndpoint endpoint = factory.getListenerContainers().get(0).getEndpoint();
			assertEquals("Wrong endpoint type", MethodJmsListenerEndpoint.class, endpoint.getClass());
			MethodJmsListenerEndpoint methodEndpoint = (MethodJmsListenerEndpoint) endpoint;
			assertTrue(AopUtils.isJdkDynamicProxy(methodEndpoint.getBean()));
			assertTrue(methodEndpoint.getBean() instanceof SimpleService);
			assertEquals(SimpleService.class.getMethod("handleIt", String.class), methodEndpoint.getMethod());
			assertEquals(ProxyTestBean.class.getMethod("handleIt", String.class), methodEndpoint.getMostSpecificMethod());

			Method m = ReflectionUtils.findMethod(endpoint.getClass(), "getDefaultResponseDestination");
			ReflectionUtils.makeAccessible(m);
			Object destination = ReflectionUtils.invokeMethod(m, endpoint);
			assertEquals("SendTo annotation not found on proxy", "foobar", destination);
		}
		finally {
			context.close();
		}
	}

	@Test
	@SuppressWarnings("resource")
	public void invalidProxy() {
		thrown.expect(BeanCreationException.class);
		thrown.expectCause(is(instanceOf(IllegalStateException.class)));
		thrown.expectMessage("handleIt2");
		new AnnotationConfigApplicationContext(Config.class, ProxyConfig.class, InvalidProxyTestBean.class);
	}


	@Component
	static class SimpleMessageListenerTestBean {

		@JmsListener(destination = "testQueue")
		public void handleIt(String body) {
		}
	}


	@Component
	static class MetaAnnotationTestBean {

		@FooListener
		public void handleIt(String body) {
		}
	}


	@JmsListener(destination = "metaTestQueue")
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface FooListener {
	}


	@Configuration
	static class Config {

		@Bean
		public JmsListenerAnnotationBeanPostProcessor postProcessor() {
			JmsListenerAnnotationBeanPostProcessor postProcessor = new JmsListenerAnnotationBeanPostProcessor();
			postProcessor.setEndpointRegistry(jmsListenerEndpointRegistry());
			postProcessor.setContainerFactoryBeanName("testFactory");
			return postProcessor;
		}

		@Bean
		public JmsListenerEndpointRegistry jmsListenerEndpointRegistry() {
			return new JmsListenerEndpointRegistry();
		}

		@Bean
		public JmsListenerContainerTestFactory testFactory() {
			return new JmsListenerContainerTestFactory();
		}
	}


	@Configuration
	@EnableTransactionManagement
	static class ProxyConfig {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return mock(PlatformTransactionManager.class);
		}
	}


	interface SimpleService {

		void handleIt(String body);
	}


	@Component
	static class ProxyTestBean implements SimpleService {

		@Override
		@Transactional
		@JmsListener(destination = "testQueue")
		@SendTo("foobar")
		public void handleIt(String body) {
		}
	}


	@Component
	static class InvalidProxyTestBean implements SimpleService {

		@Override
		public void handleIt(String body) {
		}

		@Transactional
		@JmsListener(destination = "testQueue")
		@SendTo("foobar")
		public void handleIt2(String body) {
		}
	}

}
