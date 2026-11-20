/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsjcamessageendpointmanager

import jakarta.jms.MessageListener
import jakarta.resource.spi.ResourceAdapter
import org.apache.activemq.ra.ActiveMQActivationSpec
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager

@Configuration
class AlternativeJmsConfiguration {

	// tag::snippet[]
	@Bean
	fun jmsMessageEndpointManager(
		resourceAdapter: ResourceAdapter, myMessageListener: MessageListener) = JmsMessageEndpointManager().apply {
			setResourceAdapter(resourceAdapter)
			activationSpec = ActiveMQActivationSpec().apply {
				destination = "myQueue"
				destinationType = "jakarta.jms.Queue"
			}
			messageListener = myMessageListener
		}
	// end::snippet[]
}
