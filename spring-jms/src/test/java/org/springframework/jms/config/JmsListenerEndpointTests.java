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
public class JmsListenerEndpointTests {

	@Test
	public void setupJmsMessageContainerFullConfig() {
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
	public void setupJcaMessageContainerFullConfig() {
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
	public void setupConcurrencySimpleContainer() {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		MessageListener messageListener = new MessageListenerAdapter();
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setConcurrency("5-10"); // simple implementation only support max value
		endpoint.setMessageListener(messageListener);

		endpoint.setupListenerContainer(container);
		assertThat(new DirectFieldAccessor(container).getPropertyValue("concurrentConsumers")).isEqualTo(10);
	}

	@Test
	public void setupMessageContainerNoListener() {
		DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();

		assertThatIllegalStateException().isThrownBy(() ->
				endpoint.setupListenerContainer(container));
	}

	@Test
	public void setupMessageContainerUnsupportedContainer() {
		MessageListenerContainer container = mock(MessageListenerContainer.class);
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setMessageListener(new MessageListenerAdapter());

		assertThatIllegalArgumentException().isThrownBy(() ->
				endpoint.setupListenerContainer(container));
	}

}
