/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmstxparticipation;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.docs.integration.jms.jmsreceivingasync.ExampleListener;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
public class ExternalTxJmsConfiguration {

	// tag::transactionManagerSnippet[]
	@Bean
	JtaTransactionManager transactionManager()  {
		return new JtaTransactionManager();
	}
	// end::transactionManagerSnippet[]

	// tag::jmsContainerSnippet[]
	@Bean
	DefaultMessageListenerContainer jmsContainer(ConnectionFactory connectionFactory, Destination destination,
			ExampleListener messageListener) {

		DefaultMessageListenerContainer jmsContainer = new DefaultMessageListenerContainer();
		jmsContainer.setConnectionFactory(connectionFactory);
		jmsContainer.setDestination(destination);
		jmsContainer.setMessageListener(messageListener);
		jmsContainer.setSessionTransacted(true);
		return jmsContainer;
	}
	// end::jmsContainerSnippet[]
}
