/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.jms.config;

import javax.jms.MessageListener;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.jms.listener.endpoint.JmsActivationSpecConfig;
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Stephane Nicoll
 */
public class JmsListenerEndpointTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


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
		assertEquals("myQueue", container.getDestinationName());
		assertEquals("foo = 'bar'", container.getMessageSelector());
		assertEquals("mySubscription", container.getSubscriptionName());
		assertEquals(5, container.getConcurrentConsumers());
		assertEquals(10, container.getMaxConcurrentConsumers());
		assertEquals(messageListener, container.getMessageListener());
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
		assertEquals("myQueue", config.getDestinationName());
		assertEquals("foo = 'bar'", config.getMessageSelector());
		assertEquals("mySubscription", config.getSubscriptionName());
		assertEquals(10, config.getMaxConcurrency());
		assertEquals(messageListener, container.getMessageListener());
	}

	@Test
	public void setupConcurrencySimpleContainer() {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		MessageListener messageListener = new MessageListenerAdapter();
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setConcurrency("5-10"); // simple implementation only support max value
		endpoint.setMessageListener(messageListener);

		endpoint.setupListenerContainer(container);
		assertEquals(10, new DirectFieldAccessor(container).getPropertyValue("concurrentConsumers"));
	}

	@Test
	public void setupMessageContainerNoListener() {
		DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();

		thrown.expect(IllegalStateException.class);
		endpoint.setupListenerContainer(container);
	}

	@Test
	public void setupMessageContainerUnsupportedContainer() {
		MessageListenerContainer container = mock(MessageListenerContainer.class);
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setMessageListener(new MessageListenerAdapter());

		thrown.expect(IllegalArgumentException.class);
		endpoint.setupListenerContainer(container);
	}

}
