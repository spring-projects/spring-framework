/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsannotatedsupport

import jakarta.jms.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.support.destination.DestinationResolver

// tag::snippet[]
@Configuration
@EnableJms
class JmsConfiguration {

	@Bean
	fun jmsListenerContainerFactory(connectionFactory: ConnectionFactory, destinationResolver: DestinationResolver) =
		DefaultJmsListenerContainerFactory().apply {
			setConnectionFactory(connectionFactory)
			setDestinationResolver(destinationResolver)
			setSessionTransacted(true)
			setConcurrency("3-10")
		}
}
// end::snippet[]
