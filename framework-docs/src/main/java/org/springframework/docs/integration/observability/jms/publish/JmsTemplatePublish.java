/*
 * Copyright 2002-2023 the original author or authors.
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
