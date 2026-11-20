/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
