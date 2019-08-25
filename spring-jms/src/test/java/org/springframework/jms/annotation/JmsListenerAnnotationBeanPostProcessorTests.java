/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.jms.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

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
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
public class JmsListenerAnnotationBeanPostProcessorTests {

	@Test
	public void simpleMessageListener() throws Exception {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				Config.class, SimpleMessageListenerTestBean.class);

		JmsListenerContainerTestFactory factory = context.getBean(JmsListenerContainerTestFactory.class);
		assertThat(factory.getListenerContainers().size()).as("One container should have been registered").isEqualTo(1);
		MessageListenerTestContainer container = factory.getListenerContainers().get(0);

		JmsListenerEndpoint endpoint = container.getEndpoint();
		assertThat(endpoint.getClass()).as("Wrong endpoint type").isEqualTo(MethodJmsListenerEndpoint.class);
		MethodJmsListenerEndpoint methodEndpoint = (MethodJmsListenerEndpoint) endpoint;
		assertThat(methodEndpoint.getBean().getClass()).isEqualTo(SimpleMessageListenerTestBean.class);
		assertThat(methodEndpoint.getMethod()).isEqualTo(SimpleMessageListenerTestBean.class.getMethod("handleIt", String.class));
		assertThat(methodEndpoint.getMostSpecificMethod()).isEqualTo(SimpleMessageListenerTestBean.class.getMethod("handleIt", String.class));

		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer();
		methodEndpoint.setupListenerContainer(listenerContainer);
		assertThat(listenerContainer.getMessageListener()).isNotNull();

		assertThat(container.isStarted()).as("Should have been started " + container).isTrue();
		context.close(); // Close and stop the listeners
		assertThat(container.isStopped()).as("Should have been stopped " + container).isTrue();
	}

	@Test
	public void metaAnnotationIsDiscovered() throws Exception {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				Config.class, MetaAnnotationTestBean.class);

		try {
			JmsListenerContainerTestFactory factory = context.getBean(JmsListenerContainerTestFactory.class);
			assertThat(factory.getListenerContainers().size()).as("one container should have been registered").isEqualTo(1);

			JmsListenerEndpoint endpoint = factory.getListenerContainers().get(0).getEndpoint();
			assertThat(endpoint.getClass()).as("Wrong endpoint type").isEqualTo(MethodJmsListenerEndpoint.class);
			MethodJmsListenerEndpoint methodEndpoint = (MethodJmsListenerEndpoint) endpoint;
			assertThat(methodEndpoint.getBean().getClass()).isEqualTo(MetaAnnotationTestBean.class);
			assertThat(methodEndpoint.getMethod()).isEqualTo(MetaAnnotationTestBean.class.getMethod("handleIt", String.class));
			assertThat(methodEndpoint.getMostSpecificMethod()).isEqualTo(MetaAnnotationTestBean.class.getMethod("handleIt", String.class));
			assertThat(((AbstractJmsListenerEndpoint) endpoint).getDestination()).isEqualTo("metaTestQueue");
		}
		finally {
			context.close();
		}
	}

	@Test
	public void sendToAnnotationFoundOnInterfaceProxy() throws Exception {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				Config.class, ProxyConfig.class, InterfaceProxyTestBean.class);
		try {
			JmsListenerContainerTestFactory factory = context.getBean(JmsListenerContainerTestFactory.class);
			assertThat(factory.getListenerContainers().size()).as("one container should have been registered").isEqualTo(1);

			JmsListenerEndpoint endpoint = factory.getListenerContainers().get(0).getEndpoint();
			assertThat(endpoint.getClass()).as("Wrong endpoint type").isEqualTo(MethodJmsListenerEndpoint.class);
			MethodJmsListenerEndpoint methodEndpoint = (MethodJmsListenerEndpoint) endpoint;
			assertThat(AopUtils.isJdkDynamicProxy(methodEndpoint.getBean())).isTrue();
			boolean condition = methodEndpoint.getBean() instanceof SimpleService;
			assertThat(condition).isTrue();
			assertThat(methodEndpoint.getMethod()).isEqualTo(SimpleService.class.getMethod("handleIt", String.class, String.class));
			assertThat(methodEndpoint.getMostSpecificMethod()).isEqualTo(InterfaceProxyTestBean.class.getMethod("handleIt", String.class, String.class));

			Method method = ReflectionUtils.findMethod(endpoint.getClass(), "getDefaultResponseDestination");
			ReflectionUtils.makeAccessible(method);
			Object destination = ReflectionUtils.invokeMethod(method, endpoint);
			assertThat(destination).as("SendTo annotation not found on proxy").isEqualTo("foobar");
		}
		finally {
			context.close();
		}
	}

	@Test
	public void sendToAnnotationFoundOnCglibProxy() throws Exception {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				Config.class, ProxyConfig.class, ClassProxyTestBean.class);
		try {
			JmsListenerContainerTestFactory factory = context.getBean(JmsListenerContainerTestFactory.class);
			assertThat(factory.getListenerContainers().size()).as("one container should have been registered").isEqualTo(1);

			JmsListenerEndpoint endpoint = factory.getListenerContainers().get(0).getEndpoint();
			assertThat(endpoint.getClass()).as("Wrong endpoint type").isEqualTo(MethodJmsListenerEndpoint.class);
			MethodJmsListenerEndpoint methodEndpoint = (MethodJmsListenerEndpoint) endpoint;
			assertThat(AopUtils.isCglibProxy(methodEndpoint.getBean())).isTrue();
			boolean condition = methodEndpoint.getBean() instanceof ClassProxyTestBean;
			assertThat(condition).isTrue();
			assertThat(methodEndpoint.getMethod()).isEqualTo(ClassProxyTestBean.class.getMethod("handleIt", String.class, String.class));
			assertThat(methodEndpoint.getMostSpecificMethod()).isEqualTo(ClassProxyTestBean.class.getMethod("handleIt", String.class, String.class));

			Method method = ReflectionUtils.findMethod(endpoint.getClass(), "getDefaultResponseDestination");
			ReflectionUtils.makeAccessible(method);
			Object destination = ReflectionUtils.invokeMethod(method, endpoint);
			assertThat(destination).as("SendTo annotation not found on proxy").isEqualTo("foobar");
		}
		finally {
			context.close();
		}
	}

	@Test
	@SuppressWarnings("resource")
	public void invalidProxy() {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				new AnnotationConfigApplicationContext(Config.class, ProxyConfig.class, InvalidProxyTestBean.class))
			.withCauseInstanceOf(IllegalStateException.class)
			.withMessageContaining("handleIt2");
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
			postProcessor.setContainerFactoryBeanName("testFactory");
			postProcessor.setEndpointRegistry(jmsListenerEndpointRegistry());
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

		void handleIt(String value, String body);
	}


	@Component
	static class InterfaceProxyTestBean implements SimpleService {

		@Override
		@Transactional
		@JmsListener(destination = "testQueue")
		@SendTo("foobar")
		public void handleIt(@Header String value, String body) {
		}
	}


	@Component
	static class ClassProxyTestBean {

		@Transactional
		@JmsListener(destination = "testQueue")
		@SendTo("foobar")
		public void handleIt(@Header String value, String body) {
		}
	}


	@Component
	static class InvalidProxyTestBean implements SimpleService {

		@Override
		public void handleIt(String value, String body) {
		}

		@Transactional
		@JmsListener(destination = "testQueue")
		@SendTo("foobar")
		public void handleIt2(String body) {
		}
	}

}
