/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsjcamessageendpointmanager

import jakarta.jms.MessageListener
import jakarta.resource.spi.ResourceAdapter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.listener.endpoint.JmsActivationSpecConfig
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager

@Configuration
class JmsConfiguration {

	// tag::snippet[]
	@Bean
	fun jmsMessageEndpointManager(
		resourceAdapter: ResourceAdapter, myMessageListener: MessageListener) = JmsMessageEndpointManager().apply {
			setResourceAdapter(resourceAdapter)
			activationSpecConfig = JmsActivationSpecConfig().apply {
				destinationName = "myQueue"
			}
			messageListener = myMessageListener
		}
	// end::snippet[]
}