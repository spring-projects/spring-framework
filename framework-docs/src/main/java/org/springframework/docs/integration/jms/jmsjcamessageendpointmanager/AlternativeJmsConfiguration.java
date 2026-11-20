/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsjcamessageendpointmanager;

import jakarta.jms.MessageListener;
import jakarta.resource.spi.ResourceAdapter;
import org.apache.activemq.ra.ActiveMQActivationSpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager;

@Configuration
public class AlternativeJmsConfiguration {

	// tag::snippet[]
	@Bean
	JmsMessageEndpointManager jmsMessageEndpointManager(ResourceAdapter resourceAdapter,
			MessageListener myMessageListener) {

		ActiveMQActivationSpec spec = new ActiveMQActivationSpec();
		spec.setDestination("myQueue");
		spec.setDestinationType("jakarta.jms.Queue");

		JmsMessageEndpointManager endpointManager = new JmsMessageEndpointManager();
		endpointManager.setResourceAdapter(resourceAdapter);
		endpointManager.setActivationSpec(spec);
		endpointManager.setMessageListener(myMessageListener);
		return endpointManager;
	}
	// end::snippet[]
}
