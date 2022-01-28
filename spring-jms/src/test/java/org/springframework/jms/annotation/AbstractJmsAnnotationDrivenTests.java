/*
 * Copyright 2002-2022 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.jms.StubTextMessage;
import org.springframework.jms.config.JmsListenerContainerTestFactory;
import org.springframework.jms.config.JmsListenerEndpoint;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.MethodJmsListenerEndpoint;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessagingMessageListenerAdapter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Stephane Nicoll
 */
abstract class AbstractJmsAnnotationDrivenTests {

	@Test
	abstract void sampleConfiguration();

	@Test
	abstract void fullConfiguration();

	@Test
	abstract void fullConfigurableConfiguration();

	@Test
	abstract void customConfiguration();

	@Test
	abstract void explicitContainerFactory();

	@Test
	abstract void defaultContainerFactory();

	@Test
	abstract void jmsHandlerMethodFactoryConfiguration() throws JMSException;

	@Test
	abstract void jmsListenerIsRepeatable();

	@Test
	abstract void jmsListeners();


	/**
	 * Test for {@link SampleBean} discovery. If a factory with the default name
	 * is set, an endpoint will use it automatically
	 */
	protected void testSampleConfiguration(ApplicationContext context) {
		JmsListenerContainerTestFactory defaultFactory =
				context.getBean("jmsListenerContainerFactory", JmsListenerContainerTestFactory.class);
		JmsListenerContainerTestFactory simpleFactory =
				context.getBean("simpleFactory", JmsListenerContainerTestFactory.class);
		assertThat(defaultFactory.getListenerContainers().size()).isEqualTo(1);
		assertThat(simpleFactory.getListenerContainers().size()).isEqualTo(1);
	}

	/**
	 * Test for {@link FullBean} discovery. In this case, no default is set because
	 * all endpoints provide a default registry. This shows that the default factory
	 * is only retrieved if it needs to be.
	 */
	protected void testFullConfiguration(ApplicationContext context) {
		JmsListenerContainerTestFactory simpleFactory =
				context.getBean("simpleFactory", JmsListenerContainerTestFactory.class);
		assertThat(simpleFactory.getListenerContainers().size()).isEqualTo(1);
		MethodJmsListenerEndpoint endpoint = (MethodJmsListenerEndpoint)
				simpleFactory.getListenerContainers().get(0).getEndpoint();
		assertThat(endpoint.getId()).isEqualTo("listener1");
		assertThat(endpoint.getDestination()).isEqualTo("queueIn");
		assertThat(endpoint.getSelector()).isEqualTo("mySelector");
		assertThat(endpoint.getSubscription()).isEqualTo("mySubscription");
		assertThat(endpoint.getConcurrency()).isEqualTo("1-10");

		Method m = ReflectionUtils.findMethod(endpoint.getClass(), "getDefaultResponseDestination");
		ReflectionUtils.makeAccessible(m);
		Object destination = ReflectionUtils.invokeMethod(m, endpoint);
		assertThat(destination).isEqualTo("queueOut");
	}

	/**
	 * Test for {@link CustomBean} and an manually endpoint registered
	 * with "myCustomEndpointId". The custom endpoint does not provide
	 * any factory so it's registered with the default one
	 */
	protected void testCustomConfiguration(ApplicationContext context) {
		JmsListenerContainerTestFactory defaultFactory =
				context.getBean("jmsListenerContainerFactory", JmsListenerContainerTestFactory.class);
		JmsListenerContainerTestFactory customFactory =
				context.getBean("customFactory", JmsListenerContainerTestFactory.class);
		assertThat(defaultFactory.getListenerContainers().size()).isEqualTo(1);
		assertThat(customFactory.getListenerContainers().size()).isEqualTo(1);
		JmsListenerEndpoint endpoint = defaultFactory.getListenerContainers().get(0).getEndpoint();
		assertThat(endpoint.getClass()).as("Wrong endpoint type").isEqualTo(SimpleJmsListenerEndpoint.class);
		assertThat(((SimpleJmsListenerEndpoint) endpoint).getMessageListener()).as("Wrong listener set in custom endpoint").isEqualTo(context.getBean("simpleMessageListener"));

		JmsListenerEndpointRegistry customRegistry =
				context.getBean("customRegistry", JmsListenerEndpointRegistry.class);
		assertThat(customRegistry.getListenerContainerIds().size()).as("Wrong number of containers in the registry").isEqualTo(2);
		assertThat(customRegistry.getListenerContainers().size()).as("Wrong number of containers in the registry").isEqualTo(2);
		assertThat(customRegistry.getListenerContainer("listenerId")).as("Container with custom id on the annotation should be found").isNotNull();
		assertThat(customRegistry.getListenerContainer("myCustomEndpointId")).as("Container created with custom id should be found").isNotNull();
	}

	/**
	 * Test for {@link DefaultBean} that does not define the container
	 * factory to use as a default is registered with an explicit
	 * default.
	 */
	protected void testExplicitContainerFactoryConfiguration(ApplicationContext context) {
		JmsListenerContainerTestFactory defaultFactory =
				context.getBean("simpleFactory", JmsListenerContainerTestFactory.class);
		assertThat(defaultFactory.getListenerContainers().size()).isEqualTo(1);
	}

	/**
	 * Test for {@link DefaultBean} that does not define the container
	 * factory to use as a default is registered with the default name.
	 */
	protected void testDefaultContainerFactoryConfiguration(ApplicationContext context) {
		JmsListenerContainerTestFactory defaultFactory =
				context.getBean("jmsListenerContainerFactory", JmsListenerContainerTestFactory.class);
		assertThat(defaultFactory.getListenerContainers().size()).isEqualTo(1);
	}

	/**
	 * Test for {@link ValidationBean} with a validator ({@link TestValidator}) specified
	 * in a custom {@link org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory}.
	 *
	 * The test should throw a {@link org.springframework.jms.listener.adapter.ListenerExecutionFailedException}
	 */
	protected void testJmsHandlerMethodFactoryConfiguration(ApplicationContext context) throws JMSException {
		JmsListenerContainerTestFactory simpleFactory =
				context.getBean("defaultFactory", JmsListenerContainerTestFactory.class);
		assertThat(simpleFactory.getListenerContainers().size()).isEqualTo(1);
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
	protected void testJmsListenerRepeatable(ApplicationContext context) {
		JmsListenerContainerTestFactory simpleFactory =
				context.getBean("jmsListenerContainerFactory", JmsListenerContainerTestFactory.class);
		assertThat(simpleFactory.getListenerContainers().size()).isEqualTo(2);

		MethodJmsListenerEndpoint first = (MethodJmsListenerEndpoint)
				simpleFactory.getListenerContainer("first").getEndpoint();
		assertThat(first.getId()).isEqualTo("first");
		assertThat(first.getDestination()).isEqualTo("myQueue");
		assertThat(first.getConcurrency()).isNull();

		MethodJmsListenerEndpoint second = (MethodJmsListenerEndpoint)
				simpleFactory.getListenerContainer("second").getEndpoint();
		assertThat(second.getId()).isEqualTo("second");
		assertThat(second.getDestination()).isEqualTo("anotherQueue");
		assertThat(second.getConcurrency()).isEqualTo("2-10");
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
		public void validate(@Nullable Object target, Errors errors) {
			String value = (String) target;
			if ("failValidation".equals(value)) {
				errors.reject("TEST: expected invalid value");
			}
		}
	}

}
