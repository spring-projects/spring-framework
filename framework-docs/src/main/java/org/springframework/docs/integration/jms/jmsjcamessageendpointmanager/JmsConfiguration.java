/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsjcamessageendpointmanager;


import jakarta.jms.MessageListener;
import jakarta.resource.spi.ResourceAdapter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.listener.endpoint.JmsActivationSpecConfig;
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager;

@Configuration
public class JmsConfiguration {

	// tag::snippet[]
	@Bean
	public JmsMessageEndpointManager jmsMessageEndpointManager(ResourceAdapter resourceAdapter,
			MessageListener myMessageListener) {

		JmsActivationSpecConfig specConfig = new JmsActivationSpecConfig();
		specConfig.setDestinationName("myQueue");

		JmsMessageEndpointManager endpointManager = new JmsMessageEndpointManager();
		endpointManager.setResourceAdapter(resourceAdapter);
		endpointManager.setActivationSpecConfig(specConfig);
		endpointManager.setMessageListener(myMessageListener);
		return endpointManager;
	}
	// end::snippet[]
}
