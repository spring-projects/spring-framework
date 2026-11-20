/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.config;

import jakarta.jms.MessageListener;
import org.junit.jupiter.api.Test;

import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
class SimpleJmsListenerEndpointTests {

	private final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();


	@Test
	void createListener() {
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		MessageListener messageListener = new MessageListenerAdapter();
		endpoint.setMessageListener(messageListener);
		assertThat(endpoint.createMessageListener(container)).isSameAs(messageListener);
	}

}
