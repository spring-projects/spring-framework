/*
 * Copyright 2002-present the original author or authors.
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
