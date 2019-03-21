/*
 * Copyright 2002-2015 the original author or authors.
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

import java.lang.reflect.Method;
import javax.jms.JMSException;
import javax.jms.Session;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.context.ApplicationContext;
import org.springframework.jms.StubTextMessage;
import org.springframework.jms.config.JmsListenerContainerTestFactory;
import org.springframework.jms.config.JmsListenerEndpoint;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.MethodJmsListenerEndpoint;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessagingMessageListenerAdapter;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Stephane Nicoll
 */
public abstract class AbstractJmsAnnotationDrivenTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	@Test
	public abstract void sampleConfiguration();

	@Test
	public abstract void fullConfiguration();

	@Test
	public abstract void fullConfigurableConfiguration();

	@Test
	public abstract void customConfiguration();

	@Test
	public abstract void explicitContainerFactory();

	@Test
	public abstract void defaultContainerFactory();

	@Test
	public abstract void jmsHandlerMethodFactoryConfiguration() throws JMSException;

	@Test
	public abstract void jmsListenerIsRepeatable();

	@Test
	public abstract void jmsListeners();


	/**
	 * Test for {@link SampleBean} discovery. If a factory with the default name
	 * is set, an endpoint will use it automatically
	 */
	public void testSampleConfiguration(ApplicationContext context) {
		JmsListenerContainerTestFactory defaultFactory =
				context.getBean("jmsListenerContainerFactory", JmsListenerContainerTestFactory.class);
		JmsListenerContainerTestFactory simpleFactory =
				context.getBean("simpleFactory", JmsListenerContainerTestFactory.class);
		assertEquals(1, defaultFactory.getListenerContainers().size());
		assertEquals(1, simpleFactory.getListenerContainers().size());
	}

	/**
	 * Test for {@link FullBean} discovery. In this case, no default is set because
	 * all endpoints provide a default registry. This shows that the default factory
	 * is only retrieved if it needs to be.
	 */
	public void testFullConfiguration(ApplicationContext context) {
		JmsListenerContainerTestFactory simpleFactory =
				context.getBean("simpleFactory", JmsListenerContainerTestFactory.class);
		assertEquals(1, simpleFactory.getListenerContainers().size());
		MethodJmsListenerEndpoint endpoint = (MethodJmsListenerEndpoint)
				simpleFactory.getListenerContainers().get(0).getEndpoint();
		assertEquals("listener1", endpoint.getId());
		assertEquals("queueIn", endpoint.getDestination());
		assertEquals("mySelector", endpoint.getSelector());
		assertEquals("mySubscription", endpoint.getSubscription());
		assertEquals("1-10", endpoint.getConcurrency());

		Method m = ReflectionUtils.findMethod(endpoint.getClass(), "getDefaultResponseDestination");
		ReflectionUtils.makeAccessible(m);
		Object destination = ReflectionUtils.invokeMethod(m, endpoint);
		assertEquals("queueOut", destination);
	}

	/**
	 * Test for {@link CustomBean} and an manually endpoint registered
	 * with "myCustomEndpointId". The custom endpoint does not provide
	 * any factory so it's registered with the default one
	 */
	public void testCustomConfiguration(ApplicationContext context) {
		JmsListenerContainerTestFactory defaultFactory =
				context.getBean("jmsListenerContainerFactory", JmsListenerContainerTestFactory.class);
		JmsListenerContainerTestFactory customFactory =
				context.getBean("customFactory", JmsListenerContainerTestFactory.class);
		assertEquals(1, defaultFactory.getListenerContainers().size());
		assertEquals(1, customFactory.getListenerContainers().size());
		JmsListenerEndpoint endpoint = defaultFactory.getListenerContainers().get(0).getEndpoint();
		assertEquals("Wrong endpoint type", SimpleJmsListenerEndpoint.class, endpoint.getClass());
		assertEquals("Wrong listener set in custom endpoint", context.getBean("simpleMessageListener"),
				((SimpleJmsListenerEndpoint) endpoint).getMessageListener());

		JmsListenerEndpointRegistry customRegistry =
				context.getBean("customRegistry", JmsListenerEndpointRegistry.class);
		assertEquals("Wrong number of containers in the registry", 2,
				customRegistry.getListenerContainerIds().size());
		assertEquals("Wrong number of containers in the registry", 2,
				customRegistry.getListenerContainers().size());
		assertNotNull("Container with custom id on the annotation should be found",
				customRegistry.getListenerContainer("listenerId"));
		assertNotNull("Container created with custom id should be found",
				customRegistry.getListenerContainer("myCustomEndpointId"));
	}

	/**
	 * Test for {@link DefaultBean} that does not define the container
	 * factory to use as a default is registered with an explicit
	 * default.
	 */
	public void testExplicitContainerFactoryConfiguration(ApplicationContext context) {
		JmsListenerContainerTestFactory defaultFactory =
				context.getBean("simpleFactory", JmsListenerContainerTestFactory.class);
		assertEquals(1, defaultFactory.getListenerContainers().size());
	}

	/**
	 * Test for {@link DefaultBean} that does not define the container
	 * factory to use as a default is registered with the default name.
	 */
	public void testDefaultContainerFactoryConfiguration(ApplicationContext context) {
		JmsListenerContainerTestFactory defaultFactory =
				context.getBean("jmsListenerContainerFactory", JmsListenerContainerTestFactory.class);
		assertEquals(1, defaultFactory.getListenerContainers().size());
	}

	/**
	 * Test for {@link ValidationBean} with a validator ({@link TestValidator}) specified
	 * in a custom {@link org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory}.
	 *
	 * The test should throw a {@link org.springframework.jms.listener.adapter.ListenerExecutionFailedException}
	 */
	public void testJmsHandlerMethodFactoryConfiguration(ApplicationContext context) throws JMSException {
		JmsListenerContainerTestFactory simpleFactory =
				context.getBean("defaultFactory", JmsListenerContainerTestFactory.class);
		assertEquals(1, simpleFactory.getListenerContainers().size());
		MethodJmsListenerEndpoint endpoint = (MethodJmsListenerEndpoint)
				simpleFactory.getListenerContainers().get(0).getEndpoint();

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		endpoint.setupListenerContainer(container);
		MessagingMessageListenerAdapter listener = (MessagingMessageListenerAdapter) container.getMessageListener();
		listener.onMessage(new StubTextMessage("failValidation"), mock(Session.class));
	}

	/**
	 * Test for {@link JmsListenerRepeatableBean} and {@link JmsListenersBean} that validates that the
	 * {@code @JmsListener} annotation is repeatable and generate one specific container per annotation.
	 */
	public void testJmsListenerRepeatable(ApplicationContext context) {
		JmsListenerContainerTestFactory simpleFactory =
				context.getBean("jmsListenerContainerFactory", JmsListenerContainerTestFactory.class);
		assertEquals(2, simpleFactory.getListenerContainers().size());

		MethodJmsListenerEndpoint first = (MethodJmsListenerEndpoint)
				simpleFactory.getListenerContainer("first").getEndpoint();
		assertEquals("first", first.getId());
		assertEquals("myQueue", first.getDestination());
		assertEquals(null, first.getConcurrency());

		MethodJmsListenerEndpoint second = (MethodJmsListenerEndpoint)
				simpleFactory.getListenerContainer("second").getEndpoint();
		assertEquals("second", second.getId());
		assertEquals("anotherQueue", second.getDestination());
		assertEquals("2-10", second.getConcurrency());
	}


	@Component
	static class SampleBean {

		@JmsListener(destination = "myQueue")
		public void defaultHandle(String msg) {
		}

		@JmsListener(containerFactory = "simpleFactory", destination = "myQueue")
		public void simpleHandle(String msg) {
		}
	}


	@Component
	static class FullBean {

		@JmsListener(id = "listener1", containerFactory = "simpleFactory", destination = "queueIn",
				selector = "mySelector", subscription = "mySubscription", concurrency = "1-10")
		@SendTo("queueOut")
		public String fullHandle(String msg) {
			return "reply";
		}
	}


	@Component
	static class FullConfigurableBean {

		@JmsListener(id = "${jms.listener.id}", containerFactory = "${jms.listener.containerFactory}",
				destination = "${jms.listener.destination}", selector = "${jms.listener.selector}",
				subscription = "${jms.listener.subscription}", concurrency = "${jms.listener.concurrency}")
		@SendTo("${jms.listener.sendTo}")
		public String fullHandle(String msg) {
			return "reply";
		}
	}


	@Component
	static class CustomBean {

		@JmsListener(id = "listenerId", containerFactory = "customFactory", destination = "myQueue")
		public void customHandle(String msg) {
		}
	}


	static class DefaultBean {

		@JmsListener(destination = "myQueue")
		public void handleIt(String msg) {
		}
	}


	@Component
	static class ValidationBean {

		@JmsListener(containerFactory = "defaultFactory", destination = "myQueue")
		public void defaultHandle(@Validated String msg) {
		}
	}


	@Component
	static class JmsListenerRepeatableBean {

		@JmsListener(id = "first", destination = "myQueue")
		@JmsListener(id = "second", destination = "anotherQueue", concurrency = "2-10")
		public void repeatableHandle(String msg) {
		}
	}


	@Component
	static class JmsListenersBean {

		@JmsListeners({
				@JmsListener(id = "first", destination = "myQueue"),
				@JmsListener(id = "second", destination = "anotherQueue", concurrency = "2-10")
		})
		public void repeatableHandle(String msg) {
		}
	}


	static class TestValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return String.class.isAssignableFrom(clazz);
		}

		@Override
		public void validate(Object target, Errors errors) {
			String value = (String) target;
			if ("failValidation".equals(value)) {
				errors.reject("TEST: expected invalid value");
			}
		}
	}

}
