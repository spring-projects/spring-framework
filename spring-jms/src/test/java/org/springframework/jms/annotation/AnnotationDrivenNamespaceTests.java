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

package org.springframework.jms.annotation;

import jakarta.jms.MessageListener;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.listener.adapter.ListenerExecutionFailedException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class AnnotationDrivenNamespaceTests extends AbstractJmsAnnotationDrivenTests {

	@Override
	@Test
	void sampleConfiguration() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"annotation-driven-sample-config.xml", getClass());
		testSampleConfiguration(context);
	}

	@Override
	@Test
	void fullConfiguration() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"annotation-driven-full-config.xml", getClass());
		testFullConfiguration(context);
	}

	@Override
	@Test
	void fullConfigurableConfiguration() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"annotation-driven-full-configurable-config.xml", getClass());
		testFullConfiguration(context);
	}

	@Override
	@Test
	void customConfiguration() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"annotation-driven-custom-registry.xml", getClass());
		testCustomConfiguration(context);
	}

	@Override
	@Test
	void explicitContainerFactory() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"annotation-driven-custom-container-factory.xml", getClass());
		testExplicitContainerFactoryConfiguration(context);
	}

	@Override
	@Test
	void defaultContainerFactory() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"annotation-driven-default-container-factory.xml", getClass());
		testDefaultContainerFactoryConfiguration(context);
	}

	@Override
	@Test
	void jmsHandlerMethodFactoryConfiguration() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"annotation-driven-custom-handler-method-factory.xml", getClass());

		assertThatExceptionOfType(ListenerExecutionFailedException.class).isThrownBy(() ->
				testJmsHandlerMethodFactoryConfiguration(context))
			.withCauseInstanceOf(MethodArgumentNotValidException.class);
	}

	@Override
	@Test
	void jmsListenerIsRepeatable() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"annotation-driven-jms-listener-repeatable.xml", getClass());
		testJmsListenerRepeatable(context);
	}

	@Override
	@Test
	void jmsListeners() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"annotation-driven-jms-listeners.xml", getClass());
		testJmsListenerRepeatable(context);
	}


	static class CustomJmsListenerConfigurer implements JmsListenerConfigurer {

		private MessageListener messageListener;

		@Override
		public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
			SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
			endpoint.setId("myCustomEndpointId");
			endpoint.setDestination("myQueue");
			endpoint.setMessageListener(messageListener);
			registrar.registerEndpoint(endpoint);
		}

		public void setMessageListener(MessageListener messageListener) {
			this.messageListener = messageListener;
		}
	}

}
