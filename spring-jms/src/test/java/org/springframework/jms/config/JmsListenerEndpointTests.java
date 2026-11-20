/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.config;

import jakarta.jms.MessageListener;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.jms.listener.endpoint.JmsActivationSpecConfig;
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * @author Stephane Nicoll
 */
class JmsListenerEndpointTests {

	@Test
	void setupJmsMessageContainerFullConfig() {
		DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
		MessageListener messageListener = new MessageListenerAdapter();
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setDestination("myQueue");
		endpoint.setSelector("foo = 'bar'");
		endpoint.setSubscription("mySubscription");
		endpoint.setConcurrency("5-10");
		endpoint.setMessageListener(messageListener);

		endpoint.setupListenerContainer(container);
		assertThat(container.getDestinationName()).isEqualTo("myQueue");
		assertThat(container.getMessageSelector()).isEqualTo("foo = 'bar'");
		assertThat(container.getSubscriptionName()).isEqualTo("mySubscription");
		assertThat(container.getConcurrentConsumers()).isEqualTo(5);
		assertThat(container.getMaxConcurrentConsumers()).isEqualTo(10);
		assertThat(container.getMessageListener()).isEqualTo(messageListener);
	}

	@Test
	void setupJcaMessageContainerFullConfig() {
		JmsMessageEndpointManager container = new JmsMessageEndpointManager();
		MessageListener messageListener = new MessageListenerAdapter();
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setDestination("myQueue");
		endpoint.setSelector("foo = 'bar'");
		endpoint.setSubscription("mySubscription");
		endpoint.setConcurrency("10");
		endpoint.setMessageListener(messageListener);

		endpoint.setupListenerContainer(container);
		JmsActivationSpecConfig config = container.getActivationSpecConfig();
		assertThat(config.getDestinationName()).isEqualTo("myQueue");
		assertThat(config.getMessageSelector()).isEqualTo("foo = 'bar'");
		assertThat(config.getSubscriptionName()).isEqualTo("mySubscription");
		assertThat(config.getMaxConcurrency()).isEqualTo(10);
		assertThat(container.getMessageListener()).isEqualTo(messageListener);
	}

	@Test
	void setupConcurrencySimpleContainer() {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		MessageListener messageListener = new MessageListenerAdapter();
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setConcurrency("5-10"); // simple implementation only support max value
		endpoint.setMessageListener(messageListener);

		endpoint.setupListenerContainer(container);
		assertThat(new DirectFieldAccessor(container).getPropertyValue("concurrentConsumers")).isEqualTo(10);
	}

	@Test
	void setupMessageContainerNoListener() {
		DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();

		assertThatIllegalStateException().isThrownBy(() ->
				endpoint.setupListenerContainer(container));
	}

	@Test
	void setupMessageContainerUnsupportedContainer() {
		MessageListenerContainer container = mock();
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setMessageListener(new MessageListenerAdapter());

		assertThatIllegalArgumentException().isThrownBy(() ->
				endpoint.setupListenerContainer(container));
	}

}
