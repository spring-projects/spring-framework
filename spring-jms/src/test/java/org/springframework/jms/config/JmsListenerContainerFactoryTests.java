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

import io.micrometer.observation.tck.TestObservationRegistry;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageListener;
import jakarta.jms.Session;
import jakarta.transaction.TransactionManager;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.jms.StubConnectionFactory;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.jms.listener.endpoint.JmsActivationSpecConfig;
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager;
import org.springframework.jms.listener.endpoint.StubJmsActivationSpecFactory;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * @author Stephane Nicoll
 */
class JmsListenerContainerFactoryTests {

	private final ConnectionFactory connectionFactory = new StubConnectionFactory();

	private final DestinationResolver destinationResolver = new DynamicDestinationResolver();

	private final MessageConverter messageConverter = new SimpleMessageConverter();

	private final TransactionManager transactionManager = mock();


	@Test
	void createSimpleContainer() {
		SimpleJmsListenerContainerFactory factory = new SimpleJmsListenerContainerFactory();
		setDefaultJmsConfig(factory);
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();

		MessageListener messageListener = new MessageListenerAdapter();
		endpoint.setMessageListener(messageListener);
		endpoint.setDestination("myQueue");

		SimpleMessageListenerContainer container = factory.createListenerContainer(endpoint);

		assertDefaultJmsConfig(container);
		assertThat(container.getMessageListener()).isEqualTo(messageListener);
		assertThat(container.getDestinationName()).isEqualTo("myQueue");
	}

	@Test
	void createJmsContainerFullConfig() {
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		TestObservationRegistry testObservationRegistry = TestObservationRegistry.create();
		setDefaultJmsConfig(factory);
		factory.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);
		factory.setConcurrency("3-10");
		factory.setMaxMessagesPerTask(5);
		factory.setObservationRegistry(testObservationRegistry);

		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		MessageListener messageListener = new MessageListenerAdapter();
		endpoint.setMessageListener(messageListener);
		endpoint.setDestination("myQueue");
		DefaultMessageListenerContainer container = factory.createListenerContainer(endpoint);

		assertDefaultJmsConfig(container);
		assertThat(container.getCacheLevel()).isEqualTo(DefaultMessageListenerContainer.CACHE_CONSUMER);
		assertThat(container.getConcurrentConsumers()).isEqualTo(3);
		assertThat(container.getMaxConcurrentConsumers()).isEqualTo(10);
		assertThat(container.getMaxMessagesPerTask()).isEqualTo(5);
		assertThat(container.getObservationRegistry()).isEqualTo(testObservationRegistry);

		assertThat(container.getMessageListener()).isEqualTo(messageListener);
		assertThat(container.getDestinationName()).isEqualTo("myQueue");
	}

	@Test
	void createJcaContainerFullConfig() {
		DefaultJcaListenerContainerFactory factory = new DefaultJcaListenerContainerFactory();
		setDefaultJcaConfig(factory);
		factory.setConcurrency("10");

		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		MessageListener messageListener = new MessageListenerAdapter();
		endpoint.setMessageListener(messageListener);
		endpoint.setDestination("myQueue");
		JmsMessageEndpointManager container = factory.createListenerContainer(endpoint);

		assertDefaultJcaConfig(container);
		assertThat(container.getActivationSpecConfig().getMaxConcurrency()).isEqualTo(10);
		assertThat(container.getMessageListener()).isEqualTo(messageListener);
		assertThat(container.getActivationSpecConfig().getDestinationName()).isEqualTo("myQueue");
	}

	@Test
	void jcaExclusiveProperties() {
		DefaultJcaListenerContainerFactory factory = new DefaultJcaListenerContainerFactory();
		factory.setDestinationResolver(this.destinationResolver);
		factory.setActivationSpecFactory(new StubJmsActivationSpecFactory());

		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setMessageListener(new MessageListenerAdapter());
		assertThatIllegalStateException().isThrownBy(() ->
				factory.createListenerContainer(endpoint));
	}

	@Test
	void backOffOverridesRecoveryInterval() {
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		BackOff backOff = new FixedBackOff();
		factory.setBackOff(backOff);
		factory.setRecoveryInterval(2000L);

		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		MessageListener messageListener = new MessageListenerAdapter();
		endpoint.setMessageListener(messageListener);
		endpoint.setDestination("myQueue");
		DefaultMessageListenerContainer container = factory.createListenerContainer(endpoint);

		assertThat(new DirectFieldAccessor(container).getPropertyValue("backOff")).isSameAs(backOff);
	}

	@Test
	void endpointConcurrencyTakesPrecedence() {
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		factory.setConcurrency("2-10");

		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		MessageListener messageListener = new MessageListenerAdapter();
		endpoint.setMessageListener(messageListener);
		endpoint.setDestination("myQueue");
		endpoint.setConcurrency("4-6");
		DefaultMessageListenerContainer container = factory.createListenerContainer(endpoint);
		assertThat(container.getConcurrentConsumers()).isEqualTo(4);
		assertThat(container.getMaxConcurrentConsumers()).isEqualTo(6);
	}


	private void setDefaultJmsConfig(AbstractJmsListenerContainerFactory<?> factory) {
		factory.setConnectionFactory(this.connectionFactory);
		factory.setDestinationResolver(this.destinationResolver);
		factory.setMessageConverter(this.messageConverter);
		factory.setSessionTransacted(true);
		factory.setSessionAcknowledgeMode(Session.DUPS_OK_ACKNOWLEDGE);
		factory.setPubSubDomain(true);
		factory.setReplyPubSubDomain(true);
		factory.setReplyQosSettings(new QosSettings(1, 7, 5000));
		factory.setSubscriptionDurable(true);
		factory.setClientId("client-1234");
		factory.setAutoStartup(false);
	}

	private void assertDefaultJmsConfig(AbstractMessageListenerContainer container) {
		assertThat(container.getConnectionFactory()).isEqualTo(this.connectionFactory);
		assertThat(container.getDestinationResolver()).isEqualTo(this.destinationResolver);
		assertThat(container.getMessageConverter()).isEqualTo(this.messageConverter);
		assertThat(container.isSessionTransacted()).isTrue();
		assertThat(container.getSessionAcknowledgeMode()).isEqualTo(Session.DUPS_OK_ACKNOWLEDGE);
		assertThat(container.isPubSubDomain()).isTrue();
		assertThat(container.isReplyPubSubDomain()).isTrue();
		assertThat(container.getReplyQosSettings()).isEqualTo(new QosSettings(1, 7, 5000));
		assertThat(container.isSubscriptionDurable()).isTrue();
		assertThat(container.getClientId()).isEqualTo("client-1234");
		assertThat(container.isAutoStartup()).isFalse();
	}

	private void setDefaultJcaConfig(DefaultJcaListenerContainerFactory factory) {
		factory.setDestinationResolver(this.destinationResolver);
		factory.setTransactionManager(this.transactionManager);
		factory.setMessageConverter(this.messageConverter);
		factory.setAcknowledgeMode(Session.DUPS_OK_ACKNOWLEDGE);
		factory.setPubSubDomain(true);
		factory.setReplyQosSettings(new QosSettings(1, 7, 5000));
		factory.setSubscriptionDurable(true);
		factory.setClientId("client-1234");
	}

	private void assertDefaultJcaConfig(JmsMessageEndpointManager container) {
		assertThat(container.getMessageConverter()).isEqualTo(this.messageConverter);
		assertThat(container.getDestinationResolver()).isEqualTo(this.destinationResolver);
		JmsActivationSpecConfig config = container.getActivationSpecConfig();
		assertThat(config).isNotNull();
		assertThat(config.getAcknowledgeMode()).isEqualTo(Session.DUPS_OK_ACKNOWLEDGE);
		assertThat(config.isPubSubDomain()).isTrue();
		assertThat(container.getReplyQosSettings()).isEqualTo(new QosSettings(1, 7, 5000));
		assertThat(config.isSubscriptionDurable()).isTrue();
		assertThat(config.getClientId()).isEqualTo("client-1234");
	}

}
