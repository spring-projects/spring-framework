/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
