/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.observability.jms.publish;

import io.micrometer.observation.ObservationRegistry;
import jakarta.jms.ConnectionFactory;

import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;

public class JmsTemplatePublish {

	private final JmsTemplate jmsTemplate;

	private final JmsMessagingTemplate jmsMessagingTemplate;

	public JmsTemplatePublish(ObservationRegistry observationRegistry, ConnectionFactory connectionFactory) {
		this.jmsTemplate = new JmsTemplate(connectionFactory);
		// configure the observation registry
		this.jmsTemplate.setObservationRegistry(observationRegistry);

		// For JmsMessagingTemplate, instantiate it with a JMS template that has a configured registry
		this.jmsMessagingTemplate = new JmsMessagingTemplate(this.jmsTemplate);
	}

	public void sendMessages() {
		this.jmsTemplate.convertAndSend("spring.observation.test", "test message");
	}

}
