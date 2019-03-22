/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.jms.JMSException;
import javax.jms.MessageListener;

import org.hamcrest.core.Is;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.AliasFor;
import org.springframework.jms.config.JmsListenerContainerTestFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.MessageListenerTestContainer;
import org.springframework.jms.config.MethodJmsListenerEndpoint;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.listener.adapter.ListenerExecutionFailedException;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.stereotype.Component;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
public class EnableJmsTests extends AbstractJmsAnnotationDrivenTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	@Override
	@Test
	public void sampleConfiguration() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				EnableJmsSampleConfig.class, SampleBean.class);
		testSampleConfiguration(context);
	}

	@Override
	@Test
	public void fullConfiguration() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				EnableJmsFullConfig.class, FullBean.class);
		testFullConfiguration(context);
	}

	@Override
	public void fullConfigurableConfiguration() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				EnableJmsFullConfigurableConfig.class, FullConfigurableBean.class);
		testFullConfiguration(context);
	}

	@Override
	@Test
	public void customConfiguration() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				EnableJmsCustomConfig.class, CustomBean.class);
		testCustomConfiguration(context);
	}

	@Override
	@Test
	public void explicitContainerFactory() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				EnableJmsCustomContainerFactoryConfig.class, DefaultBean.class);
		testExplicitContainerFactoryConfiguration(context);
	}

	@Override
	@Test
	public void defaultContainerFactory() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				EnableJmsDefaultContainerFactoryConfig.class, DefaultBean.class);
		testDefaultContainerFactoryConfiguration(context);
	}

	@Test
	public void containerAreStartedByDefault() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				EnableJmsDefaultContainerFactoryConfig.class, DefaultBean.class);
		JmsListenerContainerTestFactory factory =
				context.getBean(JmsListenerContainerTestFactory.class);
		MessageListenerTestContainer container = factory.getListenerContainers().get(0);
		assertTrue(container.isAutoStartup());
		assertTrue(container.isStarted());
	}

	@Test
	public void containerCanBeStarterViaTheRegistry() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				EnableJmsAutoStartupFalseConfig.class, DefaultBean.class);
		JmsListenerContainerTestFactory factory =
				context.getBean(JmsListenerContainerTestFactory.class);
		MessageListenerTestContainer container = factory.getListenerContainers().get(0);
		assertFalse(container.isAutoStartup());
		assertFalse(container.isStarted());
		JmsListenerEndpointRegistry registry = context.getBean(JmsListenerEndpointRegistry.class);
		registry.start();
		assertTrue(container.isStarted());
	}

	@Override
	@Test
	public void jmsHandlerMethodFactoryConfiguration() throws JMSException {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				EnableJmsHandlerMethodFactoryConfig.class, ValidationBean.class);

		thrown.expect(ListenerExecutionFailedException.class);
		thrown.expectCause(Is.<MethodArgumentNotValidException>isA(MethodArgumentNotValidException.class));
		testJmsHandlerMethodFactoryConfiguration(context);
	}

	@Override
	@Test
	public void jmsListenerIsRepeatable() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				EnableJmsDefaultContainerFactoryConfig.class, JmsListenerRepeatableBean.class);
		testJmsListenerRepeatable(context);
	}

	@Override
	@Test
	public void jmsListeners() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				EnableJmsDefaultContainerFactoryConfig.class, JmsListenersBean.class);
		testJmsListenerRepeatable(context);
	}

	@Test
	public void composedJmsListeners() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
			EnableJmsDefaultContainerFactoryConfig.class, ComposedJmsListenersBean.class)) {
			JmsListenerContainerTestFactory simpleFactory = context.getBean("jmsListenerContainerFactory",
				JmsListenerContainerTestFactory.class);
			assertEquals(2, simpleFactory.getListenerContainers().size());

			MethodJmsListenerEndpoint first = (MethodJmsListenerEndpoint) simpleFactory.getListenerContainer(
				"first").getEndpoint();
			assertEquals("first", first.getId());
			assertEquals("orderQueue", first.getDestination());
			assertNull(first.getConcurrency());

			MethodJmsListenerEndpoint second = (MethodJmsListenerEndpoint) simpleFactory.getListenerContainer(
				"second").getEndpoint();
			assertEquals("second", second.getId());
			assertEquals("billingQueue", second.getDestination());
			assertEquals("2-10", second.getConcurrency());
		}
	}

	@Test
	@SuppressWarnings("resource")
	public void unknownFactory() {
		thrown.expect(BeanCreationException.class);
		thrown.expectMessage("customFactory");  // not found
		new AnnotationConfigApplicationContext(EnableJmsSampleConfig.class, CustomBean.class);
	}

	@Test
	public void lazyComponent() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				EnableJmsDefaultContainerFactoryConfig.class, LazyBean.class);
		JmsListenerContainerTestFactory defaultFactory =
				context.getBean("jmsListenerContainerFactory", JmsListenerContainerTestFactory.class);
		assertEquals(0, defaultFactory.getListenerContainers().size());

		context.getBean(LazyBean.class);  // trigger lazy resolution
		assertEquals(1, defaultFactory.getListenerContainers().size());
		MessageListenerTestContainer container = defaultFactory.getListenerContainers().get(0);
		assertTrue("Should have been started " + container, container.isStarted());
		context.close();  // close and stop the listeners
		assertTrue("Should have been stopped " + container, container.isStopped());
	}


	@EnableJms
	@Configuration
	static class EnableJmsSampleConfig {

		@Bean
		public JmsListenerContainerTestFactory jmsListenerContainerFactory() {
			return new JmsListenerContainerTestFactory();
		}

		@Bean
		public JmsListenerContainerTestFactory simpleFactory() {
			return new JmsListenerContainerTestFactory();
		}
	}


	@EnableJms
	@Configuration
	static class EnableJmsFullConfig {

		@Bean
		public JmsListenerContainerTestFactory simpleFactory() {
			return new JmsListenerContainerTestFactory();
		}
	}


	@EnableJms
	@Configuration
	@PropertySource("classpath:/org/springframework/jms/annotation/jms-listener.properties")
	static class EnableJmsFullConfigurableConfig {

		@Bean
		public JmsListenerContainerTestFactory simpleFactory() {
			return new JmsListenerContainerTestFactory();
		}

		@Bean
		public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}
	}


	@Configuration
	@EnableJms
	static class EnableJmsCustomConfig implements JmsListenerConfigurer {

		@Override
		public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
			registrar.setEndpointRegistry(customRegistry());

			// Also register a custom endpoint
			SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
			endpoint.setId("myCustomEndpointId");
			endpoint.setDestination("myQueue");
			endpoint.setMessageListener(simpleMessageListener());
			registrar.registerEndpoint(endpoint);
		}

		@Bean
		public JmsListenerContainerTestFactory jmsListenerContainerFactory() {
			return new JmsListenerContainerTestFactory();
		}

		@Bean
		public JmsListenerEndpointRegistry customRegistry() {
			return new JmsListenerEndpointRegistry();
		}

		@Bean
		public JmsListenerContainerTestFactory customFactory() {
			return new JmsListenerContainerTestFactory();
		}

		@Bean
		public MessageListener simpleMessageListener() {
			return new MessageListenerAdapter();
		}
	}


	@Configuration
	@EnableJms
	static class EnableJmsCustomContainerFactoryConfig implements JmsListenerConfigurer {

		@Override
		public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
			registrar.setContainerFactory(simpleFactory());
		}

		@Bean
		public JmsListenerContainerTestFactory simpleFactory() {
			return new JmsListenerContainerTestFactory();
		}
	}


	@Configuration
	@EnableJms
	static class EnableJmsDefaultContainerFactoryConfig {

		@Bean
		public JmsListenerContainerTestFactory jmsListenerContainerFactory() {
			return new JmsListenerContainerTestFactory();
		}
	}


	@Configuration
	@EnableJms
	static class EnableJmsHandlerMethodFactoryConfig implements JmsListenerConfigurer {

		@Override
		public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
			registrar.setMessageHandlerMethodFactory(customMessageHandlerMethodFactory());
		}

		@Bean
		public MessageHandlerMethodFactory customMessageHandlerMethodFactory() {
			DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
			factory.setValidator(new TestValidator());
			return factory;
		}

		@Bean
		public JmsListenerContainerTestFactory defaultFactory() {
			return new JmsListenerContainerTestFactory();
		}
	}


	@Configuration
	@EnableJms
	static class EnableJmsAutoStartupFalseConfig implements JmsListenerConfigurer {

		@Override
		public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
			registrar.setContainerFactory(simpleFactory());
		}

		@Bean
		public JmsListenerContainerTestFactory simpleFactory() {
			JmsListenerContainerTestFactory factory = new JmsListenerContainerTestFactory();
			factory.setAutoStartup(false);
			return factory;
		}
	}


	@Component
	@Lazy
	static class LazyBean {

		@JmsListener(destination = "myQueue")
		public void handle(String msg) {
		}
	}


	@JmsListener(destination = "orderQueue")
	@Retention(RetentionPolicy.RUNTIME)
	private @interface OrderQueueListener {

		@AliasFor(annotation = JmsListener.class)
		String id() default "";

		@AliasFor(annotation = JmsListener.class)
		String concurrency() default "";
	}


	@JmsListener(destination = "billingQueue")
	@Retention(RetentionPolicy.RUNTIME)
	private @interface BillingQueueListener {

		@AliasFor(annotation = JmsListener.class)
		String id() default "";

		@AliasFor(annotation = JmsListener.class)
		String concurrency() default "";
	}


	@Component
	static class ComposedJmsListenersBean {

		@OrderQueueListener(id = "first")
		@BillingQueueListener(id = "second", concurrency = "2-10")
		public void repeatableHandle(String msg) {
		}
	}

}
