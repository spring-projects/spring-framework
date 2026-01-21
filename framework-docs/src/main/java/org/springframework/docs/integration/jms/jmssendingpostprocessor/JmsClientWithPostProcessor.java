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
