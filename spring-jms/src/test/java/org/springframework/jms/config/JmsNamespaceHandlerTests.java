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

package org.springframework.jms.config;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.parsing.EmptyReaderEventListener;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.parsing.ReaderEventListener;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.Phased;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jca.endpoint.GenericMessageEndpointManager;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager;
import org.springframework.util.ErrorHandler;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Christian Dupuis
 * @author Stephane Nicoll
 */
class JmsNamespaceHandlerTests {

	private static final String DEFAULT_CONNECTION_FACTORY = "connectionFactory";

	private static final String EXPLICIT_CONNECTION_FACTORY = "testConnectionFactory";

	private ToolingTestApplicationContext context;


	@BeforeEach
	void setup() {
		this.context = new ToolingTestApplicationContext("jmsNamespaceHandlerTests.xml", getClass());
	}

	@AfterEach
	void shutdown() {
		this.context.close();
	}


	@Test
	void testBeansCreated() {
		Map<String, ?> containers = context.getBeansOfType(DefaultMessageListenerContainer.class);
		assertThat(containers).as("Context should contain 3 JMS listener containers").hasSize(3);

		containers = context.getBeansOfType(GenericMessageEndpointManager.class);
		assertThat(containers).as("Context should contain 3 JCA endpoint containers").hasSize(3);

		assertThat(context.getBeansOfType(JmsListenerContainerFactory.class))
				.as("Context should contain 3 JmsListenerContainerFactory instances").hasSize(3);
	}

	@Test
	void testContainerConfiguration() {
		Map<String, DefaultMessageListenerContainer> containers = context.getBeansOfType(DefaultMessageListenerContainer.class);
		ConnectionFactory defaultConnectionFactory = context.getBean(DEFAULT_CONNECTION_FACTORY, ConnectionFactory.class);
		ConnectionFactory explicitConnectionFactory = context.getBean(EXPLICIT_CONNECTION_FACTORY, ConnectionFactory.class);

		int defaultConnectionFactoryCount = 0;
		int explicitConnectionFactoryCount = 0;

		for (DefaultMessageListenerContainer container : containers.values()) {
			if (container.getConnectionFactory().equals(defaultConnectionFactory)) {
				defaultConnectionFactoryCount++;
			}
			else if (container.getConnectionFactory().equals(explicitConnectionFactory)) {
				explicitConnectionFactoryCount++;
			}
		}

		assertThat(defaultConnectionFactoryCount).as("1 container should have the default connectionFactory").isEqualTo(1);
		assertThat(explicitConnectionFactoryCount).as("2 containers should have the explicit connectionFactory").isEqualTo(2);
	}

	@Test
	void testJcaContainerConfiguration() {
		Map<String, JmsMessageEndpointManager> containers = context.getBeansOfType(JmsMessageEndpointManager.class);

		assertThat(containers.containsKey("listener3")).as("listener3 not found").isTrue();
		JmsMessageEndpointManager listener3 = containers.get("listener3");
		assertThat(listener3.getResourceAdapter()).as("Wrong resource adapter").isEqualTo(context.getBean("testResourceAdapter"));
		assertThat(new DirectFieldAccessor(listener3).getPropertyValue("activationSpecFactory")).as("Wrong activation spec factory").isEqualTo(context.getBean("testActivationSpecFactory"));


		Object endpointFactory = new DirectFieldAccessor(listener3).getPropertyValue("endpointFactory");
		Object messageListener = new DirectFieldAccessor(endpointFactory).getPropertyValue("messageListener");
		assertThat(messageListener.getClass()).as("Wrong message listener").isEqualTo(MessageListenerAdapter.class);
		MessageListenerAdapter adapter = (MessageListenerAdapter) messageListener;
		DirectFieldAccessor adapterFieldAccessor = new DirectFieldAccessor(adapter);
		assertThat(adapterFieldAccessor.getPropertyValue("messageConverter")).as("Message converter not set properly").isEqualTo(context.getBean("testMessageConverter"));
		assertThat(adapterFieldAccessor.getPropertyValue("delegate")).as("Wrong delegate").isEqualTo(context.getBean("testBean1"));
		assertThat(adapterFieldAccessor.getPropertyValue("defaultListenerMethod")).as("Wrong method name").isEqualTo("setName");
	}

	@Test
	void testJmsContainerFactoryConfiguration() {
		Map<String, DefaultJmsListenerContainerFactory> containers =
				context.getBeansOfType(DefaultJmsListenerContainerFactory.class);
		DefaultJmsListenerContainerFactory factory = containers.get("testJmsFactory");
		assertThat(factory).as("No factory registered with testJmsFactory id").isNotNull();

		DefaultMessageListenerContainer container =
				factory.createListenerContainer(createDummyEndpoint());
		assertThat(container.getConnectionFactory()).as("explicit connection factory not set").isEqualTo(context.getBean(EXPLICIT_CONNECTION_FACTORY));
		assertThat(container.getDestinationResolver()).as("explicit destination resolver not set").isEqualTo(context.getBean("testDestinationResolver"));
		assertThat(container.getMessageConverter()).as("explicit message converter not set").isEqualTo(context.getBean("testMessageConverter"));
		assertThat(container.isPubSubDomain()).as("Wrong pub/sub").isTrue();
		assertThat(container.isSubscriptionDurable()).as("Wrong durable flag").isTrue();
		assertThat(container.getCacheLevel()).as("wrong cache").isEqualTo(DefaultMessageListenerContainer.CACHE_CONNECTION);
		assertThat(container.getConcurrentConsumers()).as("wrong concurrency").isEqualTo(3);
		assertThat(container.getMaxConcurrentConsumers()).as("wrong concurrency").isEqualTo(5);
		assertThat(container.getMaxMessagesPerTask()).as("wrong prefetch").isEqualTo(50);
		assertThat(container.getPhase()).as("Wrong phase").isEqualTo(99);
		assertThat(new DirectFieldAccessor(container).getPropertyValue("backOff")).isSameAs(context.getBean("testBackOff"));
	}

	@Test
	void testJcaContainerFactoryConfiguration() {
		Map<String, DefaultJcaListenerContainerFactory> containers =
				context.getBeansOfType(DefaultJcaListenerContainerFactory.class);
		DefaultJcaListenerContainerFactory factory = containers.get("testJcaFactory");
		assertThat(factory).as("No factory registered with testJcaFactory id").isNotNull();

		JmsMessageEndpointManager container =
				factory.createListenerContainer(createDummyEndpoint());
		assertThat(container.getResourceAdapter()).as("explicit resource adapter not set").isEqualTo(context.getBean("testResourceAdapter"));
		assertThat(container.getActivationSpecConfig().getMessageConverter()).as("explicit message converter not set").isEqualTo(context.getBean("testMessageConverter"));
		assertThat(container.isPubSubDomain()).as("Wrong pub/sub").isTrue();
		assertThat(container.getActivationSpecConfig().getMaxConcurrency()).as("wrong concurrency").isEqualTo(5);
		assertThat(container.getActivationSpecConfig().getPrefetchSize()).as("Wrong prefetch").isEqualTo(50);
		assertThat(container.getPhase()).as("Wrong phase").isEqualTo(77);
	}

	@Test
	void testListeners() throws Exception {
		TestBean testBean1 = context.getBean("testBean1", TestBean.class);
		TestBean testBean2 = context.getBean("testBean2", TestBean.class);
		TestMessageListener testBean3 = context.getBean("testBean3", TestMessageListener.class);

		assertThat(testBean1.getName()).isNull();
		assertThat(testBean2.getName()).isNull();
		assertThat(testBean3.message).isNull();

		TextMessage message1 = mock();
		given(message1.getText()).willReturn("Test1");

		MessageListener listener1 = getListener("listener1");
		listener1.onMessage(message1);
		assertThat(testBean1.getName()).isEqualTo("Test1");

		TextMessage message2 = mock();
		given(message2.getText()).willReturn("Test2");

		MessageListener listener2 = getListener("listener2");
		listener2.onMessage(message2);
		assertThat(testBean2.getName()).isEqualTo("Test2");

		TextMessage message3 = mock();

		MessageListener listener3 = getListener(DefaultMessageListenerContainer.class.getName() + "#0");
		listener3.onMessage(message3);
		assertThat(testBean3.message).isSameAs(message3);
	}

	@Test
	void testRecoveryInterval() {
		Object testBackOff = context.getBean("testBackOff");
		BackOff backOff1 = getBackOff("listener1");
		BackOff backOff2 = getBackOff("listener2");
		long recoveryInterval3 = getRecoveryInterval(DefaultMessageListenerContainer.class.getName() + "#0");

		assertThat(backOff1).isSameAs(testBackOff);
		assertThat(backOff2).isSameAs(testBackOff);
		assertThat(recoveryInterval3).isEqualTo(DefaultMessageListenerContainer.DEFAULT_RECOVERY_INTERVAL);
	}

	@Test
	void testConcurrency() {
		// JMS
		DefaultMessageListenerContainer listener0 = this.context
				.getBean(DefaultMessageListenerContainer.class.getName() + "#0", DefaultMessageListenerContainer.class);
		DefaultMessageListenerContainer listener1 = this.context
				.getBean("listener1", DefaultMessageListenerContainer.class);
		DefaultMessageListenerContainer listener2 = this.context
				.getBean("listener2", DefaultMessageListenerContainer.class);

		assertThat(listener0.getConcurrentConsumers()).as("Wrong concurrency on listener using placeholder").isEqualTo(2);
		assertThat(listener0.getMaxConcurrentConsumers()).as("Wrong concurrency on listener using placeholder").isEqualTo(3);
		assertThat(listener1.getConcurrentConsumers()).as("Wrong concurrency on listener1").isEqualTo(3);
		assertThat(listener1.getMaxConcurrentConsumers()).as("Wrong max concurrency on listener1").isEqualTo(5);
		assertThat(listener2.getConcurrentConsumers()).as("Wrong custom concurrency on listener2").isEqualTo(5);
		assertThat(listener2.getMaxConcurrentConsumers()).as("Wrong custom max concurrency on listener2").isEqualTo(10);

		// JCA
		JmsMessageEndpointManager listener3 = this.context
				.getBean("listener3", JmsMessageEndpointManager.class);
		JmsMessageEndpointManager listener4 = this.context
				.getBean("listener4", JmsMessageEndpointManager.class);
		assertThat(listener3.getActivationSpecConfig().getMaxConcurrency()).as("Wrong concurrency on listener3").isEqualTo(5);
		assertThat(listener4.getActivationSpecConfig().getMaxConcurrency()).as("Wrong custom concurrency on listener4").isEqualTo(7);
	}

	@Test
	void testResponseDestination() {
		// JMS
		DefaultMessageListenerContainer listener1 = this.context
				.getBean("listener1", DefaultMessageListenerContainer.class);
		DefaultMessageListenerContainer listener2 = this.context
				.getBean("listener2", DefaultMessageListenerContainer.class);
		assertThat(listener1.isPubSubDomain()).as("Wrong destination type on listener1").isTrue();
		assertThat(listener2.isPubSubDomain()).as("Wrong destination type on listener2").isTrue();
		assertThat(listener1.isReplyPubSubDomain()).as("Wrong response destination type on listener1").isFalse();
		assertThat(listener2.isReplyPubSubDomain()).as("Wrong response destination type on listener2").isFalse();

		// JCA
		JmsMessageEndpointManager listener3 = this.context
				.getBean("listener3", JmsMessageEndpointManager.class);
		JmsMessageEndpointManager listener4 = this.context
				.getBean("listener4", JmsMessageEndpointManager.class);
		assertThat(listener3.isPubSubDomain()).as("Wrong destination type on listener3").isTrue();
		assertThat(listener4.isPubSubDomain()).as("Wrong destination type on listener4").isTrue();
		assertThat(listener3.isReplyPubSubDomain()).as("Wrong response destination type on listener3").isFalse();
		assertThat(listener4.isReplyPubSubDomain()).as("Wrong response destination type on listener4").isFalse();
	}

	@Test
	void testErrorHandlers() {
		ErrorHandler expected = this.context.getBean("testErrorHandler", ErrorHandler.class);
		ErrorHandler errorHandler1 = getErrorHandler("listener1");
		ErrorHandler errorHandler2 = getErrorHandler("listener2");
		ErrorHandler defaultErrorHandler = getErrorHandler(DefaultMessageListenerContainer.class.getName() + "#0");
		assertThat(errorHandler1).isSameAs(expected);
		assertThat(errorHandler2).isSameAs(expected);
		assertThat(defaultErrorHandler).isNull();
	}

	@Test
	void testPhases() {
		int phase1 = getPhase("listener1");
		int phase2 = getPhase("listener2");
		int phase3 = getPhase("listener3");
		int phase4 = getPhase("listener4");
		int defaultPhase = getPhase(DefaultMessageListenerContainer.class.getName() + "#0");
		assertThat(phase1).isEqualTo(99);
		assertThat(phase2).isEqualTo(99);
		assertThat(phase3).isEqualTo(77);
		assertThat(phase4).isEqualTo(77);
		assertThat(defaultPhase).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void testComponentRegistration() {
		assertThat(context.containsComponentDefinition("listener1")).as("Parser should have registered a component named 'listener1'").isTrue();
		assertThat(context.containsComponentDefinition("listener2")).as("Parser should have registered a component named 'listener2'").isTrue();
		assertThat(context.containsComponentDefinition("listener3")).as("Parser should have registered a component named 'listener3'").isTrue();
		assertThat(context.containsComponentDefinition(DefaultMessageListenerContainer.class.getName() + "#0")).as("Parser should have registered a component named '"
				+ DefaultMessageListenerContainer.class.getName() + "#0'").isTrue();
		assertThat(context.containsComponentDefinition(JmsMessageEndpointManager.class.getName() + "#0")).as("Parser should have registered a component named '"
				+ JmsMessageEndpointManager.class.getName() + "#0'").isTrue();
		assertThat(context.containsComponentDefinition("testJmsFactory")).as("Parser should have registered a component named 'testJmsFactory").isTrue();
		assertThat(context.containsComponentDefinition("testJcaFactory")).as("Parser should have registered a component named 'testJcaFactory").isTrue();
		assertThat(context.containsComponentDefinition("onlyJmsFactory")).as("Parser should have registered a component named 'testJcaFactory").isTrue();
	}

	@Test
	void testSourceExtraction() {
		Iterator<ComponentDefinition> iterator = context.getRegisteredComponents();
		while (iterator.hasNext()) {
			ComponentDefinition compDef = iterator.next();
			assertThat(compDef.getSource()).as("CompositeComponentDefinition '" + compDef.getName() + "' has no source attachment").isNotNull();
			validateComponentDefinition(compDef);
		}
	}


	private void validateComponentDefinition(ComponentDefinition compDef) {
		BeanDefinition[] beanDefs = compDef.getBeanDefinitions();
		for (BeanDefinition beanDef : beanDefs) {
			assertThat(beanDef.getSource()).as("BeanDefinition has no source attachment").isNotNull();
		}
	}

	private MessageListener getListener(String containerBeanName) {
		DefaultMessageListenerContainer container = this.context.getBean(containerBeanName, DefaultMessageListenerContainer.class);
		return (MessageListener) container.getMessageListener();
	}

	private ErrorHandler getErrorHandler(String containerBeanName) {
		DefaultMessageListenerContainer container = this.context.getBean(containerBeanName, DefaultMessageListenerContainer.class);
		return (ErrorHandler) new DirectFieldAccessor(container).getPropertyValue("errorHandler");
	}

	private BackOff getBackOff(String containerBeanName) {
		DefaultMessageListenerContainer container = this.context.getBean(containerBeanName, DefaultMessageListenerContainer.class);
		return (BackOff) new DirectFieldAccessor(container).getPropertyValue("backOff");
	}

	private long getRecoveryInterval(String containerBeanName) {
		BackOff backOff = getBackOff(containerBeanName);
		assertThat(backOff.getClass()).isEqualTo(FixedBackOff.class);
		return ((FixedBackOff)backOff).getInterval();
	}

	private int getPhase(String containerBeanName) {
		Object container = this.context.getBean(containerBeanName);
		if (!(container instanceof Phased)) {
			throw new IllegalStateException("Container '" + containerBeanName + "' does not implement Phased.");
		}
		return ((Phased) container).getPhase();
	}

	private JmsListenerEndpoint createDummyEndpoint() {
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setMessageListener(new MessageListenerAdapter());
		endpoint.setDestination("testQueue");
		return endpoint;
	}


	public static class TestMessageListener implements MessageListener {

		public Message message;

		@Override
		public void onMessage(Message message) {
			this.message = message;
		}
	}


	/**
	 * Internal extension that registers a {@link ReaderEventListener} to store
	 * registered {@link ComponentDefinition}s.
	 */
	private static class ToolingTestApplicationContext extends ClassPathXmlApplicationContext {

		private Set<ComponentDefinition> registeredComponents;

		public ToolingTestApplicationContext(String path, Class<?> clazz) {
			super(path, clazz);
		}

		@Override
		protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
			this.registeredComponents = new HashSet<>();
			beanDefinitionReader.setEventListener(new StoringReaderEventListener(this.registeredComponents));
			beanDefinitionReader.setSourceExtractor(new PassThroughSourceExtractor());
		}

		public boolean containsComponentDefinition(String name) {
			for (ComponentDefinition cd : this.registeredComponents) {
				if (cd instanceof CompositeComponentDefinition) {
					ComponentDefinition[] innerCds = ((CompositeComponentDefinition) cd).getNestedComponents();
					for (ComponentDefinition innerCd : innerCds) {
						if (innerCd.getName().equals(name)) {
							return true;
						}
					}
				}
				else {
					if (cd.getName().equals(name)) {
						return true;
					}
				}
			}
			return false;
		}

		public Iterator<ComponentDefinition> getRegisteredComponents() {
			return this.registeredComponents.iterator();
		}
	}


	private static class StoringReaderEventListener extends EmptyReaderEventListener {

		protected final Set<ComponentDefinition> registeredComponents;

		public StoringReaderEventListener(Set<ComponentDefinition> registeredComponents) {
			this.registeredComponents = registeredComponents;
		}

		@Override
		public void componentRegistered(ComponentDefinition componentDefinition) {
			this.registeredComponents.add(componentDefinition);
		}
	}


	static class TestErrorHandler implements ErrorHandler {

		@Override
		public void handleError(Throwable t) {
		}
	}

}
