/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmstxparticipation

import jakarta.jms.ConnectionFactory
import jakarta.jms.Destination
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.docs.integration.jms.jmsreceivingasync.ExampleListener
import org.springframework.jms.listener.DefaultMessageListenerContainer

@Configuration
class JmsConfiguration {

	// tag::snippet[]
	@Bean
	fun jmsContainer(connectionFactory: ConnectionFactory, destination: Destination, messageListener: ExampleListener) =
		DefaultMessageListenerContainer().apply {
			setConnectionFactory(connectionFactory)
			setDestination(destination)
			setMessageListener(messageListener)
			isSessionTransacted = true
		}
	// end::snippet[]
}
