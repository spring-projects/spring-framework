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

package org.springframework.jms.core;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

/**
 * Post-processes a {@link Message}. This is the JMS equivalent of the spring-messaging
 * {@link org.springframework.messaging.core.MessagePostProcessor}.
 *
 * <p>This is involved right before a {@link JmsClient} sends a message over the wire, for setting additional
 * JMS properties and headers. With {@link JmsTemplate}, the message post processor is only involved
 * in methods accepting it as an argument, to customize the outgoing message produced
 * by a {@link org.springframework.jms.support.converter.MessageConverter}.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see JmsClient.OperationSpec#send(org.springframework.messaging.Message)
 * @see JmsTemplate#convertAndSend(String, Object, MessagePostProcessor)
 * @see JmsTemplate#convertAndSend(jakarta.jms.Destination, Object, MessagePostProcessor)
 */
@FunctionalInterface
public interface MessagePostProcessor {

	/**
	 * Process the given message.
	 * <p>The returned message is typically a modified version of the original.
	 * @param message the JMS message from the MessageConverter
	 * @return a post-processed variant of the message, or simply the incoming
	 * message; never {@code null}
	 * @throws jakarta.jms.JMSException if thrown by JMS API methods
	 */
	Message postProcessMessage(Message message) throws JMSException;

}
