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

package org.springframework.docs.integration.jms.jmssending;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.Session;

import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.JmsTemplate;

public class JmsQueueSender {

	private JmsTemplate jmsTemplate;
	private Queue queue;

	public void setConnectionFactory(ConnectionFactory cf) {
		this.jmsTemplate = new JmsTemplate(cf);
	}

	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	public void simpleSend() {
		this.jmsTemplate.send(this.queue, new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage("hello queue world");
			}
		});
	}
}
