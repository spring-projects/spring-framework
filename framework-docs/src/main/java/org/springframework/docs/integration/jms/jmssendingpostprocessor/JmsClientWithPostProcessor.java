/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmssendingpostprocessor;


import jakarta.jms.ConnectionFactory;

import org.springframework.jms.core.JmsClient;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.MessageBuilder;

public class JmsClientWithPostProcessor {

	private final JmsClient jmsClient;

	public JmsClientWithPostProcessor(ConnectionFactory connectionFactory) {
		this.jmsClient = JmsClient.builder(connectionFactory)
				.messagePostProcessor(new TenantIdMessageInterceptor("42"))
				.build();
	}

	public void sendWithPostProcessor() {
		this.jmsClient.destination("myQueue")
				.withTimeToLive(1000)
				.send("myPayload");
	}

	static class TenantIdMessageInterceptor implements MessagePostProcessor {

		private final String tenantId;

		public TenantIdMessageInterceptor(String tenantId) {
			this.tenantId = tenantId;
		}

		@Override
		public Message<?> postProcessMessage(Message<?> message) {
			return MessageBuilder.fromMessage(message)
					.setHeader("tenantId", this.tenantId)
					.build();
		}
	}
}
