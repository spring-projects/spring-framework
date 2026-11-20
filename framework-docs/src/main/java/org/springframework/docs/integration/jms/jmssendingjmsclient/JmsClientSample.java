/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmssendingjmsclient;

import jakarta.jms.ConnectionFactory;

import org.springframework.jms.core.JmsClient;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class JmsClientSample {

	private final JmsClient jmsClient;

	public JmsClientSample(ConnectionFactory connectionFactory) {
		// For custom options, use JmsClient.builder(ConnectionFactory)
		this.jmsClient = JmsClient.create(connectionFactory);
	}

	public void sendWithConversion() {
		this.jmsClient.destination("myQueue")
				.withTimeToLive(1000)
				.send("myPayload");  // optionally with a headers Map next to the payload
	}

	public void sendCustomMessage() {
		Message<?> message = MessageBuilder.withPayload("myPayload").build();  // optionally with headers
		this.jmsClient.destination("myQueue")
				.withTimeToLive(1000)
				.send(message);
	}
}
