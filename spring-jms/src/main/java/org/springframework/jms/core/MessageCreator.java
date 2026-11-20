/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.core;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;

/**
 * Creates a JMS message given a {@link Session}.
 *
 * <p>The {@code Session} typically is provided by an instance
 * of the {@link JmsTemplate} class.
 *
 * <p>Implementations <i>do not</i> need to concern themselves with
 * checked {@code JMSExceptions} (from the '{@code jakarta.jms}'
 * package) that may be thrown from operations they attempt. The
 * {@code JmsTemplate} will catch and handle these
 * {@code JMSExceptions} appropriately.
 *
 * @author Mark Pollack
 * @since 1.1
 */
@FunctionalInterface
public interface MessageCreator {

	/**
	 * Create a {@link Message} to be sent.
	 * @param session the JMS {@link Session} to be used to create the
	 * {@code Message} (never {@code null})
	 * @return the {@code Message} to be sent
	 * @throws jakarta.jms.JMSException if thrown by JMS API methods
	 */
	Message createMessage(Session session) throws JMSException;

}
