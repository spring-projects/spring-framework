/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsreceivingasyncmessagelisteneradapter;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.docs.integration.jms.jmsreceivingasync.ExampleListener;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;

@Configuration
public class JmsConfiguration {

	// tag::snippet[]
	@Bean
	MessageListenerAdapter messageListener(DefaultMessageDelegate messageDelegate) {
		return new MessageListenerAdapter(messageDelegate);
	}

	@Bean
	DefaultMessageListenerContainer jmsContainer(ConnectionFactory connectionFactory, Destination destination,
			ExampleListener messageListener) {

		DefaultMessageListenerContainer jmsContainer = new DefaultMessageListenerContainer();
		jmsContainer.setConnectionFactory(connectionFactory);
		jmsContainer.setDestination(destination);
		jmsContainer.setMessageListener(messageListener);
		return jmsContainer;
	}
	// end::snippet[]
}
