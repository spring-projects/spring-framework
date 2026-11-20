/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.support.converter;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.jspecify.annotations.Nullable;

/**
 * Strategy interface that specifies a converter between Java objects and JMS messages.
 *
 * <p>Check out {@link SimpleMessageConverter} for a default implementation,
 * converting between the 'standard' message payloads and JMS Message types.
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.jms.core.JmsTemplate#setMessageConverter
 * @see org.springframework.jms.listener.adapter.MessageListenerAdapter#setMessageConverter
 */
public interface MessageConverter {

	/**
	 * Convert a Java object to a JMS Message using the supplied session
	 * to create the message object.
	 * @param object the object to convert
	 * @param session the Session to use for creating a JMS Message
	 * @return the JMS Message
	 * @throws jakarta.jms.JMSException if thrown by JMS API methods
	 * @throws MessageConversionException in case of conversion failure
	 */
	Message toMessage(Object object, Session session) throws JMSException, MessageConversionException;

	/**
	 * Convert from a JMS Message to a Java object.
	 * @param message the message to convert
	 * @return the converted Java object, or {@code null} if the message has no
	 * body (for example, a {@link jakarta.jms.TextMessage} or
	 * {@link jakarta.jms.ObjectMessage} whose body was never set)
	 * @throws jakarta.jms.JMSException if thrown by JMS API methods
	 * @throws MessageConversionException in case of conversion failure
	 */
	@Nullable Object fromMessage(Message message) throws JMSException, MessageConversionException;

}
